package eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.OpenNCPErrorCodeException;

public class InsufficientRightsException extends OpenNCPErrorCodeException {

    private static final long serialVersionUID = -7973928727557097260L;

    private final OpenNCPErrorCode openncpErrorCode;

    public InsufficientRightsException() {
        openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
    }

    public InsufficientRightsException(Throwable cause) {
        super(cause);
        openncpErrorCode = OpenNCPErrorCode.ERROR_INSUFFICIENT_RIGHTS;
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
