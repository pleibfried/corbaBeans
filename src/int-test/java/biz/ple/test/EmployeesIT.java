package biz.ple.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import biz.ple.corba.annotations.CorbaRef;
import biz.ple.corba.beans.OrbBean;
import biz.ple.services.ExampleServiceContext;
import biz.ple.test.config.EmployeesITConfig;
import biz.ple.test.servers.config.DomainConfiguration;
import biz.ple_idl.domain.AddressRec;
import biz.ple_idl.domain.Company;
import biz.ple_idl.domain.CompanyHome;
import biz.ple_idl.domain.Employee;
import biz.ple_idl.domain.EmployeeHome;
import biz.ple_idl.domain.ParkingSpace;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EmployeesITConfig.class)
public class EmployeesIT {

    private static OrbBean serverSideORB;
    private static AnnotationConfigApplicationContext serverCtx;

    /**
     * This starts the "Domain Server" application context, including its own ORB,
     * inside the same VM as the client application context (with its own respective
     * ORB). This avoids having to spawn an external process for the "Domain Server".
     * Note that a Naming Service must still be running for the test to work.
     */
    @BeforeClass
    public static void setupClass() throws Exception
    {
        serverCtx = new AnnotationConfigApplicationContext(DomainConfiguration.class);
        serverSideORB = serverCtx.getBean("orb", OrbBean.class);
        serverSideORB.start();
    }


    @AfterClass
    public static void teardownClass() throws Exception
    {
        serverSideORB.stop(false);
        serverSideORB.waitForShutdown();
    }


    @CorbaRef(rootCtxBean = "applicationCtx", cosNamingPath="employeeHome.obj")
    EmployeeHome empHome;

    @CorbaRef(rootCtxBean = "applicationCtx", cosNamingPath="companyHome.obj")
    CompanyHome compHome;

    @Autowired
    ExampleServiceContext serviceCtx;


    private static void assertAddressRecEquals(AddressRec exp, AddressRec act)
    {
        assertEquals(exp.street, act.street);
        assertEquals(exp.number, act.number);
        assertEquals(exp.zipCode, act.zipCode);
        assertEquals(exp.city, act.city);
    }


    @Test
    public void testServiceContext()
    {
        assertNotNull(serviceCtx);
        serviceCtx.setData(42);
        empHome.deleteById(12345L); // should have no effect and throw no exception
        Integer modifiedCtxData = serviceCtx.getData();
        assertNotNull(modifiedCtxData);
        assertEquals(43, modifiedCtxData.intValue());
    }


    @Test
    public void testCreationAndRetrieval() throws Exception
    {
        String firstName = "Matthias";
        String lastName = "Mustermann";
        AddressRec addr = new AddressRec("Durchgangsstraße", "1a", 12345, "Unstadt");
        String job = "Muster-Angestellter";
        int salary = 100_000;
        Employee x = empHome.create(firstName, lastName, addr, job, salary);

        assertNotNull(x);
        // Delegate rawProxy = (Delegate) (((ObjectImpl) x)._get_delegate());
        assertEquals(firstName, x.firstName());
        assertEquals(lastName, x.lastName());
        assertAddressRecEquals(addr, x.address());
        assertEquals(job, x.jobDescription());
        assertEquals(salary, x.salary());

        long id = x.id();
        Employee y = empHome.findById(id);
        assertNotNull(y);
        assertEquals(id, y.id());
        assertEquals(firstName, y.firstName());
        assertEquals(lastName, y.lastName());
        assertAddressRecEquals(addr, y.address());
        assertEquals(job, y.jobDescription());
        assertEquals(salary, y.salary());
    }


    @Test
    public void testParkingSpace() throws Exception
    {
        String firstName = "Thomas";
        String lastName = "Edison";
        AddressRec addr = new AddressRec("Inventor Lane", "4470", 50766, "Genius City");
        String job = "Product Designer";
        int salary = 1_000_000;
        Employee x = empHome.create(firstName, lastName, addr, job, salary);

        assertNotNull(x);
        ParkingSpace p = x.getParkingSpace();
        assertNotNull(p);
        assertEquals("Thomas Edison", p.owner());
        assertEquals("TE" + String.format("%04d",  x.id()), p.parkingId());
    }


    @Test
    public void testHireAndFire() throws Exception
    {
        // GIVEN
        String firstName = "Peter";
        String lastName = "Altmann";
        AddressRec addr = new AddressRec("Durchgangsstraße", "2b", 20002, "Wasauchimmer");
        String job = "Versager vom Dienst";
        int salary = 50_000;
        Employee e = empHome.create(firstName, lastName, addr, job, salary);

        AddressRec compAddr = new AddressRec("Industriestraße", "2829", 80008, "Hintertupfingen");
        Company c = compHome.create("IrgendeinBuero", compAddr, "08/15");

        assertNotNull(e);
        assertNull(e.getCompany());
        assertNotNull(c);
        assertEquals(0, c.getEmployees().length);

        // WHEN
        e.setCompany(c);

        // THEN
        assertNotNull(e.getCompany());
        assertTrue(c._is_equivalent(e.getCompany()));
        Employee[] employees = c.getEmployees();
        assertEquals(1, employees.length);
        assertTrue(e._is_equivalent(employees[0]));

        // WHEN
        e.setCompany(null);

        // THEN
        assertNull(e.getCompany());
        assertEquals(0, c.getEmployees().length);
    }


}
