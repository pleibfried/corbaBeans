package biz.ple.corba.beans.server;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.omg.CORBA.Policy;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.CurrentHelper;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.ImplicitActivationPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantActivator;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantRetentionPolicyValue;
import org.omg.PortableServer.ThreadPolicyValue;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.interfaces.PoaInjectable;


/**
 * Encapsulates a Portable Object Adapter (POA). The purpose of the {@code PoaBean} is twofold:
 * it simplifies application interaction with the POA and makes it amenable to initialization and
 * wiring using the DI/IoC mechanisms of the Spring Framework.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class PoaBean {

    private String myPoaName;
    private PoaPolicies myPolicies;
    private PoaBean parentPoaBean;
    private OrbBean myOrb;
    private POA myPoa;
    private ServantActivator activator;
    private ServantLocator locator;
    private Servant defSrv;
    private boolean useParentPoaMgr;
    private boolean created;


    /**
     * Constructor; creates a new PoaBean ready for {@link #corbaInit() initialization}, during
     * which the encapsulated POA will be created and initialized (but not activated).
     * @param parentPoaBean
     *      A {@code PoaBean} encapsulating the new POA's parent POA. May not be {@code null}.
     *      <p>For 'top-level' POAs, use the {@code PoaBean} encapsulating the Root POA, which
     *      can be obtained by calling {@link OrbBean#getRootPoa()}.
     * @param name
     *      The name of the new POA; may not be {@code null}.
     * @param policies
     *      The POA policies of the new POA; may not be {@code null}.
     * @throws IllegalArgumentException
     *      if the {@code parentPoaBean} argument is null.
     */
    public PoaBean(PoaBean parentPoaBean, String name, PoaPolicies policies)
    {
        if (parentPoaBean == null) {
            throw new IllegalArgumentException("Parent POA (bean) may not be null!");
        }
        this.parentPoaBean = parentPoaBean;
        this.myOrb = parentPoaBean.getOrbBean();
        if (name == null) {
            throw new IllegalArgumentException("POA name may not be null.");
        }
        this.myPoaName = name;
        if (policies == null) {
            throw new IllegalArgumentException("POA policies may not be null.");
        }
        this.myPolicies = policies;
        this.myPoa = null;
        this.useParentPoaMgr = true;
        this.created = false;
    }


    /** @deprecated For use by {@link #OrbBean} only. */
    @Deprecated
    public PoaBean(POA rootPoa, OrbBean theOrb)
    {
    	this.parentPoaBean = null;
    	this.myOrb = theOrb;
    	this.myPoaName="RootPOA";
    	this.myPoa = rootPoa;
    	this.useParentPoaMgr = true;
    	this.created = true;
    	this.myPolicies = new PoaPolicies();
    	myPolicies.setPolicy(PoaPolicyName.Lifespan, PoaPolicyValue.Transient);
    	myPolicies.setPolicy(PoaPolicyName.Thread, PoaPolicyValue.OrbCtrlModel);
    	myPolicies.setPolicy(PoaPolicyName.IdAssignment, PoaPolicyValue.System);
    	myPolicies.setPolicy(PoaPolicyName.ServantRetention, PoaPolicyValue.Retain);
    	myPolicies.setPolicy(PoaPolicyName.RequestProcessing, PoaPolicyValue.UseActiveObjectMapOnly);
    	myPolicies.setPolicy(PoaPolicyName.ImplicitActivation, PoaPolicyValue.NoImplicitActivation);
    	myPolicies.setPolicy(PoaPolicyName.IdUniqueness, PoaPolicyValue.Unique);
    }


    /**
     * <p>Creates and initializes the POA encapsulated by this {@code PoaBean}, with the policies passed to
     * the {@code policies} argument of the {@link #PoaBean(PoaBean, String, PoaPolicies) constructor}.
     * Immediately after creation, the POA's Servant Locator, Servant Activator or Default Servant is set if
     * required by the POA policies. These must be set <i>before</i> {@code corbaInit()} is called.</p>
     * <p>If the Servant Locator, Servant Activator or Default Servant implements the {@link PoaInjectable}
     * interface, it is then injected with this {@code PoaBean}.</p>
     * <p>By default, the encapsulated POA is activated only when the root POA is activated, i.e.
     * {@link OrbBean#start() start()} is invoked on the OrbBean encapsulating the ORB whose RootPoa is at
     * the top of the hierarchy to which the new POA belongs. This behaviour can be changed by invoking
     * {@link #setUseParentPoaManager(boolean)} with an argument value of {@code false}.</p>
     * <p>In terms of the Spring Framework, this is the bean's init-method.</p>
     * @throws AdapterAlreadyExists
     *      if the partne POA of the new POA already has a child POA with the same name as the name passed
     *      to the constructor.
     * @throws InvalidPolicy
     *      if the policy mix passed to the constructor is invalid or does not make sense.
     * @throws IllegalArgumentException
     *      if POA Policies require the use of a Servant Locator, Servant Activator or Default Servant, and
     *      a Servant Locator, Servant Activator or Default Servant has not been set via the respective
     *      setter methods.
     * @see #setDefaultServant(Servant)
     * @see #setServantLocator(ServantLocator)
     * @see #setServantActivator(ServantActivator)
     * @see #setUseParentPoaManager(boolean)
     */
    @PostConstruct
    public void corbaInit() throws AdapterAlreadyExists, InvalidPolicy
    {
        if (parentPoaBean == null) {
            throw new IllegalArgumentException("Parent POA (bean) may not be null.");
        }

        // Create the policies for the new POA
        POA parentPoa = parentPoaBean.getPoa();
        Policy[] poaPolicies = new Policy[myPolicies.size()];
        int index = 0;
        for (Entry<PoaPolicyName, PoaPolicyValue> entry: myPolicies.entrySet()) {
        	int intValue = entry.getValue().intValue();
            switch(entry.getKey()) {
            case IdAssignment:
                poaPolicies[index++] = parentPoa.create_id_assignment_policy(IdAssignmentPolicyValue.from_int(intValue));
                break;
            case IdUniqueness:
                poaPolicies[index++] = parentPoa.create_id_uniqueness_policy(IdUniquenessPolicyValue.from_int(intValue));
                break;
            case ImplicitActivation:
                poaPolicies[index++] = parentPoa.create_implicit_activation_policy(ImplicitActivationPolicyValue.from_int(intValue));
                break;
            case Lifespan:
                poaPolicies[index++] = parentPoa.create_lifespan_policy(LifespanPolicyValue.from_int(intValue));
                break;
            case RequestProcessing:
                poaPolicies[index++] = parentPoa.create_request_processing_policy(RequestProcessingPolicyValue.from_int(intValue));
                break;
            case ServantRetention:
                poaPolicies[index++] = parentPoa.create_servant_retention_policy(ServantRetentionPolicyValue.from_int(intValue));
                break;
            case Thread:
                poaPolicies[index++] = parentPoa.create_thread_policy(ThreadPolicyValue.from_int(intValue));
                break;
            }
        }

        // Create the new POA
        POAManager parentMgr = null;
        if (useParentPoaMgr) {
            parentMgr = parentPoa.the_POAManager();
        }
        myPoa = parentPoa.create_POA(myPoaName, parentMgr, poaPolicies);
        created = true;

        // Set ServantManager or Default Servant as appropriate
        if (myPolicies.getPolicyValue(PoaPolicyName.RequestProcessing) == PoaPolicyValue.UseServantManager) {
            if (myPolicies.getPolicyValue(PoaPolicyName.ServantRetention) == PoaPolicyValue.Retain) {
                if (activator != null) {
                    try {
                        myPoa.set_servant_manager(activator);
                        if (activator instanceof PoaInjectable) {
                            ((PoaInjectable) activator).setPoaBean(this);
                        }
                    } catch (WrongPolicy wp) {
                        throw new IllegalArgumentException("Internal error (WrongPolicy while setting the Servant Activator).");
                    }
                } else {
                    throw new IllegalArgumentException(
                        "With the USE_SERVANT_MANAGER and RETAIN policies, you need to specify a non-null ServantActivator.");
                }
            }
            else {
                if (locator != null) {
                    try {
                        myPoa.set_servant_manager(locator);
                        if (locator instanceof PoaInjectable) {
                            ((PoaInjectable) locator).setPoaBean(this);
                        }
                    } catch (WrongPolicy wp) {
                        throw new IllegalArgumentException("Internal error (WrongPolicy while setting the Servant Locator).");
                    }
                } else {
                    throw new IllegalArgumentException(
                        "With the USE_SERVANT_MANAGER and NON_RETAIN policies, you need to specify a non-null ServantLocator.");
                }
            }
        }
        else
        if (myPolicies.getPolicyValue(PoaPolicyName.RequestProcessing) == PoaPolicyValue.UseDefaultServant) {
            if (defSrv != null) {
                try {
                    myPoa.set_servant(defSrv);
                    // If the default servant implements the PoaInjectable interface, inject the PoaBean
                    if (defSrv instanceof PoaInjectable) {
                        ((PoaInjectable) defSrv).setPoaBean(this);
                    }
                }
                catch (WrongPolicy e) {
                    throw new IllegalArgumentException("RequestProcessingPolicy and default servant instance do not match.");
                }
            } else {
                throw new IllegalArgumentException("With the USE_DEFAULT_SERVANT policy, you must specify a non-null default servant.");
            }
        }
    }


    /**
     * Determines whether the encapsulated POA is managed by its parent POA's POAManager (the default) or not.
     * Must be invoked <strong>before</strong> {@link #corbaInit()} to have an effect.
     * @param useParent
     *      If {@code true}, the encapsulated POA is managed by its parent POA's POAManager (default). If set to
     *      {@code false}, no POAManager is set for the encapsulated POA in {@link #corbaInit()}; to set a
     *      POAManager for the encapsulated POA, retrieve the POA via {@link #getPoa()} and set the POAManager
     *      using standard CORBA APIs.
     */
    public void setUseParentPoaManager(boolean useParent)
    {
        this.useParentPoaMgr = useParent;
    }


    /**
     * Sets the Servant Locator for the encapsulated POA. Must be invoked <strong>before</strong> {@code #corbaInit()}.
     * @param locator
     *      The POA's Servant Locator; if the POA's policies do not require use of a Servant Locator, the method
     *      call will have no effect (and {@code locator} will not be used by the encapsulated POA).
     */
    public void setServantLocator(ServantLocator locator)
    {
        if (created) {
            throw new IllegalStateException("Servant Locator cannot be set after POA bean initialization.");
        }
        this.locator = locator;
    }


    /**
     * Sets the Servant Activator for the encapsulated POA. Must be invoked <strong>before</strong> {@code #corbaInit()}.
     * @param locator
     *      The POA's Servant Activator; if the POA's policies do not require use of a Servant Activator, the method
     *      call will have no effect (and {@code activator} will not be used by the encapsulated POA).
     */
    public void setServantActivator(ServantActivator activator)
    {
        if (created) {
            throw new IllegalStateException("Servant Activator cannot be set after POA bean initialization.");
        }
        this.activator = activator;
    }


    /**
     * Sets the Default Servant for the encapsulated POA. Must be invoked <strong>before</strong> {@code #corbaInit()}.
     * @param locator
     *      The POA's Default Servant; if the POA's policies do not require use of a Default Servant, the method
     *      call will have no effect (and {@code defSrv} will not be used by the encapsulated POA).
     */
    public void setDefaultServant(Servant defSrv)
    {
        if (created) {
            throw new IllegalStateException("Default servant cannot be set after POA bean initialization.");
        }
        this.defSrv = defSrv;
    }


    /**
     * Returns the POA encapsulated by this {@code PoaBean}.
     * @return
     *      The POA encapsulated by this {@code PoaBean}, if {@link #corbaInit()} has previously been called
     *      successfully; {@code null} otherwise.
     */
    public POA getPoa()
    {
        return myPoa;
    }


    public Current getPoaCurrent()
    {
        try {
            return CurrentHelper.narrow(myOrb.getORB().resolve_initial_references("POACurrent"));
        }
        catch (InvalidName iv) {
            throw new RuntimeException("POACurrent seemd not to be understood by the ORB implementation used.");
        }
    }


    /**
     * Returns the OrbBean to which this PoaBean 'belongs'. The value returned is never {@code null},
     * although the encapsulated ORB may be {@code null} as long as the OrbBean is not fully
     * {@link OrbBean#corbaInit() initialized} yet.
     * @return
     *      An {@code OrbBean} encapsulating the ORB whose RootPOA is at the top of the hierarchy
     *      to which the encapsulated POA belongs; never {@code null}.
     */
    public OrbBean getOrbBean()
    {
        return myOrb;
    }


    /**
     * Returns the value of a specific POA Policy as applied to the encapsulated POA.
     * @param policyName
     *      The name of the policy to be queried.
     * @return
     *      The value of the POA policy identified by {@code policyName}, as applied to
     *      the encapsulated POA (i.e. as passed to this {@code PoaBean}'s constructor).
     */
    public PoaPolicyValue getPolicyValue(PoaPolicyName policyName)
    {
    	return myPolicies.getPolicyValue(policyName);
    }


    /**
     * Creates an object reference of a certain type with a user-supplied Object Id, 'bound' to the
     * encapsulated POA (i.e. the encapsulated POA must be able to resolve that reference).
     * @param objectId
     *      The user-supplied object id; will be converted to a byte array assuming that the string
     *      is UTF-8 encoded. The resulting byte array will be 'embedded' in the resulting object reference.
     * @param interfaceClass
     *      The Java class of an interface generated by the IDL compiler. The resulting object reference
     *      will be 'narrowed' (cast) to this type.
     * @return
     *      An reference to an object of type {@code interfaceClass} with the id {@code objectId}, 'pointing'
     *      to the encapsulated POA (meaning the encapsulated POA must be able to resolve this reference).
     */
    public <T extends org.omg.CORBA.Object> T createObjectReference(String objectId, Class<T> interfaceClass)
    {
        try {
            Class<?> helperClass = Class.forName(interfaceClass.getName() + "Helper");
            String corbaInterfaceId = (String) (helperClass.getDeclaredMethod("id").invoke(null));
            org.omg.CORBA.Object ref = myPoa.create_reference_with_id(objectId.getBytes("UTF-8"), corbaInterfaceId);
            return interfaceClass.cast(helperClass.getDeclaredMethod("narrow", org.omg.CORBA.Object.class).invoke(null, ref));
        }
        catch (ClassNotFoundException cnf) {
            throw new RuntimeException("Helper class " + interfaceClass.getName() + "Helper not found.");
        }
        catch (UnsupportedEncodingException uex) {
            throw new RuntimeException("ObjectId string must be UTF-8 encoded, and UTF-8 must be supported by your platform.");
        }
        catch (IllegalAccessException | IllegalArgumentException |
               InvocationTargetException | NoSuchMethodException | SecurityException xcp) {
            throw new RuntimeException("createObjectReference(" + objectId + ", " + interfaceClass.getName() + ") failed.", xcp);
        }
    }


    /**
     * Creates an object reference of a certain type with a user-supplied Object Id, 'bound' to the
     * encapsulated POA (i.e. the encapsulated POA must be able to resolve that reference).
     * @param objectId
     *      The user-supplied object id; will be 'embedded' in the resulting object reference.
     * @param interfaceClass
     *      The Java class of an interface generated by the IDL compiler. The resulting object reference
     *      will be 'narrowed' (cast) to this type.
     * @return
     *      An reference to an object of type {@code interfaceClass} with the id {@code objectId}, 'pointing'
     *      to the encapsulated POA (meaning the encapsulated POA must be able to resolve this reference).
     */
    public <T extends org.omg.CORBA.Object> T createObjectReference(byte[] objectId, Class<T> interfaceClass)
    {
        try {
            Class<?> helperClass = Class.forName(interfaceClass.getName() + "Helper");
            String corbaInterfaceId = (String) (helperClass.getDeclaredMethod("id").invoke(null));
            org.omg.CORBA.Object ref = myPoa.create_reference_with_id(objectId, corbaInterfaceId);
            return interfaceClass.cast(helperClass.getDeclaredMethod("narrow", org.omg.CORBA.Object.class).invoke(null, ref));
        }
        catch (ClassNotFoundException cnf) {
            throw new RuntimeException("Helper class " + interfaceClass.getName() + "Helper not found.");
        }
        catch (IllegalAccessException | IllegalArgumentException |
               InvocationTargetException | NoSuchMethodException | SecurityException xcp) {
            throw new RuntimeException("createObjectReference(" + objectId + ", " + interfaceClass.getName() + ") failed.", xcp);
        }
    }


    /**
     * Creates an object reference of a certain type with a system-created Object Id, 'bound' to the
     * encapsulated POA (i.e. the encapsulated POA must be able to resolve that reference).
     * @param interfaceClass
     *      The Java class of an interface generated by the IDL compiler. The resulting object reference
     *      will be 'narrowed' (cast) to this type.
     * @return
     *      An reference to an object of type {@code interfaceClass}, 'pointing' to the encapsulated POA
     *      (meaning the encapsulated POA must be able to resolve this reference).
     */
    public <T extends org.omg.CORBA.Object> T createObjectReference(Class<T> interfaceClass)
    {
        try {
            Class<?> helperClass = Class.forName(interfaceClass.getName() + "Helper");
            String corbaInterfaceId = (String) (helperClass.getDeclaredMethod("id").invoke(null));
            org.omg.CORBA.Object ref = myPoa.create_reference(corbaInterfaceId);
            return interfaceClass.cast(helperClass.getDeclaredMethod("narrow", org.omg.CORBA.Object.class).invoke(null, ref));
        }
        catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | WrongPolicy |
               InvocationTargetException | NoSuchMethodException | SecurityException xcp ) {
            throw new RuntimeException("createObjectReference(" + interfaceClass.getName() + ") failed.", xcp);
        }
    }

}
