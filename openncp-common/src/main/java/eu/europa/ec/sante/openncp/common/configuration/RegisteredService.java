package eu.europa.ec.sante.openncp.common.configuration;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum RegisteredService {

    DISPENSATION_SERVICE("DispensationService", "urn:ehealth:ProvisioningOfData:Provide::XDR::ProvideandRegisterDocumentSet-b##ITI-41"),
    ORDER_SERVICE("OrderService", "urn:ehealth:RequestOfData::XCA::CrossGatewayQuery##ITI-38"),
    ORCD_SERVICE("OrCDService", "urn:ehealth:RequestOfData::XCA::CrossGatewayQuery##ITI-38"),
    PATIENT_IDENTIFICATION_SERVICE("PatientIdentificationService", "urn:ehealth:PatientIdentificationAndAuthentication::XCPD::CrossGatewayPatientDiscovery##ITI-55"),
    PATIENT_SERVICE("PatientService", "urn:ehealth:RequestOfData::XCA::CrossGatewayQuery##ITI-38"),
    FHIR_SERVICE("FhirService", "urn:ehealth:FhirService"),
    ITI_39("ITI-39", "urn:ehealth:RequestOfData::XCA::CrossGatewayRetrieve##ITI-39"),
    ITI_40("ITI-40", "urn:ehealth:CountryBIdentityProvider::XUA::ProvideX-UserAssertion##ITI-40"),
    ITI_63("ITI-63", "urn:ehealth:RequestOfData::XCF::CrossGatewayFetchRequest##ITI-63"),
    EHDSI_91("EHDSI-91", "urn:ehealth:CountryBIdentityProvider::identityProvider::HPAuthentication##EHDSI-91"),
    EHEALTH_105("ehealth-105", "urn:ehealth:VPN::VPNGatewayServer##ehealth-105"),
    EHEALTH_106("ehealth-106", "urn:ehealth:VPN::VPNGatewayClient##ehealth-106"),
    EHEALTH_107("ehealth-107", "urn:ehealth:ISM::InternationalSearchMask##ehealth-107");

    private static final Map<String, RegisteredService> MAP = Stream.of(RegisteredService.values()).collect(Collectors.toMap(RegisteredService::getServiceName, Function.identity()));
    private final String serviceName;
    private final String urn;

    RegisteredService(final String serviceName, final String urn) {
        this.serviceName = serviceName;
        this.urn = urn;
    }

    public static RegisteredService fromName(final String name) {
        return MAP.get(name);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getUrn() {
        return urn;
    }
}
