package biz.ple.corba.beans.server;

import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;


/**
 * <p>Expresses the names of POA Policies as symbolic values, enabling a more intuitive
 * way of specifiying POA Policy sets.</p>
 * <p>In the CORBA Java binding, the POA Policy names only appear as class names and
 * in method names, never as strings or enums.</p>
 * @author Philipp Leibfried
 * @since  1.0.0
 * @see PoaPolicies
 */
public enum PoaPolicyName {

    IdAssignment(IdAssignmentPolicyValue.class),
    IdUniqueness(IdUniquenessPolicyValue.class),
    ImplicitActivation(ImplicitActivationPolicyValue.class),
    Lifespan(LifespanPolicyValue.class),
    RequestProcessing(RequestProcessingPolicyValue.class),
    ServantRetention(ServantRetentionPolicyValue.class),
    Thread(ThreadPolicyValue.class);

    private Class<?> omgValueClass;

    private PoaPolicyName(Class<?> ovClass)
    {
        this.omgValueClass = ovClass;
    }

    /**
     * Returns the CORBA binding's PolicyValue class corresponding to the policy name.
     * @return
     *      The PolicyValue class corresponding to the policy name represented by the enum value.
     */
    public Class<?> toOmgClass()
    {
        return omgValueClass;
    }

}
