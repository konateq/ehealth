package eu.europa.ec.sante.openncp.application.client.connector.assertion;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;
import org.opensaml.saml.saml2.core.Assertion;

import javax.xml.soap.*;
import javax.xml.ws.WebServiceException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;

@Domain
public interface NokAssertionRequest extends AssertionRequest {
    String NOK_NS = "https://ehdsi.eu/assertion/nok";

    @Value.Default
    @Override
    default void validate(Assertion assertion) {
        OpenNCPValidation.validateTRCAssertion(assertion, NcpSide.NCP_B);
    }

    @Value.Auxiliary
    @Override
    default void getSoapBody(SOAPBody body) {

        try {
            var soapFactory = SOAPFactory.newInstance();
            var rstName = soapFactory.createName("RequestSecurityToken", "wst", WS_TRUST_NS);
            SOAPBodyElement rstElem = body.addBodyElement(rstName);

            var reqTypeName = soapFactory.createName("RequestType", "wst", WS_TRUST_NS);
            SOAPElement reqTypeElem = rstElem.addChildElement(reqTypeName);
            reqTypeElem.addTextNode(ACTION_URI);

            var tokenName = soapFactory.createName("TokenType", "wst", WS_TRUST_NS);
            SOAPElement tokenElem = rstElem.addChildElement(tokenName);
            tokenElem.addTextNode(SAML20_TOKEN_URN);

            var nokParamsName = soapFactory.createName("NoKParameters", "nok", NOK_NS);
            SOAPElement trcParamsElem = rstElem.addChildElement(nokParamsName);

            var nextOfKinId = soapFactory.createName("NextOfKinId", "nok", NOK_NS);
            SOAPElement nextOfKinIdElem = trcParamsElem.addChildElement(nextOfKinId);
            nextOfKinIdElem.addTextNode(getNextOfKinId());

            var nextOfKinFamilyName = soapFactory.createName("NextOfKinFamilyName", "nok", NOK_NS);
            SOAPElement nextOfKinFamilyNameElem = trcParamsElem.addChildElement(nextOfKinFamilyName);
            nextOfKinFamilyNameElem.addTextNode(getNextOfKinFamilyName());

            var nextOfKinFirstName = soapFactory.createName("NextOfKinFirstName", "nok", NOK_NS);
            SOAPElement NextOfKinFirstNameElem = trcParamsElem.addChildElement(nextOfKinFirstName);
            NextOfKinFirstNameElem.addTextNode(getNextOfKinFirstName());

            var nextOfKinGender = soapFactory.createName("NextOfKinGender", "nok", NOK_NS);
            SOAPElement nextOfKinGenderElem = trcParamsElem.addChildElement(nextOfKinGender);
            nextOfKinGenderElem.addTextNode(getNextOfKinGender());

            var nextOfKinBirthDate = soapFactory.createName("NextOfKinBirthDate", "nok", NOK_NS);
            SOAPElement nextOfKinBirthDateElem = trcParamsElem.addChildElement(nextOfKinBirthDate);
            nextOfKinBirthDateElem.addTextNode(getNextOfKinBirthDate());

            var nextOfKinAddressStreet = soapFactory.createName("NextOfKinAddressStreet", "nok", NOK_NS);
            SOAPElement nextOfKinAddressStreetElem = trcParamsElem.addChildElement(nextOfKinAddressStreet);
            nextOfKinAddressStreetElem.addTextNode(getNextOfKinAddressStreet());

            var nextOfKinAddressCity = soapFactory.createName("NextOfKinAddressCity", "nok", NOK_NS);
            SOAPElement nextOfKinAddressCityElem = trcParamsElem.addChildElement(nextOfKinAddressCity);
            nextOfKinAddressCityElem.addTextNode(getNextOfKinAddressCity());

            var nextOfKinPostalCode = soapFactory.createName("NextOfKinPostalCode", "nok", NOK_NS);
            SOAPElement nextOfKinPostalCodeElem = trcParamsElem.addChildElement(nextOfKinPostalCode);
            nextOfKinPostalCodeElem.addTextNode(getNextOfKinPostalCode());

            var nextOfKinAddressCountry = soapFactory.createName("NextOfKinAddressCountry", "nok", NOK_NS);
            SOAPElement nextOfKinAddressCountryElem = trcParamsElem.addChildElement(nextOfKinAddressCountry);
            nextOfKinAddressCountryElem.addTextNode(getNextOfKinAddressCountry());

        } catch (SOAPException ex) {
            throw new STSClientException("Error creating the SOAP body", ex);
        }
    }

    String getNextOfKinAddressCountry();

    String getNextOfKinPostalCode();

    String getNextOfKinAddressCity();

      String getNextOfKinAddressStreet();

    String getNextOfKinBirthDate();

    String getNextOfKinGender();

    String getNextOfKinFirstName();

    String getNextOfKinFamilyName();

    String getNextOfKinId();
}
