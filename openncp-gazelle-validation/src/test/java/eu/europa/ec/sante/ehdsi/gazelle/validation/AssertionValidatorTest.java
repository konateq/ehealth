package eu.europa.ec.sante.ehdsi.gazelle.validation;

import eu.europa.ec.sante.ehdsi.gazelle.config.TestConfiguration;
import eu.europa.ec.sante.ehdsi.gazelle.validation.impl.DefaultGazelleValidatorFactory;
import eu.europa.ec.sante.ehdsi.gazelle.validation.impl.SchematronValidatorImpl;
import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class, loader = AnnotationConfigContextLoader.class)
@Ignore
public class AssertionValidatorTest {

    private final Logger logger = LoggerFactory.getLogger(AssertionValidatorTest.class);

    @Autowired
    private HttpClient httpClient;

    private SchematronValidator assertionValidator;

    @Before
    public void setUp() {

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("net.ihe.gazelle.jaxb.schematron.sante");

        WebServiceTemplate webServiceTemplate = new WebServiceTemplate(marshaller);
        webServiceTemplate.setDefaultUri(DefaultGazelleValidatorFactory.GAZELLE_SCHEMATRON_VALIDATOR_URI);
        webServiceTemplate.setMessageSender(new HttpComponentsMessageSender(httpClient));

        assertionValidator = new SchematronValidatorImpl(webServiceTemplate);
    }

    @Test
    public void testValidateDocument() {

        String XML_ASSERTION = "\n" +
                "\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ID=\"_83706162-b7d1-4501-bbbc-87af68a63a5d\" IssueInstant=\"2018-01-05T14:35:35.800Z\" Version=\"2.0\">\n" +
                "   <saml2:Issuer NameQualifier=\"urn:ehdsi:assertions:hcp\">urn:idp:EU:countryB</saml2:Issuer>\n" +
                "   <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <ds:SignedInfo>\n" +
                "         <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\" />\n" +
                "         <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\" />\n" +
                "         <ds:Reference URI=\"#_83706162-b7d1-4501-bbbc-87af68a63a5d\">\n" +
                "            <ds:Transforms>\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" />\n" +
                "               <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\">\n" +
                "                  <ec:InclusiveNamespaces xmlns:ec=\"http://www.w3.org/2001/10/xml-exc-c14n#\" PrefixList=\"xs\" />\n" +
                "               </ds:Transform>\n" +
                "            </ds:Transforms>\n" +
                "            <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\" />\n" +
                "            <ds:DigestValue>N5rvkdqlZ6Mtyal90hxhiPveBGw=</ds:DigestValue>\n" +
                "         </ds:Reference>\n" +
                "      </ds:SignedInfo>\n" +
                "      <ds:SignatureValue>bpm3ayn50iRlnza0d6MgbygMEhn2gHvMkp0ksnbjXwlTE74BPEF6sE0KPZAvYZ1dSx3FuSJdZcwl5VxnnmZD0wF75IaiLrXyaJMGDgGkNaBkgDWLsV7YD1oqsXOfBABdKJPACGUZVKBsk9hTdL0lcsr+a/hY32vxbcREzTQPY7w=</ds:SignatureValue>\n" +
                "      <ds:KeyInfo>\n" +
                "         <ds:X509Data>\n" +
                "            <ds:X509Certificate>MIIDrTCCAxagAwIBAgIBODANBgkqhkiG9w0BAQ0FADA1MQswCQYDVQQGEwJCRTETMBEGA1UECgwK SUhFIEV1cm9wZTERMA8GA1UEAwwIRUhEU0kgQ0EwHhcNMTcwODEyMTc1OTU0WhcNMjcwODEyMTc1 OTU0WjCBwDELMAkGA1UEBhMCQkUxHDAaBgNVBAoME0V1cm9wZWFuIENvbW1pc3Npb24xGTAXBgNV BAMMEHMtc2FudGUtemJvb2stZXUxCzAJBgNVBAwMAk1yMQ8wDQYDVQQqDAZSb2JlcnQxEDAOBgNV BAQMB1NjaHVtYW4xETAPBgNVBAsMCERHIFNhbnRlMTUwMwYJKoZIhvcNAQkBFiZzYW50ZS1laGVh bHRoLWRzaS1zdXBwb3J0QGVjLmV1cm9wYS5ldTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA uE8Vgo5r7vyJh5W9bkdRQGGJxns8qN8Mr59v3zUzj9FltNBqqJ46nFI4kMpCSzasy6enQoWGflrv hA88k0d8B/qtJI/Oxcimq0irPzig51hk1sT81VPmSkAHmIm2yIX815458PVdStwsvIb+pNXeNyrX OH7M3sqg7puVbHxQScECAwEAAaOCAT8wggE7MEkGA1UdHwRCMEAwPqA8oDqGOGh0dHBzOi8vZ2F6 ZWxsZS5laGRzaS5paGUtZXVyb3BlLm5ldC9nc3MvY3JsLzIvY2FjcmwuY3JsMEcGCWCGSAGG+EIB BAQ6FjhodHRwczovL2dhemVsbGUuZWhkc2kuaWhlLWV1cm9wZS5uZXQvZ3NzL2NybC8yL2NhY3Js LmNybDBHBglghkgBhvhCAQMEOhY4aHR0cHM6Ly9nYXplbGxlLmVoZHNpLmloZS1ldXJvcGUubmV0 L2dzcy9jcmwvMi9jYWNybC5jcmwwHwYDVR0jBBgwFoAU1CDyv36BdNxq9vygyNXRNJBkA7EwHQYD VR0OBBYEFC/DRByIkgG2RwESXzal/HozUiiTMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgbA MA0GCSqGSIb3DQEBDQUAA4GBAHhv0QOZzauWBW4ATB7hNjJHWAeJrFsiLTJZxHpOjovBJtLpKda2 fAZGLZFKz0mT0w8++XaV4oYVCB6E/d0YwbgdSCd5kY7cbrxvWcsZc58n3BCyXkxrRzmV5kUD+/A9 MZLvEixiY0kEhc9TCveyHQrPwOsjVODtx/dSnJYcAQEy</ds:X509Certificate>\n" +
                "         </ds:X509Data>\n" +
                "      </ds:KeyInfo>\n" +
                "   </ds:Signature>\n" +
                "   <saml2:Subject>\n" +
                "      <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">ehealth-doctor-mock-eu@nomail.ec.europa.eu</saml2:NameID>\n" +
                "      <saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\" />\n" +
                "   </saml2:Subject>\n" +
                "   <saml2:Conditions NotBefore=\"2018-01-05T14:34:35.800Z\" NotOnOrAfter=\"2018-01-05T16:35:35.800Z\" />\n" +
                "   <saml2:AuthnStatement AuthnInstant=\"2018-01-05T14:35:35.800Z\">\n" +
                "      <saml2:AuthnContext>\n" +
                "         <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml2:AuthnContextClassRef>\n" +
                "      </saml2:AuthnContext>\n" +
                "   </saml2:AuthnStatement>\n" +
                "   <saml2:AttributeStatement>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Subject\" Name=\"urn:oasis:names:tc:xspa:1.0:subject:subject-id\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Jack House</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Role\" Name=\"urn:oasis:names:tc:xacml:2.0:subject:role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">medical doctor</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Organization\" Name=\"urn:oasis:names:tc:xspa:1.0:subject:organization\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">eHealth OpenNCP EU Portal</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Organization ID\" Name=\"urn:oasis:names:tc:xspa:1.0:subject:organization-id\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">20155.1</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"eHealth DSI Healthcare Facility Type\" Name=\"urn:ehdsi:names:subject:healthcare-facility-type\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Resident Physician</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Purpose Of Use\" Name=\"urn:oasis:names:tc:xspa:1.0:subject:purposeofuse\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">TREATMENT</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"XSPA Locality\" Name=\"urn:oasis:names:tc:xspa:1.0:environment:locality\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">POC</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "      <saml2:Attribute FriendlyName=\"Hl7 Permissions\" Name=\"urn:oasis:names:tc:xspa:1.0:subject:hl7:permission\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\">\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-006</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-003</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-004</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-005</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-010</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-016</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-032</saml2:AttributeValue>\n" +
                "         <saml2:AttributeValue xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-033</saml2:AttributeValue>\n" +
                "      </saml2:Attribute>\n" +
                "   </saml2:AttributeStatement>\n" +
                "</saml2:Assertion>\n" +
                "\n";

        String assertionsSTR = DatatypeConverter.printBase64Binary(XML_ASSERTION.getBytes(StandardCharsets.UTF_8));
        String result = assertionValidator.validateObject(assertionsSTR, "epSOS - HCP Identity Assertion", "epSOS - HCP Identity Assertion");
        logger.info("Result:\n'{}'", result);
        Assert.assertNotNull(result);
    }
}
