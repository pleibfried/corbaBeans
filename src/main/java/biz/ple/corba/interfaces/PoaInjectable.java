package biz.ple.corba.interfaces;

import org.omg.PortableServer.ServantActivator;
import org.omg.PortableServer.ServantLocator;

import biz.ple.corba.beans.server.PoaBean;


/**
 * <p>Interface to be implemented by {@link ServantLocator Servant Locators},
 * {@link ServantActivator Servant Activators} and Default Servants which need
 * to know the POA for which they act as a servant manager.</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public interface PoaInjectable {

    /**
     * Injects a {@link PoaBean}; called by {@link PoaBean#corbaInit()} during
     * Servant Manager registration.
     * @param poaBean
     *      The injected {@code PoaBean}.
     */
    void setPoaBean(PoaBean poaBean);

}
