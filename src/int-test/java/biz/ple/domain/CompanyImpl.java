package biz.ple.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import biz.ple_idl.domain.AddressRec;
import biz.ple_idl.domain.CompanyPOA;
import biz.ple_idl.domain.Employee;


public class CompanyImpl extends CompanyPOA {

    public CompanyFactory  home;

    private long           id;
    private String         name;
    private AddressRec     address;
    private String         taxId;
    private List<Employee> employees;


    public CompanyImpl(long id)
    {
        this.id = id;
        this.employees = new LinkedList<>();
    }


    public CompanyImpl(long id, String name, AddressRec address, String taxId)
    {
        this(id);
        this.name = name;
        this.address = address;
        this.taxId = taxId;
    }


    void setHome(CompanyFactory home)
    {
        this.home = home;
    }


    @Override
    public long id()
    {
        return this.id;
    }


    @Override
    public String name()
    {
        return name;
    }


    @Override
    public void name(String name)
    {
        this.name = name;
    }


    @Override
    public AddressRec address()
    {
        return address;
    }


    @Override
    public void address(AddressRec address)
    {
        this.address = address;
    }


    @Override
    public String taxId()
    {
        return taxId;
    }


    @Override
    public void taxId(String taxId)
    {
        this.taxId = taxId;
    }


    @Override
    public int numberOfEmployees()
    {
        return employees.size();
    }


    @Override
    public void hire(Employee newEmp)
    {
        if (newEmp == null) {
            return;
        }
        long newId = newEmp.id();
        if (employees.stream().filter((emp) -> emp.id() == newId).count() > 0) {
            return;
        }
        employees.add(newEmp);
        newEmp.setCompany(home.createCorbaReference(this));
    }


    @Override
    public void fire(Employee oldEmp)
    {
        if (oldEmp == null) {
            return;
        }
        long oldId = oldEmp.id();
        List<Employee> changedRoster = employees.stream().filter(emp -> emp.id() != oldId).collect(Collectors.toList());
        if (changedRoster.size() < employees.size()) {
            employees = changedRoster;
            oldEmp.setCompany(null);
        }
    }


    @Override
    public Employee[] getEmployees()
    {
        return employees.toArray(new Employee[employees.size()]);
    }

}
