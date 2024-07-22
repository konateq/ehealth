package eu.europa.ec.sante.openncp.core.client.connector.display.cda.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class UITransformationException extends Exception {

    private final OpenNCPErrorCode openncpErrorCode;

    private final Exception originalException;

    public UITransformationException(final Exception e) {
        openncpErrorCode = OpenNCPErrorCode.ERROR_UI_TRANSFORMATION;
        originalException = e;
    }

    public String getCode() {
        return openncpErrorCode.getCode();
    }

    public Exception getOriginalException() {
        return originalException;
    }

    public OpenNCPErrorCode getErrorCode() {
        return openncpErrorCode;
    }

}
