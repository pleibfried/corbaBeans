package biz.ple.corba.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.interfaces.PICurrentInjectable;
import biz.ple.corba.interfaces.ServiceContextPropagatingClientRequestInterceptor;
import biz.ple.corba.interfaces.ServiceContextPropagatingServerRequestInterceptor;
import biz.ple.corba.springext.CorbaBeanFactoryPostProcessor;
import biz.ple.corba.springext.CorbaBeanPostProcessor;


/**
 * <p>Marks a class as a "Service Context Manager" and a Spring Bean with singleton scope. If the
 * {@link #serviceName() serviceName} attribute is non-empty, the annotated class becomes part of
 * a 'service definition' and must must implement the {@link PICurrentInjectable} interface.</p>
 * <p>The annotated class is part of the service definition implied and identified by the
 * {@link #serviceName} attribute. A service definition includes an instance of at least
 * one of:</p>
 * <ul>
 * <li>{@link ServiceContextPropagatingClientRequestInterceptor}</li>
 * <li>{@link ServiceContextPropagatingServerRequestInterceptor}</li>
 * <li>{@link PICurrentInjectable}</li>
 * </ul>
 * <p>and at most exactly one instance of each of the above. A service definition is implied by annotating
 * classes with</p>
 * <ul>
 * <li>{@link CorbaClientInterceptor @CorbaClientInterceptor}</li>
 * <li>{@link CorbaServerInterceptor @CorbaServerInterceptor}</li>
 * <li>{@code CorbaServiceContextManager}
 * </ul>
 * <p>where each annotation has the same value for the {@code serviceName} attribute.</p>
 * <p>A service definition containing only one of the above does not make much sense, as the purpose
 * of a service definition is the sharing of slot ids allocated by the ORB at initialization time.
 * It is technically possible though.</p>
 * <p>If you want an instance of the annotated class to be registered as a 'standalone' interceptor,
 * which does not need a slot ID or portable interceptor current, leave the {@code serviceName}
 * attribute blank.</p>
 * <p>The bean resulting from this annotation will be created and initialized before the referenced
 * OrbBean is initialized.</p>
 * <hr>
 * <p><b>Note:</b> For this notation to have any effect, a {@link CorbaBeanPostProcessor}
 * and a {@link CorbaBeanFactoryPostProcessor} must be present in the Spring application context.</p>
 * <hr>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public @interface CorbaServiceContextManager
{
    /**
     * The name of the Spring bean created by instaniating the annotated class.
     */
    @AliasFor(annotation = Component.class, attribute="value")
    String beanName() default "";

    /**
     * The name of the {@link OrbBean} with which an instance of the annotated class is to be injected
     * during the ORB initialization phase. This should be the same as for the request interceptors of
     * the service definition identified by {@link #serviceName() serviceName}. If this attribute is
     * blank, and exactly one OrbBean instance is registered with the application context, then the
     * instance will be injected with that unique OrbBean; otherwise,
     * a {@link BeanDefinitionValidationException} will be thrown.
     */
    String orb() default "";

    /**
     * <p>Identifies the 'service definition' of which the annotated class is a part. If it
     * is blank, the annotated class is not considered part of any service definition.</p>
     * <p>A service definition is 'created' simply by setting this attribute to the same,
     * non-blank value in at least one annotation of type</p>
     * <ul>
     * <li>{@code @CorbaClientInterceptor}</li>
     * <li>{@link CorbaServerInterceptor @CorbaServerInterceptor}</li>
     * <li>{@link CorbaServiceContextManager @CorbaServiceContextManager}</li>
     * </ul>
     */
    String serviceName();
}
