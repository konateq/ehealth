package eu.europa.ec.sante.openncp.core.common.ihe;

public enum RegistryErrorSeverity {

    ERROR_SEVERITY_WARNING("urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Warning"),
    ERROR_SEVERITY_ERROR("urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error");

    private final String text;

    RegistryErrorSeverity(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
