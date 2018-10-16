package biz.ple.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import biz.ple_idl.AddressRec;


@Component
public class EmployeeRepository {

    private AtomicLong nextId = new AtomicLong(1L);
    private Map<Long, EmployeeImpl> employees = new ConcurrentHashMap<>();


    public EmployeeImpl getEmployee(long id)
    {
        return employees.get(id);
    }


    public EmployeeImpl createEmployee(String firstName, String lastName, AddressRec address, String job, int salary)
    {
        Long id = nextId.getAndIncrement();
        EmployeeImpl newGuy = new EmployeeImpl(id, firstName, lastName, address, job, salary);
        employees.put(id, newGuy);
        return newGuy;
    }


    public Long[] getAllEmployeeIds()
    {
        Set<Long> keys = employees.keySet();
        Long[] keysArray = keys.toArray(new Long[keys.size()]);
        Arrays.sort(keysArray);
        return keysArray;
    }


    public void deleteEmployee(long id)
    {
        employees.remove(id);
    }

}
