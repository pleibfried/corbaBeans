package biz.ple.corba.util;

import org.omg.PortableInterceptor.Current;

import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.interfaces.PICurrentInjectable;


/**
 * A simple, but often sufficient implementation of {@code PICurrentInjectable}. This
 * class can be used as a base class for Service Context Managers.
 *
 * @author Philipp Leibfried
 * @since  1.0.0
 *
 */
public class PICurrentInjectableImpl implements PICurrentInjectable {

    /** The injected PICurrent. */
    protected Current piCurrent;

    /** The injected Portable Interceptors Slot ID */
    protected Integer piCurrentSlotId = null;

    /** The injected {@code OrbBean}. */
    protected OrbBean orbBean;


    @Override
    public void setPortableInterceptorCurrent(Current piCurrent, int slotId)
    {
        this.piCurrent = piCurrent;
        this.piCurrentSlotId = slotId;
    }


    /**
     * Retrieves the (injected) Portable Interceptors Slot ID.
     * @return
     *     The injected Slot ID; {@code null} if none has been injected.
     */
    public Integer getPICurrentSlotId()
    {
        return this.piCurrentSlotId;
    }


    @Override
    public void setOrb(OrbBean orbBean)
    {
        this.orbBean = orbBean;
    }

}
