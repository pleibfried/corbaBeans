package biz.ple.test.servers.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import biz.ple.corba.beans.NamingContextBean;
import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.beans.server.NamedServantObject;
import biz.ple.corba.beans.server.PoaBean;
import biz.ple.corba.config.CorbaAnnotationProcessing;
import biz.ple.corba.config.CorbaBasics;
import biz.ple.corba.util.ServerManagerImpl;
import biz.ple.domain.CompanyFactory;
import biz.ple_idl.domain.CompanyHomePOATie;
import biz.ple_idl.srvmgmt.ServerManagerPOATie;


@Configuration
@Import({ CorbaBasics.class, CorbaAnnotationProcessing.class })
@ComponentScan(basePackageClasses = { biz.ple.domain.ComponentScanMarker.class,
                                      biz.ple.services.ComponentScanMarker.class }
)
public class DomainConfiguration {

    @Autowired
    CorbaBasics basics;


    // The ORB
    // =======

    @Bean
    public OrbBean orb() throws Exception
    {
        OrbBean bean = new OrbBean();
        bean.setNameServiceInitRef("file://target/JacORB_NSRef.ior");
        return bean;
    }


    // POA Policies and POAs
    // =====================

    @Bean
    public PoaBean homesPoa() throws Exception
    {

        return new PoaBean(orb().getRootPoa(), "homesPoa", basics.defaultPoaPolicies());
    }


    @Bean
    public PoaBean serverManagerPoa() throws Exception
    {
        return new PoaBean(orb().getRootPoa(), "serverManagerPoa", basics.defaultPoaPolicies());
    }


    @Bean
    public PoaBean employeesPoa() throws Exception
    {
        PoaBean bean = new PoaBean(orb().getRootPoa(), "employeesPoa", basics.locatorPoaPolicies());
        return bean;
    }


    @Bean
    public PoaBean companiesPoa() throws Exception
    {
        PoaBean bean = new PoaBean(orb().getRootPoa(), "companiesPoa", basics.locatorPoaPolicies());
        bean.setServantLocator(companyFactory());
        return bean;
    }


    @Bean
    public PoaBean parkingPoa() throws Exception
    {
        PoaBean bean = new PoaBean(orb().getRootPoa(), "parkingPoa", basics.defaultServantPoaPolicies());
        return bean;
    }


    // Naming Contexts
    // ===============

    @Bean
    public NamingContextBean applicationCtx() throws Exception
    {
        return new NamingContextBean(orb().getRootNamingCtx(), "applications.ctx/domainApp.ctx");
    }


    @Bean
    public NamingContextBean serverManagerCtx() throws Exception
    {
        return new NamingContextBean(orb().getRootNamingCtx(), "servers.ctx/domainServer.management");
    }


    // Services / Servants
    // ===================

    @Bean
    public CompanyFactory companyFactory() throws Exception
    {
        CompanyFactory bean = new CompanyFactory();
        return bean;
    }


    @Bean
    public NamedServantObject companyHome() throws Exception
    {
        return new NamedServantObject(homesPoa(), CompanyHomePOATie.class, companyFactory(),
                                      applicationCtx(), "companyHome.obj");
    }


    @Bean
    public NamedServantObject serverManager() throws Exception
    {
        return new NamedServantObject(serverManagerPoa(), ServerManagerPOATie.class, new ServerManagerImpl(orb()),
                                      serverManagerCtx(), "serverManager.obj");
    }

}

