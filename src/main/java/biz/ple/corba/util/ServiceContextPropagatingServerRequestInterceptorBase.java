package biz.ple.corba.util;

import biz.ple.corba.beans.ServiceContextDefinition;
import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingServerRequestInterceptor;


/**
 * Base class for Server Request Interceptors which are part of a {@link ServiceContextDefinition}.
 * <p>Inhterits an implementation of the {@link PICurrentInjectable} interface and the methods
 * of a local CORBA object (via default methods).</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 */
public abstract class ServiceContextPropagatingServerRequestInterceptorBase
    extends PICurrentInjectableImpl
    implements ServiceContextPropagatingServerRequestInterceptor
{

}
