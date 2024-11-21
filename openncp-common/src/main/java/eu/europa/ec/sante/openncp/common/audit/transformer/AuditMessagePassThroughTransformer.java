package eu.europa.ec.sante.openncp.common.audit.transformer;

import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditMessagePassThroughTransformer implements AuditMessageTransformer {
    private final static Logger LOGGER = LoggerFactory.getLogger(AuditMessagePassThroughTransformer.class);

    @Override
    public boolean accepts(Object message) {
        return message instanceof AuditMessage;
    }

    @Override
    public AuditMessage transform(final Object message) {
        Validate.notNull(message, "AuditMessage cannot be null.");
        //pass through
        return (AuditMessage) message;
    }
}
