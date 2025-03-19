package eu.europa.ec.sante.openncp.core.common.assertion.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class AssertionValidationException extends OpenNCPErrorCodeException {

    private static final long serialVersionUID = -6478057187366024151L;
    private final String customMessage;
    private OpenNCPErrorCode openncpErrorCode;

    public AssertionValidationException() {
        this(null);
    }

    public AssertionValidationException(final String customMessage) {
        super();
        this.openncpErrorCode = OpenNCPErrorCode.ERROR_NOT_VALID_ASSERTION;
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        if (customMessage != null) {
            return customMessage;
        } else {
            return openncpErrorCode.getDescription();
        }
    }

    public String getCode() {
        return openncpErrorCode.getCode();
    }

    public OpenNCPErrorCode getErrorCode() {
        return openncpErrorCode;
    }

    protected void setOpenncpErrorCode(OpenNCPErrorCode openncpErrorCode) {
        this.openncpErrorCode = openncpErrorCode;
    }

}
