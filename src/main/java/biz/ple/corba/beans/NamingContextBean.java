package biz.ple.corba.beans;

import javax.annotation.PostConstruct;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.Object;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;


/**
 * <p>Encapsulates an extended COS Naming Context (a {@link NamingContextExt} object)
 * and its initialization.</p>
 * <p>As with the other beans of this package (and subpackages), the encapsulation is not
 * 'closed', but provides {@link #getNamingCtx() access} to the encapsulated object, the intent
 * not being to fully wrap the Naming Context and its interface, but to make it amenable
 * to DI/IoC in the context of the Spring Framework.</p>
 * <p>Also, some methods have been added for simplified and convenient lookup functionality.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class NamingContextBean {

	private NamingContextBean parentCtx;
	private NamingContextExt myCtx;
	private String path;
	private boolean lookupOnly;


	/**
	 * Constructor; the constructed {@code NamingContextBean} is 'prepared' but not
	 * fully initialized yet. Initialization is performed in {@link #corbaInit()}.
	 * @param parentCtx
	 *     The bean encapsulating the parent Naming Context of the new Naming Context
	 *     bean.
	 * @param path
	 *     The path (in extended COS Naming Service syntax), relative to the Naming Context
	 *     encapsulated by {@code parentCtx}, of the Naming Context encapsulated by the
	 *     new {@code NamingContextBean}.
	 * @see #corbaInit()
	 */
	public NamingContextBean(NamingContextBean parentCtx, String path)
	{
		this.parentCtx = parentCtx;
		this.path = path;
		this.lookupOnly = false;
	}


	/**
	 * @deprecated Intended for use by {@link OrbBean} only.
	 */
	@Deprecated
	public NamingContextBean(NamingContextExt rootCtx)
	{
		this.myCtx = rootCtx;
		this.path = "/";
		this.parentCtx = null;
		this.lookupOnly = false;
	}


	/**
	 * Setter for the {@code lookupOnly} attribute; default is {@code false}.
	 * @param lookupOnly
	 *     Indicates whether naming contexts on the {@code path} given in the
	 *     {@link #NamingContextBean(NamingContextBean, String) constructor} which
	 *     do not exist are to be created during initialization. If {@code true},
	 *     missing naming contexts are <strong>not</strong> created during
	 *     initialization. If {@code false}, missing contexts are created.
	 * @see #corbaInit()
	 */
	public void setLookupOnly(boolean lookupOnly)
	{
	    this.lookupOnly = lookupOnly;
	}


    /**
     * <p>Initializes (binds) the {@link NamingContextExt} object reference encapsulated by
     * this {@code NamingContextBean}.</p>
     * <p>If the encapsulated Naming Context and/or any Naming Contexts on the path from the
     * parent context passed in the constructor do not exist, and
     * {@link #setLookupOnly(boolean) lookupOnly} is not set to {@code true}, these Naming
     * Contexts are created (remotely) during initialization.</p>
     * <p>In terms of the Spring Framework, this is the {@code NamingContextBean} class'
     * init-method.</p>
	 */
	@PostConstruct
	public void corbaInit() throws Exception
	{
		if (parentCtx == null || parentCtx.getNamingCtx() == null) {
			throw new IllegalArgumentException("Parent NamingContext may not be null!");
		}

		// Create or find the specified path, create missing contexts
		NamingContextExt currCtx = parentCtx.getNamingCtx(), nextCtx;
		String[] elems = path.split("/");
		for (String elem: elems) {
			if (elem.trim().length() == 0) {
				continue;
			}
			Object entry;
			try {
				entry = currCtx.resolve_str(elem);
			} catch (InvalidName ivn) {
				throw new IllegalArgumentException("The string '" + elem + "' is not a valid CORBA naming component.");
			} catch (NotFound nfe) {
				entry = null;
			}
			nextCtx = null;
			if (entry == null) {
			    if (lookupOnly) {
			        throw new IllegalArgumentException("The name component '" + elem + "' could not be resolved.");
			    }
				nextCtx = NamingContextExtHelper.narrow(currCtx.bind_new_context(currCtx.to_name(elem)));
			}
			else {
				try {
					nextCtx = NamingContextExtHelper.narrow(entry);
				} catch (BAD_PARAM bpe) {
					throw new IllegalStateException("Node '" + elem + "' on the naming path is not a NamingContext.");
				}
			}
			currCtx = nextCtx;
		}
		myCtx = currCtx;
	}


	/**
	 * Returns the {@code NamingContextExt} object encapsulated by this {@code NamingContextBean}.
	 * @return
	 *     The {@code NamingContextExt} object encapsulated by this {@code NamingContextBean}, if
	 *     {@link #corbaInit()} has been successfully invoked before this method; {@code null}
	 *     otherwise.
	 */
	public NamingContextExt getNamingCtx()
	{
		return myCtx;
	}


	/**
	 * Looks up an object inside the Naming Context encapsulated by this {@code NamingContextBean} by
	 * its name.
	 * @param name
	 *     The name of the object to look up; this must be a simple name (not a path) in extended
	 *     CosNaming syntax: a name optionally followed by a dot and a 'kind' specifier. Examples
	 *     of valid names are: 'myService.object', 'nameWithoutKind'.
	 * @return
	 *     The object reference mapped to {@code name} in the Naming Context encapsulated by this
	 *     {@code NamingContextBean}. {@code Null} if there is nothing mapped to that name in the
	 *     Naming Context.
	 * @throws CannotProceed
	 *     if there is a technical problem while performing the lookup. This is usually due to a
	 *     an internal error.
	 * @throws IllegalArgumentException
	 *     if the string passed in {@code name} is not a valid COS Naming name.
	 */
	public org.omg.CORBA.Object lookup(String name) throws CannotProceed
	{
	    org.omg.CORBA.Object result = null;
	    try {
	        result = myCtx.resolve_str(name);
        } catch (NotFound e) {
            result = null;
        } catch (InvalidName ivn) {
            throw new IllegalArgumentException("The string '" + name + "' is not a valid CosNaming name.");
        }
	    return result;
	}

}
