package eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.web.rest;

import eu.europa.ec.dynamicdiscovery.DynamicDiscovery;
import eu.europa.ec.dynamicdiscovery.exception.ConnectionException;
import eu.europa.ec.dynamicdiscovery.exception.TechnicalException;
import eu.europa.ec.dynamicdiscovery.model.DocumentIdentifier;
import eu.europa.ec.dynamicdiscovery.model.ParticipantIdentifier;
import eu.europa.ec.sante.openncp.common.configuration.StandardProperties;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import eu.europa.ec.sante.openncp.webmanager.backend.WebManagerBackendException;
import eu.europa.ec.sante.openncp.webmanager.backend.error.ApiException;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.cfg.ReadSMPProperties;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.domain.ReferenceCollection;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.domain.SMPType;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.AuditManager;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.SimpleErrorHandler;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.service.DynamicDiscoveryClient;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.util.SslUtil;
import eu.europa.ec.sante.openncp.webmanager.backend.service.ExceptionType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

@RestController
@RequestMapping(path = "/api")
public class SMPDeleteFileController {

    private static final String HTTPS_PROTOCOL = "https";
    private static final String ERROR_RESULT = "\n ***************** ERROR RESULT - '{}'";
    private static final String ERROR_DESCRIPTION = "ErrorDescription";
    private static final String BUSINESS_CODE = "BusinessCode";
    private static final String ERROR_NOUSER = "error.nouser";
    private static final String ERROR_SERVER_FAILED = "error.server.failed";
    private static final String SMP_ADMIN_URL = "SMP_ADMIN_URL";
    private static final String IOEXCEPTION = "\n IOException - {}";
    private static final String URI_SYNTAX_EXCEPTION = "URISyntaxException";
    private static final String URI = "\n ************** URI - {}";
    private static final String PROTOCOL_HTTPS = "https://";
    private static final String PROTOCOL_HTTP = "http://";
    private static final String SMP_TYPE_REFERENCE = "SMP Type- '{}', reference - '{}'";
    private static final String REFERENCES_DECODED_UTF_8 = "references decoded UTF-8 - {}";
    private static final String DELETE_RESPONSE_STATUS_REASON = "Delete Response Status - {}, Reason - {} ";
    private static final String SUFFIX = "/";
    private static final String TECHNICAL_EXCEPTION = "TechnicalException";
    private final Logger logger = LoggerFactory.getLogger(SMPDeleteFileController.class);
    private final DynamicDiscoveryClient dynamicDiscoveryClient;
    private final DynamicDiscoveryService dynamicDiscoveryService;
    private final ReadSMPProperties readProperties;
    private final Environment env;
    private final PropertyService propertyService;
    private final AuditManager auditManager;
    private final SslUtil sslUtil;

    @Autowired
    public SMPDeleteFileController(final DynamicDiscoveryClient dynamicDiscoveryClient,
                                   final DynamicDiscoveryService dynamicDiscoveryService,
                                   final ReadSMPProperties readProperties,
                                   final Environment env,
                                   final PropertyService propertyService,
                                   final AuditManager auditManager,
                                   final SslUtil sslUtil) {
        this.dynamicDiscoveryClient = dynamicDiscoveryClient;
        this.dynamicDiscoveryService = Validate.notNull(dynamicDiscoveryService, "DynamicDiscoveryService must not be null");
        this.readProperties = readProperties;
        this.env = env;
        this.propertyService = Validate.notNull(propertyService, "PropertyService must not be null");
        this.auditManager = Validate.notNull(auditManager, "AuditManager must not be null");
        this.sslUtil = Validate.notNull(sslUtil, "SslUtil must not be null");
    }

    @GetMapping(path = "smpeditor/smpfileinfo")
    public ResponseEntity<List<ReferenceCollection>> getFilesToDeleteInfo(final String countryName) {

        final String partID = "urn:ehealth:" + countryName + ":ncp-idp";
        final String partScheme = env.getProperty("ParticipantIdentifier.Scheme");

        boolean success = true;
        String errorType = "";
        final ParticipantIdentifier participantIdentifier = new ParticipantIdentifier(partID, partScheme);
        DynamicDiscovery smpClient = null;

        try {
            smpClient = dynamicDiscoveryClient.getInstance();
        } catch (final ConnectionException ex) {
            success = false;
            if (logger.isErrorEnabled()) {
                logger.error("\n ConnectionException - {}", SimpleErrorHandler.printExceptionStackTrace(ex));
            }
        } catch (final TechnicalException | CertificateException | KeyStoreException | IOException |
                       NoSuchAlgorithmException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Connection Exception: '{}'", e.getMessage(), e);
            }
        }

        List<DocumentIdentifier> documentIdentifiers = null;
        try {
            assert smpClient != null;
            documentIdentifiers = smpClient.getService().getServiceGroup(participantIdentifier).getDocumentIdentifiers();
        } catch (final TechnicalException ex) {
            throw new WebManagerBackendException(ExceptionType.SMP_EXCEPTION, ex, ex.getMessage());
        }

        URI serviceGroup = null;
        final List<ReferenceCollection> referenceCollection = new ArrayList<>();
        URI smpURI = null;
        int i = 0;

        if (!documentIdentifiers.isEmpty()) {
            for (final DocumentIdentifier documentIdentifier : documentIdentifiers) {
                String smptype = "Unknown type";
                String documentID = "";
                final Map<String, String> propertiesMap = readProperties.readPropertiesFile();
                final Set set2 = propertiesMap.entrySet();
                for (final Object aSet2 : set2) {
                    final Map.Entry mentry2 = (Map.Entry) aSet2;
                    if (StringUtils.equalsIgnoreCase(documentIdentifier.getIdentifier(), mentry2.getKey().toString())) {
                        final String[] docs = mentry2.getValue().toString().split("\\.");
                        documentID = docs[0];
                        break;
                    }
                }
                final String smpType = documentID;
                if (logger.isDebugEnabled()) {
                    logger.debug("\n******** DOC ID - '{}'", documentIdentifier.getIdentifier());
                    logger.debug("\n******** SMP Type - '{}'", smpType);
                }
                for (final SMPType smptype1 : SMPType.values()) {
                    if (smptype1.name().equals(smpType)) {
                        smptype = smptype1.getDescription();
                        break;
                    }
                }

                try {
                    smpURI = smpClient.getService().getMetadataLocator().lookup(participantIdentifier);
                } catch (final TechnicalException ex) {
                    success = false;
                    errorType = TECHNICAL_EXCEPTION;
                    if (logger.isErrorEnabled()) {
                        logger.error("\n TechnicalException - {}", SimpleErrorHandler.printExceptionStackTrace(ex));
                    }
                }
                final URI uri = smpClient.getService().getMetadataProvider().resolveServiceMetadata(smpURI, participantIdentifier, documentIdentifier);
                final ReferenceCollection reference = new ReferenceCollection();
                reference.setReference(uri.toString());
                reference.setSmpType(smptype);
                reference.setSmpUri(smpURI.toString());
                reference.setId(i++);
                referenceCollection.add(reference);

                serviceGroup = smpClient.getService().getMetadataProvider().resolveDocumentIdentifiers(smpURI, participantIdentifier);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Smp file list is empty");
            }
            //  Returning empty List and  no Audit message expected in this case.
            return ResponseEntity.ok(Collections.emptyList());
        }

        //Audit
        final String objectID = serviceGroup.toString();
        final byte[] encodedObjectID = Base64.encodeBase64(objectID.getBytes());

        if (success) {
            auditManager.handleDynamicDiscoveryQuery(smpURI.toString(), new String(encodedObjectID), null, null);
            return ResponseEntity.ok(referenceCollection);
        } else {
            auditManager.handleDynamicDiscoveryQuery(smpURI.toString(), new String(encodedObjectID), "500", errorType.getBytes());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, errorType);
        }
    }

    @DeleteMapping(value = "smpeditor/smpfile")
    public ResponseEntity deleteSmpFile(@RequestBody final ReferenceCollection ref) {
        if (logger.isInfoEnabled()) {
            logger.info("[REST Api] Delete SMP file: '{}' from server: '{}'",
                    sanitizeString(ref.getReference()), sanitizeString(ref.getSmpUri()));
        }

        String urlServer = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_ADMIN_URL);
        if (urlServer.endsWith(SUFFIX)) {
            urlServer = urlServer.substring(0, urlServer.length() - 1);
        }
        /*Removes https:// from entered by the user so it won't repeat in uri set scheme*/
        if (urlServer.startsWith(HTTPS_PROTOCOL)) {
            urlServer = urlServer.substring(8);
        }

        String reference = ref.getReference();
        final String smpType = ref.getSmpType();
        if (logger.isDebugEnabled()) {
            logger.debug(SMP_TYPE_REFERENCE, sanitizeString(smpType), sanitizeString(reference));
        }

        if (reference.startsWith(PROTOCOL_HTTP) || reference.startsWith(PROTOCOL_HTTPS)) {
            reference = reference.substring(ref.getSmpUri().length());
        }

        reference = URLDecoder.decode(reference, StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            logger.debug(REFERENCES_DECODED_UTF_8, sanitizeString(reference));
        }
        final URI uri;
        try {
            uri = new URIBuilder().setScheme(HTTPS_PROTOCOL).setHost(urlServer).setPath(reference).build();
            if (logger.isDebugEnabled()) {
                logger.debug(URI, uri);
            }
        } catch (final URISyntaxException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, URI_SYNTAX_EXCEPTION);
        }

        // Trust own CA and all self-signed certs
        final SSLContext sslcontext = sslUtil.createSSLContext();

        //DELETE
        final HttpDelete httpdelete = new HttpDelete(uri);

        final CloseableHttpResponse response;
        try (final CloseableHttpClient closeableHttpClient = dynamicDiscoveryService.buildHttpClient(sslcontext)) {
            response = closeableHttpClient.execute(httpdelete);
        } catch (final IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IOException - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        /*Get response*/
        final int responseStatus = response.getStatusLine().getStatusCode();
        final String responseReason = response.getStatusLine().getReasonPhrase();
        if (logger.isDebugEnabled()) {
            logger.debug(DELETE_RESPONSE_STATUS_REASON, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        }

        final HttpEntity entity = response.getEntity();

        //Audit vars
        final String remoteIp = propertyService.getPropertyValueMandatory(SMP_ADMIN_URL);
        //ET_ObjectID --> Base64 of url
        final String objectID = uri.toString();
        final byte[] encodedObjectID = Base64.encodeBase64(objectID.getBytes());
        final byte[] encodedObjectDetail = Base64.encodeBase64(responseReason.getBytes());

        switch (responseStatus) {
            case 503:
            case 405:
                //Audit error
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        Integer.toString(responseStatus), encodedObjectDetail);
                throw new ApiException(HttpStatus.valueOf(responseStatus), env.getProperty(ERROR_SERVER_FAILED));
            case 401:
                //Audit error
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        Integer.toString(responseStatus), encodedObjectDetail);
                throw new ApiException(HttpStatus.valueOf(responseStatus), env.getProperty(ERROR_NOUSER));
            case 200:
            case 201:
                //Audit Success
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        null, null);
                return ResponseEntity.ok().build();
            default:
                break;
        }
        /* Get BusinessCode and ErrorDescription from response */

        //Save InputStream of response in ByteArrayOutputStream in order to read it more than once.
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(entity.getContent(), byteArrayOutputStream);
        } catch (final IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "\n IOException response - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        } catch (final UnsupportedOperationException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "\n UnsupportedOperationException response - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        }
        final byte[] bytes = byteArrayOutputStream.toByteArray();

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;
        try {
            //factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(bais);
            final Element element = doc.getDocumentElement();
            final NodeList nodes = element.getChildNodes();
            for (int j = 0; j < nodes.getLength(); j++) {
                if (nodes.item(j).getNodeName().equals(BUSINESS_CODE)) {
                    final String businessCode = nodes.item(j).getTextContent();
                    logger.debug("{}", businessCode);
                }
                if (nodes.item(j).getNodeName().equals(ERROR_DESCRIPTION)) {
                    final String errorDescription = nodes.item(j).getTextContent();
                    logger.debug("{}", errorDescription);
                }
            }
        } catch (final ParserConfigurationException | IOException | SAXException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName() + " - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        /*transform xml to string in order to send in Audit*/
        final String errorResult = auditManager.prepareEventLog(bytes);
        logger.debug(ERROR_RESULT, errorResult);
        //Audit error
        auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                Integer.toString(responseStatus), errorResult.getBytes());

        return ResponseEntity.status(responseStatus).build();
    }

    @PostMapping(value = "smpeditor/deleteSmpFile")
    public ResponseEntity postDeleteSmpFile(@RequestBody final ReferenceCollection ref) {

        String urlServer = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_ADMIN_URL);
        if (urlServer.endsWith(SUFFIX)) {
            urlServer = urlServer.substring(0, urlServer.length() - 1);
        }
        //  Removes https:// from entered by the user so it won't repeat in uri set scheme
        if (urlServer.startsWith(HTTPS_PROTOCOL)) {
            urlServer = urlServer.substring(8);
        }

        String reference = ref.getReference();
        final String smpType = ref.getSmpType();
        if (logger.isDebugEnabled()) {
            logger.debug(SMP_TYPE_REFERENCE, sanitizeString(smpType), sanitizeString(reference));
        }

        if (reference.startsWith(PROTOCOL_HTTP) || reference.startsWith(PROTOCOL_HTTPS)) {
            reference = reference.substring(ref.getSmpUri().length());
        }

        reference = URLDecoder.decode(reference, StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            logger.debug(REFERENCES_DECODED_UTF_8, sanitizeString(reference));
        }

        URI uri = null;
        try {
            uri = new URIBuilder().setScheme(HTTPS_PROTOCOL).setHost(urlServer).setPath(reference).build();
            logger.debug(URI, uri);
        } catch (final URISyntaxException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, URI_SYNTAX_EXCEPTION);
        }

        // Trust own CA and all self-signed certs
        final SSLContext sslcontext = sslUtil.createSSLContext();

        //DELETE
        final HttpDelete httpdelete = new HttpDelete(uri);

        final CloseableHttpResponse response;
        try (final CloseableHttpClient closeableHttpClient = dynamicDiscoveryService.buildHttpClient(sslcontext)) {
            response = closeableHttpClient.execute(httpdelete);
        } catch (final IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, IOEXCEPTION + SimpleErrorHandler.printExceptionStackTrace(e));
        }

        //  Get response
        final int responseStatus = response.getStatusLine().getStatusCode();
        final String responseReason = response.getStatusLine().getReasonPhrase();
        logger.debug(DELETE_RESPONSE_STATUS_REASON, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());

        final HttpEntity entity = response.getEntity();

        //  Audit vars
        final String remoteIp = propertyService.getPropertyValueMandatory(SMP_ADMIN_URL);
        //  ET_ObjectID --> Base64 of url
        final String objectID = uri.toString();
        final byte[] encodedObjectID = Base64.encodeBase64(objectID.getBytes());

        switch (responseStatus) {
            case 503:
            case 405:
                //Audit error
                byte[] encodedObjectDetail = Base64.encodeBase64(responseReason.getBytes());
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        Integer.toString(responseStatus), encodedObjectDetail);
                throw new ApiException(HttpStatus.valueOf(responseStatus), env.getProperty(ERROR_SERVER_FAILED));
            case 401:
                //Audit error
                encodedObjectDetail = Base64.encodeBase64(responseReason.getBytes());
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        Integer.toString(responseStatus), encodedObjectDetail);
                throw new ApiException(HttpStatus.valueOf(responseStatus), env.getProperty(ERROR_NOUSER));
            case 200:
            case 201:
                //Audit Success
                auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                        null, null);
                return ResponseEntity.ok().build();
            default:
                break;
        }
        /* Get BusinessCode and ErrorDescription from response */

        //Save InputStream of response in ByteArrayOutputStream in order to read it more than once.
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(entity.getContent(), byteArrayOutputStream);
        } catch (final IOException | UnsupportedOperationException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName() + " - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        }
        final byte[] bytes = byteArrayOutputStream.toByteArray();

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;
        try {
            //factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(bais);
            final Element element = doc.getDocumentElement();
            final NodeList nodes = element.getChildNodes();
            for (int j = 0; j < nodes.getLength(); j++) {
                if (nodes.item(j).getNodeName().equals(BUSINESS_CODE)) {
                    final String businessCode = nodes.item(j).getTextContent();
                    logger.debug("{}", businessCode);
                }
                if (nodes.item(j).getNodeName().equals(ERROR_DESCRIPTION)) {
                    final String errorDescription = nodes.item(j).getTextContent();
                    logger.debug("{}", errorDescription);
                }
            }
        } catch (final ParserConfigurationException | IOException | SAXException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName() + " - " + SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        /*transform xml to string in order to send in Audit*/
        final String errorResult = auditManager.prepareEventLog(bytes);
        logger.debug(ERROR_RESULT, errorResult);
        //Audit error
        auditManager.handleDynamicDiscoveryPush(remoteIp, new String(encodedObjectID),
                Integer.toString(responseStatus), errorResult.getBytes());

        return ResponseEntity.status(responseStatus).build();
    }

    private static String sanitizeString(final String stringToSanitize) {
        return stringToSanitize != null ? stringToSanitize.replaceAll("[\n\r]", "_") : "";
    }

}
