package biz.ple.domain;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INV_OBJREF;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import biz.ple.corba.annotations.CorbaServantLocator;
import biz.ple.corba.util.CorbaObjectId;
import biz.ple.corba.util.ServantLocatorBase;
import biz.ple_idl.domain.EmployeePOATie;


@CorbaServantLocator(poa = "employeesPoa")
public class EmployeeLocator extends ServantLocatorBase {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(EmployeeLocator.class);

    private EmployeeRepository employeeRepo;


    @Autowired
    public void setEmployeeRepository(EmployeeRepository repo)
    {
        this.employeeRepo = repo;
    }


    @Override
    public Servant preinvoke(byte[] oid, POA adapter, String operation, CookieHolder the_cookie)
        throws ForwardRequest
    {
        try {
            long objectId = CorbaObjectId.toLong(oid);
            EmployeeImpl impl = employeeRepo.getEmployee(objectId);
            if (impl == null) {
                throw new OBJECT_NOT_EXIST(0, CompletionStatus.COMPLETED_NO);
            }
            return new EmployeePOATie(impl, adapter);
        }
        catch (Exception xcp) {
            LOG.error("ObjectId (byte array) {} does not seem to represent a long value.", oid);
            throw new INV_OBJREF("ObjectId must be convertible to a long.");
        }
    }


    @Override
    public void postinvoke(byte[] oid, POA adapter, String operation, Object the_cookie, Servant the_servant)
    {
        // Nothing to do, so this method is blank by design
    }

}
