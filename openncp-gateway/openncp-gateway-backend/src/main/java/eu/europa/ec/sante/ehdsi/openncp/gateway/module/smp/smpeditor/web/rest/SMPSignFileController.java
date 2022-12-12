package eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.smpeditor.web.rest;

import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.gateway.error.ApiException;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.Constants;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.cfg.ReadSMPProperties;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.domain.*;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.service.SimpleErrorHandler;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.smpeditor.service.BdxSmpValidator;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.smpeditor.service.SMPConverter;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.smp.smpeditor.service.SignFileService;
import eu.europa.ec.sante.ehdsi.openncp.gateway.service.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ehdsi.EndpointType;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ehdsi.RedirectType;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ehdsi.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping(path = "/api")
public class SMPSignFileController {

    private final Logger logger = LoggerFactory.getLogger(SMPSignFileController.class);

    private final SMPConverter smpconverter;

    private final Environment env;

    private final ReadSMPProperties readProperties;

    private final SignFileService signFileService;


    @Autowired
    public SMPSignFileController(SMPConverter smpconverter, Environment env, ReadSMPProperties readProperties, SignFileService signFileService) {
        this.smpconverter = smpconverter;
        this.env = env;
        this.readProperties = readProperties;
        this.signFileService = signFileService;
    }

    @PostMapping(path = "/smpeditor/sign/fromSmpFile")
    public ResponseEntity<SMPFileOps> createSMPFileOps(@RequestBody SMPFile smpfile) {

        logger.debug("\n==== in signCreatedFile ====");
        if (smpfile.getGeneratedFile() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The requested file does not exists");
        }
        File file = new File(smpfile.getGeneratedFile().getPath());
        SMPFileOps smpFileOps = new SMPFileOps();
        smpFileOps.setFileToSign(file);
        smpFileOps.setSignFileName(file.getName());
        return ResponseEntity.ok(smpFileOps);
    }

    @PostMapping(path = "/smpeditor/sign/upload")
    public ResponseEntity<SMPFileOps> createSMPFileOps(@RequestPart MultipartFile multipartFile) {

        SMPFileOps smpFileOps = new SMPFileOps();
        FileUtil.initializeFolders(Constants.SMP_DIR_PATH);
        File convFile = new File(Constants.SMP_DIR_PATH + File.separator + multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(convFile);
        } catch (IOException | IllegalStateException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getClass().getName());
        }

        smpFileOps.setFileToSign(convFile);
        return ResponseEntity.ok(smpFileOps);

    }

    @PostMapping(path = "/smpeditor/sign/generateSmpFileOpsData")
    public ResponseEntity<SMPFileOps> generateSMPFileOpsData(@RequestBody SMPFileOps smpFileOps) throws IOException {

        String type = null;

        String contentFile = new String(Files.readAllBytes(Paths.get(smpFileOps.getFileToSign().getPath())));
        //boolean valid = XMLValidator.validate(contentFile, "/bdx-smp-201605.xsd");
        if (!BdxSmpValidator.validateFile(contentFile)) {
            boolean fileDeleted = smpFileOps.getFileToSign().delete();
            logger.debug("Converted File deleted: '{}'", fileDeleted);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.notsmp"));
        }

        Object fileConverted = smpconverter.convertFromXml(smpFileOps.getFileToSign());

        if (smpconverter.isSignedServiceMetadata(fileConverted)) {
            boolean fileDeleted = smpFileOps.getFileToSign().delete();
            logger.debug("Converted File deleted: '{}'", fileDeleted);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("warning.isSigned.sigmenu"));
        }

        ServiceMetadata serviceMetadata = smpconverter.getServiceMetadata(fileConverted);

      /*
       Condition to know the type of file (Redirect|ServiceInformation) in order to build the form
       */

        if (serviceMetadata.getRedirect() != null) {
            logger.debug("\n******** REDIRECT");
            type = "Redirect";
            smpFileOps.setType(SMPType.Redirect);

            if (!serviceMetadata.getRedirect().getExtensions().isEmpty()) {
                smpFileOps.setAlert(new Alert(env.getProperty("warning.isSignedExtension"), Alert.alertType.warning));
            }
        
        /*
          get documentIdentifier and participantIdentifier from redirect href
        */
            String participantID;
            String documentID = "";
            Pattern pattern = Pattern.compile(env.getProperty("ParticipantIdentifier.Scheme") + ".*");//SPECIFICATION
            Matcher matcher = pattern.matcher(serviceMetadata.getRedirect().getHref());
            if (matcher.find()) {
                String result = matcher.group(0);
                result = java.net.URLDecoder.decode(result, StandardCharsets.UTF_8);
                String[] ids = result.split("/services/");//SPECIFICATION
                participantID = ids[0];
                String[] cc = participantID.split(":");//SPECIFICATION May change if Participant Identifier specification change

                for (Countries country : Countries.values()) {
                    if (cc[4].equals(country.name())) {
                        smpFileOps.setCountry(cc[4]);
                    }
                }
                if (smpFileOps.getCountry() == null) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.redirect.href.participantID"));
                }

                String docID = ids[1];
                Map<String, String> propertiesMap = readProperties.readPropertiesFile();
                //SPECIFICATION May change if Document Identifier specification change
                String[] nIDs = docID.split(env.getProperty("DocumentIdentifier.Scheme") + "::");
                String docuID = nIDs[1];
                Set<Map.Entry<String, String>> set2 = propertiesMap.entrySet();
                for (Object aSet2 : set2) {
                    Map.Entry mentry2 = (Map.Entry) aSet2;

                    if (docuID.equals(mentry2.getKey().toString())) {
                        String[] docs = mentry2.getValue().toString().split("\\.");
                        documentID = docs[0];
                        break;
                    }
                }

                if (documentID.equals("")) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.redirect.href.documentID"));
                }

                /*Builds final file name*/
                String timeStamp = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
                String fileName = smpFileOps.getType().name() + "_" + documentID + "_" + smpFileOps.getCountry().toUpperCase() + "_Signed_" + timeStamp + ".xml";
                smpFileOps.setFileName(fileName);
            } else {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.redirect.href"));
            }

        } else if (serviceMetadata.getServiceInformation() != null) { /*Service Information Type*/
            type = "ServiceInformation";

            if (!serviceMetadata.getServiceInformation().getExtensions().isEmpty()) {
                Alert alert = new Alert(env.getProperty("warning.isSignedExtension"), Alert.alertType.warning);
                smpFileOps.setAlert(alert);
            }

            smpFileOps.setDocumentIdentifier(serviceMetadata.getServiceInformation().getDocumentIdentifier().getValue());
            smpFileOps.setDocumentIdentifierScheme(serviceMetadata.getServiceInformation().getDocumentIdentifier().getScheme());
            String documentIdentifier = smpFileOps.getDocumentIdentifier();

            String documentID = "";
            Map<String, String> propertiesMap = readProperties.readPropertiesFile();
            Set set2 = propertiesMap.entrySet();
            for (Object aSet2 : set2) {
                Map.Entry mentry2 = (Map.Entry) aSet2;

                if (documentIdentifier.equals(mentry2.getKey().toString())) {
                    String[] docs = mentry2.getValue().toString().split("\\.");
                    documentID = docs[0];
                    break;
                }
            }

            SMPType[] smptypes = SMPType.values();
            for (SMPType smptype : smptypes) {
                if (smptype.name().equals(documentID)) {
                    smpFileOps.setType(smptype);
                    break;
                }
            }
            if (smpFileOps.getType() == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.serviceinformation.documentID"));
            }

            String participanteID = serviceMetadata.getServiceInformation().getParticipantIdentifier().getValue();
            String[] cc = participanteID.split(":");

            for (Countries country : Countries.values()) {
                if (cc[2].equals(country.name())) {
                    smpFileOps.setCountry(cc[2]);
                }
            }
            if (smpFileOps.getCountry() == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, env.getProperty("error.serviceinformation.participantID"));
            }

            /*Builds final file name*/
            String timeStamp = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
            String fileName = smpFileOps.getType().name() + "_" + smpFileOps.getCountry().toUpperCase() + "_Signed_" + timeStamp + ".xml";
            smpFileOps.setFileName(fileName);

            smpFileOps.setParticipantIdentifier(participanteID);
            smpFileOps.setParticipantIdentifierScheme(serviceMetadata.getServiceInformation().getParticipantIdentifier().getScheme());
            smpFileOps.setProcessIdentifier(serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0).getProcessIdentifier().getValue());
            smpFileOps.setProcessIdentifierScheme(serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0).getProcessIdentifier().getScheme());
            smpFileOps.setTransportProfile(serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0).getServiceEndpointList().getEndpoints().get(0).getTransportProfile());
            smpFileOps.setRequiredBusinessLevelSig(serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0).getServiceEndpointList().getEndpoints().get(0).isRequireBusinessLevelSignature());
            smpFileOps.setMinimumAutenticationLevel(serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0).getServiceEndpointList().getEndpoints().get(0).getMinimumAuthenticationLevel());
        }

        SMPFields smpfields = readProperties.readProperties(smpFileOps.getType());

        // Handling Service Information Type
        if (StringUtils.equals("ServiceInformation", type)) {

            EndpointType endpoint = serviceMetadata.getServiceInformation().getProcessList().getProcesses().get(0)
                    .getServiceEndpointList().getEndpoints().get(0);

            X509Certificate cert;
            String subjectName = null;
            if (smpfields.getCertificate().isEnable()) {
                try {
                    InputStream in = new ByteArrayInputStream(endpoint.getCertificate());
                    logger.debug("Endpoint Certificate PEM:\n'{}'", DatatypeConverter.printBase64Binary(endpoint.getCertificate()));
                    cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
                    if (cert != null) {
                        subjectName = "Issuer: " + cert.getIssuerX500Principal().getName() + "\nSerial Number #" + cert.getSerialNumber();
                        logger.debug("Certificate: '{}'", subjectName);
                        smpFileOps.setCertificateContent(subjectName);
                        smpFileOps.setCertificate(cert.getEncoded());
                    }
                } catch (CertificateException ex) {
                    logger.error("CertificateException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
                }
            } else {
                smpFileOps.setCertificate(null);
            }

            smpFileOps.setEndpointURI(endpoint.getEndpointURI());

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Calendar serviceActivationDate = endpoint.getServiceActivationDate();
            if (serviceActivationDate != null && serviceActivationDate.getTime() != null) {
                smpFileOps.setServiceActivationDateS(format.format(serviceActivationDate.getTime()));
            } else {
                logger.error("Mandatory field ServiceActivationDate is not set");
            }
            Calendar serviceExpirationDate = endpoint.getServiceExpirationDate();
            if (serviceExpirationDate != null && serviceExpirationDate.getTime() != null) {
                smpFileOps.setServiceExpirationDateS(format.format(serviceExpirationDate.getTime()));
            } else {
                logger.error("Mandatory field serviceExpirationDate is not set");
            }
            smpFileOps.setCertificateContent(subjectName);
            smpFileOps.setServiceDescription(endpoint.getServiceDescription());
            smpFileOps.setTechnicalContactUrl(endpoint.getTechnicalContactUrl());
            smpFileOps.setTechnicalInformationUrl(endpoint.getTechnicalInformationUrl());

            if (smpfields.getExtension().isEnable()) {

                try (Scanner scanner = new Scanner(smpFileOps.getFileToSign(), StandardCharsets.UTF_8.name())) {

                    String content = scanner.useDelimiter("\\Z").next();
                    String capturedString = content.substring(content.indexOf("<Extension>"), content.indexOf("</Extension>"));
                    String[] endA = capturedString.split("<Extension>");
                    smpFileOps.setExtensionContent(endA[1]);
                } catch (IOException ex) {
                    logger.error("\n IOException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
                }
            }
        } else if ("Redirect".equals(type)) {
            RedirectType redirect = serviceMetadata.getRedirect();
            smpFileOps.setCertificateUID(redirect.getCertificateUID());
            smpFileOps.setHref(redirect.getHref());
        }

        return ResponseEntity.ok(smpFileOps);
    }

    @PostMapping(value = "/smpeditor/sign/sign")
    public ResponseEntity signSMPFile(@RequestBody SMPFileOps smpFileOps) {

        var file = new File(ConfigurationManagerFactory.getConfigurationManager().getProperty("NCP_SIG_KEYSTORE_PATH"));
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            logger.error("\n FileNotFoundException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("FileNotFoundException Keystore"));
        }
        MultipartFile keystore = null;
        try {
            keystore = new MockMultipartFile("keystore", file.getName(), "text/xml", input);
        } catch (IOException ex) {
            logger.error("\n IOException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("IO Exception Keystore"));
        }

        File fileSigned = null;

        try {
            fileSigned = signFileService.signFiles(smpFileOps.getType().name(),
                    smpFileOps.getFileName(),
                    keystore,
                    ConfigurationManagerFactory.getConfigurationManager().getProperty("NCP_SIG_KEYSTORE_PASSWORD"),
                    ConfigurationManagerFactory.getConfigurationManager().getProperty("NCP_SIG_PRIVATEKEY_ALIAS"),
                    ConfigurationManagerFactory.getConfigurationManager().getProperty("NCP_SIG_PRIVATEKEY_PASSWORD"),
                    smpFileOps.getFileToSign());
        } catch (Exception ex) {
            logger.error("\nException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(env.getProperty("Signed error"));
        }

        smpFileOps.setGeneratedFile(fileSigned);

        return ResponseEntity.ok(smpFileOps);
    }

    @PostMapping(value = "smpeditor/sign/download")
    public void downloadGeneratedFile(@RequestBody SMPFileOps smpFileOps, HttpServletResponse response) {

        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=" + smpFileOps.getFileName());
        response.setContentLength((int) smpFileOps.getGeneratedFile().length());
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(smpFileOps.getGeneratedFile()))) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (FileNotFoundException ex) {
            logger.error("FileNotFoundException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
        } catch (IOException ex) {
            logger.error("IOException - '{}'", SimpleErrorHandler.printExceptionStackTrace(ex));
        }
    }

    @PostMapping(value = "smpeditor/sign/clean")
    public ResponseEntity cleanFile(@RequestBody SMPFileOps smpFileOps) {

        if (smpFileOps.getFileToSign() != null) {
            boolean delete = smpFileOps.getFileToSign().delete();
            logger.debug("\n****DELETED ? '{}'", delete);
        }
        if (smpFileOps.getGeneratedFile() != null) {
            boolean delete = smpFileOps.getGeneratedFile().delete();
            logger.debug("\n****DELETED ? '{}'", delete);
        }
        return ResponseEntity.ok().build();
    }
}
