package biz.ple.corba.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import biz.ple.corba.beans.server.NamedServantObject;


/**
 * <p>Marks a class as a CORBA servant and a Spring Bean with singleton scope.
 * An instance of the annotated class is automatically created, registered with
 * the POA identified by the {{@link #poa()} attribute and also published in
 * the CORBA Naming Service, if the {{@link #cosNamingCtx()}} and
 * {@link #cosNamingName()} attributes are not blank.</p>
 * <p>The actual bean resulting from annotating a servant implementation class
 * {@code C} with {@code @CorbaServant} is not of type {@code C}, but of type
 * {@link NamedServantObject}.</p>
 * @see NamedServantObject
 * @author Philipp Leibfried
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Component
public @interface CorbaServant
{

    /**
     * The name of the Spring bean created by instaniating the annotated class.
     */
    @AliasFor(annotation = Component.class, attribute="value")
    String beanName() default "";


    /**
     * Name of a PoaBean instance; must be configured in the application
     * context.
     */
    String poa();


    /**
     * Appropriate TIE class; if none is given, it is assumed that the servant
     * extends a skeleton class. In both cases, it is the developer's
     * responsibility to ensure that an instance of the annotated class can be
     * registered with the POA identified by {@link #poa() poa}.
     */
    Class<?> tieClass() default Void.class;


    /**
     * The name of a {@link biz.ple.corba.beans.NamingContextBean NamingContext}
     * bean which wraps a COS Naming Context; the servant created by
     * instantiating the annotated class will be registered with the wrapped COS
     * Naming Context with the name given by the {@link #cosNamingName() cosNamingName}
     * attribute.
     */
    String cosNamingCtx() default "";


    /**
     * The COS Naming name under which the servant shall be registered with the
     * COS Naming Context identified by {@link #cosNamingCtx() cosNamingCtx}.
     * The name should consist of the 'name' and 'kind' components, separated by a dot
     * (e.g. "myObject.service"). The 'kind' component may be omitted by giving a
     * name without a dot (e.g. "myKindlessObject").
     */
    String cosNamingName() default "";

    String userObjectId() default "";

}
