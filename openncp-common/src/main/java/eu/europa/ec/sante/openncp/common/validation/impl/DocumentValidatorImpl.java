package eu.europa.ec.sante.openncp.common.validation.impl;

import eu.europa.ec.sante.openncp.common.validation.DocumentValidator;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateBase64Document;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateBase64DocumentResponse;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateDocument;
import net.ihe.gazelle.jaxb.assertion.sante.ValidateDocumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;

public class DocumentValidatorImpl extends AbstractValidator implements DocumentValidator {

    private final Logger logger = LoggerFactory.getLogger(DocumentValidatorImpl.class);

    DocumentValidatorImpl(WebServiceTemplate webServiceTemplate) {
        super(webServiceTemplate);
    }

    @Override
    public String validateDocument(String document, String validator) {

        ValidateDocument request = new ValidateDocument();
        request.setDocument(document);
        request.setValidator(validator);

        try {
            ValidateDocumentResponse response = (ValidateDocumentResponse) webServiceTemplate.marshalSendAndReceive(request);
            return response.getDetailedResult();

        } catch (WebServiceClientException e) {
            logger.error("An error occurred during validation process of the AssertionValidator. Please check the stack trace for more details.", e);
            return "N/A";
        }
    }

    @Override
    public String validateBase64Document(String base64Document, String validator) {

        ValidateBase64Document request = new ValidateBase64Document();
        request.setBase64Document(base64Document);
        request.setValidator(validator);

        try {
            ValidateBase64DocumentResponse response = (ValidateBase64DocumentResponse) webServiceTemplate.marshalSendAndReceive(request);
            return response.getDetailedResult();

        } catch (WebServiceClientException e) {
            logger.error("An error occurred during validation process of the AssertionValidator. Please check the stack trace for more details.", e);
            return "N/A";
        }
    }
}
