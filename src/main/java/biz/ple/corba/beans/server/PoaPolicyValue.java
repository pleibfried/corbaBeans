package biz.ple.corba.beans.server;

import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;


/**
 * <p>Expresses the values of POA Policies as symbolic values, enabling a more intuitive
 * way of specifiying POA Policy sets.</p>
 * <p>In the CORBA Java binding, the POA Policy Values only appear as class names and
 * in method names, never as strings or enums.</p>
 * @author Philipp Leibfried
 * @since  1.0.0
 * @see PoaPolicies
 */
public enum PoaPolicyValue {

    System("_SYSTEM_ID", IdAssignmentPolicyValue._SYSTEM_ID),
    User("_USER_ID", IdAssignmentPolicyValue._USER_ID),
    Multiple("_MULTIPLE_ID", IdUniquenessPolicyValue._MULTIPLE_ID),
    Unique("_UNIQUE_ID", IdUniquenessPolicyValue._UNIQUE_ID),
    ImplicitActivation("_IMPLICIT_ACTIVATION", ImplicitActivationPolicyValue._IMPLICIT_ACTIVATION),
    NoImplicitActivation("_NO_IMPLICIT_ACTIVATION", ImplicitActivationPolicyValue._NO_IMPLICIT_ACTIVATION),
    Transient("_TRANSIENT", LifespanPolicyValue._TRANSIENT),
    Persistent("_PERSISTENT", LifespanPolicyValue._PERSISTENT),
    UseActiveObjectMapOnly("_USE_ACTIVE_OBJECT_MAP_ONLY", RequestProcessingPolicyValue._USE_ACTIVE_OBJECT_MAP_ONLY),
    UseServantManager("_USE_SERVANT_MANAGER", RequestProcessingPolicyValue._USE_SERVANT_MANAGER),
    UseDefaultServant("_USE_DEFAULT_SERVANT", RequestProcessingPolicyValue._USE_DEFAULT_SERVANT),
    Retain("_RETAIN", ServantRetentionPolicyValue._RETAIN),
    NonRetain("_NON_RETAIN", ServantRetentionPolicyValue._NON_RETAIN),
    OrbCtrlModel("_ORB_CTRL_MODEL", ThreadPolicyValue._ORB_CTRL_MODEL),
    SingleThreadModel("_SINGLE_THREAD_MODEL", ThreadPolicyValue._SINGLE_THREAD_MODEL);

    private final String memberName;
    private int intValue;


    private PoaPolicyValue(String strValue, int intValue)
    {
        this.memberName = strValue;
        this.intValue = intValue;
    }


    /**
     * The name of the (static final) field (of the corresponding CORBA Policy Value class)
     * containig the int value corresponding to this {@code PoaPolicyValue}.
     * @return
     *      The name of the field in the corresponding CORBA Policy Value class containing
     *      the int value corresponding to this {@code PoaPolicyValue}.
     */
    public String toMemberName()
    {
        return memberName;
    }


    /**
     * Returns the integer value of the PoaPolicyValue.
     * @return
     *      The integer value of the POA Policy Value, as specified in the CORBA Specification.
     */
    public int intValue()
    {
        return intValue;
    }

}
