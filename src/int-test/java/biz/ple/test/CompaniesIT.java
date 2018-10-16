package biz.ple.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import biz.ple.corba.beans.OrbBean;
import biz.ple.services.ExampleServiceContext;
import biz.ple_idl.AddressRec;
import biz.ple_idl.Company;
import biz.ple_idl.CompanyHome;
import biz.ple_idl.Employee;
import biz.ple_idl.EmployeeHome;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:companies-test-config.xml")
public class CompaniesIT {

    private static OrbBean serverSideORB;
    private static ClassPathXmlApplicationContext serverCtx;

    /**
     * This starts the "Domain Server" application context, including its own ORB,
     * inside the same VM as the client application context (with its own respective
     * ORB). This avoids having to spawn an external process for the "Domain Server".
     * Note that a Naming Service must still be running for the test to work.
     */
    @BeforeClass
    public static void setupClass() throws Exception
    {
        serverCtx = new ClassPathXmlApplicationContext("domain-server-config.xml");
        serverSideORB = serverCtx.getBean("orb", OrbBean.class);
        serverSideORB.start();
    }


    @AfterClass
    public static void teardownClass() throws Exception
    {
        serverSideORB.stop(false);
        serverSideORB.waitForShutdown();
    }


    @Autowired
    ApplicationContext appCtx;

    CompanyHome companyHome;
    EmployeeHome empHome;
    ExampleServiceContext serviceCtx;


    @Before
    public void setup() throws Exception
    {
        serviceCtx = appCtx.getBean("exampleServiceContextManager", ExampleServiceContext.class);
        companyHome = appCtx.getBean("companyHome", CompanyHome.class);
        empHome = appCtx.getBean("employeeHome", EmployeeHome.class);
    }


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
        CompanyHome companyHome = appCtx.getBean("companyHome", CompanyHome.class);
        companyHome.deleteById(12345L); // should have no effect and throw no exception
        Integer modifiedCtxData = serviceCtx.getData();
        assertNotNull(modifiedCtxData);
        assertEquals(43, modifiedCtxData.intValue());
    }


    @Test
    public void testCreationAndRetrieval() throws Exception
    {
        String name = "Glutflüssig Gmbh";
        AddressRec address = new AddressRec("Industriestr.", "666", 66666, "Höllenloch");
        String taxId = "0815/666/42";

        CompanyHome companyHome = appCtx.getBean("companyHome", CompanyHome.class);
        Company c = companyHome.create(name, address, taxId);

        assertNotNull(c);
        assertEquals(name, c.name());
        assertEquals(taxId, c.taxId());
        assertAddressRecEquals(address, c.address());

        long id = c.id();
        Company d = companyHome.findById(id);
        assertNotNull(d);
        assertEquals(id, d.id());
        assertEquals(name, d.name());
        assertEquals(taxId, d.taxId());
        assertAddressRecEquals(address, d.address());
    }


    @Test
    public void testHireAndFire() throws Exception
    {
        String name = "EFDAH AG";
        AddressRec address = new AddressRec("Produktionsallee", "211", 75980, "Wohlstandshausen");
        String taxId = "75980/211/43";

        CompanyHome companyHome = appCtx.getBean("companyHome", CompanyHome.class);
        Company c = companyHome.create(name, address, taxId);

        String firstName = "Matthias";
        String lastName = "Mustermann";
        AddressRec addr = new AddressRec("Durchgangsstraße", "1a", 12345, "Unstadt");
        String job = "Muster-Angestellter";
        int salary = 100_000;
        Employee e = empHome.create(firstName, lastName, addr, job, salary);

        assertNotNull(e);
        assertNull(e.getCompany());
        assertNotNull(c);
        assertEquals(0, c.getEmployees().length);

        // WHEN
        c.hire(e);

        // THEN
        Employee[] employees = c.getEmployees();
        assertEquals(1, employees.length);
        assertTrue(e._is_equivalent(employees[0]));
        assertNotNull(e.getCompany());
        assertTrue(c._is_equivalent(e.getCompany()));

        // WHEN
        c.fire(e);

        // THEN
        assertEquals(0, c.getEmployees().length);
        assertNull(e.getCompany());
    }

}
