package eu.europa.ec.sante.openncp.core.common.assertion;

import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;

public class AssertionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionHelper.class);
    private static final String ERROR_MESSAGE = "{0} - attribute shall contain AttributeValue element.";

    private AssertionHelper() {
    }

    /**
     * Get attribute value from assertion.
     *
     * @param assertion     the assertion
     * @param attributeName the attribute to search for
     * @return the attribute value
     * @throws MissingFieldException If attribute is missing
     */
    public static String getAttributeFromAssertion(final Assertion assertion, final String attributeName) throws MissingFieldException {
        final Optional<String> valueForAttribute = getValueForAttribute(assertion, attributeName);

        return valueForAttribute.orElseThrow(() -> {
            final String errorMessage = MessageFormat.format(ERROR_MESSAGE, attributeName);
            return new MissingFieldException(errorMessage);
        });
    }

    public static Optional<String> getValueForAttribute(final Assertion assertion, final String attributeName) {

        for (final AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equals(attribute.getName(), attributeName)) {
                    if (!attribute.getAttributeValues().isEmpty()) {
                        final Optional<String> textContentOfFirstAttributeValue = Optional.ofNullable(attribute.getAttributeValues().get(0)).map(XMLObject::getDOM).map(Element::getTextContent);

                        if (textContentOfFirstAttributeValue.isPresent()) {
                            return textContentOfFirstAttributeValue;
                        }

                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get attribute values from assertion.
     *
     * @param assertion     the assertion
     * @param attributeName the attribute to search for
     * @return the attribute values
     * @throws MissingFieldException If attribute is missing
     */
    public static List<XMLObject> getAttributeValuesFromAssertion(final Assertion assertion, final String attributeName)
            throws MissingFieldException {

        for (final AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equals(attribute.getName(), attributeName)) {
                    return attribute.getAttributeValues();
                }
            }
        }
        final String errorMessage = MessageFormat.format(ERROR_MESSAGE, attributeName);
        throw new MissingFieldException(errorMessage);
    }

    /**
     * Returns the HL7 permissions list associated to the SAML assertion in parameter.
     *
     * @param assertion - HCP assertions
     * @return List of HL7 permissions.
     * @throws InsufficientRightsException - When no permissions are provided, HCP is not authorized as consequence.
     */
    public static List<XMLObject> getPermissionValuesFromAssertion(final Assertion assertion) throws InsufficientRightsException {

        try {
            return getAttributeValuesFromAssertion(assertion, AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION);
        } catch (final MissingFieldException e) {
            // this is to get the behavior as before...
            LOGGER.error("InsufficientRightsException: '{}'", e.getMessage(), e);
            throw new InsufficientRightsException();
        }
    }

    public static String getPurposeOfUseCodeFromAssertion(final Assertion assertion) throws MissingFieldException {
        final String attributeName = AssertionConstants.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_PURPOSEOFUSE;
        return extractAttributeValueFromAssertion(assertion, attributeName, "PurposeOfUse", "code").orElseThrow(() -> {
            final String errorMessage = MessageFormat.format(ERROR_MESSAGE, attributeName);
            return new MissingFieldException(errorMessage);
        });
    }

    public static String getRoleCodeFromAssertion(final Assertion assertion) throws MissingFieldException {
        final String attributeName = AssertionConstants.URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE;
        return extractAttributeValueFromAssertion(assertion, attributeName, "Role", "code").orElseThrow(() -> {
            final String errorMessage = MessageFormat.format(ERROR_MESSAGE, attributeName);
            return new MissingFieldException(errorMessage);
        });
    }

    public static String getRoleNameFromAssertion(final Assertion assertion) throws MissingFieldException {
        final String attributeName = AssertionConstants.URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE;
        return extractAttributeValueFromAssertion(assertion, attributeName, "Role", "displayName").orElseThrow(() -> {
            final String errorMessage = MessageFormat.format(ERROR_MESSAGE, attributeName);
            return new MissingFieldException(errorMessage);
        });
    }

    public static Optional<String> extractAttributeValueFromAssertion(final Assertion assertion, final String attributeName, final String elementName, final String elementAttributeName) {
        for (final AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equals(attribute.getName(), attributeName)) {
                    if (!attribute.getAttributeValues().isEmpty()) {
                        return attribute.getAttributeValues().stream()
                                .findFirst() // Get the first value defensively
                                .map(XMLObject::getDOM)
                                .map(dom -> dom.getElementsByTagName(elementName))
                                .filter(nodeList -> nodeList.getLength() > 0) // Ensure the node list is not empty
                                .map(nodeList -> nodeList.item(0))
                                .map(Node::getAttributes)
                                .map(attributes -> attributes.getNamedItem(elementAttributeName))
                                .map(Node::getTextContent);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isExpired(final Assertion assertion) {

        if (assertion.getConditions().getNotBefore() != null && assertion.getConditions().getNotBefore().isAfter(DateTime.now())) {
            return true;
        }

        return assertion.getConditions().getNotOnOrAfter() != null && (assertion.getConditions().getNotOnOrAfter().isBefore(DateTime.now()) ||
                assertion.getConditions().getNotOnOrAfter().equals(Instant.now()));
    }
}
