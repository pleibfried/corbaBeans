package biz.ple.corba.util;

import org.omg.CORBA.portable.InvokeHandler;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.Servant;

import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.interfaces.PoaInjectable;


/**
 * Convenience base class for Default Servants (see {@link PoaBean#setDefaultServant(Servant)} which
 * also implements the {@link PoaInjectable} interface, so that the default servant is injected with
 * its POA during initialization.
 */
public abstract class DefaultServantBase extends Servant implements InvokeHandler, PoaInjectable {

    protected Current poaCurrent;


    @Override
    public void setPoaBean(PoaBean poaBean)
    {
        poaCurrent = poaBean.getPoaCurrent();
    }

}
