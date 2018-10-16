package biz.ple.corba.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * <p>Use this class and it's init-method to use CORBA object references retrieved from
 * the COS Naming Service as Spring beans.</p>
 *
 * @author Philipp Leibfried
 * @since 1.0.0
 *
 */
public class NamedReferenceLookup {

    /**
     * <p>Looks up an object reference in the COS Naming Service, narrows (casts) it to the desired type
     * and returns it</p>
     * <p>In terms of the Spring Framework, this is the bean's init-method.</p>
     * @param namingCtx
     *      A {@code NamingContextBean} encapsulating the COS Naming Context where the {@code name} is
     *      to be looked up.
     * @param name
     *      A COS Naming name in extended syntax (name and kind components separated by a dot, where
     *      the dot and kind parts are optional).
     * @param interfaceClass
     *      The expected type of the reference identified by {@code name} in the Naming
     *      Context {@code namingCtx}.
     * @return
     *      The object reference mapped to the name {@code name} in Naming Context {@code namingCtx}, cast
     *      to the type {@code T}. {@code Null} if the name and/or Naming Context does not exist, or the
     *      objecte reference stored there is not of type {@code T}.
     * @throws Throwable
     */
    public static <T> T lookup(NamingContextBean namingCtx, String name, Class<T> interfaceClass) throws Throwable
    {
        Class<?> helperClass = null;
        try {
            helperClass = Class.forName(interfaceClass.getName() + "Helper");
        } catch (ClassNotFoundException cnf) {
            throw new IllegalArgumentException("Class " + interfaceClass.getName() + "Helper does not seem to exist.");
        }
        Object obj = namingCtx.lookup(name);
        Method caster;
        try {
            // We have to use narrow() here because simple casting is not enough for references to remote objects
            caster = helperClass.getDeclaredMethod("narrow", org.omg.CORBA.Object.class);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + helperClass.getName() + " does not have a method 'narrow(org.omg.CORBA.Object)'.");
        }
        try {
            return interfaceClass.cast(caster.invoke(null, obj));
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Method narrow(org.omg.CORBA.Object) seems not to be accessible.");
        }
        catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }


}
