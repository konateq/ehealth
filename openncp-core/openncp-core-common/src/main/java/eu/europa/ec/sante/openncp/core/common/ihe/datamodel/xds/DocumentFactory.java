package eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.SimpleConfidentialityEnum;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.util.Date;
import java.util.List;

/**
 * Factory class for Clinical Document container (metadata and associations).
 */
public class DocumentFactory {

    /**
     * Private constructor restricting Object creation
     */
    private DocumentFactory() {
    }

    /**
     * Method creating an empty SearchCriteria class used by XCA Service transactions
     *
     * @return an implementation of the SearchCriteria interface used to pass the search parameters
     */
    public static SearchCriteria createSearchCriteria() {
        return new SearchCriteriaImpl();
    }

    /**
     * Method creating a SearchCriteria class used by XCA Service transactions and set with parameters
     *
     * @param criteria - Criteria from the Enum (PATIENT_ID, REPOSITORY_ID, DOCUMENT_ID etc.)
     * @param value    - Value of the parameter
     * @return an initialized instance of the SearchCriteria interface used to pass the search parameters
     */
    public static SearchCriteria createSearchCriteria(final SearchCriteria.Criteria criteria, final String value) {
        final SearchCriteria searchCriteria = new SearchCriteriaImpl();
        searchCriteria.add(criteria, value);
        return searchCriteria;
    }

    /**
     * Method creating an association between two clinical document
     *
     * @param xml - Structured document L3 as XML
     * @param pdf - Original document L1 as PDF
     * @param <T> - Specified type of object
     * @return a DocumentAssociation of the two documents
     */
    public static <T extends EPSOSDocumentMetaData> DocumentAssociation<T> createDocumentAssociation(final T xml, final T pdf) {
        return new DocumentAssociationImpl<>(xml, pdf);
    }

    /**
     * Method creating a generic Clinical Document
     *
     * @param patientId - identifier of the patient involved into the transaction
     * @param classCode - classCode of the type of Clinical Document
     * @param document  - generic Clinical Document
     * @return an instance of EPSOSDocument initialized with the parameters
     */
    public static EPSOSDocument createEPSOSDocument(final String patientId, final ClassCode classCode, final Document document) {
        return new EPSOSDocumentImpl(patientId, classCode, document);
    }

    //  Management of the EPDocument

    /**
     * Private Method creating a Prescription Document Metadata based on a minimal list of metadata
     *
     * @param documentFormat - classCode of the document exchanged.
     * @param id             - identifier of the clinical document.
     * @param patientId      - identifier of the patient involved into the transaction
     * @param effectiveDate  - date of the document
     * @param repositoryId   - identifier of the National clinical document repository
     * @param title          - title of the document
     * @param author         - author of the document
     * @param description    - summary of the document content
     * @param size           - size of the document exchanged in bytes
     * @param hash           - hash of the document exchanged
     * @return an instance of ePrescription Document Metadata
     */
    private static EPDocumentMetaData createEPDocument(final int documentFormat, final String id, final String patientId,
                                                       final Date effectiveDate, final String repositoryId, final String title,
                                                       final String author, final String description, final Long size, final String hash) {

        final EpListParam epListParam = new EpListParam(true, null, null, null,
                null, null, null);
        return createEPDocument(documentFormat, id, patientId, effectiveDate, repositoryId, title, author, description,
                null, null, epListParam, SimpleConfidentialityEnum.N, null, size, hash);
    }

    /**
     * Private Method creating a Prescription Document Metadata considering the required and optional information including
     * hash and size values of the document
     *
     * @param documentFormat  - classCode of the document exchanged.
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param description     - summary of the document content
     * @param productCode     - code of the product prescribed
     * @param productName     - name of the product described
     * @param epListParam     - ePrescription parameters as a List
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash of the document exchanged
     * @return an instance of ePrescription Document Metadata
     */
    private static EPDocumentMetaData createEPDocument(final int documentFormat, final String id, final String patientId,
                                                       final Date effectiveDate, final String repositoryId, final String title,
                                                       final String author, final String description, final String productCode,
                                                       final String productName, final EpListParam epListParam,
                                                       final SimpleConfidentialityEnum confidentiality, final String languageCode,
                                                       final Long size, final String hash) {

        final EPDocumentMetaDataImpl.SimpleConfidentialityMetadata confidentialityMetadata =
                new EPDocumentMetaDataImpl.SimpleConfidentialityMetadata(confidentiality);
        final EPSOSDocumentMetaData metaData = new EPSOSDocumentMetaDataImpl(id, patientId, documentFormat, effectiveDate,
                ClassCode.EP_CLASSCODE, repositoryId, title, author, confidentialityMetadata, languageCode, size, hash);

        if (StringUtils.isNoneBlank(productCode, productName)) {
            return new EPDocumentMetaDataImpl(metaData, description,
                    new EPDocumentMetaDataImpl.SimpleProductMetadata(productCode, productName), epListParam);
        } else {
            return new EPDocumentMetaDataImpl(metaData, description, epListParam);
        }
    }

    public static EPDocumentMetaData createEPDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final Long size, final String hash) {

        return createEPDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, description,
                true, size, hash);
    }

    public static EPDocumentMetaData createEPDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final Long size, final String hash) {

        return createEPDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, description, productCode,
                productName, new EpListParam(true, null, null, null, null,
                        null, null), SimpleConfidentialityEnum.N, null, size, hash);
    }

    public static EPDocumentMetaData createEPDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final boolean dispensable, final long size, final String hash) {

        return createEPDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, description, null,
                null, new EpListParam(dispensable, null, null, null, null,
                        null, null), SimpleConfidentialityEnum.N, null, size, hash);
    }

    /**
     * Method creating an original Prescription Document Metadata L1 containing only the required information of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param description     - summary of the document content
     * @param productCode     - code of the product prescribed
     * @param productName     - name of the product described
     * @param epListParam     - ePrescription parameters as a List
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @return a fully initialized original ePrescription Document Metadata
     */
    public static EPDocumentMetaData createEPDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final EpListParam epListParam,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode) {

        return createEPDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, description, productCode,
                productName, epListParam, confidentiality, languageCode, null, null);
    }

    /**
     * Method creating an original Prescription Document Metadata L1 containing required and optional information
     * of the document (hash and  size)
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param description     - summary of the document content
     * @param productCode     - code of the product prescribed
     * @param productName     - name of the product described
     * @param epListParam     - ePrescription parameters as a List
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash of the document exchanged
     * @return a fully initialized original ePrescription Document Metadata
     */
    public static EPDocumentMetaData createEPDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final EpListParam epListParam, final SimpleConfidentialityEnum confidentiality,
                                                         final String languageCode, final Long size, final String hash) {

        return createEPDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF, id, patientId, effectiveDate,
                repositoryId, title, author, description, productCode, productName, epListParam, confidentiality,
                languageCode, size, hash);
    }

    public static EPDocumentMetaData createEPDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author, final String description, final Long size, final String hash) {

        return createEPDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, description,
                true, size, hash);
    }

    public static EPDocumentMetaData createEPDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final Long size, final String hash) {

        return createEPDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, description,
                productCode, productName, new EpListParam(true, null, null, null,
                        null, null, null), SimpleConfidentialityEnum.N, null, size, hash);
    }

    public static EPDocumentMetaData createEPDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final boolean dispensable, final Long size, final String hash) {

        return createEPDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, description, null,
                null, new EpListParam(dispensable, null, null, null, null,
                        null, null), SimpleConfidentialityEnum.N, null, size, hash);
    }

    /**
     * Method creating a structured Prescription Document Metadata L3 containing only the required information of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param description     - summary of the document content
     * @param productCode     - code of the product prescribed
     * @param productName     - name of the product described
     * @param epListParam     - ePrescription parameters as a List
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @return a fully initialized structured ePrescription Document Metadata
     */
    public static EPDocumentMetaData createEPDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final EpListParam epListParam,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode) {

        return createEPDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, description, productCode,
                productName, epListParam, confidentiality, languageCode, null, null);
    }

    /**
     * Method creating a structured Prescription Document Metadata L3 containing required and optional information
     * of the document (hash and size)
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param author          - author of the document
     * @param description     - summary of the document content
     * @param productCode     - code of the product prescribed
     * @param productName     - name of the product described
     * @param epListParam     - ePrescription parameters as a List
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash value of the document
     * @return a fully initialized original ePrescription Document Metadata
     */
    public static EPDocumentMetaData createEPDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final String description, final String productCode, final String productName,
                                                         final EpListParam epListParam, final SimpleConfidentialityEnum confidentiality,
                                                         final String languageCode, final Long size, final String hash) {

        return createEPDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML, id, patientId, effectiveDate,
                repositoryId, title, author, description, productCode, productName, epListParam, confidentiality,
                languageCode, size, hash);
    }

    //  Management of the PSDocument

    /**
     * Private Method creating a Patient Summary Document Metadata
     *
     * @param documentFormat  - classCode of the document exchanged.
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash of the document exchanged
     * @return an instance of Patient Summary Document Metadata
     */
    private static PSDocumentMetaData createPSDocument(final int documentFormat, final String id, final String patientId,
                                                       final Date effectiveDate, final String repositoryId, final String title,
                                                       final String author, final SimpleConfidentialityEnum confidentiality,
                                                       final String languageCode, final Long size, final String hash) {

        final PSDocumentMetaDataImpl.SimpleConfidentialityMetadata confidentialityMetadata =
                new PSDocumentMetaDataImpl.SimpleConfidentialityMetadata(confidentiality);
        final EPSOSDocumentMetaData metaData = new EPSOSDocumentMetaDataImpl(id, patientId, documentFormat, effectiveDate,
                ClassCode.PS_CLASSCODE, repositoryId, title, author, confidentialityMetadata, languageCode, size, hash);
        return new PSDocumentMetaDataImpl(metaData);
    }

    /**
     * Method creating an original Patient Summary Document Metadata L1 containing only the required information of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @return a fully initialized structured Patient Summary Document Metadata
     */
    public static PSDocumentMetaData createPSDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode) {

        return createPSDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, confidentiality,
                languageCode, null, null);
    }

    /**
     * Method creating an original Patient Summary Document Metadata L1 containing all the required and optional information including
     * hash and size values of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash of the document exchanged
     * @return a fully initialized original Patient Summary Document Metadata
     */
    public static PSDocumentMetaData createPSDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode,
                                                         final Long size, final String hash) {

        return createPSDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF, id, patientId, effectiveDate,
                repositoryId, title, author, confidentiality, languageCode, size, hash);
    }

    /**
     * Method creating a structured Patient Summary Document Metadata L3 containing only the required information of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @return a fully initialized structured Patient Summary Document Metadata
     */
    public static PSDocumentMetaData createPSDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode) {

        return createPSDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, confidentiality, languageCode, null, null);
    }

    /**
     * Method creating a structured Patient Summary Document Metadata L3 containing all the required and optional information including
     * hash and size values of the document
     *
     * @param id              - identifier of the clinical document.
     * @param patientId       - identifier of the patient involved into the transaction
     * @param effectiveDate   - date of the document
     * @param repositoryId    - identifier of the National clinical document repository
     * @param title           - title of the document
     * @param author          - author of the document
     * @param confidentiality - level of confidentiality of the document
     * @param languageCode    - language of the document
     * @param size            - size of the document exchanged in bytes
     * @param hash            - hash of the document exchanged
     * @return a fully initialized structured Patient Summary Document Metadata
     */
    public static PSDocumentMetaData createPSDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final SimpleConfidentialityEnum confidentiality, final String languageCode,
                                                         final Long size, final String hash) {

        return createPSDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML, id, patientId, effectiveDate,
                repositoryId, title, author, confidentiality, languageCode, size, hash);
    }

    //  Management of the OrCDDocument
    private static OrCDDocumentMetaData createOrCDDocument(final ClassCode orCDClassCode,
                                                           final int documentFormat,
                                                           final String id,
                                                           final String patientId,
                                                           final Date effectiveDate,
                                                           final Date serviceStartTime,
                                                           final String description,
                                                           final String repositoryId,
                                                           final String title,
                                                           final String author,
                                                           final SimpleConfidentialityEnum confidentiality,
                                                           final String languageCode,
                                                           final OrCDDocumentMetaData.DocumentFileType documentFileType,
                                                           final Long size,
                                                           final List<OrCDDocumentMetaData.Author> authors,
                                                           final OrCDDocumentMetaData.ReasonOfHospitalisation reasonOfHospitalisation,
                                                           final String hash) {

        final OrCDDocumentMetaDataImpl.SimpleConfidentialityMetadata confidentialityMetadata =
                new OrCDDocumentMetaDataImpl.SimpleConfidentialityMetadata(confidentiality);
        final EPSOSDocumentMetaData metaData = new EPSOSDocumentMetaDataImpl(id, patientId, documentFormat, effectiveDate,
                orCDClassCode, repositoryId, title, author, confidentialityMetadata, languageCode, size, hash);
        return new OrCDDocumentMetaDataImpl(metaData, documentFileType, description, serviceStartTime, authors, reasonOfHospitalisation);
    }

    /**
     * Laboratory Results
     */
    public static OrCDDocumentMetaData createOrCDLaboratoryResultsDocument(final String id,
                                                                           final String patientId,
                                                                           final Date effectiveDate,
                                                                           final Date serviceStartTime,
                                                                           final String description,
                                                                           final String repositoryId,
                                                                           final String title,
                                                                           final String author,
                                                                           final SimpleConfidentialityEnum confidentiality,
                                                                           final String languageCode,
                                                                           final Long size,
                                                                           final List<OrCDDocumentMetaData.Author> authors,
                                                                           final String hash) {

        return createOrCDDocument(ClassCode.ORCD_LABORATORY_RESULTS_CLASSCODE, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML,
                id, patientId, effectiveDate, serviceStartTime, description, repositoryId, title, author, confidentiality,
                languageCode, OrCDDocumentMetaData.DocumentFileType.PDF, size, authors, null, hash);
    }

    /**
     * Hospital Discharge Reports
     */
    public static OrCDDocumentMetaData createOrCDHospitalDischargeReportsDocument(final String id,
                                                                                  final String patientId,
                                                                                  final Date effectiveDate,
                                                                                  final Date serviceStartTime,
                                                                                  final String description,
                                                                                  final String repositoryId,
                                                                                  final String title,
                                                                                  final String author,
                                                                                  final SimpleConfidentialityEnum confidentiality,
                                                                                  final String languageCode,
                                                                                  final Long size,
                                                                                  final List<OrCDDocumentMetaData.Author> authors,
                                                                                  final OrCDDocumentMetaData.ReasonOfHospitalisation reasonOfHospitalisation,
                                                                                  final String hash) {

        return createOrCDDocument(ClassCode.ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML,
                id, patientId, effectiveDate, serviceStartTime, description, repositoryId, title, author, confidentiality,
                languageCode, OrCDDocumentMetaData.DocumentFileType.PDF, size, authors, reasonOfHospitalisation, hash);
    }

    /**
     * Medical Imaging Reports
     */
    public static OrCDDocumentMetaData createOrCDMedicalImagingReportsDocument(final String id,
                                                                               final String patientId,
                                                                               final Date effectiveDate,
                                                                               final Date serviceStartTime,
                                                                               final String description,
                                                                               final String repositoryId,
                                                                               final String title,
                                                                               final String author,
                                                                               final SimpleConfidentialityEnum confidentiality,
                                                                               final String languageCode,
                                                                               final Long size,
                                                                               final List<OrCDDocumentMetaData.Author> authors,
                                                                               final OrCDDocumentMetaData.ReasonOfHospitalisation reasonOfHospitalisation,
                                                                               final String hash) {

        return createOrCDDocument(ClassCode.ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML,
                id, patientId, effectiveDate, serviceStartTime, description, repositoryId, title, author, confidentiality,
                languageCode, OrCDDocumentMetaData.DocumentFileType.PDF, size, authors, reasonOfHospitalisation, hash);
    }

    /**
     * Medical Images
     */
    public static OrCDDocumentMetaData createOrCDMedicalImagesDocument(final String id,
                                                                       final String patientId,
                                                                       final Date effectiveDate,
                                                                       final Date serviceStartTime,
                                                                       final String description,
                                                                       final String repositoryId,
                                                                       final String title,
                                                                       final String author,
                                                                       final SimpleConfidentialityEnum confidentiality,
                                                                       final String languageCode,
                                                                       final OrCDDocumentMetaData.DocumentFileType documentFileType,
                                                                       final Long size,
                                                                       final List<OrCDDocumentMetaData.Author> authors,
                                                                       final OrCDDocumentMetaData.ReasonOfHospitalisation reasonOfHospitalisation,
                                                                       final String hash) {

        return createOrCDDocument(ClassCode.ORCD_MEDICAL_IMAGES_CLASSCODE, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML,
                id, patientId, effectiveDate, serviceStartTime, description, repositoryId, title, author, confidentiality,
                languageCode, documentFileType, size, authors, reasonOfHospitalisation, hash);
    }


    //  Management of the EDDocument

    /**
     * Private Method creating a Dispense Document Metadata considering the required and optional information including
     * hash and size values of the document
     *
     * @param documentFormat - classCode of the document exchanged
     * @param id             - identifier of the clinical document
     * @param patientId      - patient identifier involved during the exchange
     * @param effectiveDate  - date of the document
     * @param repositoryId   - identifier of the National clinical document repository
     * @param title          - title of the document exchanged
     * @param author         - author of the clinical document exchanged
     * @param size           - size of the document exchanged in bytes
     * @param hash           - hash of the document exchanged
     * @return a fully initialized Dispense Document Metadata
     */
    private static EDDocumentMetaData createEDDocument(final int documentFormat, final String id, final String patientId,
                                                       final Date effectiveDate, final String repositoryId, final String title,
                                                       final String author, final Long size, final String hash) {

        final EPSOSDocumentMetaData metaData = new EPSOSDocumentMetaDataImpl(id, patientId, documentFormat, effectiveDate,
                ClassCode.ED_CLASSCODE, repositoryId, title, author, null, null, size, hash);

        return new EDDocumentMetaDataImpl(metaData);
    }

    /**
     * Method creating a Dispense Document Metadata L1 containing only the required information of the document
     *
     * @param id            - identifier of the clinical document
     * @param patientId     - patient identifier involved during the exchange
     * @param effectiveDate - date of the document
     * @param repositoryId  - identifier of the National clinical document repository
     * @param title         - title of the document exchanged
     * @param author        - author of the clinical document exchanged
     * @return a fully initialized original Dispense Document Metadata
     */
    public static EDDocumentMetaData createEDDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author) {

        return createEDDocumentPDF(id, patientId, effectiveDate, repositoryId, title, author, null, null);
    }

    /**
     * Method creating a Dispense Document Metadata L1 containing all the required and optional information including
     * hash and size values of the document
     *
     * @param id            - identifier of the clinical document
     * @param patientId     - patient identifier involved during the exchange
     * @param effectiveDate - date of the document
     * @param repositoryId  - identifier of the National clinical document repository
     * @param title         - title of the document exchanged
     * @param author        - author of the clinical document exchanged
     * @param size          - size of the document exchanged in bytes
     * @param hash          - hash of the document exchanged
     * @return a fully initialized original Dispense Document Metadata
     */
    public static EDDocumentMetaData createEDDocumentPDF(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final Long size, final String hash) {

        return createEDDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF, id, patientId, effectiveDate,
                repositoryId, title, author, size, hash);
    }

    /**
     * Method creating a Dispense Document Metadata L3 containing only the required information of the document
     *
     * @param id            - identifier of the clinical document
     * @param patientId     - patient identifier involved during the exchange
     * @param effectiveDate - date of the document
     * @param repositoryId  - identifier of the National clinical document repository
     * @param title         - title of the document exchanged
     * @param author        - author of the clinical document exchanged
     * @return a fully initialized structured Dispense Document Metadata
     */
    public static EDDocumentMetaData createEDDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author) {

        return createEDDocumentXML(id, patientId, effectiveDate, repositoryId, title, author, null, null);
    }

    /**
     * Method creating a Dispense Document Metadata L3 containing all the required and optional information including
     * hash and size values of the document
     *
     * @param id            - identifier of the clinical document
     * @param patientId     - patient identifier involved during the exchange
     * @param effectiveDate - date of the document
     * @param repositoryId  - identifier of the National clinical document repository
     * @param title         - title of the document exchanged
     * @param author        - author of the clinical document exchanged
     * @param size          - size of the document exchanged in bytes
     * @param hash          - hash of the document exchanged
     * @return a fully initialized structured Dispense Document Metadata
     */
    public static EDDocumentMetaData createEDDocumentXML(final String id, final String patientId, final Date effectiveDate,
                                                         final String repositoryId, final String title, final String author,
                                                         final Long size, final String hash) {

        return createEDDocument(EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML, id, patientId, effectiveDate,
                repositoryId, title, author, size, hash);
    }
}
