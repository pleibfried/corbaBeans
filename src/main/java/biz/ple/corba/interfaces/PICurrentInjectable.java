package biz.ple.corba.interfaces;

import org.omg.PortableInterceptor.Current;

import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.beans.ServiceContextDefinition;


/**
 * <p>Interface to be implemented by any class which is part of a {@link ServiceContextDefinition}
 * or any standalone request interceptor which should be injected with the Portable Interceptor
 * Current, a slot ID and an OrbBean during ORB initialization.</p>
 * <p>This interface is meant to be implemented by Request Interceptors and Service Context Managers.</p>
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public interface PICurrentInjectable {

    /**
     * Injects the PI Current and a Slot ID into the implementing object. Called by
     * CorbaBeans(tm) ORBInitializers during interceptor registration.
     * @param current
     *      The Portable Interceptor Current (PICurrent).
     * @param slotId
     *      A slot ID; used to identify data shared via the PICurrent.
     */
    void setPortableInterceptorCurrent(Current current, int slotId);

    /**
     * Injects an {@code OrbBean} encapsulating the initializing ORB into the implementing
     * object. Called by CorbaBeans(tm) ORBInitializers during interceptor registration.
     * @param orbBean
     *      An {@code OrbBean} encapsulating the 'initializing' ORB.
     */
    void setOrb(OrbBean orbBean);

}
