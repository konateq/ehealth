package eu.epsos.util;

import eu.esens.abb.nonrep.*;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
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
import tr.com.srdc.epsos.util.FileUtil;
import tr.com.srdc.epsos.util.XMLUtil;

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

    private EvidenceUtils() {
    }

    /**
     * @param messageType
     * @return
     */
    private static boolean checkCorrectnessOfIHEXCA(final MessageType messageType) {
        // TODO: Review the reason why the MessageType is not analyzed and the validity is set to true by default.
        return true;
    }

    /**
     * @param incomingMsg
     * @param issuerKeyStorePath
     * @param issuerKeyPassword
     * @param issuerCertAlias
     * @param senderKeyStorePath
     * @param senderKeyPassword
     * @param senderCertAlias
     * @param recipientKeyStorePath
     * @param recipientKeyPassword
     * @param recipientCertAlias
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
    public static void createEvidenceREMNRR(Document incomingMsg, String issuerKeyStorePath, String issuerKeyPassword,
                                            String issuerCertAlias, String senderKeyStorePath, String senderKeyPassword,
                                            String senderCertAlias, String recipientKeyStorePath, String recipientKeyPassword,
                                            String recipientCertAlias, String eventType, DateTime submissionTime,
                                            String status, String title)
            throws IOException, URISyntaxException, TOElementException, EnforcePolicyException, ObligationDischargeException,
            TransformerException, SyntaxException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRR()\nIncoming message:\n'{}'\n Issuer Info: '{}'-'{}'-'{}', " +
                            "Sender Info: '{}'-'{}'-'{}', Recipient Info: '{}'-'{}'-'{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingMsg), issuerKeyStorePath, issuerKeyPassword, issuerCertAlias, senderKeyStorePath,
                    senderKeyPassword, senderCertAlias, recipientKeyStorePath, recipientKeyPassword, recipientCertAlias, eventType,
                    submissionTime, status, title);
        }
        MessageType messageType;
        String messageIdentifier;
        try {
            MessageInspector messageInspector = new MessageInspector(incomingMsg);
            logMessage(incomingMsg);
            messageType = messageInspector.getMessageType();
            messageIdentifier = messageInspector.getMessageUUID();
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingMsg);
            messageIdentifier = UUID.randomUUID().toString();
        }
        LOGGER.info("[Evidence Emitter] Creation of REMNRR for message type: '{}' with ID: '{}'", messageType, messageIdentifier);
        createEvidenceREMNRR(incomingMsg, issuerKeyStorePath, issuerKeyPassword, issuerCertAlias, senderKeyStorePath,
                senderKeyPassword, senderCertAlias, recipientKeyStorePath, recipientKeyPassword, recipientCertAlias,
                eventType, submissionTime, status, title, messageIdentifier);
    }

    /**
     * @param incomingMsg
     * @param issuerKeyStorePath
     * @param issuerKeyPassword
     * @param issuerCertAlias
     * @param senderKeyStorePath
     * @param senderKeyPassword
     * @param senderCertAlias
     * @param recipientKeyStorePath
     * @param recipientKeyPassword
     * @param recipientCertAlias
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
    public static void createEvidenceREMNRR(Document incomingMsg, String issuerKeyStorePath, String issuerKeyPassword,
                                            String issuerCertAlias, String senderKeyStorePath, String senderKeyPassword,
                                            String senderCertAlias, String recipientKeyStorePath, String recipientKeyPassword,
                                            String recipientCertAlias, String eventType, DateTime submissionTime,
                                            String status, String title, String msguuid)
            throws IOException, URISyntaxException, TOElementException, EnforcePolicyException,
            ObligationDischargeException, TransformerException, SyntaxException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {


        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRR()\nIncoming message:\n'{}'\n Issuer Info: '{}'-'{}'-'{}', " +
                            "Sender Info: '{}'-'{}'-'{}', Recipient Info: '{}'-'{}'-'{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingMsg), issuerKeyStorePath, issuerKeyPassword, issuerCertAlias, senderKeyStorePath,
                    senderKeyPassword, senderCertAlias, recipientKeyStorePath, recipientKeyPassword, recipientCertAlias, eventType,
                    submissionTime, status, title, msguuid);
        }
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isInfoEnabled()) {
            LOGGER_CLINICAL.info("DOCUMENT:\n'{}'", XMLUtil.documentToString(incomingMsg));
            LOGGER_CLINICAL.info("MSGUUID: '{}'", msguuid);
        }
        String statusmsg = "failure";
        if (StringUtils.equals("0", status)) {
            statusmsg = "success";
        }

        PDP simplePDP = SimplePDPFactory.getSimplePDP();
        UnorderedPolicyRepository polrep = (UnorderedPolicyRepository) simplePDP.getPolicyRepository();
        ClassLoader loader = EvidenceUtils.class.getClassLoader();
        InputStream inputStream = loader.getResourceAsStream("policy/samplePolicyNRR.xml");
        polrep.deploy(PolicyMarshaller.unmarshal(inputStream));

        /*
         * Instantiate the message inspector, to see which type of message is
         */
        MessageType messageType;

        try {
            MessageInspector messageInspector = new MessageInspector(incomingMsg);
            logMessage(incomingMsg);
            messageType = messageInspector.getMessageType();
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingMsg);
        }
        /*
         * Now create the XACML request
         */
        LinkedList<XACMLAttributes> actionList = new LinkedList<>();
        XACMLAttributes action = new XACMLAttributes();
        action.setDataType(new URI(DATATYPE_STRING));
        action.setIdentifier(new URI("urn:eSENS:outcome"));
        actionList.add(action);
        action.setValue(statusmsg);

        LinkedList<XACMLAttributes> environmentList = new LinkedList<>();
        XACMLAttributes environment = new XACMLAttributes();
        environment.setDataType(new URI(DATATYPE_DATETIME));
        environment.setIdentifier(new URI("urn:esens:2014:event"));
        environment.setValue(new DateTime().toString());
        environmentList.add(environment);

        XACMLRequestCreator requestCreator = new XACMLRequestCreator(messageType, null, null,
                actionList, environmentList);

        Element request = requestCreator.getRequest();

        // just some printouts
        Utilities.serialize(request);

        EnforcePolicy enforcePolicy = new EnforcePolicy(simplePDP);
        enforcePolicy.decide(request);

        Utilities.serialize(enforcePolicy.getResponseAsDocument().getDocumentElement());

        List<ESensObligation> obligations = enforcePolicy.getObligationList();

        Context context = new Context();
        context.setIncomingMsg(incomingMsg);

        /* Loading the different certificates */
        X509Certificate issuerCert = getCertificate(issuerKeyStorePath, issuerKeyPassword, issuerCertAlias);
        X509Certificate senderCert = getCertificate(senderKeyStorePath, senderKeyPassword, senderCertAlias);
        X509Certificate recipientCert = getCertificate(recipientKeyStorePath, recipientKeyPassword, recipientCertAlias);
        context.setIssuerCertificate(issuerCert);
        context.setSenderCertificate(senderCert);
        context.setRecipientCertificate(recipientCert);

        /* Signing key is the issuer key */
        PrivateKey key = getSigningKey(issuerKeyStorePath, issuerKeyPassword, issuerCertAlias);
        context.setSigningKey(key);
        context.setSubmissionTime(submissionTime);
        context.setEvent(eventType);
        context.setMessageUUID(msguuid);
        context.setAuthenticationMethod("http://uri.etsi.org/REM/AuthMethod#Strong");
        context.setRequest(request);
        context.setEnforcer(enforcePolicy);

        ObligationHandlerFactory handlerFactory = ObligationHandlerFactory.getInstance();
        List<ObligationHandler> handlers = handlerFactory.createHandler(messageType, obligations, context);

        for (ObligationHandler oh : handlers) {

            oh.discharge();
            Utilities.serialize(oh.getMessage().getDocumentElement());
            String oblString = XMLUtil.documentToString(oh.getMessage());
            if (title == null || title.isEmpty()) {
                title = getPath() + "nrr" + File.separator + getDocumentTitle(msguuid, oh.toString(), "NRR") + ".xml";
            } else {
                title = getPath() + "nrr" + File.separator + getDocumentTitle(msguuid, title, "NRR") + ".xml";
            }
            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isInfoEnabled()) {
                LOGGER_CLINICAL.info("MSGUUID: '{}'  NRR TITLE: '{}'", msguuid, title);
                LOGGER_CLINICAL.info("NRR:\n'{}'", oblString);
            }
            FileUtil.constructNewFile(title, oblString.getBytes());
        }
    }

    /**
     * @param incomingSoap
     * @param issuerKeyStorePath
     * @param issuerKeyPassword
     * @param issuerCertAlias
     * @param senderKeyStorePath
     * @param senderKeyPassword
     * @param senderCertAlias
     * @param recipientKeyStorePath
     * @param recipientKeyPassword
     * @param recipientCertAlias
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @throws Exception
     */
    public static void createEvidenceREMNRO(Document incomingSoap, String issuerKeyStorePath, String issuerKeyPassword,
                                            String issuerCertAlias, String senderKeyStorePath, String senderKeyPassword,
                                            String senderCertAlias, String recipientKeyStorePath, String recipientKeyPassword,
                                            String recipientCertAlias, String eventType, DateTime submissionTime, String status,
                                            String title) throws Exception {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRO()\nIncoming message:\n'{}'\n Issuer Info: '{}'-'{}'-'{}', " +
                            "Sender Info: '{}'-'{}'-'{}', Recipient Info: '{}'-'{}'-'{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingSoap), issuerKeyStorePath, issuerKeyPassword, issuerCertAlias, senderKeyStorePath,
                    senderKeyPassword, senderCertAlias, recipientKeyStorePath, recipientKeyPassword, recipientCertAlias, eventType, submissionTime, status, title);
        }
        MessageType messageType;
        String msguuid;
        try {
            MessageInspector messageInspector = new MessageInspector(incomingSoap);
            logMessage(incomingSoap);
            messageType = messageInspector.getMessageType();
            msguuid = messageInspector.getMessageUUID();
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingSoap);
            msguuid = UUID.randomUUID().toString();
        }
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.info("MSGUUID: '{}'", msguuid);
            LOGGER_CLINICAL.info("Evidences for MessageType: '{}'", messageType.getClass());
        }
        createEvidenceREMNRO(incomingSoap, issuerKeyStorePath, issuerKeyPassword,
                issuerCertAlias, senderKeyStorePath, senderKeyPassword,
                senderCertAlias, recipientKeyStorePath, recipientKeyPassword,
                recipientCertAlias, eventType, submissionTime, status, title, msguuid);
    }

    /**
     * @param incomingSoap
     * @param issuerKeyStorePath
     * @param issuerKeyPassword
     * @param issuerCertAlias
     * @param senderKeyStorePath
     * @param senderKeyPassword
     * @param senderCertAlias
     * @param recipientKeyStorePath
     * @param recipientKeyPassword
     * @param recipientCertAlias
     * @param eventType
     * @param submissionTime
     * @param status
     * @param title
     * @param msguuid
     * @throws Exception
     */
    public static void createEvidenceREMNRO(Document incomingSoap, String issuerKeyStorePath, String issuerKeyPassword,
                                            String issuerCertAlias, String senderKeyStorePath, String senderKeyPassword,
                                            String senderCertAlias, String recipientKeyStorePath, String recipientKeyPassword,
                                            String recipientCertAlias, String eventType, DateTime submissionTime,
                                            String status, String title, String msguuid) throws Exception {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[Evidences] createEvidenceREMNRO()\nIncoming message:\n'{}'\n Issuer Info: '{}'-'{}'-'{}', " +
                            "Sender Info: '{}'-'{}'-'{}', Recipient Info: '{}'-'{}'-'{}'\nEvent Info: '{}'-'{}'-'{}'-'{}'-'{}'",
                    XMLUtil.documentToString(incomingSoap), issuerKeyStorePath, issuerKeyPassword, issuerCertAlias, senderKeyStorePath,
                    senderKeyPassword, senderCertAlias, recipientKeyStorePath, recipientKeyPassword, recipientCertAlias, eventType,
                    submissionTime, status, title, msguuid);
        }
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.info("DOCUMENT:\n'{}'", XMLUtil.documentToString(incomingSoap));
            LOGGER_CLINICAL.info("MSGUUID: '{}'", msguuid);
        }
        String statusmsg = "failure";
        if (StringUtils.equals("0", status)) {

            statusmsg = "success";
        }

        PDP simplePDP = SimplePDPFactory.getSimplePDP();
        UnorderedPolicyRepository polrep = (UnorderedPolicyRepository) simplePDP.getPolicyRepository();

        JAXBMarshallerConfiguration conf = new JAXBMarshallerConfiguration();
        conf.setValidateParsing(false);
        conf.setValidateWriting(false);
        PolicyMarshaller.setJAXBMarshallerConfiguration(conf);
        // Populate the policy repository
        ClassLoader loader;
        loader = EvidenceUtils.class.getClassLoader();
        InputStream inputStream = loader.getResourceAsStream("policy/samplePolicy.xml");
        polrep.deploy(PolicyMarshaller.unmarshal(inputStream));

        // Read the message as it arrives at the facade
        MessageType messageType;
        try {
            MessageInspector messageInspector = new MessageInspector(incomingSoap);
            logMessage(incomingSoap);
            messageType = messageInspector.getMessageType();
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            messageType = new UnknownMessageType(incomingSoap);
        }
        if (checkCorrectnessOfIHEXCA(messageType)) {
            LOGGER.info("The message type : '{}' is correct.", messageType);
        }

        /*
         * Now create the XACML request
         */
        LinkedList<XACMLAttributes> actionList = new LinkedList<>();
        XACMLAttributes action = new XACMLAttributes();
        action.setDataType(new URI(DATATYPE_STRING));
        action.setIdentifier(new URI("urn:eSENS:outcome"));
        actionList.add(action);
        action.setValue(statusmsg);

        LinkedList<XACMLAttributes> environmentList = new LinkedList<>();
        XACMLAttributes environment = new XACMLAttributes();
        environment.setDataType(new URI(DATATYPE_DATETIME));
        environment.setIdentifier(new URI("urn:esens:2014:event"));
        environment.setValue(new DateTime().toString());
        environmentList.add(environment);

        XACMLRequestCreator requestCreator = new XACMLRequestCreator(messageType, null, null,
                actionList, environmentList);

        Element request = requestCreator.getRequest();
        Utilities.serialize(request);

        /*
         * Call the XACML engine.
         *
         * The policy has been deployed in the setupBeforeClass.
         */
        EnforcePolicy enforcePolicy = new EnforcePolicy(simplePDP);
        enforcePolicy.decide(request);

        Utilities.serialize(enforcePolicy.getResponseAsDocument().getDocumentElement());
        List<ESensObligation> obligations = enforcePolicy.getObligationList();
        Context context = new Context();
        context.setIncomingMsg(incomingSoap);

        /* Loading the different certificates */
        X509Certificate issuerCert = getCertificate(issuerKeyStorePath, issuerKeyPassword, issuerCertAlias);
        X509Certificate senderCert = getCertificate(senderKeyStorePath, senderKeyPassword, senderCertAlias);
        X509Certificate recipientCert = getCertificate(recipientKeyStorePath, recipientKeyPassword, recipientCertAlias);
        context.setIssuerCertificate(issuerCert);
        context.setSenderCertificate(senderCert);
        context.setRecipientCertificate(recipientCert);

        /* Signing key is the issuer key */
        PrivateKey key = getSigningKey(issuerKeyStorePath, issuerKeyPassword, issuerCertAlias);
        context.setSigningKey(key);
        context.setSubmissionTime(submissionTime);
        context.setEvent(eventType);
        context.setMessageUUID(msguuid);
        context.setAuthenticationMethod("http://uri.etsi.org/REM/AuthMethod#Strong");
        context.setRequest(request);
        context.setEnforcer(enforcePolicy);
        ObligationHandlerFactory handlerFactory = ObligationHandlerFactory.getInstance();
        List<ObligationHandler> handlers = handlerFactory.createHandler(messageType, obligations, context);

        // Here I discharge manually. This behavior is to let free an implementation
        for (ObligationHandler handler : handlers) {

            handler.discharge();
            Utilities.serialize(handler.getMessage().getDocumentElement());
            String oblString = XMLUtil.documentToString(handler.getMessage());
            if (title == null || title.isEmpty()) {
                title = getPath() + "nro" + File.separator + getDocumentTitle(msguuid, handler.toString(), "NRO") + ".xml";
            } else {
                title = getPath() + "nro" + File.separator + getDocumentTitle(msguuid, title, "NRO") + ".xml";
            }
            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isInfoEnabled()) {
                LOGGER_CLINICAL.info("MSGUUID: '{}'  NRO TITLE: '{}'", msguuid, title);
                LOGGER_CLINICAL.info("NRO:\n'{}'", oblString);
            }
            FileUtil.constructNewFile(title, oblString.getBytes());
        }
    }

    /**
     * @return
     */
    private static String getPath() {

        String exportPath = System.getenv("EPSOS_PROPS_PATH");
        String evidencesPath = exportPath + "obligations" + File.separator;
        LOGGER.debug("Evidences Path: '{}'", evidencesPath);
        return evidencesPath;
    }

    /**
     * @param uuid
     * @param title
     * @return
     */
    private static String getDocumentTitle(String uuid, String title, String evidenceType) {

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
    public static Document readMessage(String file) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new File(file));
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
    private static X509Certificate getCertificate(String keyStorePath, String keyPassword, String certAlias)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {

        LOGGER.debug("X509Certificate getCertificate('{}', '{}', '{}')", keyStorePath, StringUtils.isNotBlank(keyPassword) ? "******" : "N/A", certAlias);
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream keyStream = new FileInputStream(new File(keyStorePath));
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
    private static PrivateKey getSigningKey(String keyStorePath, String keyPassword, String certAlias)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        LOGGER.debug("PrivateKey getSigningKey('{}', '{}', '{}')", keyStorePath, StringUtils.isNotBlank(keyPassword) ? "******" : "N/A", certAlias);
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream keyStream = new FileInputStream(new File(keyStorePath));
        ks.load(keyStream, keyPassword == null ? null : keyPassword.toCharArray());
        return (PrivateKey) ks.getKey(certAlias, keyPassword == null ? null : keyPassword.toCharArray());
    }

    /**
     * Print message in Clinical logs DEBUG.
     *
     * @param message - Incoming SOAP message.
     */
    private static void logMessage(Document message) {

        if (!StringUtils.equals(System.getProperty(OpenNCPConstants.SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name())
                && LOGGER_CLINICAL.isDebugEnabled()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                Utilities.serialize(message.getDocumentElement(), outputStream);
                String messageAsString = outputStream.toString();
                LOGGER_CLINICAL.debug("Message:\n'{}'", messageAsString);
            } catch (TransformerException e) {
                LOGGER_CLINICAL.error("TransformerException: Cannot display Incoming Message '{}'", e.getMessage());
            }
        }
    }
}
