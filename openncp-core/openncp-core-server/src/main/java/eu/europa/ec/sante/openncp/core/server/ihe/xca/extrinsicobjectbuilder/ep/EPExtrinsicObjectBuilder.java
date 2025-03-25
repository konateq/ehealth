package eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.ep;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocumentMetaData;
import eu.europa.ec.sante.openncp.core.server.CodeSystem;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ClassificationType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.ClassificationBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.SlotBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class EPExtrinsicObjectBuilder extends AbstractExtrinsicObjectBuilder {

    public static String build(final AdhocQueryRequest request, final ExtrinsicObjectType eot, final EPDocumentMetaData documentMetaData) {

        final var ofRim = new ObjectFactory();

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final boolean isPDF = documentMetaData.getFormat() == EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF;

        final var title = "eHDSI - ePrescription";
        build(request, eot, documentMetaData, ofRim, uuid, title);

        // Description
        eot.setDescription(ofRim.createInternationalStringType());
        eot.getDescription().getLocalizedStrings().add(ofRim.createLocalizedString());
        eot.getDescription().getLocalizedStrings().get(0).setValue(documentMetaData.getDescription());

        // Dispensable
        if (documentMetaData.isDispensable()) {
            final ClassificationType dispensableClassification = ClassificationBuilder.build(IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid, "urn:ihe:iti:xdw:2011:eventCode:open", "1.3.6.1.4.1.19376.1.2.3", "Open");
            eot.getClassifications().add(dispensableClassification);
        } else {
            final ClassificationType dispensableClassification = ClassificationBuilder.build(IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid, "urn:ihe:iti:xdw:2011:eventCode:closed", "1.3.6.1.4.1.19376.1.2.3", "Closed");
            eot.getClassifications().add(dispensableClassification);
        }

        // ATC code (former Product element)
        if (StringUtils.isNotBlank(documentMetaData.getAtcCode())) {
            final ClassificationType atcCodeClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    documentMetaData.getAtcCode(), CodeSystem.ATC.getOID(), documentMetaData.getAtcName());
            eot.getClassifications().add(atcCodeClassification);
        }

        // Dose Form Code
        if (StringUtils.isNotBlank(documentMetaData.getDoseFormCode())) {
            final ClassificationType doseFormClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid,
                    documentMetaData.getDoseFormCode(),
                    "0.4.0.127.0.16.1.1.2.1", documentMetaData.getDoseFormName());
            eot.getClassifications().add(doseFormClassification);
        }

        // Strength
        if (StringUtils.isNotBlank(documentMetaData.getStrength())) {
            final ClassificationType strengthClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    documentMetaData.getStrength(), "eHDSI_Strength_CodeSystem", "Strength of medication");
            eot.getClassifications().add(strengthClassification);
        }

        // Substitution
        final EPDocumentMetaData.SubstitutionMetaData substitutionMetaData = documentMetaData.getSubstitution();
        if (substitutionMetaData != null) {
            final ClassificationType substitutionClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    substitutionMetaData.getSubstitutionCode(), "2.16.840.1.113883.5.1070", substitutionMetaData.getSubstitutionDisplayName());
            eot.getClassifications().add(substitutionClassification);
        }

        // FormatCode
        if (isPDF) {
            eot.getClassifications().add(ClassificationBuilder.build(IheConstants.FORMAT_CODE_SCHEME,
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.NODE_REPRESENTATION, "IHE PCC",
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.DISPLAY_NAME));
        } else {
            eot.getClassifications().add(ClassificationBuilder.build(IheConstants.FORMAT_CODE_SCHEME,
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.NODE_REPRESENTATION,
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.CODING_SCHEME,
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.DISPLAY_NAME));
        }

        // Author Person
        final ClassificationType authorClassification = ClassificationBuilder.build(
                IheConstants.CLASSIFICATION_SCHEME_AUTHOR_UUID, uuid, "");
        authorClassification.getSlots().add(SlotBuilder.build(IheConstants.AUTHOR_PERSON_STR, documentMetaData.getAuthor()));
        eot.getClassifications().add(authorClassification);

        return uuid;
    }
}
