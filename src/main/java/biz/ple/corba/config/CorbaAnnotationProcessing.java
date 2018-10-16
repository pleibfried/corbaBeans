package biz.ple.corba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import biz.ple.corba.springext.CorbaBeanFactoryPostProcessor;
import biz.ple.corba.springext.CorbaBeanPostProcessor;


@Configuration
public class CorbaAnnotationProcessing {

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

}
