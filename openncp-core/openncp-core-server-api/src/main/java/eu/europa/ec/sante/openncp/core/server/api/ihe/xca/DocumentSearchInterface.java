package eu.europa.ec.sante.openncp.core.server.api.ihe.xca;

import eu.europa.ec.sante.openncp.core.common.ihe.NationalConnectorInterface;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.*;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;

import java.util.List;

/**
 * Combined interface for Patient Summary, ePrescription and OrCD XCA Service implementation.
 * Implementations of the interface in the countries supporting only eP, only PS or only OrCD should throw UnsupportedOperationException on the missing methods.
 */
public interface DocumentSearchInterface extends NationalConnectorInterface {

    /**
     * This method returns one DocumentAssociation (with PSDocumentMetaData in XML and/or PDF format) that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return DocumentAssociation<PSDocumentMetaData>
     */
    DocumentAssociation<PSDocumentMetaData> getPSDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one/several DocumentAssociation(s) (EPDocumentMetaData in XML and/or PDF format) that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return List<DocumentAssociation < EPDocumentMetaData>>
     */
    List<DocumentAssociation<EPDocumentMetaData>> getEPDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one/several OrCD Laboratory Results DocumentMetaData in XML format that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return List<OrCDDocumentMetaData>
     */
    List<OrCDDocumentMetaData> getOrCDLaboratoryResultsDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one/several OrCD Hospital Discharge Reports DocumentMetaData in XML format that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return List<OrCDDocumentMetaData>
     */
    List<OrCDDocumentMetaData> getOrCDHospitalDischargeReportsDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one/several OrCD Medical Imaging Reports DocumentMetaData in XML format that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return List<OrCDDocumentMetaData>
     */
    List<OrCDDocumentMetaData> getOrCDMedicalImagingReportsDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one/several OrCD Medical Images DocumentMetaData in XML format that matches the searchCriteria.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return List<OrCDDocumentMetaData>
     */
    List<OrCDDocumentMetaData> getOrCDMedicalImagesDocumentList(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;

    /**
     * This method returns one EPSOSDocument which includes document metaData
     * and the DOM document itself matching the searchCriteria. The
     * searchCriteria shall have PatientId and DocumentId as mandatory fields
     * filled.
     *
     * @param searchCriteria (see SearchCriteria interface for more info)
     * @return EPSOSDocument
     */
    EPSOSDocument getDocument(SearchCriteria searchCriteria) throws NIException, InsufficientRightsException;
}
