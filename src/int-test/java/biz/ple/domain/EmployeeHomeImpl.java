package biz.ple.domain;

import static biz.ple.corba.util.CorbaObjectId.fromLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import biz.ple.corba.annotations.CorbaServant;
import biz.ple.corba.beans.server.PoaBean;
import biz.ple_idl.AddressRec;
import biz.ple_idl.Employee;
import biz.ple_idl.EmployeeHelper;
import biz.ple_idl.EmployeeHomeOperations;
import biz.ple_idl.EmployeeHomePOATie;


@CorbaServant(beanName = "employeeHome", poa = "homesPoa", tieClass = EmployeeHomePOATie.class,
              cosNamingCtx = "applicationCtx", cosNamingName = "employeeHome.obj")
public class EmployeeHomeImpl implements EmployeeHomeOperations {

    private PoaBean employeePoa;
    private EmployeeRepository repo;


    // Constructors and initialization
    // ===============================

    @Autowired
    @Qualifier("employeesPoa")
    public void setEmployeePoa(PoaBean empPoa)
    {
        this.employeePoa = empPoa;
    }


    @Autowired
    public void setRepository(EmployeeRepository repo)
    {
        this.repo = repo;
    }


    // Methods which are not part of the IDL interface
    // ===============================================

    public Employee createCorbaReference(EmployeeImpl emp)
    {
        if (emp == null) {
            return null;
        }
        return employeePoa.createObjectReference(fromLong(emp.id()), Employee.class);
    }


    // Implementation of interface EmployeHomeOperations (IDL interface)
    // =================================================================

    @Override
    public Employee create(String firstName, String lastName, AddressRec address, String job, int salary)
    {
        EmployeeImpl newImpl = repo.createEmployee(firstName, lastName, address, job, salary);
        newImpl.setHome(this);
        return employeePoa.createObjectReference(fromLong(newImpl.id()), Employee.class);
    }


    @Override
    public Employee findById(long id)
    {
        EmployeeImpl emp = repo.getEmployee(id);
        if (emp == null) {
            return EmployeeHelper.narrow(null);
        }
        return employeePoa.createObjectReference(fromLong(id), Employee.class);
    }


    @Override
    public Employee[] findAll()
    {
        Long[] allIds = repo.getAllEmployeeIds();
        Employee[] result = new Employee[allIds.length];
        for (int k = 0; k < allIds.length; ++k) {
              result[k] = employeePoa.createObjectReference(fromLong(allIds[k]), Employee.class);
        }
        return result;
    }


    @Override
    public void delete(Employee emp)
    {
        if (emp != null) {
            repo.deleteEmployee(emp.id());
        }
    }


    @Override
    public void deleteById(long id)
    {
        repo.deleteEmployee(id);
    }

}
