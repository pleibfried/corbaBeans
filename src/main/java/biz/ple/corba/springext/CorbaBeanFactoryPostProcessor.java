package biz.ple.corba.springext;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;

import biz.ple.corba.annotations.CorbaClientInterceptor;
import biz.ple.corba.annotations.CorbaDefaultServant;
import biz.ple.corba.annotations.CorbaServantActivator;
import biz.ple.corba.annotations.CorbaServantLocator;
import biz.ple.corba.annotations.CorbaServerInterceptor;
import biz.ple.corba.annotations.CorbaServiceContextManager;
import biz.ple.corba.beans.OrbBean;


/**
 *  Makes sure that beans annotated with {@link CorbaServantActivator @CorbaServantActivator} or
 *  {@link CorbaServantLocator @CorbaServantLocator} are created before the
 *  {@link biz.ple.corba.beans.server.PoaBean PoaBean} they reference. This is because a PoaBean's
 *  {@link biz.ple.corba.beans.server.PoaBean#setServantLocator ServantLocator} or
 *  {@link biz.ple.corba.beans.server.PoaBean#setServantActivator ServantActivator} must be set before
 *  {@link biz.ple.corba.beans.server.PoaBean#corbaInit() initialization}.
 */
public class CorbaBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static String singleOrbName = null;

    public static String getSingleOrbName() { return singleOrbName; }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
        throws BeansException
    {
        // Process POA references in ServantManager-related annotations
        processPoaReferences(beanFactory, CorbaServantLocator.class);
        processPoaReferences(beanFactory, CorbaServantActivator.class);
        processPoaReferences(beanFactory, CorbaDefaultServant.class);

        // Process ORB references in RequestInterceptor-related annotations
        processOrbReferences(beanFactory, CorbaServiceContextManager.class);
        processOrbReferences(beanFactory, CorbaClientInterceptor.class);
        processOrbReferences(beanFactory, CorbaServerInterceptor.class);
    }


    private void processPoaReferences(ConfigurableListableBeanFactory beanFactory, Class<? extends Annotation> annotationType)
    {
        //System.out.println("processPoaReferences(beanFactory, @" + annotationType.getSimpleName() + ")");
        Method poaAttr = null;
        try {
            poaAttr = annotationType.getMethod("poa", (Class[]) null);
            if (!String.class.isAssignableFrom(poaAttr.getReturnType())) {
                throw new IllegalArgumentException(
                    annotationType.getName() + "'s 'poa' attribute is not of type java.lang.String.");
            }
        }
        catch (NoSuchMethodException nsm) {
            throw new IllegalArgumentException(annotationType.getName() + " does not have a 'poa' attribute.");
        }

        // Find all the bean definitions whose class is annotated with the annotationType
        // and register a dependency with the PoaBean referenced in the annotation
        for (String beanName: beanFactory.getBeanNamesForAnnotation(annotationType)) {
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            String poaName = null;
            try {
                Object actualAnnotation = Class.forName(beanDef.getBeanClassName())
                                               .getAnnotation(annotationType);
                poaName = (String) poaAttr.invoke(actualAnnotation, (Object[]) null);
                BeanDefinition poaDef = beanFactory.getBeanDefinition(poaName);
                String[] oldDeps = poaDef.getDependsOn();
                if (oldDeps == null) {
                    poaDef.setDependsOn(beanName);
                }
                else {
                    String[] newDeps = Arrays.copyOf(oldDeps, oldDeps.length + 1);
                    newDeps[oldDeps.length] = beanName;
                    poaDef.setDependsOn(newDeps);
                }
            }
            catch (ClassNotFoundException cnf) {
                throw new BeanDefinitionValidationException("Bean class could not be loaded.", cnf);
            }
            catch (NoSuchBeanDefinitionException nsd) {
                throw new BeanDefinitionValidationException("No PoaBean named '" + poaName +
                    "' defined, referenced in @" + annotationType.getName() + " annotation of class " + beanDef.getBeanClassName());
            }
            catch (InvocationTargetException | IllegalAccessException rfx) {
                throw new BeanInitializationException("Could not access the value of the 'poa' attribute in the @" +
                    annotationType.getSimpleName() + " annotation of class " + beanDef.getBeanClassName() + ".", rfx);
            }
        }
    }


    private void processOrbReferences(ConfigurableListableBeanFactory beanFactory, Class<? extends Annotation> annotationType)
    {
        Method orbAttr = null;
        try {
            orbAttr = annotationType.getMethod("orb", (Class[]) null);
            if (!String.class.isAssignableFrom(orbAttr.getReturnType())) {
                throw new IllegalArgumentException(annotationType.getName() + "'s 'orb' attribute is not of type java.lang.String.");
            }
        }
        catch (NoSuchMethodException nsm) {
            throw new IllegalArgumentException(annotationType.getName() + " does not have an 'orb' attribute.");
        }

        // Find all the bean definitions whose class is annotated with the annotationType
        // and register a dependency with the OrbBean referenced in the annotation
        for (String beanName : beanFactory.getBeanNamesForAnnotation(annotationType)) {
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            String orbName = null;
            try {
                Object actualAnnotation = Class.forName(beanDef.getBeanClassName()).getAnnotation(annotationType);
                orbName = (String) orbAttr.invoke(actualAnnotation, (Object[]) null);
                if (orbName.trim().isEmpty()) {
                    if (singleOrbName == null) {
                        String[] allOrbBeans = beanFactory.getBeanNamesForType(OrbBean.class);
                        if (allOrbBeans.length == 1) {
                            singleOrbName = allOrbBeans[0];
                        }
                        else {
                            throw new BeanDefinitionValidationException("Class " + beanDef.getBeanClassName() +
                                " is annotated with @" + annotationType.getSimpleName() + " and does not specify an 'orb' " +
                                "attribute, but there are either more than one OrbBean definitions or none at all configured.");
                        }
                    }
                    orbName = singleOrbName;
                }
                BeanDefinition orbDef = beanFactory.getBeanDefinition(orbName);
                String[] oldDeps = orbDef.getDependsOn();
                if (oldDeps == null) {
                    orbDef.setDependsOn(beanName);
                }
                else {
                    String[] newDeps = Arrays.copyOf(oldDeps, oldDeps.length + 1);
                    newDeps[oldDeps.length] = beanName;
                    orbDef.setDependsOn(newDeps);
                }
            }
            catch (ClassNotFoundException cnf) {
                throw new BeanDefinitionValidationException("Bean class of bean '" + beanName + "' could not be loaded.", cnf);
            }
            catch (NoSuchBeanDefinitionException nsd) {
                throw new BeanDefinitionValidationException("No OrbBean named '" + orbName + "' defined, " +
                    "referenced in @" + annotationType.getSimpleName() + " annotation of class " + beanDef.getBeanClassName());
            }
            catch (InvocationTargetException | IllegalAccessException rfx) {
                throw new BeanInitializationException("Could not access the value of the 'orb' attribute in the @" +
                    annotationType.getSimpleName() + " annotation of class " + beanDef.getBeanClassName() + ".", rfx);
            }
        }
    }

}
