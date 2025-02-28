package eu.europa.ec.sante.openncp.common.validation;

public interface DocumentValidator {

    String validateDocument(String document, String validator);

    String validateBase64Document(String base64Document, String validator);
}
