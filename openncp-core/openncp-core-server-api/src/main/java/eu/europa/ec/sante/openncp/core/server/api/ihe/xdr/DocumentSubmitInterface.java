package eu.europa.ec.sante.openncp.core.server.api.ihe.xdr;

import eu.europa.ec.sante.openncp.core.common.ihe.NationalConnectorInterface;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.DiscardDispenseDetails;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;

/**
 * Interface for XDR document submit service implementation
 */
public interface DocumentSubmitInterface extends NationalConnectorInterface {

    /**
     * Stores a dispensation in the national infrastructure
     *
     * @param dispensationDocument - eDispensation document in epSOS pivot (CDA) form
     */
    void submitDispensation(EPSOSDocument dispensationDocument) throws NIException, InsufficientRightsException;

    /**
     * Discards a previously submitted dispensation
     *
     * @param dispensationToDiscard - Metadata of the dispensation to be discarded (XML and PDF versions)
     */
    void cancelDispensation(DiscardDispenseDetails discardDispenseDetails, EPSOSDocument dispensationToDiscard) throws NIException, InsufficientRightsException;
}
