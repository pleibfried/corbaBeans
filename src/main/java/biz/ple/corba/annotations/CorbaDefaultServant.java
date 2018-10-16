package biz.ple.corba.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.springext.CorbaBeanFactoryPostProcessor;
import biz.ple.corba.springext.CorbaBeanPostProcessor;


/**
 * <p>Marks a class as a CORBA Default Servant and a Spring Bean with singleton scope.  An instance of the
 * annotated class is automatically created and set as the default servant of the POA encapsulated by the
 * the {@link PoaBean} identified by the {@link #poa()} attribute.</p>
 * <p>The PoaBean's policies must allow the use of a Default Servant and the annotated class must</p>
 * <ul>
 * <li>extend the {@link org.omg.PortableServer.Servant Servant} class</li>
 * <li>implement the {@link org.omg.CORBA.portable.InvokeHandler InvokeHandler} interface.
 * </ul>
 * <p>The bean resulting from this annotation will be created and initialized before the referenced
 * PoaBean is initialized.</p>
 * <hr>
 * <p><b>Note:</b> For this notation to have any effect, a {@link CorbaBeanPostProcessor}
 * and a {@link CorbaBeanFactoryPostProcessor} must be present in the Spring application context.</p>
 * <hr>
 */
@Retention(RUNTIME)
@Target(TYPE)
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public @interface CorbaDefaultServant
{
    /**
     * The name of the Spring bean created by instaniating the annotated class.
     */
    @AliasFor(annotation = Component.class, attribute="value")
    String beanName() default "";


    /**
     * <p>The name of the POA bean (instance of {@link PoaBean}) for which the instance of the annotated
     * class is to be registered as the Default Servant.</p>
     * <p>It is the responsibility of the developer that the POA's policies allow the use of a
     * Default Servant and that only one Default Servant be created/registered per POA. If any of
     * these conditions are not met, a {@link BeanInitializationException} will be thrown during
     * container initialization.</p>
     */
    String poa();

}
