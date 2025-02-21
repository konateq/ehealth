package eu.europa.ec.sante.openncp.application.client.connector.assertion;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;
import org.opensaml.saml.saml2.core.Assertion;

import javax.xml.soap.*;

@Domain
public interface TrcAssertionRequest extends AssertionRequest {
    String TRC_NS = "https://ehdsi.eu/assertion/trc";

    String getPurposeOfUse();

    @Value.Default
    default String getPrescriptionId() {
        return StringUtils.EMPTY;
    }

    String getPatientId();

    @Value.Default
    default String getDispensationPinCode() {
        return StringUtils.EMPTY;
    }

    @Value.Default
    @Override
    default void validate(Assertion assertion) {
        GazelleValidation.validateTRCAssertion(assertion, NcpSide.NCP_B);
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

            var trcParamsName = soapFactory.createName("TRCParameters", "trc", TRC_NS);
            SOAPElement trcParamsElem = rstElem.addChildElement(trcParamsName);

            var purposeOfUseName = soapFactory.createName("PurposeOfUse", "trc", TRC_NS);
            SOAPElement purposeOfUseElem = trcParamsElem.addChildElement(purposeOfUseName);
            purposeOfUseElem.addTextNode(getPurposeOfUse());

            var patientIdName = soapFactory.createName("PatientId", "trc", TRC_NS);
            SOAPElement patientIdElem = trcParamsElem.addChildElement(patientIdName);
            patientIdElem.addTextNode(getPatientId());

            if (StringUtils.isNotBlank(getDispensationPinCode())) {
                var dispensationPinCodeName = soapFactory.createName("DispensationPinCode", "trc", TRC_NS);
                SOAPElement dispensationPinCodeElement = trcParamsElem.addChildElement(dispensationPinCodeName);
                dispensationPinCodeElement.addTextNode(getDispensationPinCode());
            }
            if (StringUtils.isNotBlank(getPrescriptionId())) {
                var prescriptionIdName = soapFactory.createName("PrescriptionId", "trc", TRC_NS);
                SOAPElement prescriptionIdElement = trcParamsElem.addChildElement(prescriptionIdName);
                prescriptionIdElement.addTextNode(getPrescriptionId());
            }
        } catch (SOAPException ex) {
            throw new STSClientException("Error creating the SOAP body", ex);
        }
    }
}
