package biz.ple.corba.interfaces;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Just 'merges' the interfaces it extends. Server Request Interceptors used to propagate service context information
 * can implement this interface and thereby inherit default methods for {@link LocalObject}.
 */
public interface ServiceContextPropagatingServerRequestInterceptor
    extends DefaultLocalObject,
            ServerRequestInterceptor,
            PICurrentInjectable
{

}
