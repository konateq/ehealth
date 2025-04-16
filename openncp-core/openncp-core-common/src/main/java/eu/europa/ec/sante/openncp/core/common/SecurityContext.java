package eu.europa.ec.sante.openncp.core.common;


import eu.europa.ec.sante.openncp.common.immutables.Domain;

import javax.xml.soap.SOAPHeader;
import java.util.Optional;

@Domain
public interface SecurityContext {
    Optional<SamlDetails> getSamlDetails();

    Optional<SOAPHeader> getSoapHeader();
}
