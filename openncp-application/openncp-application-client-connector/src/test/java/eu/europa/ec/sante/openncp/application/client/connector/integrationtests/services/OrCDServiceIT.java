package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.services;

import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorException;
import eu.europa.ec.sante.openncp.application.client.connector.ClientConnectorService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.STSClientException;
import eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util.AssertionUtils;
import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OrCDServiceIT  extends BaseIntegrationTest {

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ClientConnectorService clientConnectorService;

    @Autowired
    private KeyStoreManager keyStoreManager;

    @Test
    void queryDocuments() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("3-1234-W8");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final List<GenericDocumentCode> classCodes = new ArrayList<>();
        classCodes.add(buildGenericDocumentCode(objectFactory, ClassCode.ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE.getCode(), Constants.ORCD_HOSPITAL_DISCHARGE_REPORTS_TITLE));
        classCodes.add(buildGenericDocumentCode(objectFactory, ClassCode.ORCD_LABORATORY_RESULTS_CLASSCODE.getCode(), Constants.ORCD_LABORATORY_RESULTS_TITLE));
        classCodes.add(buildGenericDocumentCode(objectFactory, ClassCode.ORCD_MEDICAL_IMAGES_CLASSCODE.getCode(), Constants.ORCD_MEDICAL_IMAGES_TITLE));
        classCodes.add(buildGenericDocumentCode(objectFactory, ClassCode.ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE.getCode(), Constants.ORCD_MEDICAL_IMAGING_REPORTS_TITLE));

        final List<EpsosDocument> documentList = clientConnectorService.queryDocuments(assertions, "BE", patientId, classCodes, null);
        assertThat(documentList).isNotNull().hasSize(4);
    }

    private static @NotNull GenericDocumentCode buildGenericDocumentCode(ObjectFactory objectFactory, String classCodeCode, String classCodeTitle) {
        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(classCodeCode);
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(classCodeTitle);
        return classCode;
    }

    @Test
    void retrieveDocument() throws ClientConnectorException, STSClientException, MarshallingException, MalformedURLException {
        final Map<AssertionType, Assertion> assertions = new HashMap<>();
        final Assertion clinicalAssertion = AssertionUtils.createClinicalAssertion(keyStoreManager, "Doctor House", "John House", "house@ehdsi.eu");

        final ObjectFactory objectFactory = new ObjectFactory();
        final PatientId patientId = objectFactory.createPatientId();
        patientId.setRoot("1.3.6.1.4.1.48336");
        patientId.setExtension("3-1234-W8");

        final var documentId = objectFactory.createDocumentId();
        documentId.setDocumentUniqueId("1.2.3^eHDSI_Test_File_Laboratory_Report.1");
        documentId.setRepositoryUniqueId("1.3.6.1.4.1.48336");

        assertions.put(AssertionType.HCP, clinicalAssertion);
        final Assertion treatmentConfirmationAssertion = AssertionUtils.createTRCAssertion(assertionService, configurationManager, clinicalAssertion, patientId, "TREATMENT");
        assertions.put(AssertionType.TRC, treatmentConfirmationAssertion);

        final GenericDocumentCode classCode = objectFactory.createGenericDocumentCode();
        classCode.setNodeRepresentation(ClassCode.ORCD_LABORATORY_RESULTS_CLASSCODE.getCode());
        classCode.setSchema("2.16.840.1.113883.6.1");
        classCode.setValue(Constants.ORCD_LABORATORY_RESULTS_TITLE);

        final EpsosDocument document = clientConnectorService.retrieveDocument(assertions, "BE", documentId, "1.3.6.1.4.1.48336", classCode, null);
        assertThat(document).isNotNull();
    }
}
