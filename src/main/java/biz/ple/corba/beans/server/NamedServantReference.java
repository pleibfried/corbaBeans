package biz.ple.corba.beans.server;

import java.io.UnsupportedEncodingException;

import javax.annotation.PostConstruct;

import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;

import biz.ple.corba.beans.NamingContextBean;


/**
 * <p>Encapsulates a CORBA object reference and (optionally) the COS Naming Context where
 * it is published.</p><p>The main purpose of this class is to make the creation of object
 * references and their publication in the COS Naming Service amenable to initialization
 * and configuration by the Spring Framework.</p>
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class NamedServantReference {

	private PoaBean poaBean;
    private byte[] objectId;
    private String name;
    private String idlType;
	private NamingContextBean namingCtx;
    private org.omg.CORBA.Object ref;


    /**
     * Constructor; creates a new bean fully prepared for {@link #corbaInit() initialization},
     * during which the actual object reference will be created and published in the Naming
     * Service.
     * @param poaBean
     *     A {@link PoaBean} encapsulating the POA from which the object reference will be created.
     *     This POA must then also own the Servant Activator, Servant Locator or Default Servant
     *     capable of resolving the object reference. <p>It is the caller's responsibility to ensure
     *     that the POA's policies support the creation of an object reference via
     *     {@link POA#create_reference_with_id(byte[], String) create_reference_with_id}.</p>
     *     <p>The {@code NamedServantReference} does not check this; in case the POA does not support
     *     creating an object with a user-supplied id, a CORBA exception will be thrown during
     *     initialization.</p>
     * @param idlType
     *     A string specifiying the type (IDL interface) of the object reference created during
     *     initialization.
     *     <p>The string must be in 'canonical' format, i.e. <tt>IDL:{moduleName}/{moduleName}/..../
     *     {interfaceName}:1.0}</tt>, <strong>not</strong> a Java class name.
     * @param objectId
     *     The object id that is to be embedded in the object reference created during initialization.
     *     <p>A CORBA object id is a byte array - the argument will be converted to a byte array by
     *     calling the String's {@link String#getBytes(String)} method, assumimng an UTF-8 encoding.</p>
     * @param namingCtxWrapper
     *     A {@code NamingContextBean} encapsulating the COS Naming Context where the object reference
     *     is to be published during initialization.<p>May be {@code null}, in which case
     *     the {@code name} parameter is ignored (and the object reference not published).</p>
     * @param name
     *     The name, in extended COS Naming syntax (name plus optionally a dot and a 'kind' component),
     *     under which the object reference is to be published in the Naming Context specified by
     *     {@code namingCtxWrapper}. <p>If {@code namingCtxWrapper} is not {@code null}, then
     *     {@code name} may not be {@code null} either.</p>
     * @see #corbaInit()
     */
	public NamedServantReference(PoaBean poaBean, String idlType, String objectId, NamingContextBean namingCtxWrapper, String name)
	{
		this.poaBean = poaBean;
		this.namingCtx = namingCtxWrapper;
		this.name = (name != null ? name.trim() : name);
		this.idlType = idlType;
		try {
		    this.objectId = objectId.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
		    this.objectId = objectId.getBytes();
		}
	}


	/**
	 * Equivalent to the {@link #NamedServantReference(PoaBean, String, String, NamingContextBean, String) main
	 * constructor}, with the {@code namingCtxWrapper} and {@code name} arguments set to null.
	 */
	public NamedServantReference(PoaBean poaBean, String idlType, String objectId)
	{
		this(poaBean, idlType, objectId, (NamingContextBean) null, (String) null);
	}


    /**
     * <p>Creates a CORBA reference to an object implementing the interface specified by the {@code idlType}
     * argument to the {@link #NamedServantReference(PoaBean, String, String, NamingContextBean, String)
     * constructor}. If a Naming Context bean and a name were given in the constructor, the reference is
     * published in the COS Naming Service.</p>
     * <p>In terms of the Spring Framework, this is the bean's init-method.</p>
     * <p>Note that the servant corresponding to the reference need not exist at the time of reference
     * creation. However, the POA encapsulated by the {@link PoaBean} passed to the constructor must be
     * able to provide such a servant when requested by the ORB (e.g. when an operation is invoked on the
     * reference).</p>
     * @throws Exception
     *     in case anything goes wrong, for example if the reference cannot be created due to a POA policy
     *     mismatch.
     */
	@PostConstruct
	public void corbaInit() throws Exception
	{
	    // Create the reference (without activating or instantiating any Servant object)
		POA poa = poaBean.getPoa();
		if (poaBean.getPolicyValue(PoaPolicyName.IdAssignment) != PoaPolicyValue.User) {
		    throw new IllegalArgumentException("The referenced POA must have the USER_ID ID assignment policy.");
		}
		if (poaBean.getPolicyValue(PoaPolicyName.RequestProcessing) != PoaPolicyValue.UseServantManager &&
		    poaBean.getPolicyValue(PoaPolicyName.RequestProcessing) != PoaPolicyValue.UseDefaultServant)
		{
		    throw new IllegalArgumentException("The referenced POA must have the USE_SERVANT_MANAGER or " +
		                                       "USE_DEFAULT_SERVANT request processing policy.");
		}
		ref = poa.create_reference_with_id(objectId, idlType);

		// If applicable, store the reference in the Naming Service
		if (namingCtx != null) {
			if (name == null || name.length() == 0) {
				throw new IllegalArgumentException("CosNaming names may not be blank!");
			}
			if (name.indexOf('/') != -1) {
				throw new IllegalArgumentException("No compound NS names may be used for NamedServiceObjectWrapper.");
			}
			NamingContextExt ctx = namingCtx.getNamingCtx();
			try {
				ctx.rebind(ctx.to_name(name), ref);
			} catch (NotFound | InvalidName e) {
				throw new IllegalArgumentException("Invalid NS name '" + name + "'.");
			}
		}
	}


	/**
	 * Returns the object id used to create the object reference.
	 * @return
	 *     The object id resulting from converting the string passed in the {@code objectId}
	 *     argument to the {@link #NamedServantReference(PoaBean, String, String, NamingContextBean, String)
	 *     constructor} to a byte array, using the UTF-8 encoding.
	 */
	public byte[] getObjectId()
	{
		return objectId;
	}


	/**
	 * Returns the actual object reference created during {@link #corbaInit() initialization}.
	 * @return
	 *     An reference to an object implementing the IDL interface specified in the {@code
	 *     idlType} argument passed to the
	 *     {@link #NamedServantReference(PoaBean, String, String, NamingContextBean, String)
	 *     constructor} and resolvable by the POA encapsulated by the {@link PoaBean} passed
	 *     to the constructor, if {@link #corbaInit()} has been previously invoked;
	 *     {@code null} otherwise.
	 */
	public org.omg.CORBA.Object getReference()
	{
		return ref;
	}

}
