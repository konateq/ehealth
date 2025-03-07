package eu.europa.ec.sante.openncp.core.common.ihe.exception;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

/**
 * Holds exceptions originated in the XDR Submit process.
 */
public class OpenNCPException extends ExceptionWithContext {

    public OpenNCPException(OpenNCPErrorCode openncpErrorCode, Throwable e) {
        super(openncpErrorCode, e);
    }

    public OpenNCPException(OpenNCPErrorCode openncpErrorCode, String message, String codeContext) {
        super(openncpErrorCode, message, codeContext);
    }

}
