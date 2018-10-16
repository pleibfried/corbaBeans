package biz.ple.test.servers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import biz.ple.corba.beans.OrbBean;


public class DomainServerXml {

    private static final Logger LOG = LoggerFactory.getLogger(DomainServerXml.class);

    public static void main(String[] args)
    {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("domain-server-config.xml");
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
