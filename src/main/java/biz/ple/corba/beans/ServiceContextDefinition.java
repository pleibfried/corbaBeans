package biz.ple.corba.beans;

import org.omg.PortableInterceptor.Current;

import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingClientRequestInterceptor;
import biz.ple.corba.interfaces.ServiceContextPropagatingServerRequestInterceptor;


/**
 * <p>Bundles one or two Request Interceptors and a 'Service Context Manager' so that they
 * can be initialized together and share data.</p>
 * <p>The 'Service Context Manager' is actually just an implementation of the
 * {@link PICurrentInjectable} interface; it is assumed that it is used to get and set data
 * on the {@link Current Portable Interceptor Current} by the application. The Portable
 * Interceptor Current (or PICurrent for short) can be thought as a 'thread local' data
 * store.</p>
 * <p>During ORB initialization, the interceptors and Service Context Manager bundled in one
 * {@code ServiceContextDefinition} are injected with the same 'Slot ID' (allocated by the
 * ORB), which has to be used to set and retrieve data on the PICurrent. This enables the
 * interceptors and Service Context Manager to share data and pass it on to each other.</p>
 * <p>A {@code ServiceContextDefinition} must be
 * {@link OrbBean#registerServiceContext(ServiceContextDefinition) registered} with an
 * {@code OrbBean} prior to its initialization for its request interceptors to be registered
 * with the ORB and its elements to be injected with the ORB and Slot ID.</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 * @see    OrbBean#registerServiceContext(ServiceContextDefinition)
 *
 */
public class ServiceContextDefinition {

    protected PICurrentInjectable manager;
    protected ServiceContextPropagatingClientRequestInterceptor clientInterceptor;
    protected ServiceContextPropagatingServerRequestInterceptor serverInterceptor;


    /**
     * Constructor; bundles a Service Context Manager and a Client Request Interceptor
     * into a newly created {@code ServiceContextDefinition}. Intended for 'pure clients',
     * i.e. applications that to not receive and process CORBA requests.
     * @param manager
     *      The Service Context Manager of the new {@code ServiceContextDefinition}; may
     *      be {@code null}.
     * @param clientInterceptor
     *      The Client Request Interceptor of the new {@code ServiceContextDefinition}; may
     *      be {@code null}.
     */
    public ServiceContextDefinition(PICurrentInjectable manager,
                                    ServiceContextPropagatingClientRequestInterceptor clientInterceptor)
    {
        super();
        this.manager = manager;
        this.clientInterceptor = clientInterceptor;
    }


    /**
     * Constructor; bundles a Service Context Manager, a Client Request Interceptor and a
     * Server Request Interceptor into a newly created {@code ServiceContextDefinition}.
     * @param manager
     *      The Service Context Manager of the new {@code ServiceContextDefinition}; may
     *      be {@code null}.
     * @param clientInterceptor
     *      The Client Request Interceptor of the new {@code ServiceContextDefinition}; may
     *      be {@code null}.
     * @param serverInterceptor
     *      The Server Request Interceptor of the new {@code ServiceContextDefinition}; may
     *      be {@code null}.
     */
    public ServiceContextDefinition(PICurrentInjectable manager,
                                    ServiceContextPropagatingClientRequestInterceptor clientInterceptor,
                                    ServiceContextPropagatingServerRequestInterceptor serverInterceptor)
    {
        super();
        this.manager = manager;
        this.clientInterceptor = clientInterceptor;
        this.serverInterceptor = serverInterceptor;
    }


    /**
     * Retrieves the Service Context Manager of this {@code ServiceContextDefinition}.
     * @return
     *      The object passed as {@code manager} argument to the constructor.
     */
    public PICurrentInjectable getManager()
    {
        return manager;
    }


    /**
     * Retrieves the Client Request Interceptor of this {@code ServiceContextDefinition}.
     * @return
     *      The object passed as {@code clientInterceptor} argument to the constructor.
     */
    public ServiceContextPropagatingClientRequestInterceptor getClientInterceptor()
    {
        return clientInterceptor;
    }


    /**
     * Retrieves the Server Request Interceptor of this {@code ServiceContextDefinition}.
     * @return
     *      The object passed as {@code serverInterceptor} argument to the constructor, if any.
     */
    public ServiceContextPropagatingServerRequestInterceptor getServerInterceptor()
    {
        return serverInterceptor;
    }

}
