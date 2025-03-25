package eu.europa.ec.sante.openncp.core.client.ihe.datamodel;

import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.OrCDDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocumentAssociation;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class represents a Data Transfer Service, used by XCA and AdhocQueryResponse messages.
 */
public final class AdhocQueryResponseConverter {

    private static final String RIM_CODING_SCHEME = "codingScheme";

    /**
     * Private constructor to avoid instantiation.
     */
    private AdhocQueryResponseConverter() {
    }

    /**
     * Transforms a AdhocQueryResponse in a QueryResponse.
     *
     * @param adhocQueryResponse - in AdhocQueryResponse format
     * @return a QueryResponse object.
     */
    public static QueryResponse convertAdhocQueryResponse(final AdhocQueryResponse adhocQueryResponse) {

        final var queryResponse = new QueryResponse();

        if (adhocQueryResponse.getRegistryObjectList() != null) {
            final Map<String, String> documentAssociationsMap = new TreeMap<>();
            final List<XDSDocument> documents = new ArrayList<>();
            String classificationScheme;

            for (var i = 0; i < adhocQueryResponse.getRegistryObjectList().getIdentifiables().size(); i++) {
                final JAXBElement<?> o = adhocQueryResponse.getRegistryObjectList().getIdentifiables().get(i);
                final String declaredTypeName = o.getDeclaredType().getSimpleName();

                if (StringUtils.equals("ExtrinsicObjectType", declaredTypeName)) {
                    final var xdsDocument = new XDSDocument();
                    final JAXBElement<ExtrinsicObjectType> eo = (JAXBElement<ExtrinsicObjectType>) adhocQueryResponse.getRegistryObjectList().getIdentifiables().get(i);

                    //Set id
                    xdsDocument.setId(eo.getValue().getId());

                    //Set Home Community ID
                    xdsDocument.setHcid(eo.getValue().getHome());

                    // Set name
                    xdsDocument.setName(eo.getValue().getName().getLocalizedStrings().get(0).getValue());

                    // Set mimeType
                    xdsDocument.setMimeType(eo.getValue().getMimeType());

                    // Set documentUniqueId
                    setDocumentUniqueId(eo.getValue(), xdsDocument);

                    setAdministrativeXdsMetadata(eo.getValue(), xdsDocument);

                    for (final ClassificationType classificationType : eo.getValue().getClassifications()) {

                        final var documentClassCodeType = classificationType.getNodeRepresentation();
                        classificationScheme = classificationType.getClassificationScheme();
                        //Set isPDF
                        setIsPDF(classificationScheme, classificationType, xdsDocument);

                        // Set healthcareFacility
                        setHealthcareFacility(classificationScheme, classificationType, xdsDocument);

                        // Set ClassCode
                        setClassCode(documentClassCodeType, classificationScheme, classificationType, xdsDocument);

                        // Set AuthorPerson
                        setAuthorPerson(classificationScheme, classificationType, xdsDocument);

                        // Set ATC Code (ATC => Anatomical Therapeutic Chemical)
                        setATCCode(classificationScheme, classificationType, xdsDocument);

                        // Set Dose Form Code
                        setDoseFormCode(classificationScheme, classificationType, xdsDocument);

                        // Set Strength
                        setStrength(classificationScheme, classificationType, xdsDocument);

                        // Set Substitution
                        setSubstitution(classificationScheme, classificationType, xdsDocument);

                        // Set Dispensable
                        setDispensable(classificationType, xdsDocument);

                        // Set Reason of Hospitalisation
                        setReasonOfHospitalisation(classificationScheme, classificationType, xdsDocument);
                    }

                    //  Set Description
                    setDescription(eo.getValue(), xdsDocument);

                    //  Add XDS Document processed to the list of documents associated into the QueryResponse.
                    documents.add(xdsDocument);

                } else if (StringUtils.equals("AssociationType1", declaredTypeName)) {

                    final JAXBElement<AssociationType1> associationType1JAXBElement = (JAXBElement<AssociationType1>) adhocQueryResponse.getRegistryObjectList().getIdentifiables().get(i);
                    processDocumentAssociationMap(associationType1JAXBElement, documentAssociationsMap);
                }
            }

            //  Set Document Associations
            final List<XDSDocumentAssociation> documentAssociations = new ArrayList<>();
            setDocumentAssociations(documentAssociations, documentAssociationsMap, documents);
            queryResponse.setDocumentAssociations(documentAssociations);
        }

        //Set FailureMessages
        setFailureMessages(queryResponse, adhocQueryResponse);

        return queryResponse;
    }

    private static void processDocumentAssociationMap(final JAXBElement<AssociationType1> jaxbElement,
                                                      final Map<String, String> documentAssociationsMap) {

        if (StringUtils.equals(jaxbElement.getValue().getAssociationType(), "urn:ihe:iti:2007:AssociationType:XFRM")) {
            documentAssociationsMap.put(jaxbElement.getValue().getSourceObject(), jaxbElement.getValue().getTargetObject());
        }
    }

    private static void setAdministrativeXdsMetadata(final ExtrinsicObjectType extrinsicObjectType, final XDSDocument xdsDocument) {

        for (final Slot slotType : extrinsicObjectType.getSlots()) {
            final var valueList = slotType.getValueList().getValues();
            if (CollectionUtils.isNotEmpty(valueList)) {
                switch (slotType.getName()) {
                    case "creationTime":
                        xdsDocument.setCreationTime(valueList.get(0));
                        break;
                    case "serviceStartTime":
                        xdsDocument.setEventTime(valueList.get(0));
                        break;
                    case "size":
                        xdsDocument.setSize(valueList.get(0));
                        break;
                    case "repositoryUniqueId":
                        xdsDocument.setRepositoryUniqueId(valueList.get(0));
                        break;
                    default:
                        // No metadata to process.
                        break;
                }
            }
        }
    }

    private static void setATCCode(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (StringUtils.equals(classificationScheme, IheConstants.CLASSIFICATION_EVENT_CODE_LIST) && classificationType.getSlots() != null) {
            final var ATC_CODE_SYSTEM_OID = "2.16.840.1.113883.6.73";
            for (final Slot slot : classificationType.getSlots()) {
                final var valueList = slot.getValueList().getValues();
                if (StringUtils.equals(slot.getName(), RIM_CODING_SCHEME) && CollectionUtils.isNotEmpty(valueList)) {
                    final var codingScheme = valueList.get(0);
                    if (StringUtils.equals(StringUtils.trimToEmpty(codingScheme), ATC_CODE_SYSTEM_OID)) {
                        xdsDocument.setAtcCode(classificationType.getNodeRepresentation());
                        if (CollectionUtils.isNotEmpty(classificationType.getName().getLocalizedStrings())) {
                            xdsDocument.setAtcText(classificationType.getName().getLocalizedStrings().get(0).getValue());
                        }
                    }
                }
            }
        }
    }

    private static void setDoseFormCode(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (StringUtils.equals(classificationScheme, IheConstants.CLASSIFICATION_EVENT_CODE_LIST) && classificationType.getSlots() != null) {
            final var EDQM_CODE_SYSTEM_OID = "0.4.0.127.0.16.1.1.2.1";
            for (final Slot slot : classificationType.getSlots()) {
                final var valueList = slot.getValueList().getValues();
                if (slot.getName().equals(RIM_CODING_SCHEME) && CollectionUtils.isNotEmpty(valueList)) {
                    final var codingScheme = valueList.get(0);
                    if (StringUtils.equals(StringUtils.trimToEmpty(codingScheme), EDQM_CODE_SYSTEM_OID)) {
                        xdsDocument.setDoseFormCode(classificationType.getNodeRepresentation());
                        if (CollectionUtils.isNotEmpty(classificationType.getName().getLocalizedStrings())) {
                            xdsDocument.setDoseFormText(classificationType.getName().getLocalizedStrings().get(0).getValue());
                        }
                    }
                }
            }
        }
    }

    private static void setAuthorPerson(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (classificationScheme.equals(IheConstants.CLASSIFICATION_SCHEME_AUTHOR_UUID) && classificationType.getSlots() != null) {
            final var author = new OrCDDocumentMetaData.Author();
            for (final Slot slot : classificationType.getSlots()) {
                final var valueList = slot.getValueList().getValues();
                if (StringUtils.equals(slot.getName(), IheConstants.AUTHOR_PERSON_STR) && CollectionUtils.isNotEmpty(valueList)) {
                    author.setAuthorPerson(valueList.get(0));
                } else if (StringUtils.equals(slot.getName(), IheConstants.AUTHOR_SPECIALITY_STR) && CollectionUtils.isNotEmpty(valueList)) {
                    author.setAuthorSpeciality(valueList);
                }
            }
            xdsDocument.getAuthors().add(author);
        }
    }

    private static void setClassCode(final String documentClassCodeType, final String classificationScheme,
                                     final ClassificationType classificationType, final XDSDocument xdsDocument) {

        final var valueList = classificationType.getSlots().get(0).getValueList().getValues();
        if (StringUtils.equals(classificationScheme, XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME) && CollectionUtils.isNotEmpty(valueList)) {
            xdsDocument.setClassCode(valueList.get(0), documentClassCodeType);
        }
    }

    private static void setDescription(final ExtrinsicObjectType extrinsicObjectType, final XDSDocument xdsDocument) {
        if (extrinsicObjectType.getDescription() != null
                && CollectionUtils.isNotEmpty(extrinsicObjectType.getDescription().getLocalizedStrings())) {
            xdsDocument.setDescription(extrinsicObjectType.getDescription().getLocalizedStrings().get(0).getValue());
        }
    }

    private static void setDispensable(final ClassificationType classificationType, final XDSDocument xdsDocument) {
        final var valueList = classificationType.getSlots().get(0).getValueList().getValues();
        if (StringUtils.equals(classificationType.getClassificationScheme(), "urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4")
                && CollectionUtils.isNotEmpty(valueList)
                && StringUtils.equals(valueList.get(0), "1.3.6.1.4.1.19376.1.2.3")) {
            xdsDocument.setDispensable(StringUtils.equals(classificationType.getNodeRepresentation(), "urn:ihe:iti:xdw:2011:eventCode:open"));
        }
    }

    private static void setDocumentUniqueId(final ExtrinsicObjectType extrinsicObjectType, final XDSDocument xdsDocument) {

        for (final ExternalIdentifierType externalIdentifierType : extrinsicObjectType.getExternalIdentifiers()) {
            final var localizedStringList = externalIdentifierType.getName().getLocalizedStrings();
            if (CollectionUtils.isNotEmpty(localizedStringList) &&
                    StringUtils.equalsIgnoreCase(localizedStringList.get(0).getValue(),
                            XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_STR)) {
                xdsDocument.setDocumentUniqueId(externalIdentifierType.getValue());
            }
        }
    }

    private static void setDocumentAssociations(final List<XDSDocumentAssociation> documentAssociations,
                                                final Map<String, String> documentAssociationsMap, final List<XDSDocument> documents) {

        for (final Map.Entry<String, String> entry : documentAssociationsMap.entrySet()) {

            final String sourceObjectId = entry.getKey();
            final String targetObjectId = entry.getValue();

            XDSDocument sourceObject = null;
            XDSDocument targetObject = null;

            for (final XDSDocument doc : documents) {
                if (doc.getId().matches(targetObjectId) && doc.getId().matches(sourceObjectId)) {
                    //OrCD
                    sourceObject = doc;
                    targetObject = doc;
                } else if (doc.getId().matches(sourceObjectId)) {
                    sourceObject = doc;
                } else if (doc.getId().matches(targetObjectId)) {
                    targetObject = doc;
                } else {
                    continue;
                }

                if (sourceObject != null && targetObject != null) {
                    break;
                }
            }

            if (sourceObject != null && targetObject != null) {
                final var xdsDocumentAssociation = new XDSDocumentAssociation();

                if (sourceObject.isPDF()) {
                    xdsDocumentAssociation.setCdaPDF(sourceObject);
                } else {
                    xdsDocumentAssociation.setCdaXML(sourceObject);
                }

                if (targetObject.isPDF()) {
                    xdsDocumentAssociation.setCdaPDF(targetObject);
                } else {
                    xdsDocumentAssociation.setCdaXML(targetObject);
                }

                documentAssociations.add(xdsDocumentAssociation);
            }

            documents.remove(sourceObject);
            documents.remove(targetObject);
        }

        for (final XDSDocument xdsDocument : documents) {
            final var xdsDocumentAssociation = new XDSDocumentAssociation();
            xdsDocumentAssociation.setCdaPDF(xdsDocument.isPDF() ? xdsDocument : null);
            xdsDocumentAssociation.setCdaXML(xdsDocument.isPDF() ? null : xdsDocument);

            documentAssociations.add(xdsDocumentAssociation);
        }
    }

    private static void setFailureMessages(final QueryResponse queryResponse, final AdhocQueryResponse adhocQueryResponse) {

        if (adhocQueryResponse.getRegistryErrorList() != null) {
            final List<String> errors = new ArrayList<>(adhocQueryResponse.getRegistryErrorList().getRegistryErrors().size());

            for (var i = 0; i < adhocQueryResponse.getRegistryErrorList().getRegistryErrors().size(); i++) {
                errors.add(adhocQueryResponse.getRegistryErrorList().getRegistryErrors().get(i).getCodeContext());
            }

            queryResponse.setFailureMessages(errors);
        }
    }

    private static void setHealthcareFacility(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (StringUtils.equals(classificationScheme, "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1")
                && classificationType != null
                && classificationType.getName() != null) {
            final var localizedStringList = classificationType.getName().getLocalizedStrings();
            if (CollectionUtils.isNotEmpty(localizedStringList)) {
                xdsDocument.setHealthcareFacility(localizedStringList.get(0).getValue());
            }
        }
    }

    private static void setIsPDF(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (StringUtils.equals(classificationScheme, IheConstants.FORMAT_CODE_SCHEME)) {
            xdsDocument.setPDF(classificationType.getNodeRepresentation().equals("urn:ihe:iti:xds-sd:pdf:2008"));
            final var valueList = classificationType.getSlots().get(0).getValueList().getValues();
            // Set FormatCode
            if (CollectionUtils.isNotEmpty(valueList)) {
                xdsDocument.setFormatCode(valueList.get(0), classificationType.getNodeRepresentation());
            }
        }
    }

    private static void setReasonOfHospitalisation(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {

        if (classificationScheme.equals(IheConstants.CLASSIFICATION_EVENT_CODE_LIST) && classificationType != null) {
            final var ICD_10_CODE_SYSTEM_OID = "1.3.6.1.4.1.12559.11.10.1.3.1.44.2";
            final var code = classificationType.getNodeRepresentation();
            var text = StringUtils.EMPTY;
            if (classificationType.getName() != null &&
                    CollectionUtils.isNotEmpty(classificationType.getName().getLocalizedStrings())) {
                text = classificationType.getName().getLocalizedStrings().get(0).getValue();
            }
            for (final Slot slot : classificationType.getSlots()) {
                if (StringUtils.equals(slot.getName(), RIM_CODING_SCHEME) && CollectionUtils.isNotEmpty(slot.getValueList().getValues())) {
                    final var codingScheme = slot.getValueList().getValues().get(0);
                    if (StringUtils.equals(StringUtils.trimToEmpty(codingScheme), ICD_10_CODE_SYSTEM_OID)) {
                        xdsDocument.setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation(code, codingScheme, text));
                    }
                }
            }

        }
    }

    private static void setStrength(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {
        if (StringUtils.equals(classificationScheme, IheConstants.CLASSIFICATION_EVENT_CODE_LIST) && classificationType.getSlots() != null) {
            final var EHDSI_STRENGTH_CODE_SYSTEM_OID = "eHDSI_Strength_CodeSystem";
            for (final Slot slot : classificationType.getSlots()) {
                final var valueList = slot.getValueList().getValues();
                if (slot.getName().equals(RIM_CODING_SCHEME) && CollectionUtils.isNotEmpty(valueList)) {
                    final var codingScheme = valueList.get(0);
                    if (StringUtils.equals(StringUtils.trimToEmpty(codingScheme), EHDSI_STRENGTH_CODE_SYSTEM_OID)) {
                        xdsDocument.setStrength(classificationType.getNodeRepresentation());
                    }
                }
            }
        }
    }

    private static void setSubstitution(final String classificationScheme, final ClassificationType classificationType, final XDSDocument xdsDocument) {
        if (StringUtils.equals(classificationScheme, IheConstants.CLASSIFICATION_EVENT_CODE_LIST) && classificationType.getSlots() != null) {
            final var EHDSI_SUBSTITUTION_CODE_SYSTEM_OID = "2.16.840.1.113883.5.1070";
            for (final Slot slot : classificationType.getSlots()) {
                final var valueList = slot.getValueList().getValues();
                if (slot.getName().equals(RIM_CODING_SCHEME) && CollectionUtils.isNotEmpty(valueList)) {
                    final var codingScheme = valueList.get(0);
                    if (StringUtils.equals(StringUtils.trimToEmpty(codingScheme), EHDSI_SUBSTITUTION_CODE_SYSTEM_OID) && classificationType.getNodeRepresentation() != null) {
                        xdsDocument.setSubstitution(classificationType.getNodeRepresentation());
                    }
                }
            }
        }
    }
}
