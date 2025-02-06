package eu.europa.ec.sante.openncp.core.client.ihe.dto;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.client.api.QueryPatientRequest;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;

@Domain
public interface QueryPatientOperation {
    SamlDetails getSamlDetails();

    QueryPatientRequest getRequest();
}
