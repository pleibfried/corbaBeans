package biz.ple.test.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import biz.ple.corba.beans.NamingContextBean;
import biz.ple.corba.beans.OrbBean;
import biz.ple.corba.springext.CorbaBeanFactoryPostProcessor;
import biz.ple.corba.springext.CorbaBeanPostProcessor;
import biz.ple.services.ComponentScanMarker;


@Configuration
@ComponentScan(basePackageClasses = ComponentScanMarker.class)
public class EmployeesITConfig {

    // The basics: Spring extensions and the ORB
    // =========================================

    @Bean
    public static CorbaBeanFactoryPostProcessor corbaBeanFactoryPostProcessor()
    {
        return new CorbaBeanFactoryPostProcessor();
    }


    @Bean
    public static CorbaBeanPostProcessor corbaBeanPostProcessor()
    {
        return new CorbaBeanPostProcessor();
    }


    @Bean
    public OrbBean orb() throws Exception
    {
        OrbBean bean = new OrbBean();
        bean.setNameServiceInitRef("file://c:/JacORB_NSRef.ior");
        return bean;
    }


    // Naming Contexts
    // ===============

    @Bean
    public NamingContextBean applicationCtx() throws Exception
    {
        return new NamingContextBean(orb().getRootNamingCtx(), "applications.ctx/domainApp.ctx");
    }

}
