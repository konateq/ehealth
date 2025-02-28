package eu.europa.ec.sante.openncp.common.validation;

public interface IGazelleValidatorFactory {

    DocumentValidator getAssertionValidator();

    AuditMessageValidator getAuditMessageValidator();

    CdaValidator getCdaValidator();

    CertificateValidator getCertificateValidator();

    HL7v3Validator getHL7v3Validator();

    SchematronValidator getSchematronValidator();

    XdsValidator getXdsValidator();
}
