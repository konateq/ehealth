package eu.europa.ec.sante.openncp.core.common.util;

import eu.europa.ec.sante.openncp.common.security.SAML;
import eu.europa.ec.sante.openncp.core.common.assertion.AssertionConstants;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.dom.DOMSource;

/**
 * TODO: improve the implementation by implementing a method which picks attribute values by attribute names
 */
public class SoapElementHelper {

    private static final Logger logger = LoggerFactory.getLogger(SoapElementHelper.class);

    private SoapElementHelper() {
    }

    public static Assertion getHCPAssertion(final Element soapHeader) {

        try {
            // TODO: Since the XCA simulator sends this value in a wrong way, we are trying like this for the moment
            final NodeList securityList = soapHeader.getElementsByTagNameNS("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
            final Element security;
            if (securityList.getLength() > 0) {
                security = (Element) securityList.item(0);
            } else {
                throw (new MissingFieldException("Security element is required."));
            }
            final NodeList assertionList = security.getElementsByTagNameNS(AssertionConstants.URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION, "Assertion");
            Element hcpAss;
            Assertion hcpAssertion = null;

            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    hcpAss = (Element) assertionList.item(i);
                    final var samlSchemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                    samlSchemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(hcpAss));

                    hcpAssertion = (Assertion) SAML.fromElement(hcpAss);
                    if (StringUtils.equals(hcpAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:hcp")) {
                        break;
                    }
                }
            }
            if (hcpAssertion == null) {
                throw (new MissingFieldException("HCP Assertion element is required."));
            }
            return hcpAssertion;

        } catch (final Exception e) {
            logger.debug("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }

    public static String getAssertionsIssuer(final Element soapHeader) {

        final Assertion assertion = getHCPAssertion(soapHeader);
        if (assertion != null) {
            return assertion.getIssuer().getValue();
        } else {
            return "Unknown idP";
        }
    }

    public static String getAssertionsSPProvidedId(final Element soapHeader) {

        final Assertion assertion = getHCPAssertion(soapHeader);
        if (assertion != null) {
            return assertion.getSubject().getNameID().getSPProvidedID();
        } else {
            return "";
        }
    }

    public static String getUserID(final Element soapHeader) {
        String result = "N/A";

        try {
            final Assertion assertion = getHCPAssertion(soapHeader);
            if (assertion != null) {
                final String val = assertion.getSubject().getNameID().getValue();
                if (StringUtils.isNotBlank(val)) {
                    result = val;
                }
            }
        } catch (final Exception e) {
            logger.debug("Exception: '{}'", e.getMessage(), e);
        }

        return result;
    }

    public static String getAlternateUserID(final Element soapHeader) {
        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_SUBJECT_ID, false);
        if (result == null) {
            return "N/A";
        }
        return result;
    }

    public static String getRoleID(final Element soapHeader) {
        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE, false);
        if (result == null) {
            return "N/A";
        }
        return result;
    }

    public static String getXSPALocality(final Element soapHeader) {

        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_ENVIRONMENT_LOCALITY, false);
        return StringUtils.isBlank(result) ? "N/A" : result;
    }

    /**
     * Util method which return the Point of Care information related to the HCP assertions, based on the element provided
     * Organization is the subject:Organization (Optional) value or if not present the environment:locality value (Required).
     */
    public static String getPointOfCareUserId(final Element soapHeader) {

        String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_ORGANIZATION, false);
        if (result == null) {
            result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_ENVIRONMENT_LOCALITY, false);
        }
        return StringUtils.isBlank(result) ? "N/A" : result;
    }

    public static String getOrganizationId(final Element soapHeader) {
        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_ORGANIZATION_ID, false);
        if (result == null) {
            return "N/A";
        }
        return result;
    }

    public static String getOrganization(final Element soapHeader) {
        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_ORGANIZATION, false);
        if (result == null) {
            return "N/A";
        }
        return result;
    }

    public static String getPC_RoleID(final Element soapHeader) {
        final String result = getXSPAAttributeByName(soapHeader, AssertionConstants.URN_EHDSI_NAMES_SUBJECT_HEALTHCARE_FACILITY_TYPE, false);
        if (result == null) {
            return "N/A";
        }
        return result;
    }

    /**
     * @author Konstantin.Hypponen@kela.fi
     */
    public static Assertion getTRCAssertion(final Element soapHeader) {

        try {
            final NodeList securityList = soapHeader.getElementsByTagNameNS(
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", "Security");
            final Element security;
            if (securityList.getLength() > 0) {
                security = (Element) securityList.item(0);
            } else {
                throw (new MissingFieldException("Security element is required."));
            }
            final NodeList assertionList = security.getElementsByTagNameNS(AssertionConstants.URN_OASIS_NAMES_TC_SAML_2_0_ASSERTION, "Assertion");
            Assertion trcAssertion = null;

            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    final Element assertionElement = (Element) assertionList.item(i);
                    trcAssertion = (Assertion) SAML.fromElement(assertionElement);
                    if (StringUtils.equals(trcAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:trc")) {
                        break;
                    }
                }
            }
            if (trcAssertion == null) {
                throw (new MissingFieldException("TRC Assertion element is required."));
            }
            return trcAssertion;

        } catch (final Exception e) {
            logger.debug("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts XDS-formatted patient ID from TRCAssertion
     *
     * @param soapHeaderElement SOAP header which includes TRC assertion
     * @return Patient ID in XDS format
     */
    public static String getDocumentEntryPatientIdFromTRCAssertion(final Element soapHeaderElement) {

        final String patientId = getXSPAAttributeByName(soapHeaderElement, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_SUBJECT_ID, true);
        if (patientId == null) {
            logger.error("Patient ID not found in TRC assertion");
        }
        return patientId;
    }

    /**
     * @param soapHeaderElement SOAP Header
     * @param attributeName     Attribute name
     * @param trc               true, if attribute should be picked from TRC assertion
     * @return attribute value
     */
    private static String getXSPAAttributeByName(final Element soapHeaderElement, final String attributeName, final boolean trc) {

        String result = null;
        final Assertion assertion;

        try {

            if (trc) {
                assertion = getTRCAssertion(soapHeaderElement);
            } else {
                assertion = getHCPAssertion(soapHeaderElement);
            }
            if (assertion == null) {
                return null;
            }
            for (final Attribute attribute : assertion.getAttributeStatements().get(0).getAttributes()) {

                if (StringUtils.equals(attribute.getName(), attributeName)) {
                    final String value = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                    if (StringUtils.isNotBlank(value)) {
                        result = value;
                    }
                }
            }
        } catch (final Exception e) {
            final String assertionType = trc ? "TRC" : "HCP";
            logger.error("XSPA attribute '{}' not found in '{}' assertion", attributeName, assertionType);
            logger.debug("Exception: '{}'", e.getMessage(), e);
        }

        return result;
    }
}
