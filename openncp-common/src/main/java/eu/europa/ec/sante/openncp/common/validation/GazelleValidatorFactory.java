package eu.europa.ec.sante.openncp.common.validation;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.validation.impl.DefaultGazelleValidatorFactory;

public class GazelleValidatorFactory {

    private GazelleValidatorFactory() {
    }

    public static DocumentValidator getAssertionValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getAssertionValidator();
    }

    public static AuditMessageValidator getAuditMessageValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getAuditMessageValidator();
    }

    public static CdaValidator getCdaValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getCdaValidator();
    }

    public static CertificateValidator getCertificateValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getCertificateValidator();
    }

    public static HL7v3Validator getHL7v3Validator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getHL7v3Validator();
    }

    public static SchematronValidator getSchematronValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getSchematronValidator();
    }

    public static XdsValidator getXdsValidator() {
        IGazelleValidatorFactory iGazelleValidatorFactory = getIGazelleValidatorFactory();
        return iGazelleValidatorFactory.getXdsValidator();
    }

    private static IGazelleValidatorFactory getIGazelleValidatorFactory() {
        return new DefaultGazelleValidatorFactory(ConfigurationManagerFactory.getConfigurationManager());
    }
}
