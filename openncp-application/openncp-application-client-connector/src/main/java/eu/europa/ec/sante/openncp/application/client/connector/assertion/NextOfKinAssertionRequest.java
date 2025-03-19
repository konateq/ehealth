package eu.europa.ec.sante.openncp.application.client.connector.assertion;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;
import org.opensaml.saml.saml2.core.Assertion;

import javax.xml.soap.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Domain
public interface NextOfKinAssertionRequest extends AssertionRequest {
    String NOK_NS = "https://ehdsi.eu/assertion/nok";
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String getPurposeOfUse();

    String getPatientId();

    String getNextOfKinId();

    Optional<String> getNextOfKinFamilyName();

    Optional<String> getNextOfKinFirstName();

    Optional<LocalDate> getNextOfKinBirthDate();

    Optional<String> getNextOfKinGender();

    Optional<String> getNextOfKinAddressStreet();

    Optional<String> getNextOfKinAddressPostalCode();

    Optional<String> getNextOfKinAddressCity();

    Optional<String> getNextOfKinAddressCountry();

    @Value.Default
    @Override
    default void validate(final Assertion assertion) {
        GazelleValidation.validateTRCAssertion(assertion, NcpSide.NCP_B);
    }

    @Value.Auxiliary
    @Override
    default void getSoapBody(final SOAPBody body) {
        try {
            final var soapFactory = SOAPFactory.newInstance();
            final var rstName = soapFactory.createName("RequestSecurityToken", "wst", WS_TRUST_NS);
            final SOAPBodyElement rstElem = body.addBodyElement(rstName);

            final var reqTypeName = soapFactory.createName("RequestType", "wst", WS_TRUST_NS);
            final SOAPElement reqTypeElem = rstElem.addChildElement(reqTypeName);
            reqTypeElem.addTextNode(ACTION_URI);

            final var tokenName = soapFactory.createName("TokenType", "wst", WS_TRUST_NS);
            final SOAPElement tokenElem = rstElem.addChildElement(tokenName);
            tokenElem.addTextNode(SAML20_TOKEN_URN);

            final var assertionParamName = soapFactory.createName("NoKParameters", "nok", NOK_NS);
            final SOAPElement nokParametersElement = rstElem.addChildElement(assertionParamName);

            addElementToParameters(soapFactory, "PatientId", nokParametersElement, getPatientId());
            addElementToParameters(soapFactory, "PurposeOfUse", nokParametersElement, getPurposeOfUse());
            addElementToParameters(soapFactory, "NextOfKinId", nokParametersElement, getNextOfKinId());

            getNextOfKinFirstName().ifPresent(nextOfKinFirstName -> {
                addElementToParameters(soapFactory, "NextOfKinFirstName", nokParametersElement, nextOfKinFirstName);
            });

            getNextOfKinFamilyName().ifPresent(nextOfKinFamilyName -> {
                addElementToParameters(soapFactory, "NextOfKinFamilyName", nokParametersElement, nextOfKinFamilyName);
            });

            getNextOfKinGender().filter(StringUtils::isNotBlank).ifPresent(nextOfKinGender -> {
                addElementToParameters(soapFactory, "NextOfKinGender", nokParametersElement, nextOfKinGender);
            });

            getNextOfKinAddressStreet().filter(StringUtils::isNotBlank).ifPresent(nextOfKinAddressStreet -> {
                addElementToParameters(soapFactory, "NextOfKinAddressStreet", nokParametersElement, nextOfKinAddressStreet);
            });

            getNextOfKinAddressCity().filter(StringUtils::isNotBlank).ifPresent(nextOfKinAddressCity -> {
                addElementToParameters(soapFactory, "NextOfKinAddressCity", nokParametersElement, nextOfKinAddressCity);
            });

            getNextOfKinAddressPostalCode().filter(StringUtils::isNotBlank).ifPresent(nextOfKinAddressPostalCode -> {
                addElementToParameters(soapFactory, "NextOfKinAddressPostalCode", nokParametersElement, nextOfKinAddressPostalCode);
            });

            getNextOfKinAddressCountry().filter(StringUtils::isNotBlank).ifPresent(nextOfKinAddressCountry -> {
                addElementToParameters(soapFactory, "NextOfKinAddressCountry", nokParametersElement, nextOfKinAddressCountry);
            });

            getNextOfKinBirthDate()
                    .map(birthDate -> birthDate.format(DATE_FORMATTER))
                    .ifPresent(nextOfKinBirthDate -> {
                        addElementToParameters(soapFactory, "NextOfKinBirthDate", nokParametersElement, nextOfKinBirthDate);
                    });
        } catch (final SOAPException ex) {
            throw new STSClientException("Error creating the SOAP body", ex);
        }
    }

    private static void addElementToParameters(final SOAPFactory soapFactory, final String name, final SOAPElement assertionParamElement, final String value) {
        try {
            final var idName = soapFactory.createName(name, "nok", NOK_NS);
            final SOAPElement element = assertionParamElement.addChildElement(idName);
            element.addTextNode(value);
        } catch (final SOAPException e) {
            throw new STSClientException("Error creating the SOAP body", e);
        }
    }
}
