package eu.europa.ec.sante.openncp.core.server.ihe;

import eu.europa.ec.sante.openncp.core.server.api.ihe.xca.DocumentSearchInterface;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterface;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xdr.DocumentSubmitInterface;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class NationalConnectorFactory {

    private final ApplicationContext applicationContext;

    public NationalConnectorFactory(ApplicationContext applicationContext) {
        this.applicationContext = Validate.notNull(applicationContext, "applicationContext must not be null");
    }

    public DocumentSearchInterface createDocumentSearchInstance() {
        return applicationContext.getBean(DocumentSearchInterface.class);
    }

    public PatientSearchInterface createPatientSearchInstance() {
        return applicationContext.getBean(PatientSearchInterface.class);
    }

    public DocumentSubmitInterface createDocumentSubmitInstance() {
        return applicationContext.getBean(DocumentSubmitInterface.class);
    }
}
