package eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.exceptions;

import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;

public class InvalidFieldException extends AssertionValidationException {

    private static final long serialVersionUID = 8922552840552468444L;

    private final String messageDetailed;

    public InvalidFieldException(String messageDetailed) {
        super();
        this.messageDetailed = messageDetailed;
    }

    public InvalidFieldException(OpenNCPErrorCode openncpErrorCode, String messageDetailed) {
        super();
        this.setOpenncpErrorCode(openncpErrorCode);
        this.messageDetailed = messageDetailed;
    }

    @Override
    public String getMessage() {
        return this.getErrorCode().getDescription() + " " + messageDetailed;
    }
}
