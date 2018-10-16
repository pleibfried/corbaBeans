package biz.ple.corba.beans;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.omg.PortableInterceptor._ORBInitializerLocalBase;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingClientRequestInterceptor;
import biz.ple.corba.interfaces.ServiceContextPropagatingServerRequestInterceptor;


/**
 * <p>Encapsulates the ORB, i.e. the CORBA runtime, and its initialization.</p>
 * <p>As with the other beans of this package (and subpackages), the encapsulation is not
 * 'closed', but provides {@link #getORB() access} to the encapsulated object, the intent
 * not being to fully wrap the ORB and its interface, but to make it amenable to DI/IoC
 * in the context of the Spring Framework.</p>
 * <p>Also, methods have been added for easy initialization and convenient access to
 * often-used 'subordinate' objects such as the {@link #getRootNamingCtx() Root Naming
 * Context} and the {@link #getPICurrent() Portable Interceptor current}.</p>
 * <p>Some easy-to-use methods for {@link #start() starting} and {@link #stop stopping}
 * the ORB are provided.</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class OrbBean {

    private static class OrbRunnerThread extends Thread
    {
    	private ORB orb;
    	private Object sync;

    	public OrbRunnerThread(ORB orb, Object sync)
    	{
    		this.orb = orb;
    		this.sync = sync;
    	}

		@Override
		public void run() {
		    // Start CORBA request processing (this will block indefinitely)
			orb.run();

			// The only way we get here is when orb.run() is interrupted,
			// e.g. by another thread invoking orb.shutdown().
			synchronized (sync) {
			    // Notify any threads which might be waiting for the ORB to shutdown
				sync.notifyAll();
			}
		}
    }


    private static class OrbShutdownThread extends Thread
    {
        private ORB orb;
        private boolean waitForRequests;

        public OrbShutdownThread(ORB orb, boolean waitForRequests)
        {
            this.orb = orb;
            this.waitForRequests = waitForRequests;
        }

        @Override
        /** Simply invokes the ORB.shutdown() method on the ORB
         *  passed in the constructor. */
        public void run()
        {
            orb.shutdown(waitForRequests);
        }
    }


    private static class InterceptorRegistration implements MethodInterceptor
    {
        private ClientRequestInterceptor cltInterceptor;
        private ServerRequestInterceptor srvInterceptor;
        private OrbBean orbBean;

        public InterceptorRegistration(ClientRequestInterceptor corbaInterceptor, OrbBean orbBean)
        {
            this.cltInterceptor = corbaInterceptor;
            this.orbBean = orbBean;
        }

        public InterceptorRegistration(ServerRequestInterceptor corbaInterceptor, OrbBean orbBean)
        {
            this.srvInterceptor = corbaInterceptor;
            this.orbBean = orbBean;
        }

        @Override
        public Object intercept(Object self, Method meth, Object[] args, MethodProxy proxy)
            throws Throwable
        {
            if ("pre_init".equals(meth.getName())) {
                return null;
            }
            else
            if ("post_init".equals(meth.getName())) {
                ORBInitInfo info = (ORBInitInfo) args[0];
                if (cltInterceptor != null) {
                    info.add_client_request_interceptor(cltInterceptor);
                    if (cltInterceptor instanceof PICurrentInjectable) {
                        PICurrentInjectable inj = (PICurrentInjectable) cltInterceptor;
                        inj.setOrb(orbBean);
                        allocateSlotAndInjectPICurrent(info, inj);
                    }
                }
                else
                if (srvInterceptor != null) {
                    info.add_server_request_interceptor(srvInterceptor);
                    if (cltInterceptor instanceof PICurrentInjectable) {
                        PICurrentInjectable inj = (PICurrentInjectable) cltInterceptor;
                        inj.setOrb(orbBean);
                        allocateSlotAndInjectPICurrent(info, inj);
                    }
                }
                return null;
            }
            else {
                return proxy.invokeSuper(self, args);
            }
        }

        protected void allocateSlotAndInjectPICurrent(ORBInitInfo info, PICurrentInjectable inj) throws Throwable
        {
            inj.setPortableInterceptorCurrent(CurrentHelper.narrow(info.resolve_initial_references(PI_CURRENT_NAME)),
                                              info.allocate_slot_id());
        }

    }


    private static class ServiceContextRegistration implements MethodInterceptor
    {
        private ServiceContextDefinition svcDef;
        private OrbBean orbBean;

        public ServiceContextRegistration(ServiceContextDefinition svcDef, OrbBean orbBean)
        {
            this.svcDef = svcDef;
            this.orbBean = orbBean;
        }

        @Override
        public Object intercept(Object self, Method meth, Object[] args, MethodProxy proxy)
            throws Throwable
        {
            if ("pre_init".equals(meth.getName())) {
                return null;
            }
            else
            if ("post_init".equals(meth.getName())) {
                // Allocate a slotID and get the PICurrent
                ORBInitInfo info = (ORBInitInfo) args[0];
                int slotId = info.allocate_slot_id();
                Current piCurrent = CurrentHelper.narrow(info.resolve_initial_references(PI_CURRENT_NAME));

                // Inject PICurrent and slotID into Service Context Manager and Interceptors
                PICurrentInjectable ctxMgr = svcDef.getManager();
                if (ctxMgr != null) {
                    ctxMgr.setPortableInterceptorCurrent(piCurrent, slotId);
                    ctxMgr.setOrb(orbBean);
                }
                ServiceContextPropagatingClientRequestInterceptor cltIcp = svcDef.getClientInterceptor();
                if (cltIcp != null) {
                    info.add_client_request_interceptor(cltIcp);
                    cltIcp.setPortableInterceptorCurrent(piCurrent, slotId);
                    cltIcp.setOrb(orbBean);
                }
                ServiceContextPropagatingServerRequestInterceptor srvIcp = svcDef.getServerInterceptor();
                if (srvIcp != null) {
                    info.add_server_request_interceptor(srvIcp);
                    srvIcp.setPortableInterceptorCurrent(piCurrent, slotId);
                    srvIcp.setOrb(orbBean);
                }

                // That's all folks
                return null;
            }
            else {
                return proxy.invokeSuper(self, args);
            }
        }

    }

    public static final String NAMING_SERVICE_NAME = "NameService";
    public static final String PI_CURRENT_NAME     = "PICurrent";
    public static final String ROOT_POA_NAME       = "RootPOA";

    private ORB theORB;
    private POA rootPoa;
    private String nsInitRef;
    private NamingContextExt rootNamingCtx;
    private Current piCurrent;
    private Object orbSync = new Object();
    private List<Class<?>> initializers = new LinkedList<>();


    private String registerInterceptor(Callback callback)
    {
        Enhancer enh = new Enhancer();
        enh.setUseCache(false);
        enh.setSuperclass(_ORBInitializerLocalBase.class);
        enh.setInterfaces(new Class[] { ORBInitializer.class });
        enh.setCallbackType(MethodInterceptor.class);
        Class<?> genClazz = enh.createClass();
        Enhancer.registerStaticCallbacks(genClazz, new Callback[] { callback });
        initializers.add(genClazz);
        return genClazz.getName();
    }


    /**
     * <p>Registers a {@link ServiceContextDefinition 1Service Context Definition} with this OrbBean.</p>
     * <p>By invoking this method several times, several Service Context Definitions can be registered.
     * During {@link #corbaInit() initialization}, an ORBInitializer is created and registered for each
     * Service Context Definition, i.e. the definition's interceptors are added to the ORB's request
     * interceptor chain, and the interceptors and context manager are injected with the Portable Interceptor
     * Current, the Slot ID, and this {@code OrbBean}.</p>
     * <p>It follows from the above that this method must be invoked before {@link #corbaInit()} to have any
     * effect, and that invocation after {@code corbaInit()} does not have any effect.
     * @param serviceCtx
     *      The Service Context Definition to be registered.
     * @return
     *      The class name of the ORBInizializer resulting from the registration.
     * @see ServiceContextDefinition
     */
    public String registerServiceContext(ServiceContextDefinition serviceCtx)
    {
        // Create a new ORBInitializer-implementing class so the application programmer doesn't have to
        Enhancer enh = new Enhancer();
        enh.setUseCache(false);
        enh.setSuperclass(_ORBInitializerLocalBase.class);
        enh.setInterfaces(new Class[] { ORBInitializer.class });
        enh.setCallbackType(MethodInterceptor.class);
        Class<?> genClazz = enh.createClass();
        Enhancer.registerStaticCallbacks(genClazz,
                                         new Callback[] { new ServiceContextRegistration(serviceCtx, this) });

        // Add the new ORBInitializer class to the list of ORB initializers
        // Upon ORB initialization, the ORB will create instances of them by reflection and call their methods
        initializers.add(genClazz);

        // Return the (machine-generated) name of the new ORBInitializer class in case anyone's interested
        return genClazz.getName();
    }


    /**
     * <p>Has the same effect as invoking {@link #registerServiceContext} for each of the Service Context
     * Definitions passed in the {@code svcDefs} parameter.</p>
     * <p>Note that this method <strong>does not really have "setter" semantics</strong>, as invoking it
     * several times will register Service Context Definitions <i>in addition</i> to already registered
     * ones (and not replace them). It is provided for use by Spring configuration via XML.</p>
     * @param svcDefs
     *      A list of Service Context Definitions, each of which is to be registered with the ORB.
     * @see #registerServiceContext
     */
    public void setServiceContextDefinitions(List<ServiceContextDefinition> svcDefs)
    {
        for (ServiceContextDefinition def: svcDefs) {
            registerServiceContext(def);
        }
    }


    /**
     * <p>Registers a {@link ClientRequestInterceptor Client Request Interceptor} with this OrbBean.</p>
     * <p>By invoking this method several times, multiple Client Request Interceptorscan be registered.
     * During {@link #corbaInit() initialization}, an ORBInitializer is created and registered for each
     * Client Request Interceptor, i.e. interceptor is added to the ORB's request interceptor chain.</p>
     * <p>If it the interceptor object implements the {@link PICurrentInjectable} interface, a slot ID
     * is allocated and theinterceptor is injected with the {@link Current Portable Interceptor Current}
     * and this {@code OrbBean}. (Since Slot IDs are allocated at registration time and only make sense
     * when shared among several objects, it is usually better to use {@link ServiceContextDefinition
     * Service Context Definition} beans when using the PICurrent.)</p>
     * <p>This method must be invoked before {@link #corbaInit()} to have any effect, and that invocation
     * after {@code corbaInit()} does not have any effect.
     * @param cltIc
     *      The Client Request Interceptor to be registered
     * @return
     *      The class name of the ORBInizializer resulting from the registration.
     * @see ServiceContextDefinition
     */
    public String registerClientInterceptor(ClientRequestInterceptor cltIc)
    {
        return registerInterceptor(new InterceptorRegistration(cltIc, this));
    }


    /**
     * <p>Registers a {@link ServerRequestInterceptor Server Request Interceptor} with this OrbBean.</p>
     * <p>By invoking this method several times, multiple Server Request Interceptorscan be registered.
     * During {@link #corbaInit() initialization}, an ORBInitializer is created and registered for each
     * Server Request Interceptor, i.e. interceptor is added to the ORB's request interceptor chain.</p>
     * <p>If it the interceptor object implements the {@link PICurrentInjectable} interface, a slot ID
     * is allocated and theinterceptor is injected with the {@link Current Portable Interceptor Current}
     * and this {@code OrbBean}. (Since Slot IDs are allocated at registration time and only make sense
     * when shared among several objects, it is usually better to use {@link ServiceContextDefinition
     * Service Context Definition} beans when using the PICurrent.)</p>
     * <p>This method must be invoked before {@link #corbaInit()} to have any effect, and that invocation
     * after {@code corbaInit()} does not have any effect.
     * @param srvIc
     *      The Server Request Interceptor to be registered
     * @return
     *      The class name of the ORBInizializer resulting from the registration.
     * @see ServiceContextDefinition
     */
    public String registerServerInterceptor(ServerRequestInterceptor srvIc)
    {
        return registerInterceptor(new InterceptorRegistration(srvIc, this));
    }


    /**
     * <p>Has the same effect as invoking {@link #registerClientInterceptor} for each of the
     * {@code ClientRequestInterceptor}s passed in the {@code cltIcps} parameter.</p>
     * <p>Note that this method <strong>does not really have "setter" semantics</strong>, as invoking it
     * several times will register Client Request Interceptors <i>in addition</i> to already registered
     * ones (and not replace them). It is provided for use by Spring configuration via XML.</p>
     * @param cltIcps
     *      A list of Client Request Interceptors, each of which is to be registered with the ORB.
     * @see #registerClientInterceptor
     */
    public void setClientInterceptors(List<ClientRequestInterceptor> cltIcps)
    {
        for (ClientRequestInterceptor cltIcp: cltIcps) {
            registerInterceptor(new InterceptorRegistration(cltIcp, this));
        }
    }


    /**
     * <p>Has the same effect as invoking {@link #registerServerInterceptor} for each of the
     * {@code ServerRequestInterceptor}s passed in the {@code srvIcps} parameter.</p>
     * <p>Note that this method <strong>does not really have "setter" semantics</strong>, as invoking it
     * several times will register Server Request Interceptors <i>in addition</i> to already registered
     * ones (and not replace them). It is provided for use by Spring configuration via XML.</p>
     * @param srvIcps
     *      A list of Server Request Interceptors, each of which is to be registered with the ORB.
     * @see #registerServerInterceptor
     */
    public void setServerInterceptors(List<ServerRequestInterceptor> srvIcps)
    {
        for (ServerRequestInterceptor srvIcp: srvIcps) {
            registerInterceptor(new InterceptorRegistration(srvIcp, this));
        }
    }


    public void setNameServiceInitRef(String nsInitRef)
    {
        this.nsInitRef = nsInitRef;
    }


    /**
     * <p>Initializes the {@link ORB} encapsulated by this {@code OrbBean}.</p>
     * <p>Before calling the ORB's initialization method, properties are set and
     * ORB Initializers are registered according to the Naming Service initial
     * reference set using {@link #setNameServiceInitRef} and the Request Interceptors
     * and Service Contexts registered via {@link #registerServiceContext},
     * {@link #registerClientInterceptor} and {@link #registerServerInterceptor}
     * methods.</p>
     * <p>In terms of the Spring Framework, this is the {@code OrbBean} class'
     * init-method.</p>
     * @throws InvalidName
     *      if the encapsulated {@code ORB}'s Root POA, Portable Interceptor Current
     *      or reference to the Naming Service cannot be found. This usually indicates
     *      an internal error.
     */
    @PostConstruct
    public void corbaInit()
        throws InvalidName
    {
        // Basic initialization properties
        Properties orbProps = new Properties();
        orbProps.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
        orbProps.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
        if (nsInitRef != null) {
            orbProps.put("ORBInitRef.NameService", nsInitRef);
        }

        // Add initializer classes (names) to ORB properties if applicable
        if (initializers != null) {
            int index = 0;
            for (Class<?> initClass: initializers) {
                orbProps.put("org.omg.PortableInterceptor.ORBInitializerClass.corbaBeans." + index, initClass.getName());
                ++index;
            }
        }

        // Actual ORB initialization
        theORB = ORB.init((String[]) null, orbProps);

        // Get RootPOA and NamingService
        rootPoa = POAHelper.narrow(theORB.resolve_initial_references(ROOT_POA_NAME));
        rootNamingCtx = NamingContextExtHelper.narrow(theORB.resolve_initial_references(NAMING_SERVICE_NAME));
        piCurrent = CurrentHelper.narrow(theORB.resolve_initial_references(PI_CURRENT_NAME));
    }


    /**
     * Returns the {@link ORB} object encapsulated by this {@code ORBBean}.
     * @return
     *      The (fully initialized} ORB, if the method is invoked after {@link #corbaInit()}
     *      has been invoked; {@code null} otherwise.
     */
    public ORB getORB()
    {
        return theORB;
    }


    /**
     * Returns the Root POA of this ORB.
     * @return
     *      A a {@link PoaBean} encapsulating the Root POA of the ORB encapsulated by
     *      this {@code OrbBean}.
     */
    public PoaBean getRootPoa()
    {
        return new PoaBean(rootPoa, this);
    }


    /**
     * Returns the COS Naming services' Root Naming Context.
     * @return
     *      A reference to the Naming Service's Root Context, if the method is invoked
     *      after {@link #corbaInit()} has been invoked; {@code null} otherwise. The
     *      nature and location of the reference returnd depends on the value {@link
     *      #setNameServiceInitRef set} for the Naming Service's initial reference prior
     *      to initializaion.
     * @see #setNameServiceInitRef
     */
    @SuppressWarnings("deprecation")
    public NamingContextBean getRootNamingCtx()
    {
    	return new NamingContextBean(rootNamingCtx);
    }


    /**
     * Returns the Portable Interceptor Current of this ORB.
     * @return
     *      The "PICurrent" (Portable Interceptor Current) of
     *      the ORB encapsulated by this {@code OrbBean}.
     */
    public Current getPICurrent()
    {
        return piCurrent;
    }


    /**
     * Returns a new {@link Any}.
     * @return
     *     An Any, as returned by invoking {@link ORB#create_any()} on
     *     the ORB encapsulated by this {@code OrbBean}.
     */
    public Any createAny()
    {
        return theORB.create_any();
    }


    /**
     * <p>Activates the POA Manager of the ORB encapsulated by this {@code OrbBean} and starts processing
     * of CORBA requests by invoking the ORB's {@link ORB#run() run()} method in a separate thread,
     * meaning that this method is non-blocking and returns after that thread has been started.</p>
     * @see #stop(boolean) stop()
     * @see #waitForShutdown()
     */
    public void start()
    {
        try {
            rootPoa.the_POAManager().activate();
        }
        catch (AdapterInactive e) {
            throw new IllegalStateException("Could not active RootPOA because of AdapterInactive exception.");
        }
        OrbRunnerThread orbThread = new OrbRunnerThread(theORB, orbSync);
        orbThread.start();
    }


    /**
     * <p>Stops processing of CORBA requests by the ORB encapsulating by this {@code OrbBean} by invoking
     * the ORB's {@link ORB#shutdown(boolean) shutDown()} method in a separate thread. meaning that this method
     * is non-blocking and returns after that thread has been started.
     * @param waitForRequests
     *      Indicates whether all requests being processed at the time of invocation should be allowed to
     *      complete before the ORB is actually shut down.
     */
    public void stop(boolean waitForRequests)
    {
    	OrbShutdownThread shutdownThread = new OrbShutdownThread(theORB, waitForRequests);
    	shutdownThread.start();
    }


    /**
     * Waits for the {@link ORB#run ORB.run()} method invoked in the thread started by {@link #start()} to return.
     * This usually happens only when the ORB is shut down, e.g. by invoking the {@link #stop(boolean) stop()} method.
     * @throws InterruptedException
     *      in case waiting for the ORB's shutdown is interrupted.
     */
    public void waitForShutdown() throws InterruptedException
    {
    	synchronized(orbSync) {
    		orbSync.wait();
    	}
    }

}
