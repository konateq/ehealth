package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.audit.handler.FailedLogsHandlerService;
import eu.europa.ec.sante.openncp.common.audit.handler.FailedLogsHandlerServiceImpl;
import eu.europa.ec.sante.openncp.common.audit.handler.MessageHandlerListener;
import eu.europa.ec.sante.openncp.common.audit.serialization.AuditLogSerializer;
import eu.europa.ec.sante.openncp.common.audit.serialization.AuditLogSerializerImpl;
import eu.europa.ec.sante.openncp.common.audit.transformer.AuditMessageTransformer;
import eu.europa.ec.sante.openncp.common.audit.utils.SerializableMessage;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.util.MoreCollectors;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.List;

/**
 * This service provides access to the system defined properties
 *
 * @author Kostas Karkaletsis
 * @see net.RFC3881 http://www.rfc3881.net/ generated classes using JAXB Library for populating audit trail entries
 */
@Service
public class AuditService implements MessageHandlerListener {

    private final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private FailedLogsHandlerService failedLogsHandlerService;
    private AuditLogSerializer auditLogSerializer;

    private final List<AuditMessageTransformer> auditMessageTransformers;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    public AuditService(final List<AuditMessageTransformer> auditMessageTransformers, final ConfigurationManager configurationManager) {
        this(auditMessageTransformers);
        this.configurationManager = Validate.notNull(configurationManager, "Configuration manager cannot be null.");
    }

    public AuditService(final List<AuditMessageTransformer> auditMessageTransformers) {
        this.auditMessageTransformers = Validate.notEmpty(auditMessageTransformers, "At least one audit message transformer is needed for the audit service.");
    }

    @PostConstruct
    public void postConstruct() {
        logger.debug("Initializing Audit Service...");

        auditLogSerializer = new AuditLogSerializerImpl(AuditLogSerializer.Type.AUDIT_MANAGER);
        failedLogsHandlerService = new FailedLogsHandlerServiceImpl(this, AuditLogSerializer.Type.AUDIT_MANAGER, configurationManager);
        failedLogsHandlerService.start();
    }

    /**
     * Provides a method to write an Audit Log.
     *
     * @param eventObject Message to be submitted to the ATNA repository.
     * @param facility    the facility number according to log4j.
     * @param severity    the severity of the message.
     * @return true if auditLog is attempted to be sent.
     */
    public synchronized Boolean write(final Object eventObject, final String facility, final String severity) {

        logger.info("[Audit Service] Writing Audit Message");
        try {
            final AuditMessage message = auditMessageTransformers.stream()
                    .filter(transformer -> transformer.accepts(eventObject))
                    .collect(MoreCollectors.oneOrNone(eventObject, auditMessageTransformers))
                    .map(transformer -> transformer.transform(eventObject))
                    .orElseThrow(() -> new IllegalArgumentException("No audit message transformer found for message [%s]"));

            AuditTrailUtils.getInstance().sendATNASyslogMessage(auditLogSerializer, message, facility, severity);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public boolean handleMessage(final Serializable message) {

        if (message instanceof SerializableMessage) {
            final SerializableMessage sm = (SerializableMessage) message;
            final boolean sent = write(sm.getMessage(), sm.getFacility(), sm.getSeverity());
            logger.info("Attempt to write message to OpenATNA server. Result '{}'", sent);
            return sent;
        } else {
            logger.warn("Message null or unknown type! Cannot handle message.");
            return false;
        }
    }

    @PreDestroy
    protected void stopFailedHandler() {
        this.failedLogsHandlerService.stop();
    }
}
