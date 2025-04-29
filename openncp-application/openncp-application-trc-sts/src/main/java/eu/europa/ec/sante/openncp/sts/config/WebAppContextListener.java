package eu.europa.ec.sante.openncp.sts.config;


import eu.europa.ec.sante.openncp.common.audit.AuditServiceFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppContextListener implements ServletContextListener {

    private final Logger logger = LoggerFactory.getLogger(WebAppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        if (logger.isDebugEnabled()) {
            logger.debug("Web Application Initialization");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        if (logger.isDebugEnabled()) {
            logger.info("Web Application Destroyed");
        }
        AuditServiceFactory.stopAuditService();
    }
}
