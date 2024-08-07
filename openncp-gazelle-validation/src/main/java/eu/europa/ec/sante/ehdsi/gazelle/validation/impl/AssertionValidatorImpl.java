package eu.europa.ec.sante.ehdsi.gazelle.validation.impl;

import eu.europa.ec.sante.ehdsi.gazelle.validation.AssertionValidator;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateBase64Document;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateBase64DocumentResponse;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateDocument;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateDocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;

public class AssertionValidatorImpl extends AbstractValidator implements AssertionValidator {

    private final Logger logger = LoggerFactory.getLogger(AssertionValidatorImpl.class);

    AssertionValidatorImpl(final WebServiceTemplate webServiceTemplate) {
        super(webServiceTemplate);
    }

    @Override
    public String validateDocument(final String document, final String validator) {

        final ValidateDocument request = new ValidateDocument();
        request.setDocument(document);
        request.setValidator(validator);

        try {
            final ValidateDocumentResponse response = (ValidateDocumentResponse) webServiceTemplate.marshalSendAndReceive(request);
            return response.getDetailedResult();

        } catch (final WebServiceClientException e) {
            logger.error("An error occurred during validation process of the AssertionValidator. Please check the stack trace for more details.", e);
            return "N/A";
        }
    }

    @Override
    public String validateBase64Document(final String base64Document, final String validator) {

        final ValidateBase64Document request = new ValidateBase64Document();
        request.setBase64Document(base64Document);
        request.setValidator(validator);

        try {
            final ValidateBase64DocumentResponse response = (ValidateBase64DocumentResponse) webServiceTemplate.marshalSendAndReceive(request);
            return response.getDetailedResult();

        } catch (final WebServiceClientException e) {
            logger.error("An error occurred during validation process of the AssertionValidator. Please check the stack trace for more details.", e);
            return "N/A";
        }
    }
}
