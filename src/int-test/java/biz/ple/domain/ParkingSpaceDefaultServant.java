package biz.ple.domain;

import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.BAD_OPERATIONHelper;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.OBJECT_NOT_EXISTHelper;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.CurrentPackage.NoContext;
import org.omg.PortableServer.CurrentPackage.NoContextHelper;
import org.springframework.beans.factory.annotation.Autowired;

import biz.ple.corba.annotations.CorbaDefaultServant;
import biz.ple.corba.util.CorbaObjectId;
import biz.ple.corba.util.DefaultServantBase;


@CorbaDefaultServant(poa = "parkingPoa")
public class ParkingSpaceDefaultServant extends DefaultServantBase {

    private static final String[] implementedIFs = { "IDL:biz/ple_idl/domain/ParkingSpace:1.0" };

    private EmployeeRepository employeeRepo;


    @Autowired
    public ParkingSpaceDefaultServant(EmployeeRepository employeeRepo)
    {
        this.employeeRepo = employeeRepo;
    }


    @Override
    public OutputStream _invoke(String method, InputStream input, ResponseHandler handler)
        throws SystemException
    {
        // Find the employee who 'owns' this parking space (he has the same ID as his parking space)
        Long oid = null;
        try {
            oid = CorbaObjectId.toLong(poaCurrent.get_object_id());
        }
        catch (NoContext ncx) {
            OutputStream xcpStrm = handler.createExceptionReply();
            NoContextHelper.write(xcpStrm, ncx);
            return xcpStrm;
        }
        EmployeeImpl emp = employeeRepo.getEmployee(oid);
        if (emp == null) {
            OutputStream xcpStrm = handler.createExceptionReply();
            OBJECT_NOT_EXISTHelper.write(xcpStrm, new OBJECT_NOT_EXIST("No ParkingSpace for Employee with ID " + oid + " exists."));
            return xcpStrm;
        }
        switch (method) {
        case "_get_owner":
            String ownerName = emp.firstName() + " " + emp.lastName();
            OutputStream resp = handler.createReply();
            resp.write_string(ownerName);
            return resp;
        case "_get_parkingId":
            String f = emp.firstName();
            String l = emp.lastName();
            String parkingId = (f != null && f.length() > 0 ? f.substring(0, 1).toUpperCase() : "X") +
                               (l != null && l.length() > 0 ? l.substring(0, 1).toUpperCase() : "X") +
                               String.format("%04d", oid);
            OutputStream resp2 = handler.createReply();
            resp2.write_string(parkingId);
            return resp2;
        default:
            OutputStream xcpStrm = handler.createExceptionReply();
            BAD_OPERATIONHelper.write(xcpStrm, new BAD_OPERATION("No method '" + method + "' in IDL interface ParkingSpace."));
            return xcpStrm;
        }
    }


    @Override
    public String[] _all_interfaces(POA poa, byte[] objectId)
    {
        return implementedIFs;
    }

}
