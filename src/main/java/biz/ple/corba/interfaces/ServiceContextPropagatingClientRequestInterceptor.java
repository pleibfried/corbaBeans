package biz.ple.corba.interfaces;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ClientRequestInterceptor;


/**
 * Just 'merges' the interfaces it extends. Client Request Interceptors used to propagate service context information
 * can implement this interface and thereby inherit default methods for {@link LocalObject}.
 */
public interface ServiceContextPropagatingClientRequestInterceptor
    extends DefaultLocalObject,
            ClientRequestInterceptor,
            PICurrentInjectable
{

}
