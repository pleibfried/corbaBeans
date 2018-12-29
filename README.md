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

### XML configuration

It is of course possible to configure CORBA clients using the "traditional" Spring XML configuration. For example, given a configuration equivalent to the one above, you can provide a Sring bean named "employeeHome" which is a remote reference of type `EmployeeHome` by declaring the following in your Spring XML configuration:
```
<bean id="employeeHome" class="biz.ple.corba.beans.NamedReferenceLookup" factory-method="lookup">
  <constructor-arg name="namingCtx" ref="appCtx" />
  <constructor-arg name="name" value="employeeHome.service" />
  <constructor-arg name="interfaceClass" value="EmployeeHome" />
</bean>
```
The ORB and Naming Service Contexts can also be configured via XML; look into the integration tests to find some examples.

## Basic server applications

On the server side, in addition to an ORB (encapsulated by an `OrbBean`), you also need a POA (encapsulated by a `PoaBean`) with which to register the interface implementation (servant) you provide. The `CorbaBasics` configuration class provides a set of POA policy mixes covering the most relevant cases. In case of a simple service implementation, of which there is exactly one instance, the "default POA policies" is appropriate. To make the servant accessible to clients, a reference to it needs to be registered with the CORBA Name Service; we'll put it into the name context referenced in the client code above. Accordingly, the Spring configuration should look something like this:
```
@Configuration
@Import({ CorbaBasics.class, CorbaAnnotationProcessing.class })
public class MySpringConfig {

  @Autowired
  CorbaBasics corbaBasics;

  @Bean
  public OrbBean orb() throws Exception 
  {
    OrbBean orb = new OrbBean();
    bean.setNameServiceInitRef("file://somepath/JacORB_NSRef.ior");
    return orb;
  }
    
  @Bean
  public PoaBean simplePoa() throws Exception
  {
    return new PoaBean(orb().getRootPoa(), "simplePoa", corbaBasics.defaultPoaPolicies());
  }
  
  @Bean
  public NamingContextBean appCtx() throws Exception
  {
    return new NamingContextBean(orb().getRootNamingCtx(), "applications/myApp.prog");
  }
  
}
```
Note that the name "simplePoa" passed to the `PoaBean` constructor need not be the same as the Spring bean name - it is the POA's name in the ORB's POA hierarchy and has nothing to do with Spring bean names. To avoid confusion however, it is best to use the same name for the POA itself and the Spring bean encapsulating it.

To register the implementation of the `EmployeeHome` interface with the POA and the Name Service, you can write a bean method returning an instance of class `NamedServantObject`, to which you pass the actual implementation as a constructor argument. An even simpler way is to annotate your implementation class with `@CorbaServant`, as follows:
```
@CorbaServant(beanName = "employeeHome", poa = "simplePoa", tieClass = EmployeeHomePOATie.class,
              cosNamingCtx = "appCtx", cosNamingName = "employeeHome.service")
public class EmployeeHomeImpl implements EmployeeHomeOperations {

    // implementation of EmployeeHome interface goes here ...
    
}
```
If you'd rather not use the "TIE approach" for registering your implementation with the POA, have it extend the (generated) `EmployeeHomePOA` class and do not specify a `tieClass` attribute in the annotation:
```
@CorbaServant(beanName = "employeeHome", poa = "simplePoa", 
              cosNamingCtx = "appCtx", cosNamingName = "employeeHome.service")
public class EmployeeHomeImpl extends EmployeeHomePOA {

    // implementation of EmployeeHome interface goes here ...
    
}
```
Note that in both cases, the Spring bean named "employeeHome" will be of type `NamedServantObject` and *not* of type `EmployeeHomeImpl`. 

After starting the Spring application containing the above configuration and implementation classes, your client application should be able to access your `EmployeeHome` implementation, i.e. invoke methods on it.

### XML configuration

It is of course possible to configure CORBA servers using the "traditional" Spring XML configuration. For example, given a configuration equivalent to the one above, you can register an instance of `EmployeeHomeImpl` with the POA and Name Service by removing the `@CorbaServant` annotation from the `EmployeeHomeImpl` class and declaring the following in an XML configuration:
```
<bean id="employeeHomeImpl" class="your.package.EmployeeHomeImpl">
  <!-- plus whatever properties/constructor arguments your class needs -->
</bean>

<bean id="employeeHome" class="biz.ple.corba.beans.server.NamedServantObject">
  <constructor-arg name="poaBean" ref="simplePoa" />
  <constructor-arg name="tieClass" value="biz.ple_idl.domain.EmployeeHomePOATie" />
  <constructor-arg name="servant" ref="employeeHomeImpl" />
  <constructor-arg name="namingCtxWrapper" ref="appCtx" />
  <constructor-arg name="name" value="employeeHome.service" />
</bean>  
```
It is **very important** that in this case, you either not annotate `EmployeeHomeImpl` with `@CorbaServant` or disable annotation processing at least for CorbaBeans (by not importing `CorbaAnnotationProcessing` in your configuration and making sure that no beans of class `CorbaBeanPostProcessor` and `CorbaBeanFactoryPostProcessor` are present in your Spring application context). You could also just switch off annotation configuration for your entire Spring container.

## CORBA Service Contexts and complex server applications

More on this topic to come soon ... This documentation is a work in progress. In the meantime, the integration tests of the project provide extensive usage examples for CorbaBeans.


