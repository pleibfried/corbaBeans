package biz.ple.corba.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import biz.ple.corba.beans.server.PoaPolicies;
import biz.ple.corba.beans.server.PoaPolicyName;
import biz.ple.corba.beans.server.PoaPolicyValue;


@Configuration
public class CorbaBasics {

    // Common POA Policies
    // ===================

    @Bean
    @Qualifier("default")
    public PoaPolicies defaultPoaPolicies()
    {
        PoaPolicies bean = new PoaPolicies();
        bean.setPolicy(PoaPolicyName.IdAssignment, PoaPolicyValue.System);
        bean.setPolicy(PoaPolicyName.IdUniqueness, PoaPolicyValue.Unique);
        bean.setPolicy(PoaPolicyName.ImplicitActivation, PoaPolicyValue.NoImplicitActivation);
        bean.setPolicy(PoaPolicyName.Lifespan, PoaPolicyValue.Transient);
        bean.setPolicy(PoaPolicyName.RequestProcessing, PoaPolicyValue.UseActiveObjectMapOnly);
        bean.setPolicy(PoaPolicyName.ServantRetention, PoaPolicyValue.Retain);
        bean.setPolicy(PoaPolicyName.Thread, PoaPolicyValue.OrbCtrlModel);
        return bean;
    }


    @Bean
    @Qualifier("locator")
    public PoaPolicies locatorPoaPolicies()
    {
        PoaPolicies bean = new PoaPolicies(defaultPoaPolicies());
        bean.setPolicy(PoaPolicyName.IdAssignment, PoaPolicyValue.User);
        bean.setPolicy(PoaPolicyName.RequestProcessing, PoaPolicyValue.UseServantManager);
        bean.setPolicy(PoaPolicyName.ServantRetention, PoaPolicyValue.NonRetain);
        return bean;
    }


    @Bean
    @Qualifier("activator")
    public PoaPolicies activatorPoaPolicies()
    {
        PoaPolicies bean = new PoaPolicies(locatorPoaPolicies());
        bean.setPolicy(PoaPolicyName.ServantRetention, PoaPolicyValue.Retain);
        return bean;
    }


    @Bean
    @Qualifier("defaultServant")
    public PoaPolicies defaultServantPoaPolicies()
    {
        PoaPolicies bean = new PoaPolicies(locatorPoaPolicies());
        bean.setPolicy(PoaPolicyName.RequestProcessing, PoaPolicyValue.UseDefaultServant);
        return bean;
    }

}
