package biz.ple.corba.interfaces;

import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.DomainManager;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.Object;
import org.omg.CORBA.Policy;
import org.omg.CORBA.Request;
import org.omg.CORBA.SetOverrideType;


/**
 * <p>An extension of the {@link org.omg.CORBA.Object} interface which adds default
 * methods for all of {@code Object}'s methods, assuming that the {@code Object}
 * is local (most methods just throw {@link org.omg.CORBA.NO_IMPLEMENT NO_IMPLEMENT}).</p>
 * <p>A local object class can 'inherit' these default methods without having to extend a
 * class.</p>
 * @author Philipp Leibfried
 * @since  1.0.0
 */
public interface DefaultLocalObject extends org.omg.CORBA.Object {

    public static final String NO_IMPL_REASON = "This is a locally constrained object.";

    @Override
    default boolean _is_a(String repositoryIdentifier)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default boolean _is_equivalent(Object other)
    {
        return equals(other);
    }

    @Override
    default boolean _non_existent()
    {
        return false;
    }

    @Override
    default int _hash(int maximum)
    {
        return hashCode();
    }

    @Override
    default Object _duplicate()
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default void _release()
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Object _get_interface_def()
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Request _request(String operation)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Request _create_request(Context ctx, String operation, NVList arg_list, NamedValue result)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Request _create_request(Context ctx, String operation, NVList arg_list, NamedValue result, ExceptionList exclist,
                    ContextList ctxlist)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Policy _get_policy(int policy_type)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default DomainManager[] _get_domain_managers()
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

    @Override
    default Object _set_policy_override(Policy[] policies, SetOverrideType set_add)
    {
        throw new org.omg.CORBA.NO_IMPLEMENT(NO_IMPL_REASON);
    }

}
