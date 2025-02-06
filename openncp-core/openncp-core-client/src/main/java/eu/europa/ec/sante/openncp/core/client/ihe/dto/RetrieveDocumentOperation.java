package eu.europa.ec.sante.openncp.core.client.ihe.dto;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.client.api.RetrieveDocumentRequest;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;

@Domain
public interface RetrieveDocumentOperation {
    SamlDetails getSamlDetails();

    RetrieveDocumentRequest getRequest();
}
