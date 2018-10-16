package biz.ple.test.servers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import biz.ple.corba.beans.OrbBean;
import biz.ple.test.servers.config.DomainConfiguration;


public class DomainServer {

    private static final Logger LOG = LoggerFactory.getLogger(DomainServer.class);

    public static void main(String[] args)
    {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(DomainConfiguration.class);
        OrbBean orb = ctx.getBean("orb", OrbBean.class);
        LOG.info("===> ORB START <===");
        orb.start();
        // Here, you can do all sorts of things, e.g. start a "Server CLI" or a monitoring thread
        try {
            LOG.info("Waiting for shutdown (e.g. via ServerManager) ...");
            orb.waitForShutdown();
        } catch (InterruptedException ite) {
            ite.printStackTrace();
        }
        ctx.close();
    }

}
