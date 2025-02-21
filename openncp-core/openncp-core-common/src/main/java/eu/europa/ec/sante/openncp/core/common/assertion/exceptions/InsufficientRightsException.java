package eu.europa.ec.sante.openncp.core.common.assertion.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class InsufficientRightsException extends OpenNCPErrorCodeException {

    private static final long serialVersionUID = -7973928727557097260L;

    private final OpenNCPErrorCode openncpErrorCode;
    private final String detailMessage;

    public InsufficientRightsException() {
        super();
        this.openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
        this.detailMessage = openncpErrorCode.getDescription();
    }

    public InsufficientRightsException(final Throwable cause) {
        super(cause);
        this.openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
        this.detailMessage = openncpErrorCode.getDescription();
    }

    public InsufficientRightsException(final String detailMessage) {
        super();
        this.openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
        this.detailMessage = detailMessage;
    }

    public InsufficientRightsException(final String detailMessage, final Throwable cause) {
        super(cause);
        this.openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
        this.detailMessage = detailMessage;
    }


    @Override
    public String getMessage() {
        return openncpErrorCode.getDescription();
    }

    public String getCode() {
        return openncpErrorCode.getCode();
    }

    public OpenNCPErrorCode getErrorCode() {
        return openncpErrorCode;
    }

}
