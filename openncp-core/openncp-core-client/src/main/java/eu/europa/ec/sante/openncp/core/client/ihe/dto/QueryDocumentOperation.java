package eu.europa.ec.sante.openncp.core.client.ihe.dto;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.client.api.QueryDocumentRequest;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;

@Domain
public interface QueryDocumentOperation {
    SamlDetails getSamlDetails();

    QueryDocumentRequest getRequest();
}
