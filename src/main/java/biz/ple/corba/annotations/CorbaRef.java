package biz.ple.corba.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.BeanInitializationException;

import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.springext.CorbaBeanPostProcessor;


/**
 * <p>Marks a field or setter method as a CORBA object reference. Annotating a field
 * or a method with exactly one parameter with @CorbaRef will cause the field resp.
 * the method's parameter to be replaced with the result of a COS Naming lookup, if
 * said result can be narrowed (cast) to the type of the field/parameter. If follows
 * from this that the type of the field or parameter should be a class representing
 * (usually generated from) an OMG IDL interface.</p>
 * <p>If the lookup cannot be performed, yields nothing, or the result cannot be
 * cast to the expected type, a {@link BeanInitializationException} is thrown.</p>
 * <hr>
 * <p><b>Note:</b> For this notation to have any effect, a {@link CorbaBeanPostProcessor}
 * must be present in the Spring application context.</p>
 * <hr>
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface CorbaRef
{

    /**
     * <p>The COS Naming lookup path of the (remote) object being referenced. The path
     * must be in extended COS naming syntax, i.e. name components separated by
     * forward slashes, 'name' and 'kind' of a name component separated by a dot.
     * Where no dot is present, it is assumed that 'kind' is the empty string.</p>
     * <p>For example, the path might look like this:<br>
     * {@code applications.ctx/myLittleApp/myService.obj}</p>
     */
    String cosNamingPath();


    /**
     * <p>The id of a {@link biz.ple.corba.beans.NamingContextBean NamingContext} bean
     * encapsulating the COS Naming Context which is to be used as the root of
     * the COS Naming path given in {@link #cosNamingPath() cosNamingPath}.</p>
     * <p>If this attribute is empty, and there is only exactly one bean of type
     * {@link OrbBean} present in the application context, the Naming Context
     * returned by that OrbBean's {@link OrbBean#getRootNamingCtx() getRootNamingCtx()}
     * method is used as the root of {@code cosNamingPath}; otherwise, a
     * {@link BeanInitializationException} is thrown.</p>
     */
    String rootCtxBean() default "";


    /**
     * Indicates whether a failed lookup is acceptable, in which case {@code null}
     * will be injected into the annotated field or method.
     */
    boolean optional() default false;

}
