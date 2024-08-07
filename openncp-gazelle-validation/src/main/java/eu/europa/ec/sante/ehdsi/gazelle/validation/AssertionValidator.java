package eu.europa.ec.sante.ehdsi.gazelle.validation;

public interface AssertionValidator {

    String validateDocument(String document, String validator);

    String validateBase64Document(String base64Document, String validator);
}
