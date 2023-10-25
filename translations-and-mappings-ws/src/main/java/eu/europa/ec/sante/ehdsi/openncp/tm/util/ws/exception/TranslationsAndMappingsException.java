package eu.europa.ec.sante.ehdsi.openncp.tm.util.ws.exception;

public enum TranslationsAndMappingsException {

    BASE64_DOM_DECODING_EXCEPTION("Problem with decoding Base64 String to DOM Document");

    private final String message;

    TranslationsAndMappingsException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
