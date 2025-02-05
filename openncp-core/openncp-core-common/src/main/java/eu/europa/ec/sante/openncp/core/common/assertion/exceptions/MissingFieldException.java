package eu.europa.ec.sante.openncp.core.common.assertion.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class MissingFieldException extends AssertionValidationException {
	
    private static final long serialVersionUID = 9006271227090138486L;

    private final String messageDetailed;

    public MissingFieldException(String messageDetailed) {
        super();
        this.messageDetailed = messageDetailed;
    }

    public MissingFieldException(OpenNCPErrorCode openncpErrorCode, String messageDetailed) {
        super();
        this.setOpenncpErrorCode(openncpErrorCode);
        this.messageDetailed = messageDetailed;
    }

    @Override
    public String getMessage() {
        return messageDetailed;
    }
}
