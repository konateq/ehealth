package eu.europa.ec.sante.openncp.api.client;


import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;

@Domain
public interface AssertionContext {
    SamlDetails getSamlDetails();
}
