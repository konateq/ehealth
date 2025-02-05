package eu.europa.ec.sante.openncp.core.client.ihe.dto;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.client.api.SubmitDocumentRequest;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;

@Domain
public interface SubmitDocumentOperation {
    SamlDetails getSamlDetails();

    SubmitDocumentRequest getRequest();

}
