package eu.europa.ec.sante.openncp.core.common.dynamicdiscovery;

import eu.europa.ec.dynamicdiscovery.DynamicDiscovery;
import eu.europa.ec.dynamicdiscovery.core.locator.dns.impl.DefaultDNSLookup;
import eu.europa.ec.dynamicdiscovery.core.locator.impl.DefaultBDXRLocator;
import eu.europa.ec.dynamicdiscovery.core.reader.impl.DefaultBDXRReader;
import eu.europa.ec.dynamicdiscovery.core.security.impl.DefaultSignatureValidator;
import eu.europa.ec.dynamicdiscovery.model.DocumentIdentifier;
import eu.europa.ec.dynamicdiscovery.model.ParticipantIdentifier;
import eu.europa.ec.dynamicdiscovery.model.ServiceMetadata;
import eu.europa.ec.dynamicdiscovery.util.HashUtil;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.*;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.EndpointType;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ProcessListType;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ProcessType;
import org.oasis_open.docs.bdxr.ns.smp._2016._05.ServiceEndpointList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Service
public class DynamicDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDiscoveryService.class);

    /**
     * Static constants for SMP identifiers.
     */
    private static final String PARTICIPANT_IDENTIFIER_SCHEME = "ehealth-participantid-qns";
    private static final String PARTICIPANT_IDENTIFIER_VALUE = "urn:ehealth:%2s:ncp-idp";
    private static final String DOCUMENT_IDENTIFIER_SCHEME = "ehealth-resid-qns";
    private ConfigurationManager configurationManager;

    /**
     * The certificate of the remote endpoint.
     */
    private X509Certificate certificate;

    /**
     * The URL address of the remote endpoint.
     */
    private URL address;

    /**
     * The XML contained in the Extension (used by search masks).
     */
    private Document extension;

    /**
     * @deprecated Use new {@link DynamicDiscoveryService#DynamicDiscoveryService(ConfigurationManager)} instead or via Spring beans
     */
    @Deprecated
    public DynamicDiscoveryService() {
    }

    public DynamicDiscoveryService(final ConfigurationManager configurationManager) {
        this.configurationManager = Validate.notNull(configurationManager, "ConfigurationManager must not be null");
    }

    private ConfigurationManager getConfigurationManager() {
        if (configurationManager == null) {
            configurationManager = ConfigurationManagerFactory.getConfigurationManager();
        }

        return configurationManager;
    }

    private static void sendAuditQuery(final String sc_fullname, final String sc_email, final String sp_fullname, final String sp_email,
                                       final String partid, final String sourceip, final String targetip, final String objectID,
                                       final String EM_PatricipantObjectID, final byte[] EM_PatricipantObjectDetail, final String smpServer) {

        LOGGER.info("sendAuditQuery('{}', '{}','{}','{}','{}','{}','{}','{}','{}','{}')", sc_fullname, sc_email,
                sp_fullname, sp_email, partid, sourceip, targetip, objectID, "EM_PatricipantObjectID", "EM_PatricipantObjectDetail");
        try {
            final AuditService asd = AuditServiceFactory.getInstance();
            final GregorianCalendar c = new GregorianCalendar();
            c.setTime(new Date());
            XMLGregorianCalendar date2 = null;
            try {
                date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            } catch (final DatatypeConfigurationException ex) {
                LOGGER.error(null, ex);
            }
            final String serviceConsumerUserId = HttpUtil.getSubjectDN(false);
            final String serviceProviderUserId = HttpUtil.getTlsCertificateCommonName(smpServer);

            final EventLog eventLog1 = EventLog.createEventLogPatientPrivacy(TransactionName.SMP_QUERY, EventActionCode.EXECUTE,
                    date2, EventOutcomeIndicator.FULL_SUCCESS, null, null, null,
                    serviceConsumerUserId, serviceProviderUserId, partid, null, EM_PatricipantObjectID,
                    EM_PatricipantObjectDetail, objectID, null, new byte[1], null,
                    new byte[1], sourceip, targetip);
            eventLog1.setNcpSide(NcpSide.NCP_B);
            eventLog1.setEventType(EventType.SMP_QUERY);

            // According to https://tools.ietf.org/html/rfc5424 (Syslog Protocol)
            // facility = 13 --> log audit | severity = 2 --> Critical: critical conditions
            asd.write(eventLog1, "13", "2");

        } catch (final Exception e) {
            LOGGER.error("Error sending audit for eHealth SMP Query: '{}'", e.getMessage(), e);
        }
    }

    public String getEndpointUrl(final String countryCode, final RegisteredService service) {

        return getEndpointUrl(countryCode, service, false);
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(final X509Certificate certificate) {
        this.certificate = certificate;
    }

    public URL getAddress() {
        return address;
    }

    public void setAddress(final URL address) {
        this.address = address;
    }

    public Document getExtension() {
        return extension;
    }

    public void setExtension(final Document extension) {
        this.extension = extension;
    }

    public String getEndpointUrl(final String countryCode, final RegisteredService service, final boolean refresh) {

        Validate.notNull(countryCode, "countryCode must not be null!");
        Validate.notNull(service, "service must not be null!");
        LOGGER.info("getEndpointUrl('{}', '{}')", countryCode, service.getServiceName());
        final String key = countryCode.toLowerCase() + "." + service.getServiceName() + ".WSE";
        try {
            if (!refresh) {
                try {
                    return getConfigurationManager().getProperty(key);
                } catch (final PropertyNotFoundException e) {
                    LOGGER.warn("PropertyNotFoundException: '{}'", e.getMessage());
                    lookup(countryCode, service.getUrn(), key);
                    return getAddress().toExternalForm();
                }
            }
            lookup(countryCode, service.getUrn(), key);
            return getAddress().toExternalForm();
        } catch (final ConfigurationManagerException e) {
            LOGGER.error("SMLSMPClientException: '{}'", e.getMessage(), e);
            throw new ConfigurationManagerException("An internal error occurred while retrieving the endpoint URL", e);
        }
    }

    private void lookup(final String countryCode, final String documentType, final String key) throws ConfigurationManagerException {

        LOGGER.info("SML Client: '{}'-'{}'", countryCode, documentType);
        try {

            final String participantIdentifierValue = String.format(PARTICIPANT_IDENTIFIER_VALUE, countryCode);
            LOGGER.debug("****** participantIdentifierValue '{}'.", participantIdentifierValue);
            LOGGER.debug("****** NAPTR Hash: '{}'", HashUtil.getSHA256HashBase32(participantIdentifierValue));
            LOGGER.debug("****** CNAME Hash: '{}'", StringUtils.lowerCase("b-" + HashUtil.getMD5Hash(participantIdentifierValue)));
            final KeyStore ks = KeyStore.getInstance("JKS");

            final File file = new File(getConfigurationManager().getProperty("TRUSTSTORE_PATH"));
            final FileInputStream fileInputStream = new FileInputStream(file);
            ks.load(fileInputStream, getConfigurationManager().getProperty("TRUSTSTORE_PASSWORD").toCharArray());

            final DynamicDiscovery smpClient = getConfigurationManager().initializeDynamicDiscoveryFetcher()
                    .locator(new DefaultBDXRLocator(getConfigurationManager().getProperty("SML_DOMAIN"), new DefaultDNSLookup()))
                    .reader(new DefaultBDXRReader(new DefaultSignatureValidator(ks)))
                    .build();

            final DocumentIdentifier documentIdentifier = new DocumentIdentifier(documentType, DOCUMENT_IDENTIFIER_SCHEME);
            final ParticipantIdentifier participantIdentifier = new ParticipantIdentifier(participantIdentifierValue, PARTICIPANT_IDENTIFIER_SCHEME);

            final ServiceMetadata serviceMetadata = smpClient.getServiceMetadata(participantIdentifier, documentIdentifier);
            LOGGER.info("ServiceMetadata '{}'.", serviceMetadata.toString());
            final ProcessListType processListType = serviceMetadata.getOriginalServiceMetadata().getServiceMetadata().getServiceInformation().getProcessList();
            for (final ProcessType processType : processListType.getProcess()) {

                LOGGER.info("ProcessType: '{}' - '{}'", processType.getProcessIdentifier().getValue(), processType.getProcessIdentifier().getScheme());
                final ServiceEndpointList serviceEndpointList = processType.getServiceEndpointList();
                for (final EndpointType endpointType : serviceEndpointList.getEndpoint()) {
                    LOGGER.info("Endpoint: '{}'", endpointType.getEndpointURI());
                }
                final List<EndpointType> endpoints = serviceEndpointList.getEndpoint();

                /*
                 * Constraint: here I think I have just one endpoint
                 */
                final int size = endpoints.size();
                if (size != 1) {
                    throw new Exception(
                            "Invalid number of endpoints found (" + size + "). This implementation works just with 1.");
                }

                final EndpointType e = endpoints.get(0);
                final String address = e.getEndpointURI();
                if (StringUtils.isEmpty(address)) {
                    throw new Exception("No address found for: " + documentType + ":" + participantIdentifierValue);
                }
                final URL urlAddress = new URL(address);

                final InputStream inStream = new ByteArrayInputStream(e.getCertificate());
                final CertificateFactory cf = CertificateFactory.getInstance("X.509");
                final X509Certificate certificate = (X509Certificate) cf.generateCertificate(inStream);

                if (certificate == null) {
                    throw new Exception("no certificate found for endpoint: " + e.getEndpointURI());
                }
                LOGGER.info("Certificate: '{}'-'{}", certificate.getIssuerDN().getName(), certificate.getSerialNumber());
                setAddress(urlAddress);
                setCertificate(certificate);
            }

            final URL endpointUrl = getAddress();
            if (endpointUrl == null) {
                throw new PropertyNotFoundException("Property '" + key + "' not found!");
            }

            final String value = endpointUrl.toExternalForm();
            LOGGER.info("Storing endpoint to database: '{}' - '{}'", key, value);
            getConfigurationManager().setProperty(key, value);

            final X509Certificate certificate = getCertificate();
            if (certificate != null) {
                final String endpointId = countryCode.toLowerCase() + "_" + StringUtils.substringAfter(documentType, "##");
                storeEndpointCertificate(endpointId, certificate);
            }

            //Audit vars
            final String ncp = getConfigurationManager().getProperty("ncp.country");
            final String ncpemail = getConfigurationManager().getProperty("ncp.email");
            final String country = getConfigurationManager().getProperty("COUNTRY_PRINCIPAL_SUBDIVISION");

            final String localIp = IPUtil.getPrivateServerIp();
            final String smp = getConfigurationManager().getProperty("SMP_SUPPORT");
            final String smpemail = getConfigurationManager().getProperty("SMP_SUPPORT_EMAIL");
            //ET_ObjectID --> Base64 of url
            final String objectID = getAddress().toString(); //ParticipantObjectID
            LOGGER.info("No address found for: '{}'", getAddress());
            LOGGER.info("objectID: '{}'", objectID);
            final byte[] encodedObjectID = Base64.encodeBase64(objectID.getBytes());
            if (encodedObjectID != null) {
                LOGGER.info("encodedObjectID not NULL");
            } else {
                LOGGER.info("encodedObjectID NULL");
            }
            LOGGER.info("Sending audit trail");
            //TODO: Request Audit SMP Query
            final URI smpURI = smpClient.getService().getMetadataLocator().lookup(participantIdentifier);
            LOGGER.info("DNS: '{}'", smpURI);
            sendAuditQuery(ncp, ncpemail, smp, smpemail, country, localIp, smpURI.getHost(), new String(encodedObjectID),
                    null, null, smpURI.toASCIIString());


        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            throw new ConfigurationManagerException(e);
        }
    }

    private void storeEndpointCertificate(final String endpointId, final X509Certificate certificate) {

        // Store the endpoint certificate in the truststore
        final String trustStorePath = getConfigurationManager().getProperty(StandardProperties.NCP_TRUSTSTORE);
        final char[] trustStorePassword = getConfigurationManager().getProperty(StandardProperties.NCP_TRUSTSTORE_PASSWORD).toCharArray();

        try {
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (final InputStream is = new FileInputStream(trustStorePath)) {
                trustStore.load(is, trustStorePassword);
            }
            final String alias = Base64.encodeBase64String(DigestUtils.md5(certificate.getSubjectDN().getName()));
            trustStore.setCertificateEntry(alias, certificate);
            try (final OutputStream os = new FileOutputStream(trustStorePath)) {
                trustStore.store(os, trustStorePassword);
            }
        } catch (final GeneralSecurityException | IOException e) {
            throw new ConfigurationManagerException("An error occurred while storing the endpoint certificate in the truststore!", e);
        }

        // Store the endpoint certificate in the file system
        final File certificateFile = new File(getConfigurationManager().getProperty(
                StandardProperties.NCP_CERTIFICATES_DIRECTORY), endpointId + ".der");
        try (final OutputStream os = new FileOutputStream(certificateFile)) {
            os.write(certificate.getEncoded());

        } catch (final CertificateException | IOException e) {
            throw new ConfigurationManagerException("An error occurred while storing the endpoint certificate in the file system!", e);
        }
    }
}
