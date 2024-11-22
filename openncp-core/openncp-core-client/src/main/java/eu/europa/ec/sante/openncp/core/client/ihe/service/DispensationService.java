package eu.europa.ec.sante.openncp.core.client.ihe.service;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.core.client.api.EpsosDocument;
import eu.europa.ec.sante.openncp.core.client.api.PatientDemographics;
import eu.europa.ec.sante.openncp.core.client.ihe.xdr.XdrDocumentSource;
import eu.europa.ec.sante.openncp.core.client.ihe.xdr.XdrRequest;
import eu.europa.ec.sante.openncp.core.client.ihe.xdr.XdrRequestDts;
import eu.europa.ec.sante.openncp.core.client.ihe.xdr.XdrResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XDRException;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Map;

@Service
public class DispensationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispensationService.class);

    private final XdrDocumentSource xdrDocumentSource;

    public DispensationService(final XdrDocumentSource xdrDocumentSource) {
        this.xdrDocumentSource = Validate.notNull(xdrDocumentSource, "XdrDocumentSource cannot be null");
    }

    /**
     * Notify the patient’s country of affiliation on a successful dispensation of an ePrescription.
     * <p>
     * <br/> <dl> <dt><b>Preconditions: </b> <dd>Service consumer and service provider share a common identifier for
     * the patient <dd>The patient has given consent to the use of epSOS <dd>The service consumer has previously
     * retrieved the list of the patient’s available ePrescriptions
     * <dd>All available ePrescriptions for the identified patient are accessible for NCP-A and the provided eDispensation
     * data relates to these ePrescriptions
     * <dd>A treatment relationship exists between the patient and the
     * requesting HCP and the attesting assertion can be verified by the service
     * provider <dd>The HCP is authorised to dispense medication for the patient
     * </dl> <dl> <dt><b>Fault Conditions: </b> <dd>Preconditions for a success
     * scenario are not met <dd>The requesting HCP has insufficient rights to
     * dispense the identified patient’s ePrescriptions <dd>One or more of the
     * provided dispensation items do not relate to available ePrescriptions of
     * the identified patient <dd>The ePrescription that is referred to by an
     * eDispensation has already been dispensed. <dd>No consent for
     * ePrescription sharing and dispensing is registered for the identified
     * patient <dd>The eDispensation data is not provided in all mandatory
     * encodings <dd>Temporary failure (e.g. verification of a signature cannot
     * be performed due to a PKI failure) </dl> <dl> <dt><b>Warning Conditions:
     * </b> <dd>eDispensation data is not processed by the patient’s country of
     * affiliation </dl>
     *
     * @param document     document to be submitted
     * @param patient      patient's demographic data
     * @param assertionMap HCP Assertion
     * @throws ParseException
     */
    public XdrResponse initialize(final EpsosDocument document, final PatientDemographics patient, final String countryCode,
                                         final Map<AssertionType, Assertion> assertionMap) throws XDRException, ParseException {

        LOGGER.info("[CC] Dispense Service: Initialize");
        final XdrRequest request = XdrRequestDts.newInstance(document, patient);
        return xdrDocumentSource.initialize(request, countryCode, assertionMap);
    }

    /**
     * Notify the patient’s country of affiliation on an erroneous eDispensation notification, in order to allow it
     * to roll back any changes made on its internal data that were triggered by the erroneous notification.
     * <p>
     * <br/> <dl> <dt><b>Preconditions: </b> <dd>Service consumer and service provider share a common identifier for the patient
     * <dd>The service consumer has previously retrieved the list of the patient’s available ePrescriptions and dispensed the identified medicine</dl>
     * <dl><dt><b>Fault Conditions: </b> <dd>Preconditions for a success scenario are not met
     * <dd>The HCP has insufficient rights to process the patient’s ePrescription data <dd>The HCP was not the original
     * dispenser of the identified medication item <dd>The identified item had not been dispensed previously
     * <dd>Temporary failure (e.g. service provider is temporarily unable to access an internal service) </dl>
     * <dl> <dt><b>Warning Conditions: </b> <dd>eDispensation data is not processed by the country of affiliation
     * <dd>eDispensations are not rolled back automatically by the country of affiliation </dl>
     */
    public XdrResponse discard(final EpsosDocument document, final PatientDemographics patient, final String countryCode,
                                      final Map<AssertionType, Assertion> assertionMap) throws XDRException, ParseException {

        LOGGER.info("[CC] Dispense Service: DISCARD");
        try {
            final Document dispense = XMLUtil.parseContent(document.getBase64Binary());
            NodeList nodeList = dispense.getElementsByTagNameNS("urn:hl7-org:v3", "code");
            Node search = nodeList.item(0);
            NamedNodeMap namedNodeMap = search.getAttributes();
            Node nodeAttr = namedNodeMap.getNamedItem("code");
            nodeAttr.setTextContent(ClassCode.EDD_CLASSCODE.getCode());

            nodeList = dispense.getElementsByTagNameNS("urn:hl7-org:v3", "templateId");
            search = nodeList.item(0);
            namedNodeMap = search.getAttributes();
            nodeAttr = namedNodeMap.getNamedItem("root");
            nodeAttr.setTextContent("1.3.6.1.4.1.12559.11.10.1.3.1.1.2-DISCARD");

            final String updated = XMLUtil.documentToString(dispense);
            document.setBase64Binary(updated.getBytes(StandardCharsets.UTF_8));
        } catch (final ParserConfigurationException | SAXException | IOException | TransformerException e) {
            LOGGER.error("Exception: '{}'", e.getMessage());
        }
        final XdrRequest request = XdrRequestDts.newInstance(document, patient);
        return xdrDocumentSource.discard(request, countryCode, assertionMap);
    }
}
