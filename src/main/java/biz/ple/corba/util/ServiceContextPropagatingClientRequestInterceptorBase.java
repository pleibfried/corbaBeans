package biz.ple.corba.util;

import biz.ple.corba.beans.ServiceContextDefinition;
import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingClientRequestInterceptor;


/**
 * Base class for Client Request Interceptors which are part of a {@link ServiceContextDefinition}.
 * <p>Inhterits an implementation of the {@link PICurrentInjectable} interface and the methods
 * of a local CORBA object (via default methods).</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 */
public abstract class ServiceContextPropagatingClientRequestInterceptorBase
    extends PICurrentInjectableImpl
    implements ServiceContextPropagatingClientRequestInterceptor
{

}
