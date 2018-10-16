package biz.ple.corba.springext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantActivator;
import org.omg.PortableServer.ServantLocator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import biz.ple.corba.annotations.CorbaClientInterceptor;
import biz.ple.corba.annotations.CorbaDefaultServant;
import biz.ple.corba.annotations.CorbaRef;
import biz.ple.corba.annotations.CorbaServant;
import biz.ple.corba.annotations.CorbaServantActivator;
import biz.ple.corba.annotations.CorbaServantLocator;
import biz.ple.corba.annotations.CorbaServerInterceptor;
import biz.ple.corba.annotations.CorbaServiceContextManager;
import biz.ple.corba.beans.NamedReferenceLookup;
import biz.ple.corba.beans.NamingContextBean;
import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.beans.ServiceContextDefinition;
import biz.ple.corba.beans.server.NamedServantObject;
import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingClientRequestInterceptor;
import biz.ple.corba.interfaces.ServiceContextPropagatingServerRequestInterceptor;


public class CorbaBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private static class ServantMgrRec {

        ServantLocator locator;
        ServantActivator activator;
        Servant defaultServant;

        public ServantMgrRec(ServantLocator locator)
        {
            this.locator = locator;
        }

        public ServantMgrRec(ServantActivator activator)
        {
            this.activator = activator;
        }

        public ServantMgrRec(Servant defaultServant)
        {
            this.defaultServant = defaultServant;
        }

    }


    private static class ServiceDefinition {

        ServiceContextPropagatingClientRequestInterceptor cltIcp;
        ServiceContextPropagatingServerRequestInterceptor srvIcp;
        PICurrentInjectable ctxMgr;

        public ServiceDefinition(ServiceContextPropagatingClientRequestInterceptor cltIcp)
        {
            this.cltIcp = cltIcp;
        }

        public ServiceDefinition(ServiceContextPropagatingServerRequestInterceptor srvIcp)
        {
            this.srvIcp = srvIcp;
        }

        public ServiceDefinition(PICurrentInjectable ctxMgr)
        {
            this.ctxMgr = ctxMgr;
        }

    }


    private Map<String, ServantMgrRec> servantMgrs = new HashMap<>();
    private Map<String, List<ClientRequestInterceptor>> cltInterceptors = new HashMap<>();
    private Map<String, List<ServerRequestInterceptor>> srvInterceptors = new HashMap<>();
    private Map<String, Map<String, ServiceDefinition>> serviceDefs = new HashMap<>();

    private ApplicationContext appCtx;


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
        throws BeansException
    {
        // Check for ClientRequestInterceptor annotation; if present, keep bean for later assignment to OrbBean
        CorbaClientInterceptor cltIc = bean.getClass().getAnnotation(CorbaClientInterceptor.class);
        if (cltIc != null) {
            registerClientInterceptor(beanName, bean, cltIc);
        }

        // Check for ServerRequestInterceptor annotation; if present, keep bean for later assignment to OrbBean
        CorbaServerInterceptor srvIc = bean.getClass().getAnnotation(CorbaServerInterceptor.class);
        if (srvIc != null) {
            registerServerInterceptor(beanName, bean, srvIc);
        }

        // Check for ServiceContextManager annotation; if present, keep bean for later registration with OrbBean
        CorbaServiceContextManager ctxMgr = bean.getClass().getAnnotation(CorbaServiceContextManager.class);
        if (ctxMgr != null) {
            registerServiceContextManager(beanName, bean, ctxMgr);
        }

        // Check for ServantLocator annotation; if present, keep bean for later assignment to PoaBean
        boolean locator = false;
        CorbaServantLocator loc = bean.getClass().getAnnotation(CorbaServantLocator.class);
        if (loc != null) {
            locator = true;
            registerServantLocator(beanName, bean, loc);
        }

        // Check for ServantActivator annotation; if present, keep bean for later assignment to PoaBean
        boolean activator = false;
        CorbaServantActivator act = bean.getClass().getAnnotation(CorbaServantActivator.class);
        if (act != null) {
            if (locator) {
                throw new BeanDefinitionValidationException(
                    "Bean '" + beanName + " is annotated with both CorbaServantLocator and CorbaServantActivator.");
            }
            activator = true;
            registerServantActivator(beanName, bean, act);
        }

        // Check for DefaultServant annotation; if present, keep bean for later assignment to PoaBean
        CorbaDefaultServant defSrv = bean.getClass().getAnnotation(CorbaDefaultServant.class);
        if (defSrv != null) {
            if (locator) {
                throw new BeanDefinitionValidationException(
                    "Bean '" + beanName + " is annotated with both CorbaServantLocator and CorbaDefaultServant.");
            }
            if (activator) {
                throw new BeanDefinitionValidationException(
                    "Bean '" + beanName + " is annotated with both CorbaServantActivator and CorbaDefaultServant.");
            }
            registerDefaultServant(beanName, bean, defSrv);
        }

        // If the bean is an OrbBean, register services and request interceptors if necessary
        if (bean instanceof OrbBean) {
            OrbBean orbBean = (OrbBean) bean;
            Map<String, ServiceDefinition> svcDefs = serviceDefs.get(beanName);
            if (svcDefs != null) {
                for (ServiceDefinition svcDef: svcDefs.values()) {
                    orbBean.registerServiceContext(new ServiceContextDefinition(
                        svcDef.ctxMgr,
                        svcDef.cltIcp,
                        svcDef.srvIcp)
                    );
                }
            }
            List<ClientRequestInterceptor> cltIcList = cltInterceptors.get(beanName);
            if (cltIcList != null) {
                for (ClientRequestInterceptor ic: cltIcList) {
                    orbBean.registerClientInterceptor(ic);
                }
            }
            List<ServerRequestInterceptor> srvIcList = srvInterceptors.get(beanName);
            if (srvIcList != null) {
                for (ServerRequestInterceptor ic: srvIcList) {
                    orbBean.registerServerInterceptor(ic);
                }
            }
        }

        // If the bean is a PoaBean, register ServantLocator/Activator with it if necessary
        if (bean instanceof PoaBean) {
            registerServantManagers(beanName, (PoaBean) bean);
        }

        return bean;
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
        throws BeansException
    {
        CorbaServant ts = bean.getClass().getAnnotation(CorbaServant.class);
        if (ts != null) {
            return makeNamedServantObject(beanName, bean, ts);
        }
        else {
            // Check the bean fo @CorbaRef annotated members and setter methods,
            // resolve and inject references accordingly
            List<Method> corbaSetters = new LinkedList<>();
            List<Field> corbaFields = new LinkedList<>();
            if (isCorbaRefAnnotated(beanName, bean, corbaSetters, corbaFields)) {
                injectCorbaRefs(beanName, bean, corbaSetters, corbaFields);
            }
        }
        return bean;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException
    {
        this.appCtx = applicationContext;
    }


    private Object makeNamedServantObject(String beanName, Object bean, CorbaServant ts)
        throws BeansException
    {
        // Get the required POA bean
        PoaBean reqPoa = getPoaBean(ts.poa());

        // Set NamingContext and CosNaming name to null/empty (default)
        NamingContextBean ctxBean = null;
        String corbaName = null;

        // If a naming context is specified, retrieve the appropriate NamingContext bean
        if (ts.cosNamingCtx() != null && ts.cosNamingCtx().length() > 0) {
            ctxBean = appCtx.getBean(ts.cosNamingCtx().trim(), NamingContextBean.class);
            if (ctxBean == null) {
                // If the specified NamingContext bean does not exist, throw an exception
                throw new BeanDefinitionValidationException("No NamingContext bean named '" + ts.cosNamingCtx() + "' available.");
            }
            // Also get the CosNaming name for the servant from the annotation
            // If this is invalid, it will cause an exception when initializing the new NamedServantObject
            corbaName = ts.cosNamingName();
        }

        // Create an appropriate NamedServantObject with the data from the annotation; this will be the actual Spring bean
        try {
            // Note that null is a perfectly legal value for the tieClass constructor argument
            NamedServantObject namedServant = new NamedServantObject(reqPoa, ts.tieClass(), bean, ctxBean, corbaName);
            if (ts.userObjectId().trim().isEmpty()) {
                namedServant.setUserObjectId(ts.userObjectId().trim().getBytes());
            }
            // Set the user-defined object id, if present
            if (!ts.userObjectId().trim().isEmpty()) {
                namedServant.setUserObjectId(ts.userObjectId().trim().getBytes());
            }
            // Immediately initialize the newly created bean
            namedServant.corbaInit();
            return namedServant;
        }
        catch (Exception xcp) {
            // If anything goes wrong, we throw an exception (packaging the original exception)
            throw new BeanInitializationException("Could not register servant '" + beanName + "' with POA/ORB.", xcp);
        }
    }


    private boolean isCorbaRefAnnotated(String beanName, Object bean, List<Method> corbaSetters, List<Field> corbaFields)
        throws BeansException
    {
        boolean result = false;

        // Check methods for the @CorbaRef annotation
        Class<?> beanClass = bean.getClass();
        for (Method meth: beanClass.getDeclaredMethods()) {
            if (meth.isAnnotationPresent(CorbaRef.class)) {
                if (meth.getParameterCount() != 1) {
                    throw new BeanDefinitionValidationException(
                        "Only methods with exactly one parameter may be annotated with @CorbaRef, but method " +
                        meth.getName() + "() in bean '" + beanName + "' has " + meth.getParameterCount() + ".");
                }
                Parameter param = meth.getParameters()[0];
                if (!org.omg.CORBA.Object.class.isAssignableFrom(param.getType())) {
                    throw new BeanDefinitionValidationException(
                        "The type of the (only) parameter of a method annotated with @CorbaRef must be a subtype of " +
                        "org.omg.CORBA.Object, but it is of type " + param.getType().getClass() + " in method " +
                        meth.getName() + "() of bean '" + beanName + "'.");
                }
                corbaSetters.add(meth);
                result = true;
            }
        }

        // Check fields for the @CorbaRef annotation
        for (Field fld: beanClass.getDeclaredFields()) {
            if (fld.isAnnotationPresent(CorbaRef.class)) {
                if (!org.omg.CORBA.Object.class.isAssignableFrom(fld.getType())) {
                    throw new BeanDefinitionValidationException(
                        "The type of a field annotated with @CorbaRef must be a subtype of org.omg.CORBA.Object, but the type " +
                        "of field " + fld.getName() + " in bean '" + beanName + "' is not.");
                }
                corbaFields.add(fld);
                result = true;
            }
        }

        return result;
    }


    private <T> T resolveCorbaRef(CorbaRef refInfo, Class<T> refType)
        throws BeansException
    {
        // Obtain the starting context for the lookup
        NamingContextBean ctxBean = null;
        if (refInfo.rootCtxBean() != null && refInfo.rootCtxBean().length() > 0) {
            ctxBean = appCtx.getBean(refInfo.rootCtxBean(), NamingContextBean.class);
            if (ctxBean == null) {
                throw new BeanInitializationException("No NamingContext bean named '" + refInfo.rootCtxBean() +
                                                      "' in application context.");
            }
        }
        else {
            try {
                OrbBean theOrb = appCtx.getBean(OrbBean.class);
                ctxBean = theOrb.getRootNamingCtx();
                if (ctxBean == null) {
                    throw new BeanInitializationException("Fetching the root naming context from the unique ORB bean failed.");
                }
            }
            catch (NoSuchBeanDefinitionException nbd) {
                throw new BeanInitializationException("No starting naming context given and no (unique) ORB bean found.", nbd);
            }
        }
        // Lop off the (last) name from the COS Naming Path given in the annotation
        String path = null, name = null, fullPath = refInfo.cosNamingPath();
        int lastSlashPos = fullPath.lastIndexOf('/');
        if (lastSlashPos > -1) {
            path = fullPath.substring(0, lastSlashPos);
            name = fullPath.substring(lastSlashPos + 1);
        }
        else {
            path = "";
            name = fullPath;
        }
        NamingContextBean fullCtx = new NamingContextBean(ctxBean, path);
        fullCtx.setLookupOnly(true);
        try {
            fullCtx.corbaInit(); // this is of course NOT done by the container, since we just created the thing
            return NamedReferenceLookup.lookup(fullCtx, name, refType);
        }
        catch (CannotProceed cnp) {
            throw new BeanInitializationException("COS Naming lookup failed.", cnp);
        }
        catch (org.omg.CORBA.BAD_PARAM cbp) {
            throw new BeanInitializationException("Object resolved in NamingContext could not be narrowed to " + refType.getName());
        }
        catch (Throwable t) {
            throw new BeanInitializationException("NamingContext bean initialization failed.", t);
        }
    }


    private void registerClientInterceptor(String beanName, Object bean, CorbaClientInterceptor icp)
    {
        // Get the relevant data from the annotation
        String orbName = icp.orb();
        String serviceName = icp.serviceName();
        if (orbName.trim().isEmpty()) {
            orbName = CorbaBeanFactoryPostProcessor.getSingleOrbName();
        }

        // If the annotation has a non-empty "service" attribute, then register/augment a Service Definition
        if (!icp.serviceName().trim().isEmpty()) {
            // Check that the bena implements the ServiceContextPropagatingClientRequestInterceptor interface
            Class<ServiceContextPropagatingClientRequestInterceptor> icClass =
                ServiceContextPropagatingClientRequestInterceptor.class;
            if (!icClass.isAssignableFrom(bean.getClass())) {
                throw new BeanDefinitionValidationException("Bean '" + beanName + "' is annotated with " +
                    "@CorbaClientInterceptor(serviceName=\"" + serviceName + "\") , but does not implement the " +
                    icClass.getName() + " interface.");
            }
            // Get or create the relevant ServiceDefinition
            Map<String, ServiceDefinition> svcDefs = serviceDefs.get(orbName);
            if (svcDefs == null) {
                svcDefs = new HashMap<>();
                serviceDefs.put(orbName, svcDefs);
            }
            ServiceDefinition svcDef = svcDefs.get(serviceName);
            if (svcDef == null) {
                svcDefs.put(serviceName, new ServiceDefinition(icClass.cast(bean)));
            }
            else {
                // Check that the ServiceDefinition does not already contain a client interceptor
                if (svcDef.cltIcp != null) {
                    throw new BeanDefinitionValidationException("There is more than one @CorbaClientInterceptor annotation " +
                    "with orb='" + icp.orb() + "' and serviceName='" + serviceName + "' (one is on class " +
                    bean.getClass().getName() + ").");
                }
                svcDef.cltIcp = icClass.cast(bean);
            }
        }
        // Otherwise, register a 'standalone' ClientRequestInterceptor
        else {
            // Check that the bean implements the ClientRequestInterceptor interface
            Class<ClientRequestInterceptor> icClass = ClientRequestInterceptor.class;
            if (!icClass.isAssignableFrom(bean.getClass())) {
                throw new BeanDefinitionValidationException("Bean '" + beanName +
                    "' is annotated with @CorbaClientInterceptor, but does not implement the " +
                    icClass.getName() + " interface.");
            }
            // Get or create the interceptor list for the referenced orb
            List<ClientRequestInterceptor> interceptors = cltInterceptors.get(orbName);
            if (interceptors == null) {
                interceptors = new LinkedList<>();
                cltInterceptors.put(orbName, interceptors);
            }
            // Add the newly created bean to the list of interceptors for the referenced ORB
            interceptors.add(icClass.cast(bean));
        }
    }


    private void registerServerInterceptor(String beanName, Object bean, CorbaServerInterceptor icp)
    {
        // Get the relevant data from the annotation
        String orbName = icp.orb();
        String serviceName = icp.serviceName();
        if (orbName.trim().isEmpty()) {
            orbName = CorbaBeanFactoryPostProcessor.getSingleOrbName();
        }

        // If the annotation has a non-empty "service" attribute, then register/augment a Service Definition
        if (!icp.serviceName().trim().isEmpty()) {
            // Check that the bean implements the ServiceContextPropagatingServiceRequestInterceptor interface
            Class<ServiceContextPropagatingServerRequestInterceptor> icClass =
                ServiceContextPropagatingServerRequestInterceptor.class;
            if (!icClass.isAssignableFrom(bean.getClass())) {
                throw new BeanDefinitionValidationException("Bean '" + beanName + "' is annotated with " +
                    "@CorbaServerInterceptor(serviceName=\"" + serviceName + "\") , but does not implement the " +
                    icClass.getName() + " interface.");
            }
            // Get or create the relevant ServiceDefinition
            Map<String, ServiceDefinition> svcDefs = serviceDefs.get(orbName);
            if (svcDefs == null) {
                svcDefs = new HashMap<>();
                serviceDefs.put(orbName, svcDefs);
            }
            ServiceDefinition svcDef = svcDefs.get(serviceName);
            if (svcDef == null) {
                // If it does not exist yet, create it
                svcDefs.put(serviceName, new ServiceDefinition(icClass.cast(bean)));
            }
            else {
                // Check that the ServiceDefinition does not already contain a server interceptor
                if (svcDef.srvIcp != null) {
                    throw new BeanDefinitionValidationException("There is more than one @CorbaServerInterceptor annotation " +
                        "with orb='" + icp.orb() + "' and serviceName='" + serviceName + "' (one is on class " +
                        bean.getClass().getName() + ").");
                }
                svcDef.srvIcp = icClass.cast(bean);
            }
        }
        // Otherwise, register a 'standalone' ServerRequestInterceptor
        else {
            // Check that the bean implements the ServerRequestInterceptor interface
            Class<ServerRequestInterceptor> icClass = ServerRequestInterceptor.class;
            if (!icClass.isAssignableFrom(bean.getClass())) {
                throw new BeanDefinitionValidationException("Bean '" + beanName + "' is annotated with " +
                    "@CorbaServerInterceptor, but does not implement the " + icClass.getName() + " interface.");
            }

            // Get or create the interceptor list for the referenced orb
            List<ServerRequestInterceptor> interceptors = srvInterceptors.get(orbName);
            if (interceptors == null) {
                interceptors = new LinkedList<>();
                srvInterceptors.put(orbName, interceptors);
            }

            // Add the newly created bean to the list of interceptors for the referenced ORB
            interceptors.add(icClass.cast(bean));
        }
    }


    private void registerServiceContextManager(String beanName, Object bean, CorbaServiceContextManager ctxMgr)
    {
        // Get the relevant data from the annotation
        String orbName = ctxMgr.orb();
        String serviceName = ctxMgr.serviceName();
        if (orbName.trim().isEmpty()) {
            orbName = CorbaBeanFactoryPostProcessor.getSingleOrbName();
        }

        // Check that the bean implements the PICurrentInjectable interface
        Class<PICurrentInjectable> mgrIF = PICurrentInjectable.class;
        if (!mgrIF.isAssignableFrom(bean.getClass())) {
            throw new BeanDefinitionValidationException("Bean '" + beanName + "' is annotated with " +
                "@CorbaServiceContextManager(serviceName=\"" + serviceName + "\") , but does not implement the " +
                mgrIF.getName() + " interface.");
        }
        // Get or create the relevant ServiceDefinition
        Map<String, ServiceDefinition> svcDefs = serviceDefs.get(orbName);
        if (svcDefs == null) {
            svcDefs = new HashMap<>();
            serviceDefs.put(orbName, svcDefs);
        }
        ServiceDefinition svcDef = svcDefs.get(serviceName);
        if (svcDef == null) {
            // If it does not exist yet, create it
            svcDefs.put(serviceName, new ServiceDefinition(mgrIF.cast(bean)));
        }
        else {
            // Check that the ServiceDefinition does not already contain a context manager
            if (svcDef.srvIcp != null) {
                throw new BeanDefinitionValidationException("There is more than one @CorbaServiceContextManager annotation " +
                    "with orb='" + ctxMgr.orb() + "' and serviceName='" + serviceName + "' (one is on class " +
                    bean.getClass().getName() + ").");
            }
            svcDef.ctxMgr = mgrIF.cast(bean);
        }
    }


    private void registerServantManagers(String beanName, PoaBean poaBean)
    {
        ServantMgrRec rec = servantMgrs.get(beanName);
        if (rec != null) {
            if (rec.activator != null) {
                poaBean.setServantActivator(rec.activator);
            }
            if (rec.locator != null) {
                poaBean.setServantLocator(rec.locator);
            }
            if (rec.defaultServant != null) {
                poaBean.setDefaultServant(rec.defaultServant);
            }
        }
    }


    private void registerServantLocator(String beanName, Object bean, CorbaServantLocator loc)
        throws BeansException
    {
        // Check that the bean implements the ServantLocator interface
        if (!ServantLocator.class.isAssignableFrom(bean.getClass())) {
            throw new BeanDefinitionValidationException("Bean '" + beanName +
              "' is annotated with @CorbaServantLocator, but does not implement the ServantLocator interface.");
        }

        // Get the corresponding POABean record, if present
        ServantMgrRec rec = servantMgrs.get(loc.poa());
        if (rec == null) {
            // If it is not present, create one - locator registration will happen in registerServantManagers
            servantMgrs.put(loc.poa(), new ServantMgrRec((ServantLocator) bean));
        }
        else {
            // If it is present, check that only exactly one locator or activator is assigned
            if (rec.locator != null) {
                throw new BeanInitializationException("Attempt to assign more than one ServantLocator to PoaBean '" + beanName +
                    "' via annotations on classes " + rec.locator.getClass().getName() + " and " + bean.getClass().getName() + ".");
            }
            if (rec.activator != null) {
                throw new BeanInitializationException("Attempt to assign both a ServantActivator and a ServantLocator to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.activator.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            if (rec.defaultServant != null) {
                throw new BeanInitializationException("Attempt to assign both a DefaultServant and a ServantLocator to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.defaultServant.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            rec.locator = (ServantLocator) bean;
        }
    }


    private void registerServantActivator(String beanName, Object bean, CorbaServantActivator act)
    {
        // Check that the bean implements the ServantActivator interface
        if (!ServantActivator.class.isAssignableFrom(bean.getClass())) {
            throw new BeanDefinitionValidationException("Bean '" + beanName +
              "' is annotated with @CorbaServantLocator, but does not implement the ServantLocator interface.");
        }

        // Get the corresponding POABean record, if present
        ServantMgrRec rec = servantMgrs.get(act.poa());
        if (rec == null) {
            // If it is not present, create one - locator registration will happen in registerPoaBean
            servantMgrs.put(act.poa(), new ServantMgrRec((ServantActivator) bean));
        }
        else {
            // If it is present, check that only exactly one locator, activator or default servant is assigned
            if (rec.activator != null) {
                throw new BeanInitializationException("Attempt to assign more than one ServantActivator to PoaBean '" + beanName +
                    "' via annotations on classes " + rec.activator.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            if (rec.locator != null) {
                throw new BeanInitializationException("Attempt to assign both a ServantLocator and a ServantActivator to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.locator.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            if (rec.defaultServant != null) {
                throw new BeanInitializationException("Attempt to assign both a DefaultServant and a ServantActivator to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.defaultServant.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            rec.activator = (ServantActivator) bean;
        }
    }


    private void registerDefaultServant(String beanName, Object bean, CorbaDefaultServant defSrv)
    {
        // Check that the bean extends the Servant abstract class
        if (!Servant.class.isAssignableFrom(bean.getClass())) {
           throw new BeanDefinitionValidationException("Bean '" + beanName +
               "' is annotated with @CorbaDefaultServant, but does not extend org.omg.PortableServer.Servant.");
        }

        // Check that the bean implements the InvokeHandler interface
        if (!org.omg.CORBA.portable.InvokeHandler.class.isAssignableFrom(bean.getClass())) {
            throw new BeanDefinitionValidationException("Bean '" + beanName + "' is annotated with @CorbaDefaultServant, " +
                "but does not implement the org.omg.CORBA.portable.InvokeHandler interface.");
        }

        // Get the corresponding POABean record, if present
        ServantMgrRec rec = servantMgrs.get(defSrv.poa());
        if (rec == null) {
            // If it is not present, create one - locator registration will happen in registerPoaBean
            servantMgrs.put(defSrv.poa(), new ServantMgrRec((Servant) bean));
        }
        else {
            // If it is present, check that only exactly one locator, activator or default servant is assigned
            if (rec.defaultServant != null) {
                throw new BeanInitializationException("Attempt to assign more than one DefaultServant to PoaBean '" + beanName +
                    "' via annotations on classes " + rec.defaultServant.getClass().getName()
                    + " and " + bean.getClass().getName() + ".");
            }
            if (rec.locator != null) {
                throw new BeanInitializationException("Attempt to assign both a ServantLocator and a DefaultServant to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.locator.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            if (rec.activator != null) {
                throw new BeanInitializationException("Attempt to assign both a ServantActivator and a DefaultServant to PoaBean '" +
                    beanName + "' via annotations on classes " + rec.activator.getClass().getName() +
                    " and " + bean.getClass().getName() + ".");
            }
            rec.defaultServant = (Servant) bean;
        }
    }


    private void injectCorbaRefs(String beanName, Object bean, List<Method> corbaSetters, List<Field> corbaFields)
        throws BeansException
    {
        for (Method setter: corbaSetters) {
            // Resolve the object (reference) specified by the annotation
            CorbaRef ref = setter.getAnnotation(CorbaRef.class);
            Class<?> refType = setter.getParameterTypes()[0];
            Object obj = resolveCorbaRef(ref, refType);
            if (obj == null && !ref.optional()) {
                throw new BeanInitializationException("COS Naming lookup failed for setter method " + setter.getName() +
                                                      "() on bean '" + beanName + "'.");
            }
            // Immediately invoke the setter method with the resolved object as argument
            setter.setAccessible(true);
            try {
                setter.invoke(bean, obj);
            }
            catch (Exception xcp) {
                throw new BeanInitializationException("Could not invoke setter method " + setter.getName() + "() of bean '" + beanName + "'.", xcp);
            }
        }
        for (Field fld: corbaFields) {
            // Resolve the object (reference) specified by the annotation
            CorbaRef ref = fld.getAnnotation(CorbaRef.class);
            Class<?> refType = fld.getType();
            Object obj = resolveCorbaRef(ref, refType);
            if (obj == null && !ref.optional()) {
                throw new BeanInitializationException("COS Naming lookup failed for field '" + fld.getName() +
                                                      "' of bean '" + beanName + "'.");
            }
            // Immediately assign the resolved reference to the field
            fld.setAccessible(true);
            try {
                fld.set(bean, obj);
            }
            catch (Exception xcp) {
                throw new BeanInitializationException("Could not assign field '" + fld.getName() + "' of bean '" + beanName + "'.", xcp);
            }
        }
    }


    private PoaBean getPoaBean(String name)
        throws BeansException
    {
        PoaBean reqPoa = appCtx.getBean(name, PoaBean.class);
        if (reqPoa == null) {
            // If the required POA bean does not exist, we can only throw an exception
            throw new BeanDefinitionValidationException("No PoaBean named '" + name + "' available.");
        }
        return reqPoa;
    }

}
