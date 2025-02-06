package eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.web.rest;

import eu.europa.ec.dynamicdiscovery.DynamicDiscovery;
import eu.europa.ec.dynamicdiscovery.exception.TechnicalException;
import eu.europa.ec.dynamicdiscovery.model.DocumentIdentifier;
import eu.europa.ec.dynamicdiscovery.model.ParticipantIdentifier;
import eu.europa.ec.sante.openncp.common.configuration.StandardProperties;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import eu.europa.ec.sante.openncp.webmanager.backend.error.ApiException;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.Constants;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.domain.SMPFileOps;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.domain.SMPHttp;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.AuditManager;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.service.SimpleErrorHandler;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.service.BdxSmpValidator;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.service.DynamicDiscoveryClient;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.smpeditor.service.SMPConverter;
import eu.europa.ec.sante.openncp.webmanager.backend.module.smp.util.SslUtil;
import eu.europa.ec.sante.openncp.webmanager.backend.service.PermissionUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ehdsi.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@RequestMapping(path = "/api")
public class SMPUploadFileController {

    private final Logger logger = LoggerFactory.getLogger(SMPUploadFileController.class);
    private final SMPConverter smpconverter;
    private final Environment environment;
    private final DynamicDiscoveryService dynamicDiscoveryService;
    private final PropertyService propertyService;
    private final SslUtil sslUtil;
    private final AuditManager auditManager;
    private final DynamicDiscoveryClient dynamicDiscoveryClient;

    public SMPUploadFileController(final SMPConverter smpconverter,
                                   final Environment environment,
                                   final AuditManager auditManager,
                                   final DynamicDiscoveryClient dynamicDiscoveryClient,
                                   final DynamicDiscoveryService dynamicDiscoveryService,
                                   final PropertyService propertyService,
                                   final SslUtil sslUtil) {
        this.auditManager = Validate.notNull(auditManager, "AuditManager must not be null");
        this.dynamicDiscoveryClient = dynamicDiscoveryClient;
        this.smpconverter = smpconverter;
        this.environment = environment;
        this.dynamicDiscoveryService = Validate.notNull(dynamicDiscoveryService, "DynamicDiscoveryService must not be null");
        this.propertyService = Validate.notNull(propertyService, "PropertyService must not be null");
        this.sslUtil = Validate.notNull(sslUtil, "SslUtil must not be null");
    }

    @PostMapping(path = "/smpeditor/uploader/fromSmpFileOps")
    public ResponseEntity<SMPHttp> createSMPFileOps(@RequestBody final SMPFileOps smpFileOps) {

        if (smpFileOps.getGeneratedFile() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The requested file does not exists");
        }
        final File file = new File(smpFileOps.getGeneratedFile().getPath());
        final SMPHttp smpHttp = new SMPHttp();
        smpHttp.setSmpFile(file);
        return ResponseEntity.ok(smpHttp);
    }

    @PostMapping(path = "/smpeditor/uploader/fileToUpload")
    public ResponseEntity<SMPHttp> createSMPHttp(@RequestPart final MultipartFile multipartFile) {
        final SMPHttp smpHttp = new SMPHttp();
        PermissionUtil.initializeFolders(Constants.SMP_DIR_PATH);
        final File convFile = new File(Constants.SMP_DIR_PATH + File.separator + multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(convFile);
        } catch (final IOException | IllegalStateException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName());
        }
        smpHttp.setSmpFile(convFile);
        return ResponseEntity.ok(smpHttp);

    }

    @PostMapping(path = "smpeditor/uploader/upload")
    public ResponseEntity<SMPHttp> uploadToServer(@RequestBody final SMPHttp smpHttp) throws Exception {

        final String contentFile = new String(Files.readAllBytes(Paths.get(smpHttp.getSmpFile().getPath())));
        final boolean fileDeleted;

        if (!BdxSmpValidator.validateFile(contentFile)) {
            fileDeleted = smpHttp.getSmpFile().delete();
            if (logger.isDebugEnabled()) {
                logger.debug("Converted File deleted: '{}'", fileDeleted);
            }
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.notsmp"));
        }


        final Object fileConverted = smpconverter.convertFromXml(smpHttp.getSmpFile());
        if (smpconverter.isSignedServiceMetadata(fileConverted)) {
            fileDeleted = smpHttp.getSmpFile().delete();
            logger.debug("Converted File deleted: '{}'", fileDeleted);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("warning.isSigned.sigmenu"));
        }

        final ServiceMetadata serviceMetadata = smpconverter.getServiceMetadata(fileConverted);

        String participantID = "";
        String documentTypeID = "";
        String partID = "";
        String partScheme = "";
        String docID = "";
        String docScheme = "";

        if (serviceMetadata.getRedirect() != null) {
            logger.debug("\n******** REDIRECT");

            if (serviceMetadata.getRedirect().getExtensions().isEmpty()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.notsigned"));
            }

            /* Check if url of redirect is correct */
            final String href = serviceMetadata.getRedirect().getHref();
            final Pattern pattern = Pattern.compile("ehealth-participantid-qns.*");
            final Matcher matcher = pattern.matcher(href);
            if (matcher.find()) {
                String result = matcher.group(0);
                result = java.net.URLDecoder.decode(result, StandardCharsets.UTF_8);
                final String[] ids = result.split("/services/");
                participantID = ids[0];
                documentTypeID = ids[1];
            } else {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.redirect.href"));
            }

        } else if (serviceMetadata.getServiceInformation() != null) {
            logger.debug("\n******** SERVICE INFORMATION");

            if (serviceMetadata.getServiceInformation().getExtensions().isEmpty()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.notsigned"));
            }

            partID = serviceMetadata.getServiceInformation().getParticipantIdentifier().getValue();
            partScheme = serviceMetadata.getServiceInformation().getParticipantIdentifier().getScheme();
            participantID = partScheme + "::" + partID;

            docID = serviceMetadata.getServiceInformation().getDocumentIdentifier().getValue();
            docScheme = serviceMetadata.getServiceInformation().getDocumentIdentifier().getScheme();
            documentTypeID = docScheme + "::" + docID;
        }

        String urlServer = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_ADMIN_URL);
        if (urlServer.endsWith("/")) {
            urlServer = urlServer.substring(0, urlServer.length() - 1);
        }

        final String serviceMetdataUrl = "/" + participantID + "/services/" + documentTypeID;

        // Removes https:// from entered by the user so it won't repeat in uri set scheme
        if (urlServer.startsWith("https")) {
            urlServer = urlServer.substring(8);
        }

        logger.info("Build SMP Admin Uri: '{}' - '{}'", urlServer, serviceMetdataUrl);

        URI uri = null;
        try {

            uri = new URIBuilder().setScheme("https").setHost(urlServer).setPath(serviceMetdataUrl).build();
        } catch (final URISyntaxException ex) {
            logger.error("URISyntaxException: '{}", SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        logger.info("SMP Uri endpoint: '{}'", uri);

        String content = "";
        try (final Scanner scanner = new Scanner(smpHttp.getSmpFile(), StandardCharsets.UTF_8)) {
            content = scanner.useDelimiter("\\Z").next();
        } catch (final IOException ex) {
            logger.error("IOException: '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        final StringEntity entityPut = new StringEntity(content, ContentType.create("application/xml", "UTF-8"));
        if (logger.isDebugEnabled()) {
            logger.debug("Entity that will be put on the SMP server : '{}'", IOUtils.toString(entityPut.getContent(), StandardCharsets.UTF_8));
        }

        // Trust own CA and all self-signed certs
        final SSLContext sslcontext = sslUtil.createSSLContext();

        //PUT
        final HttpPut httpput = new HttpPut(uri);
        httpput.setEntity(entityPut);
        final CloseableHttpResponse response;

        final byte[] responseEntity;
        try (final CloseableHttpClient closeableHttpClient = dynamicDiscoveryService.buildHttpClient(sslcontext)) {
            response = closeableHttpClient.execute(httpput);
            final HttpEntity entity = response.getEntity();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(entity.getContent(), baos);
            responseEntity = baos.toByteArray();
        } catch (final IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.server.failed"), ex);
        }

        // Get Http Client response
        smpHttp.setStatusCode(response.getStatusLine().getStatusCode());

        logger.debug("Http Client Response Status Code: '{}' - Reason: '{}'", response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase());

        //Audit vars
        final String ncp = propertyService.getPropertyValueMandatory("ncp.country");
        final String ncpemail = propertyService.getPropertyValueMandatory("ncp.email");
        final String country = propertyService.getPropertyValueMandatory("COUNTRY_PRINCIPAL_SUBDIVISION");
        final String serverSMPUrl = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_ADMIN_URL);//Source Gateway
        final String smp = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_SUPPORT);
        final String smpemail = propertyService.getPropertyValueMandatory(StandardProperties.SMP_SML_SUPPORT_EMAIL);

        //ET_ObjectID --> Base64 of url
        final String objectID = uri.toString(); //ParticipantObjectID
        final byte[] encodedObjectID = Base64.encodeBase64(objectID.getBytes());

        logger.info("SMP Put request response code: '{}'", smpHttp.getStatusCode());
        if (smpHttp.getStatusCode() == 404 || smpHttp.getStatusCode() == 503 || smpHttp.getStatusCode() == 405) {
            logger.error("SMP request response body [{}]'", new String(responseEntity, StandardCharsets.UTF_8));
            //Audit Error
            final byte[] encodedObjectDetail = Base64.encodeBase64(response.getStatusLine().getReasonPhrase().getBytes());
            auditManager.handleDynamicDiscoveryPush(serverSMPUrl, new String(encodedObjectID),
                    Integer.toString(response.getStatusLine().getStatusCode()), encodedObjectDetail);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.server.failed"));
        } else if (smpHttp.getStatusCode() == 401) {
            logger.warn("SMP Put request response body [{}]'", new String(responseEntity, StandardCharsets.UTF_8));
            //Audit Error
            final byte[] encodedObjectDetail = Base64.encodeBase64(response.getStatusLine().getReasonPhrase().getBytes());
            auditManager.handleDynamicDiscoveryPush(serverSMPUrl, new String(encodedObjectID),
                    Integer.toString(response.getStatusLine().getStatusCode()), encodedObjectDetail);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, environment.getProperty("error.nouser"));
        }

        if (!(smpHttp.getStatusCode() == 200 || smpHttp.getStatusCode() == 201)) {
            /* Get BusinessCode and ErrorDescription from response */
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            final DocumentBuilder builder;
            try {
                final ByteArrayInputStream bais = new ByteArrayInputStream(responseEntity);
                builder = factory.newDocumentBuilder();
                final Document doc = builder.parse(bais);
                final Element element = doc.getDocumentElement();
                final NodeList nodes = element.getChildNodes();
                for (int j = 0; j < nodes.getLength(); j++) {
                    if (nodes.item(j).getNodeName().equals("BusinessCode")) {
                        final String businessCode = nodes.item(j).getTextContent();
                        smpHttp.setBusinessCode(businessCode);
                    }
                    if (nodes.item(j).getNodeName().equals("ErrorDescription")) {
                        final String errorDescription = nodes.item(j).getTextContent();
                        smpHttp.setErrorDescription(errorDescription);
                    }
                }
            } catch (final ParserConfigurationException ex) {
                logger.error("ParserConfigurationException: '{}", SimpleErrorHandler.printExceptionStackTrace(ex));
            } catch (final SAXException ex) {
                logger.error("SAXException: '{}", SimpleErrorHandler.printExceptionStackTrace(ex));
            } catch (final IOException ex) {
                logger.error("IOException: '{}", SimpleErrorHandler.printExceptionStackTrace(ex));
            }

            // Transform XML to String in order to send in Audit
            final String errorResult = auditManager.prepareEventLog(responseEntity);
            logger.debug("Error Result: '{}", errorResult);
            //Audit error
            auditManager.handleDynamicDiscoveryPush(serverSMPUrl, new String(encodedObjectID),
                    Integer.toString(response.getStatusLine().getStatusCode()), errorResult.getBytes(StandardCharsets.UTF_8));
        }

        if (smpHttp.getStatusCode() == 200 || smpHttp.getStatusCode() == 201) {
            //Audit Success
            auditManager.handleDynamicDiscoveryPush(serverSMPUrl, new String(encodedObjectID),
                    null, null);
        }

        //GET
        boolean success = true;
        String errorType = "";
        final ParticipantIdentifier participantIdentifier = new ParticipantIdentifier(partID, partScheme);
        final DocumentIdentifier documentIdentifier = new DocumentIdentifier(docID, docScheme);

        logger.info("Instantiating DynamicDiscovery: '{}', '{}', '{}', '{}'", partID, partScheme, docID, docScheme);
        final DynamicDiscovery smpClient = dynamicDiscoveryClient.getInstance();

        if (smpClient == null) {
            throw new Exception("Cannot instantiate SMPClient!!!!");
        }
        URI smpURI = null;
        try {
            smpURI = smpClient.getService().getMetadataLocator().lookup(participantIdentifier);
        } catch (final TechnicalException ex) {
            success = false;
            errorType = "TechnicalException";
            logger.error("TechnicalException: '{}", SimpleErrorHandler.printExceptionStackTrace(ex));
        }

        final URI serviceGroup = smpClient.getService().getMetadataProvider().resolveDocumentIdentifiers(smpURI, participantIdentifier);
        final URI serviceMetadataUri = smpClient.getService().getMetadataProvider().resolveServiceMetadata(smpURI, participantIdentifier, documentIdentifier);

        logger.info("URI ServiceGroup: '{}'\nURI serviceMetadataUri: '{}'", serviceGroup.toASCIIString(), serviceMetadataUri.toASCIIString());

        smpHttp.setServiceGroupUrl(serviceGroup.toString());
        smpHttp.setSignedServiceMetadataUrl(serviceMetadataUri.toString());


        return ResponseEntity.ok(smpHttp);

    }


    @PostMapping(value = "smpeditor/uploader/clean")
    public ResponseEntity cleanFile(@RequestBody final SMPHttp smpHttp) {
        if (smpHttp.getSmpFile() != null) {
            final boolean delete = smpHttp.getSmpFile().delete();
            logger.debug("\n****DELETED ? '{}'", delete);
        }

        return ResponseEntity.ok().build();
    }
}
