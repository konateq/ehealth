package eu.europa.ec.sante.openncp.core.client.connector.display.cda.exceptions;

public class TerminologyFileNotFoundException extends Exception {

    public TerminologyFileNotFoundException(final String message) {
        super(message);
    }

    public TerminologyFileNotFoundException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
