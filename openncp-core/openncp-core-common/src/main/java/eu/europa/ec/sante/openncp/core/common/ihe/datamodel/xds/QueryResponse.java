package eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds;

import java.util.List;

public class QueryResponse {

    private List<XDSDocumentAssociation> documentAssociations;
    private List<String> failureMessages;

    public List<XDSDocumentAssociation> getDocumentAssociations() {
        return documentAssociations;
    }

    public void setDocumentAssociations(final List<XDSDocumentAssociation> documentAssociations) {
        this.documentAssociations = documentAssociations;
    }

    public List<String> getFailureMessages() {
        return failureMessages;
    }

    public void setFailureMessages(final List<String> failureMessages) {
        this.failureMessages = failureMessages;
    }
}
