package biz.ple.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INV_OBJREF;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.ple.corba.util.CorbaObjectId;
import biz.ple.corba.util.ServantLocatorBase;
import biz.ple_idl.domain.AddressRec;
import biz.ple_idl.domain.Company;
import biz.ple_idl.domain.CompanyHelper;
import biz.ple_idl.domain.CompanyHome;
import biz.ple_idl.domain.CompanyHomeOperations;


/**
 * This is both an implementation of the IDL interface {@link CompanyHome} and
 * a Servant Locator for servants implementing the {@link Company} IDL interface.
 *
 * It is not necessarily recommended to merge both the "Home" and the "Activator/Locator"
 * functionalities into one class, but it does work. For an alternative, see how it is
 * done with the {@link EmployeeHomeImpl} and {@link EmployeeLocator}.
 */
public class CompanyFactory extends ServantLocatorBase implements CompanyHomeOperations
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CompanyFactory.class);

    private AtomicLong nextId = new AtomicLong(1L);
    private Map<Long, CompanyImpl> companies = new ConcurrentHashMap<>();


    @Override
    public Servant preinvoke(byte[] oid, POA adapter, String operation, CookieHolder the_cookie)
        throws ForwardRequest
    {
        try {
            long objectId = CorbaObjectId.toLong(oid);
            CompanyImpl impl = companies.get(objectId);
            if (impl == null) {
                throw new OBJECT_NOT_EXIST(0, CompletionStatus.COMPLETED_NO);
            }
            return impl;
        }
        catch (Exception xcp) {
            LOG.error("ObjectId (byte array) {} does not seem to represent a long value.", oid);
            throw new INV_OBJREF("ObjectId must be convertible to a long.");
        }
    }


    @Override
    public void postinvoke(byte[] oid, POA adapter, String operation, Object the_cookie, Servant the_servant)
    {
        // Nothing to do here
    }


    @Override
    public Company create(String name, AddressRec address, String taxId)
    {
        long oid = nextId.getAndIncrement();
        CompanyImpl newComp = new CompanyImpl(oid, name, address, taxId);
        companies.put(oid, newComp);
        newComp.setHome(this);
        return myPoa.createObjectReference(CorbaObjectId.fromLong(oid), Company.class);
    }


    Company createCorbaReference(CompanyImpl impl)
    {
        if (impl == null) {
            return null;
        }
        return myPoa.createObjectReference(CorbaObjectId.fromLong(impl.id()), Company.class);
    }


    @Override
    public Company findById(long id)
    {
        if (!companies.containsKey(id)) {
            return CompanyHelper.narrow(null);
        }
        return myPoa.createObjectReference(CorbaObjectId.fromLong(id), Company.class);
    }


    @Override
    public Company[] findAll()
    {
        Long[] allIds = companies.keySet().toArray(new Long[companies.keySet().size()]);
        Arrays.sort(allIds);
        Company[] result = new Company[allIds.length];
        for (int k = 0; k < allIds.length; ++k) {
            result[k] = myPoa.createObjectReference(CorbaObjectId.fromLong(allIds[k]), Company.class);
        }
        return result;
    }


    @Override
    public void delete(Company comp)
    {
        if (comp != null) {
            companies.remove(comp.id());
        }
    }


    @Override
    public void deleteById(long id)
    {
        companies.remove(id);
    }

}
