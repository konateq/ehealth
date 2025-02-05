package eu.europa.ec.sante.openncp.core.common.assertion.exceptions;


import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;

public class XSDValidationException extends OpenNCPErrorCodeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6854562291880477762L;

	private String message;

	private OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.ERROR_SEC_DATA_INTEGRITY_NOT_ENSURED;

	public XSDValidationException(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public OpenNCPErrorCode getErrorCode() {
		return openncpErrorCode;
	}
}
