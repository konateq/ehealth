package eu.europa.ec.sante.openncp.webmanager.backend.service;

import org.springframework.http.HttpStatus;

public enum ExceptionType {

    HTTP_MESSAGE_NOT_READABLE(HttpStatus.INTERNAL_SERVER_ERROR, "Http message not readable."),
    METHOD_ARGUMENT_NOT_VALID(HttpStatus.INTERNAL_SERVER_ERROR, "Method argument is nog valid."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred."),
    SMP_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Problem calling the SMP Server [{0}]"),
    // Gateway Admin module
    PWD_INVALID_FORMAT(HttpStatus.FORBIDDEN, "Invalid password : Length should between 8 and 30 characters with at least one uppercase letter, one lowercase letter, one number and one special character and no white spaces"),
    PWD_NOT_MATCHING(HttpStatus.FORBIDDEN, "Invalid password : Current password does not match");

    private final HttpStatus status;
    private final String message;

    ExceptionType(final HttpStatus status, final String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
