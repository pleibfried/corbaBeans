package biz.ple.corba.util;

import biz.ple.corba.beans.OrbBean;
import biz.ple_idl.srvmgmt.ServerManagerOperations;


/**
 * <p>Implementation of the {@code ServerManager} IDL interface.</p>
 * <p>This object, if registered with a POA and thus made accessible
 * to remote clients, enables remote shutdown of the ORB.</p>
 * @author Philipp Leibfried
 * @since  1.0.0
 */
public class ServerManagerImpl implements ServerManagerOperations {

	private OrbBean orbWrapper;


	public ServerManagerImpl(OrbBean orb)
	{
		this.orbWrapper = orb;
	}


	/**
	 * Shuts down the server-side ORB, i.e. the ORB with whose POA hierarchy
	 * this {@code ServerManager} is registered.
	 * @param waitForCompletion
	 *     Indicates whether the ORB should wait for all requests pending at
	 *     the time of invocation to complete ({@code true}, recommended, since
	 *     this leads to a 'graceful' shutdown) or if the ORB should just shut
	 *     down immediately ({@code false}).
	 */
	@Override
	public void shutdownServer(boolean waitForCompletion) {
	    System.out.println("shutDownServer(" + waitForCompletion + ") invoked.");
		orbWrapper.stop(waitForCompletion);
	}

}
