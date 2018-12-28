# CorbaBeans

simplifies development of CORBA servers and clients using the Spring Framework

## What is it?

CorbaBeans is a set of wrapper classes around key API elements of the CORBA standard, plus annotations and Spring bean post-processors which simplify the use of those classes.

These beans are designed for ease-of-use, e.g. by encapsulating cumbersome and complicated CORBA APIs, and easy assembly ("wiring"), making CORBA applications (both client- and server-side) amenable to configuration and initialization using the Spring Framework.

The CORBA Beans do not wrap the CORBA API elements entirely, but focus on functionality essential for most CORBA applications. The original API elements are accessible via simple getter methods, so that less commonly used functionality is still available when required, even for "wrapped" objects.

The classes and APIs encapsulated by the CORBA Beans are:
- the Object Request Broker (ORB)
- the Portable Object Adapter (POA) and its servant management
- the Name Service 
- Service Contexts, i.e. client- and server-side Request Interceptors

In addition to beans encapsulating the above, CorbaBeans also provides annotations and bean post-processors which make the configuration and assembly of CORBA applications with the Spring Framework even easier.

Finally, CorbaBeans also encompasses utility and convenience base classes geared towards development of CORBA applications.

## Prerequisites

### Basic knowledge of CORBA

This documentation assumes that you are at least somewhat familiar with CORBA concepts such as ORBs, POAs, the Naming Service, Servant Locators, Interceptors etc., as well as the OMG IDL (interface definition language). CorbaBeans supports the development of both simple and very complex CORBA applications; for simple applications, only a subset of the framework needs to be used.

### CORBA implementation

Currently, CorbaBeans only supports JacORB. It should be easy to adapt to other ORBs, as its only JacORB-specific part is initialization, i.e. specifying the ORBClass and setting the initial reference for the Name Service. 
To run the examples/integration tests, you should have a JacORB 3.9 installation on your system (or change the JacORB version property in the POM accordingly). The JacORB installation is needed for the IDL compiler and the Name Service.

## Basic client applications

Given an IDL interface such as this:
```
interface EmployeeHome {  
   Employee    create(in string firstName, in string lastName, 
                      in AddressRec address, in string job, in long salary);                                
   Employee    findById(in long long id);
   employeeSeq findAll();     
   void        delete(in Employee emp);
   void        deleteById(in long long id);     
};
```
and the Java interface `EmployeeHome` generated from it using the IDL compiler, all you need to write a client application accessing an **EmployeeHome** instance registered with the CORBA Name Service is:
- a Spring configuration class which
  - imports the `CorbaBasics` and `CorbaAnnotationProcessing` configurations
  - configures an `OrbBean`
  - (optionally) configures a `NamingContextBean`
- a variable (or setter method) annotated with the appropriate `@CorbaRef` annotation

This is what your Spring configuration class should look like:
```
@Configuration
@Import({ CorbaBasics.class, CorbaAnnotationProcessing.class })
public class MySpringConfig {

  @Bean
  public OrbBean orb() throws Exception 
  {
    OrbBean orb = new OrbBean();
    bean.setNameServiceInitRef("file://somepath/JacORB_NSRef.ior");
    return orb;
  }
  
}
```
The path passed to the `setNameServiceInitRef` method should of course point to the file the JacORB Name Service writes its IOR to. In fact, the argument to that method is set as the value of the `ORBInitRef.NameService` ORB property; check the JacORB documentation for specifics.

Assuming that a reference to an **EmployeeHome** implementation is stored in the naming context `/applications/myApp.prog` under the name `employeeHome.service`, annotate a member variable (inside a Spring bean) of type `EmployeeHome` as follows to automatically lookup and inject a remote reference to the **EmployeeHome** implementation:
```
@Component
public class SomeSpringBean {

  @CorbaRef(cosNamingPath = "applications/myApp.prog/employeeHome.service")
  private EmployeeHome employeeHome;

}
```
Alternatively, if several remote references are stored in the above naming context, you might want to configre a `NamingContextBean` and reference that in the annotation, shortening the lookup path. Add the following to your configuration class:
```
@Bean
public NamingContextBean appCtx() throws Exception
{
  return new NamingContextBean(orb().getRootNamingCtx(), "applications/myApp.prog");
}
```
and change the annotation as follows:
```
@Component
public class SomeSpringBean {

  @CorbaRef(rootCtxBean = "appCtx", cosNamingPath = "employeeHome.service")
  private EmployeeHome employeeHome;

}
```
Note that a `@CorbaRef` annotation including the `rootCtxBean` attribute always works, whereas the simpler form above only works if exactly one ORB instance is present in the application (which is usually the case for client applications).

## Basic server applications

(coming soon) 

This documentation is a work in progress. In the meantime, the integration tests of the project provide extensive usage examples for CorbaBeans.


