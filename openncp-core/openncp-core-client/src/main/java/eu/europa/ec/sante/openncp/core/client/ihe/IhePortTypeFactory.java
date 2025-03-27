package eu.europa.ec.sante.openncp.core.client.ihe;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.RespondingGatewayPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType;

public interface IhePortTypeFactory {
    RespondingGatewayPortType createXCPDPort(ConfigurationManager configurationManager, String endpointUrl);

    eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xca.RespondingGatewayPortType createXCAPort(ConfigurationManager configurationManager, String endpointUrl);

    DocumentRecipientPortType createXDRPort(ConfigurationManager configurationManager, String endpointUrl);
}
