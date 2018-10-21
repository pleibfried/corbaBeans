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

Currently, CORBA Beans only supports JacORB. It should be easy to adapt to other ORBs, as its only JacORB-specific part is initialization, i.e. specifying the ORBClass and setting the initial reference for the Name Service. 
To run the examples/integration tests, you should have a JacORB 3.9 installation on your system (or change the JacORB version property in the POM accordingly). The JacORB installation is needed for the IDL compiler and the Name Service.

## Examples

This documentation will be expanded and improved soon. In the meantime, the integration tests of the project provide extensive usage examples for CorbaBeans.
