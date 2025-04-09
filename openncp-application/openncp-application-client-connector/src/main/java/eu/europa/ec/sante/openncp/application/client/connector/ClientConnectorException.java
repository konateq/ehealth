package eu.europa.ec.sante.openncp.application.client.connector;

import java.util.Optional;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class ClientConnectorException extends Exception {

    private OpenNCPErrorCode openncpErrorCode;
    private String niExceptionMessage;

    public ClientConnectorException(final OpenNCPErrorCode openncpErrorCode, final String message, final String niExceptionMessage) {
        super(message);
        this.openncpErrorCode = openncpErrorCode;
        this.niExceptionMessage = niExceptionMessage;
    }

    public ClientConnectorException(final OpenNCPErrorCode openncpErrorCode, final String message, final String niExceptionMessage, final Throwable cause) {
        super(message, cause);
        this.openncpErrorCode = openncpErrorCode;
        this.niExceptionMessage = niExceptionMessage;
    }

    public ClientConnectorException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public Optional<OpenNCPErrorCode> getOpenNCPErrorCode() {
        return Optional.ofNullable(openncpErrorCode);
    }

    public Optional<String> getNiErrorMessage() {return Optional.ofNullable(niExceptionMessage); }

}
