package biz.ple.corba.beans.server;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.ple.corba.beans.NamingContextBean;


/**
 * <p>Encapsulates a servant and (optionally) the COS Naming path of the object reference.</p>
 * <p>The purpose of this bean class is mainly to make the activation of CORBA Servants and
 * their publication in the COS Naming Service amenable to initialization and configuration
 * by the Spring Framework mechanisms.</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class NamedServantObject {

    private static final Logger LOG = LoggerFactory.getLogger(NamedServantObject.class);

	private PoaBean poaBean;
	private NamingContextBean namingCtx;
	private String name;
	Class<?> tieClass;
	private Object servant;
	private byte[] userObjectId;
	private byte[] objectId;
	private org.omg.CORBA.Object ref;


	/**
	 * Constructor; creates a new bean fully prepared for {@link #corbaInit() initialization}.
	 * How and if the encapsulated servant is activated and registered with the Naming Service
	 * depends on the arguments.
	 * @param poaBean
	 *     A {@link PoaBean} encapsulating the POA with which the {@code servant} is to be
	 *     activated. It is the caller's responsibility to ensure that the POA's policies
	 *     support activating an object using {@link POA#activate_object(Servant) activate_object()}
	 *     or {@link POA#activate_object_with_id(byte[], Servant) artivate_object_with_id()}.
	 *     <p>The {@code NamedServantObject} does not check this; in case the POA does not support
	 *     activating an object, a CORBA exception will be thrown during initialization..</p>
	 * @param tieClass
	 *     The class of the TIE object which will be instantiated and activated, with the {@code servant}
	 *     as delegate, upon initialization.
	 *     <p>It is the caller's responsibility to ensure that the class given here be a real TIE class
	 *     (usually generated by the IDL compiler) and that it matches the {@code servant} (i.e. the
	 *     servant implements the interface necessary for acting as the TIE instance's delegate).
	 *     <p>This argument may be {@code null}, in which case it is assumed that {@code servant} is
	 *     an 'direct' Servant (e.g. by extending a class generated by the IDL compiler).</p>
	 * @param servant
	 *     The servant to be activated and (optionally) registered with the Naming Service on
	 *     initialization; may <strong>not</strong> be {@code null}.
	 *     <p>The object passed as an argument must either be a CORBA Servant (e.g. by extending the
	 *     appropriate generated class) or implement the necessary (generated) interface to serve as
	 *     a delegate, in which case {@code tieClass} may not be {@code null}.</p>
	 * @param namingCtxWrapper
	 *     A {@code NamingContextBean} encapsulating the COS Naming Context where an reference to the
	 *     activated servant is to be published on initialization.<p>May be {@code null}, in which case
	 *     the {@code name} parameter is ignored (and the reference not published).</p>
	 * @param name
	 *     The name, in extended COS Naming syntax (name plus optionally a dot and a 'kind' component),
	 *     under which a reference to the activated servant is to be published in the Naming Context
	 *     specified by {@code namingCtxWrapper}. <p>If {@code namingCtxWrapper} is not {@code null}, then
	 *     {@code name} may not be {@code null} either.</p>
	 * @see #corbaInit()
	 */
	public NamedServantObject(PoaBean poaBean, Class<?> tieClass, Object servant,
			                  NamingContextBean namingCtxWrapper, String name)
	{
		this.poaBean = poaBean;
		this.namingCtx = namingCtxWrapper;
		this.tieClass = tieClass;
		this.servant = servant;
		this.name = (name != null ? name.trim() : name);
		this.objectId = null;
	}


	/**
	 * Equivalent to the {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) main
	 * constructor}, with the {@code tieClass} argument set to {@code null}.
	 */
	public NamedServantObject(PoaBean poaBean, Object servant, NamingContextBean namingCtxWrapper, String name)
	{
		this(poaBean, (Class<?>) null, servant, namingCtxWrapper, name);
	}


    /**
     * Equivalent to the {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) main
     * constructor}, with the {@code namingCtxWrapper} and {@code name} arguments set to {@code null}.
     */
	public NamedServantObject(PoaBean poaBean, Class<?> tieClass, Object servant)
	{
		this(poaBean, tieClass, servant, (NamingContextBean) null, (String) null);
	}


    /**
     * Equivalent to the {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) main
     * constructor}, with the {@code tieClass}, {@code namingCtxWrapper} and {@code name} arguments set to
     * {@code null}.
     */
	public NamedServantObject(PoaBean poaBean, Object servant)
	{
		this(poaBean, (Class<?>) null, servant, (NamingContextBean) null, (String) null);
	}


	/**
	 * Sets the object id to be used when activating the Servant or TIE object given in the
	 * {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) constructor}.
	 * <p>This is used only if the POA encapsulated by the {@link PoaBean} given in the constructor
	 * has its {@link PoaPolicyName#IdAssignment ID Assignment} policy set to
	 * {@link PoaPolicyValue#User User}. If that is not the case, setting the object id has no
	 * effect.</p>
	 * <p>If that is the case, however, the object id <i>must</i> be set to a non-null value using
	 * this method, before {@link #corbaInit()} is called.</p>
	 * @param userObjectId
	 *     The object id to be used when activating the Servant or TIE object during initialization.
	 */
	public void setUserObjectId(byte[] userObjectId)
	{
		this.userObjectId = userObjectId;
	}


	/**
	 * Activates the Servant/TIE object passed in the
	 * {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) constructor}. If
	 * a Naming Context bean and a name were given in the constructor, a reference to the activated
	 * servant is stored in the Naming Service.
	 * <p>In terms of the Spring Framework, this is the bean's init-method.</p>
	 * @throws Exception
	 *     in case anything goes wrong, for example if the TIE object cannot be instantiated or the
	 *     servant cannot be activated due to a POA policy mismatch.
	 */
	@PostConstruct
	public void corbaInit() throws Exception
	{
	    LOG.debug("corbaInit() called.");
		// If we need a TIE object, instantiate it
		Object tieObject = null;
		if (tieClass != null) {
			Constructor<?>[] cs = tieClass.getDeclaredConstructors();
			for (Constructor<?> c: cs) {
				if (c.getParameterTypes().length == 2) {
					tieObject = c.newInstance(servant, poaBean.getPoa());
					LOG.debug("TIE object created.");
					break;
				}
			}
		} else {
			LOG.debug("No TIE object created, assuming inheritance-based approach.");
			tieObject = servant;
		}

		// Register the TIE or the servant with the POA
		POA poa = poaBean.getPoa();
		if (poaBean.getPolicyValue(PoaPolicyName.IdAssignment) == PoaPolicyValue.User) {
			if (userObjectId == null || userObjectId.length == 0) {
				throw new IllegalArgumentException("User-supplied ObjectId may not be null or empty. Did you forget to set the userObjectId?");
			}
			poa.activate_object_with_id(userObjectId, (Servant)tieObject);
			objectId = Arrays.copyOf(userObjectId, userObjectId.length);
			LOG.debug("Object activated, with user-supplied ObjectId '{}'.", new String(objectId));
		} else {
			objectId = poa.activate_object((Servant)tieObject);
			LOG.debug("Object activated, with system-supplied objectId.");
		}

		// Store a reference for convenience
		ref = poa.id_to_reference(objectId);

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
	 * Returns the object id of the activated servant; if the POA encapsulated by the
	 * {@link PoaBean} given in the
	 * {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) constructor}
     * has its {@link PoaPolicyName#IdAssignment ID Assignment} policy set to
     * {@link PoaPolicyValue#System System}, this is the object id generated by the CORBA runtime.
     * Otherwise, it is the object id set via {@link #setUserObjectId(byte[]) setUserObjectId()}.
	 * @return
	 *     The object id of the servant, if the servant has been activated (i.e. {@link #corbaInit()}
	 *     has been called; {@code null} otherwise.
	 */
	public byte[] getObjectId()
	{
		return objectId;
	}


	/**
	 * A CORBA object reference of the activated servant.
	 * @return
	 *     A CORBA object reference 'pointing' to the Servant passed in the
	 *     {@link #NamedServantObject(PoaBean, Class, Object, NamingContextBean, String) constructor},
	 *     if this method is called after the bean has been initialized (i.e. {@link #corbaInit()} has
	 *     been called); {@code null} otherwise.
	 */
	public org.omg.CORBA.Object getReference()
	{
		return ref;
	}
}
