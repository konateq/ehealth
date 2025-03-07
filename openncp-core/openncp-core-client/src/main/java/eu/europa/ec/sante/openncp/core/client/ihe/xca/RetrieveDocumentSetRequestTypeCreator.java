package eu.europa.ec.sante.openncp.core.client.ihe.xca;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.apache.commons.lang3.StringUtils;

public class RetrieveDocumentSetRequestTypeCreator {

    public RetrieveDocumentSetRequestType createRetrieveDocumentSetRequestType(String documentId, String homeCommunityId, String repositoryUniqId) throws OpenNCPException {

        RetrieveDocumentSetRequestType retrieveDocumentSetRequest = new RetrieveDocumentSetRequestType();
        RetrieveDocumentSetRequestType.DocumentRequest documentRequest = new RetrieveDocumentSetRequestType.DocumentRequest();
        if (!OidUtil.isValidHomeCommunityId(StringUtils.remove(homeCommunityId, Constants.OID_PREFIX))) {
            throw new OpenNCPException(OpenNCPErrorCode.ERROR_GENERIC, "Invalid message: HomeCommunity format not correct", null);
        }
        // Check for OID prefix, and adds it if not present (The OID prefix is required, as present in ITI TF-2b: 3.38.4.1.2.1);
        if (!homeCommunityId.startsWith(Constants.OID_PREFIX)) {
            homeCommunityId = Constants.OID_PREFIX + homeCommunityId;
        }

        // Set DocumentRequest/HomeCommunityId
        documentRequest.setHomeCommunityId(homeCommunityId);

        // Set DocumentRequest/RepositoryUniqueId
        documentRequest.setRepositoryUniqueId(repositoryUniqId);

        // Set DocumentRequest/DocumentUniqueId
        documentRequest.setDocumentUniqueId(documentId);

        retrieveDocumentSetRequest.getDocumentRequest().add(documentRequest);

        return retrieveDocumentSetRequest;
    }
}
