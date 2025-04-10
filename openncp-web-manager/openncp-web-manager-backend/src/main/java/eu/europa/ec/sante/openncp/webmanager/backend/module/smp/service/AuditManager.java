package eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import eu.europa.ec.sante.openncp.common.util.CertificatesDataHolder;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.common.util.ImmutableCertificatesDataHolder;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.util.DateTimeUtil;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Inês Garganta
 */
@Service
public class AuditManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditManager.class);
    private final PropertyService propertyService;
    private final AuditService auditService;

    public AuditManager(final PropertyService propertyService, final AuditService auditService) {
        this.propertyService = Validate.notNull(propertyService, "PropertyService must not be null");
        this.auditService = Validate.notNull(auditService, "AuditService must not be null");
    }

    /**
     * @param smpServerUri
     * @param objectID
     * @param errorMessagePartObjectId
     * @param errorMessagePartObjectDetail
     */
    public void handleDynamicDiscoveryQuery(final String smpServerUri, final String objectID, final String errorMessagePartObjectId,
                                            final byte[] errorMessagePartObjectDetail) {
        final EventLog eventLog = createDynamicDiscoveryEventLog(TransactionName.SMP_QUERY, objectID, errorMessagePartObjectId,
                errorMessagePartObjectDetail, smpServerUri);
        eventLog.setEventType(EventType.SMP_QUERY);
        eventLog.setNcpSide(NcpSide.NCP_B);
        auditService.write(eventLog, "13", "2");
    }

    /**
     * @param smpServerUri
     * @param objectID
     * @param errorMessagePartObjectId
     * @param errorMessagePartObjectDetail
     */
    public void handleDynamicDiscoveryPush(final String smpServerUri, final String objectID, final String errorMessagePartObjectId,
                                           final byte[] errorMessagePartObjectDetail) {
        final EventLog eventLog = createDynamicDiscoveryEventLog(TransactionName.SMP_PUSH, objectID, errorMessagePartObjectId,
                errorMessagePartObjectDetail, smpServerUri);
        eventLog.setEventType(EventType.SMP_PUSH);
        eventLog.setNcpSide(NcpSide.NCP_A);
        auditService.write(eventLog, "13", "2");
    }

    /**
     * @param bytes
     * @return
     */
    public String prepareEventLog(final byte[] bytes) {

        final StringWriter sw = new StringWriter();
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setXIncludeAware(false);
            final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            final Document doc = builder.parse(stream);
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(sw));

        } catch (final TransformerException | ParserConfigurationException | UnsupportedOperationException |
                       SAXException | IOException e) {
            LOGGER.error("{} response: '{}'", e.getClass(), SimpleErrorHandler.printExceptionStackTrace(e));
        }
        return sw.toString();
    }

    /**
     * @param transactionName
     * @param objectID
     * @param errorMessagePartObjectId
     * @param errorMessagePartObjectDetail
     * @param smpServerUri
     * @return
     */
    private EventLog createDynamicDiscoveryEventLog(final TransactionName transactionName, final String objectID,
                                                    final String errorMessagePartObjectId, final byte[] errorMessagePartObjectDetail,
                                                    final String smpServerUri) {
        final CertificatesDataHolder.CertificateData providerCertificateData = CertificatesDataHolder.CertificateData.builder()
                .keystorePath(propertyService.getPropertyValueMandatory(Constant.SP_KEYSTORE_PATH.getKey()))
                .keystorePassword(propertyService.getPropertyValueMandatory(Constant.SP_KEYSTORE_PASSWORD.getKey()))
                .privateKeyAlias(propertyService.getPropertyValueMandatory(Constant.SP_PRIVATEKEY_ALIAS.getKey()))
                .build();
        final CertificatesDataHolder.CertificateData consumerCertificateData = CertificatesDataHolder.CertificateData.builder()
                .keystorePath(propertyService.getPropertyValueMandatory(Constant.SC_KEYSTORE_PATH.getKey()))
                .keystorePassword(propertyService.getPropertyValueMandatory(Constant.SC_KEYSTORE_PASSWORD.getKey()))
                .privateKeyAlias(propertyService.getPropertyValueMandatory(Constant.SC_PRIVATEKEY_ALIAS.getKey()))
                .build();
        final CertificatesDataHolder certificatesDataHolder = ImmutableCertificatesDataHolder.builder()
                .trustoreData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(propertyService.getPropertyValueMandatory(Constant.TRUSTSTORE_PATH.getKey()))
                        .keystorePassword(propertyService.getPropertyValueMandatory(Constant.TRUSTSTORE_PASSWORD.getKey()))
                        .build())
                .serviceProviderData(providerCertificateData)
                .serviceConsumerData(consumerCertificateData)
                .build();

        final String serviceConsumerUserId = HttpUtil.getSubjectDN(certificatesDataHolder, false);
        final String serviceProviderUserId = HttpUtil.getTlsCertificateCommonName(consumerCertificateData, smpServerUri);
        final String localIp = IPUtil.getPrivateServerIp();
        final String participantId = propertyService.getPropertyValueMandatory("COUNTRY_PRINCIPAL_SUBDIVISION");
        URI uri = null;
        try {
            uri = new URI(smpServerUri);
        } catch (final URISyntaxException e) {
            LOGGER.error("URISyntaxException: '{}'", e.getMessage(), e);
        }

        return EventLog.createEventLogPatientPrivacy(transactionName, EventActionCode.EXECUTE, DateTimeUtil.timeUTC(),
                EventOutcomeIndicator.FULL_SUCCESS, null, null, null,
                serviceConsumerUserId, serviceProviderUserId, participantId, null,
                errorMessagePartObjectId, errorMessagePartObjectDetail, objectID, null,
                new byte[1], null, new byte[1], localIp, uri != null ? uri.getHost() : smpServerUri);
    }
}
