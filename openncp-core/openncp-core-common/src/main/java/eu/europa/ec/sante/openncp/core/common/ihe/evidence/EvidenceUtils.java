package eu.europa.ec.sante.openncp.core.common.ihe.evidence;

import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import org.apache.commons.lang3.StringUtils;
import org.herasaf.xacml.core.SyntaxException;
import org.herasaf.xacml.core.api.PDP;
import org.herasaf.xacml.core.api.UnorderedPolicyRepository;
import org.herasaf.xacml.core.policy.PolicyMarshaller;
import org.herasaf.xacml.core.simplePDP.SimplePDPFactory;
import org.herasaf.xacml.core.utils.JAXBMarshallerConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * @author karkaletsis
 */
public class EvidenceUtils {

    public static final String IHE_ITI_XCA_RETRIEVE = "urn:ihe:iti:2007:CrossGatewayRetrieve";
    private static final Logger LOGGER = LoggerFactory.getLogger(EvidenceUtils.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final String DATATYPE_STRING = "http://www.w3.org/2001/XMLSchema#string";
    private static final String DATATYPE_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    private static final String HIDDEN_CREDS = "******";

    private EvidenceUtils() {
    }

    /**
     * @param messageType
     * @return
     */
    private static boolean checkCorrectnessOfIHEXCA(final MessageType messageType) {
        // TODO: Review the reason why the MessageType is not analyzed and the validity is set to true by default.
        LOGGER.debug("The message type : '{}' is correct.", messageType);
        return true;
    }

    /**
     * @param incomingMsg
     * @param issuerCert
     * @param senderCert
     * @param recipientCert
     * @param key
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @throws IOException
     * @throws URISyntaxException
     * @throws TOElementException
     * @throws EnforcePolicyException
     * @throws ObligationDischargeException
     * @throws TransformerException
     * @throws SyntaxException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    public static void createEvidenceREMNRR(final Document incomingMsg,
                                            final X509Certificate issuerCert,
                                            final X509Certificate senderCert,
                                            final X509Certificate recipientCert,
                                            final PrivateKey key,
                                            final String eventType,
                                            final DateTime submissionTime,
                                            final String status,
                                            final String title)
            throws IOException, URISyntaxException, TOElementException, EnforcePolicyException, ObligationDischargeException,
            TransformerException, SyntaxException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException {

        MessageType messageType;
        String messageIdentifier;
        try {
            final MessageInspector messageInspector = new MessageInspector(incomingMsg);
            messageType = messageInspector.getMessageType();
            messageIdentifier = messageInspector.getMessageUUID();
        } catch (final Exception e) {
            LOGGER.warn("[REM-NRR] Message Type Unknown: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingMsg);
            messageIdentifier = UUID.randomUUID().toString();
        }
        LOGGER.info("[Evidence Emitter] Creation of REMNRR for message type: '{}' with ID: '{}'", messageType, messageIdentifier);
        createEvidenceREMNRR(incomingMsg, issuerCert, senderCert, recipientCert, key, eventType, submissionTime, status, title, messageIdentifier);

    }

    /**
     * @param incomingSoap
     * @param issuerCert
     * @param senderCert
     * @param recipientCert
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @param msguuid
     * @throws IOException
     * @throws URISyntaxException
     * @throws TOElementException
     * @throws EnforcePolicyException
     * @throws ObligationDischargeException
     * @throws TransformerException
     * @throws SyntaxException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    public static void createEvidenceREMNRR(final Document incomingSoap,
                                            final X509Certificate issuerCert,
                                            final X509Certificate senderCert,
                                            final X509Certificate recipientCert,
                                            final PrivateKey key,
                                            final String eventType,
                                            final DateTime submissionTime,
                                            final String status,
                                            String title,
                                            final String msguuid)
            throws IOException, URISyntaxException, TOElementException, EnforcePolicyException,
            ObligationDischargeException, TransformerException, SyntaxException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {


        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRR()\nIncoming message:\n'{}'\n Issuer Info: '{}', " +
                            "Sender Info: '{}', Recipient Info: '{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingSoap), issuerCert.toString(), senderCert.toString(), recipientCert.toString(), eventType, submissionTime, status, title, msguuid);
        }
        String messageStatus = "failure";
        if (StringUtils.equals("0", status)) {
            messageStatus = "success";
        }

        final PDP simplePDP = SimplePDPFactory.getSimplePDP();
        final UnorderedPolicyRepository policyRepository = (UnorderedPolicyRepository) simplePDP.getPolicyRepository();
        final ClassLoader loader = EvidenceUtils.class.getClassLoader();
        final InputStream inputStream = loader.getResourceAsStream("policy/samplePolicyNRR.xml");
        policyRepository.deploy(PolicyMarshaller.unmarshal(inputStream));

        /*
         * Instantiate the message inspector, to see which type of message is
         */
        MessageType messageType;

        try {
            final MessageInspector messageInspector = new MessageInspector(incomingSoap);
            messageType = messageInspector.getMessageType();
        } catch (final Exception e) {
            LOGGER.warn("[REM-NRR] Message Type Unknown: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingSoap);
        }

        final List<ObligationHandler> handlers = getObligationHandlers(
                incomingSoap,
                issuerCert,
                senderCert,
                recipientCert,
                key,
                eventType,
                submissionTime,
                msguuid,
                messageStatus,
                messageType,
                simplePDP);


        for (final ObligationHandler oh : handlers) {
            oh.discharge();
            Utilities.serialize(oh.getMessage().getDocumentElement());
            final String oblString = XMLUtil.documentToString(oh.getMessage());
            if (StringUtils.isBlank(title)) {
                title = getPath() + "nrr" + File.separator + getDocumentTitle(msguuid, oh.toString(), "NRR") + ".xml";
            } else {
                title = getPath() + "nrr" + File.separator + getDocumentTitle(msguuid, title, "NRR") + ".xml";
            }
            FileUtil.constructNewFile(title, oblString.getBytes());
        }
    }

    /**
     * @param incomingSoap
     * @param issuerCert
     * @param senderCert,
     * @param recipientCert
     * @param key
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @throws Exception
     */
    public static void createEvidenceREMNRO(
            final Document incomingSoap,
            final X509Certificate issuerCert,
            final X509Certificate senderCert,
            final X509Certificate recipientCert,
            final PrivateKey key,
            final String eventType,
            final DateTime submissionTime,
            final String status,
            final String title) throws Exception {

        String msguuid;
        try {
            final MessageInspector messageInspector = new MessageInspector(incomingSoap);
            logMessage(incomingSoap);
            msguuid = messageInspector.getMessageUUID();
        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            msguuid = UUID.randomUUID().toString();
        }
        createEvidenceREMNRO(
                incomingSoap,
                issuerCert,
                senderCert,
                recipientCert,
                key,
                eventType,
                submissionTime,
                status,
                title,
                msguuid);
    }

    /**
     * @param incomingSoap
     * @param issuerCert
     * @param senderCert
     * @param recipientCert
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @param msguuid
     * @throws Exception
     */
    public static void createEvidenceREMNRO(final Document incomingSoap,
                                            final X509Certificate issuerCert,
                                            final X509Certificate senderCert,
                                            final X509Certificate recipientCert,
                                            final PrivateKey key,
                                            final String eventType,
                                            final DateTime submissionTime,
                                            final String status,
                                            String title,
                                            final String msguuid) throws Exception {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRO()\nIncoming message:\n'{}'\n Issuer Info: '{}'-'{}'-'{}', " +
                            "Sender Info: '{}'-'{}'-'{}', Recipient Info: '{}'-'{}'-'{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingSoap), issuerCert.toString(), senderCert.toString(), recipientCert.toString(), eventType, submissionTime, status, title, msguuid);
        }
        String messageStatus = "failure";
        if (StringUtils.equals("0", status)) {

            messageStatus = "success";
        }

        final PDP simplePDP = SimplePDPFactory.getSimplePDP();
        final UnorderedPolicyRepository policyRepository = (UnorderedPolicyRepository) simplePDP.getPolicyRepository();

        final JAXBMarshallerConfiguration conf = new JAXBMarshallerConfiguration();
        conf.setValidateParsing(false);
        conf.setValidateWriting(false);
        PolicyMarshaller.setJAXBMarshallerConfiguration(conf);
        // Populate the policy repository
        final ClassLoader loader;
        loader = EvidenceUtils.class.getClassLoader();
        final InputStream inputStream = loader.getResourceAsStream("policy/samplePolicy.xml");
        policyRepository.deploy(PolicyMarshaller.unmarshal(inputStream));

        // Read the message as it arrives at the facade
        MessageType messageType;
        try {
            final MessageInspector messageInspector = new MessageInspector(incomingSoap);
            logMessage(incomingSoap);
            messageType = messageInspector.getMessageType();
        } catch (final Exception e) {
            LOGGER.warn("[REM-NRO] Message Type Unknown: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingSoap);
        }
        //TODO: 2021-04-21  Review the following method and if any validation is expected based on the message type.
        checkCorrectnessOfIHEXCA(messageType);

        final List<ObligationHandler> handlers = getObligationHandlers(
                incomingSoap,
                issuerCert,
                senderCert,
                recipientCert,
                key,
                eventType,
                submissionTime,
                msguuid,
                messageStatus,
                messageType,
                simplePDP);

        // Here I discharge manually. This behavior is to let free an implementation
        for (final ObligationHandler handler : handlers) {

            handler.discharge();
            Utilities.serialize(handler.getMessage().getDocumentElement());
            final String oblString = XMLUtil.documentToString(handler.getMessage());
            if (StringUtils.isBlank(title)) {
                title = getPath() + "nro" + File.separator + getDocumentTitle(msguuid, handler.toString(), "NRO") + ".xml";
            } else {
                title = getPath() + "nro" + File.separator + getDocumentTitle(msguuid, title, "NRO") + ".xml";
            }
            FileUtil.constructNewFile(title, oblString.getBytes());
        }
    }

    private static List<ObligationHandler> getObligationHandlers(final Document incomingSoap, final X509Certificate issuerCert, final X509Certificate senderCert, final X509Certificate recipientCert, final PrivateKey key, String eventType, DateTime submissionTime, String msguuid, String messageStatus, MessageType messageType, PDP simplePDP) throws URISyntaxException, TOElementException, TransformerException, EnforcePolicyException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, ObligationDischargeException {
        /*
         * Now create the XACML request
         */
        final LinkedList<XACMLAttributes> actionList = new LinkedList<>();
        final XACMLAttributes action = new XACMLAttributes();
        action.setDataType(new URI(DATATYPE_STRING));
        action.setIdentifier(new URI("urn:eSENS:outcome"));
        actionList.add(action);
        action.setValue(messageStatus);

        final LinkedList<XACMLAttributes> environmentList = new LinkedList<>();
        final XACMLAttributes environment = new XACMLAttributes();
        environment.setDataType(new URI(DATATYPE_DATETIME));
        environment.setIdentifier(new URI("urn:esens:2014:event"));
        environment.setValue(new DateTime().toString());
        environmentList.add(environment);

        final XACMLRequestCreator requestCreator = new XACMLRequestCreator(messageType, null, null,
                actionList, environmentList);

        final Element request = requestCreator.getRequest();
        Utilities.serialize(request);

        /*
         * Call the XACML engine.
         * The policy has been deployed in the setupBeforeClass.
         */
        final EnforcePolicy enforcePolicy = new EnforcePolicy(simplePDP);
        enforcePolicy.decide(request);

        Utilities.serialize(enforcePolicy.getResponseAsDocument().getDocumentElement());
        final List<ESensObligation> obligations = enforcePolicy.getObligationList();
        final Context context = new Context();
        context.setIncomingMsg(incomingSoap);

        /* Loading the different certificates */

        context.setIssuerCertificate(issuerCert);
        context.setSenderCertificate(senderCert);
        context.setRecipientCertificate(recipientCert);

        /* Signing key is the issuer key */
        context.setSigningKey(key);
        context.setSubmissionTime(submissionTime);
        context.setEvent(eventType);
        context.setMessageUUID(msguuid);
        context.setAuthenticationMethod("http://uri.etsi.org/REM/AuthMethod#Strong");
        context.setRequest(request);
        context.setEnforcer(enforcePolicy);
        final ObligationHandlerFactory handlerFactory = ObligationHandlerFactory.getInstance();
        final List<ObligationHandler> handlers = handlerFactory.createHandler(messageType, obligations, context);
        return handlers;
    }

    /**
     * @return
     */
    private static String getPath() {

        final String exportPath = System.getenv("EPSOS_PROPS_PATH");
        final String evidencesPath = exportPath + "obligations" + File.separator;
        LOGGER.debug("Evidences Path: '{}'", evidencesPath);
        return evidencesPath;
    }

    /**
     * @param uuid
     * @param title
     * @return
     */
    private static String getDocumentTitle(String uuid, final String title, final String evidenceType) {

        //ISO 8601 format: 2017-11-25T10:59:53Z
        String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        date = StringUtils.replace(date, ":", "-");
        uuid = StringUtils.replace(uuid, ":", "_");
        return date + "_" + evidenceType + "_" + (StringUtils.isNotBlank(uuid) ? uuid : "NO-UUID") + "_" + title;
    }

    /**
     * @param file
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document readMessage(final String file) throws ParserConfigurationException, SAXException, IOException {

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setNamespaceAware(true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new File(file));
    }

    public static Document readXmlMessage(final String xml) throws ParserConfigurationException, SAXException, IOException {

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setNamespaceAware(true);
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(xml);
    }

    /**
     * @param keyStorePath
     * @param keyPassword
     * @param certAlias
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public static X509Certificate getCertificate(final String keyStorePath, final String keyPassword, final String certAlias)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        LOGGER.debug("X509Certificate getCertificate('{}', '{}', '{}')", keyStorePath, StringUtils.isNotBlank(keyPassword) ? HIDDEN_CREDS : "N/A", certAlias);
        final KeyStore ks = KeyStore.getInstance("JKS");
        final InputStream keyStream = new FileInputStream(new File(keyStorePath));
        ks.load(keyStream, keyPassword == null ? null : keyPassword.toCharArray());
        return (X509Certificate) ks.getCertificate(certAlias);
    }

    /**
     * @param keyStorePath
     * @param keyPassword
     * @param certAlias
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     */
    public static PrivateKey getSigningKey(final String keyStorePath, final String keyPassword, final String certAlias)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        LOGGER.debug("PrivateKey getSigningKey('{}', '{}', '{}')", keyStorePath, StringUtils.isNotBlank(keyPassword) ? HIDDEN_CREDS : "N/A", certAlias);
        final KeyStore ks = KeyStore.getInstance("JKS");
        final InputStream keyStream = new FileInputStream(keyStorePath);
        ks.load(keyStream, keyPassword == null ? null : keyPassword.toCharArray());
        return (PrivateKey) ks.getKey(certAlias, keyPassword == null ? null : keyPassword.toCharArray());
    }

    /**
     * Print message in Clinical logs DEBUG.
     *
     * @param message - Incoming SOAP message.
     */
    private static void logMessage(final Document message) {

        if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())
                && LOGGER_CLINICAL.isDebugEnabled()) {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                Utilities.serialize(message.getDocumentElement(), outputStream);
                final String messageAsString = outputStream.toString();
                LOGGER_CLINICAL.debug("Message:\n'{}'", messageAsString);
            } catch (final TransformerException e) {
                LOGGER_CLINICAL.error("TransformerException: Cannot display Incoming Message '{}'", e.getMessage());
            }
        }
    }
}
