package eu.europa.ec.sante.openncp.core.client.ihe.xdr;

import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryResponseType;

/**
 * This is a Data Transformation Service. This provide functions to transform data into a XdrResponse object.
 *
 */
public class XdrResponseDts {

    /**
     * Private constructor to disable class instantiation.
     */
    private XdrResponseDts() {
    }

    public static XdrResponse newInstance(final RegistryResponseType registryResponse) {

        final XdrResponse result = new XdrResponse();

        if (registryResponse.getStatus() != null) {
            result.setResponseStatus(registryResponse.getStatus());
        }

        return result;
    }
}
