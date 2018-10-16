package biz.ple.corba.util;

import org.omg.PortableServer._ServantLocatorLocalBase;

import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.interfaces.PoaInjectable;


/**
 * Convenience base class for Servant Locators which want to implement the {@link PoaInjectable}
 * interface. Provides an implementation of {@code PoaInjectable}.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public abstract class ServantLocatorBase extends _ServantLocatorLocalBase implements PoaInjectable {

    private static final long serialVersionUID = 1L;

    /** The injected {@link PoaBean} */
    protected PoaBean myPoa;


    @Override
    public void setPoaBean(PoaBean poaBean)
    {
        myPoa = poaBean;
    }

}
