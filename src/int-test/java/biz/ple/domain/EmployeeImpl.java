package biz.ple.domain;

import biz.ple_idl.domain.AddressRec;
import biz.ple_idl.domain.Company;
import biz.ple_idl.domain.Employee;
import biz.ple_idl.domain.EmployeeOperations;
import biz.ple_idl.domain.ParkingSpace;


public class EmployeeImpl implements EmployeeOperations {

    private EmployeeHomeImpl home;

    private long       id;
    private String     firstName;
    private String     lastName;
    private AddressRec address;
    private String     job;
    private int        salary;
    private Company    company;


    public EmployeeImpl(long id)
    {
        this.id = id;
    }


    public EmployeeImpl(long id, String firstName, String lastName, AddressRec address, String job, int salary)
    {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.job = job;
        this.salary = salary;
    }


    void setHome(EmployeeHomeImpl home)
    {
        this.home = home;
    }


    @Override
    public long id()
    {
        return id;
    }


    @Override
    public String firstName()
    {
        return firstName;
    }


    @Override
    public void firstName(String arg)
    {
        firstName = arg;
    }


    @Override
    public String lastName()
    {
        return lastName;
    }


    @Override
    public void lastName(String arg)
    {
        lastName = arg;
    }


    @Override
    public AddressRec address()
    {
        return address;
    }


    @Override
    public void address(AddressRec arg)
    {
        address = arg;
    }


    @Override
    public String jobDescription()
    {
        return job;
    }


    @Override
    public void jobDescription(String arg)
    {
        job = arg;
    }


    @Override
    public int salary()
    {
        return salary;
    }


    @Override
    public void salary(int arg)
    {
        salary = arg;
    }


    @Override
    public Company getCompany()
    {
        return company;
    }


    @Override
    public void setCompany(Company newCompany)
    {
        if ((newCompany == null && company == null) ||
            (newCompany != null && company != null && newCompany._is_equivalent(company))) {
            return;
        }
        Company del = company;
        company = newCompany;
        Employee corbaThis = null;
        if (del != null || company != null) {
            corbaThis = home.createEmployeeRef(this);
        }
        if (del != null) {
           del.fire(corbaThis);
        }
        if (company != null) {
            company.hire(corbaThis);
        }
    }


    @Override
    public ParkingSpace getParkingSpace()
    {
        return home.createParkingSpaceRef(this.id);
    }

}
