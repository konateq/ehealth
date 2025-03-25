package eu.europa.ec.sante.openncp.core.client.ihe.dts;


import eu.europa.ec.sante.openncp.core.client.api.ObjectFactory;
import eu.europa.ec.sante.openncp.core.client.api.RetrieveDocumentResponse;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RetrieveDocumentSetResponse;

/**
 * This is a Data Transformation Service providing functions to transform data into a RetrieveDocumentResponseDTS object.
 */
public class RetrieveDocumentResponseDts {

    static final ObjectFactory objectFactory = new ObjectFactory();

    private RetrieveDocumentResponseDts() {
    }

    public static RetrieveDocumentResponse newInstance(final RetrieveDocumentSetResponse.DocumentResponse documentResponse) {

        if (documentResponse == null) {
            return null;
        }

        final RetrieveDocumentResponse result = objectFactory.createRetrieveDocumentResponse();
        result.setReturn(DocumentDts.newInstance(documentResponse));
        return result;
    }
}
