package eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.ep;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ClassificationType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.server.CodeSystem;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.ClassificationBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.SlotBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class EPExtrinsicObjectBuilder extends AbstractExtrinsicObjectBuilder {

    public static String build(AdhocQueryRequest request, ExtrinsicObjectType eot, EPDocumentMetaData documentMetaData) {

        var ofRim = new ObjectFactory();

        String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        boolean isPDF = documentMetaData.getFormat() == EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF;

        var title = "eHDSI - ePrescription";
        build(request, eot, documentMetaData, ofRim, uuid, title);

        // Description
        eot.setDescription(ofRim.createInternationalStringType());
        eot.getDescription().getLocalizedString().add(ofRim.createLocalizedStringType());
        eot.getDescription().getLocalizedString().get(0).setValue(documentMetaData.getDescription());

        // Dispensable
        if (documentMetaData.isDispensable()) {
            ClassificationType dispensableClassification = ClassificationBuilder.build(IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid, "urn:ihe:iti:xdw:2011:eventCode:open", "1.3.6.1.4.1.19376.1.2.3", "Open");
            eot.getClassification().add(dispensableClassification);
        } else {
            ClassificationType dispensableClassification = ClassificationBuilder.build(IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid, "urn:ihe:iti:xdw:2011:eventCode:closed", "1.3.6.1.4.1.19376.1.2.3", "Closed");
            eot.getClassification().add(dispensableClassification);
        }

        // ATC code (former Product element)
        if (StringUtils.isNotBlank(documentMetaData.getAtcCode())) {
            ClassificationType atcCodeClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    documentMetaData.getAtcCode(), CodeSystem.ATC.getOID(), documentMetaData.getAtcName());
            eot.getClassification().add(atcCodeClassification);
        }

        // Dose Form Code
        if (StringUtils.isNotBlank(documentMetaData.getDoseFormCode())) {
            ClassificationType doseFormClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST,
                    uuid,
                    documentMetaData.getDoseFormCode(),
                    "0.4.0.127.0.16.1.1.2.1", documentMetaData.getDoseFormName());
            eot.getClassification().add(doseFormClassification);
        }

        // Strength
        if (StringUtils.isNotBlank(documentMetaData.getStrength())) {
            ClassificationType strengthClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    documentMetaData.getStrength(), "eHDSI_Strength_CodeSystem", "Strength of medication");
            eot.getClassification().add(strengthClassification);
        }

        // Substitution
        EPDocumentMetaData.SubstitutionMetaData substitutionMetaData = documentMetaData.getSubstitution();
        if (substitutionMetaData != null) {
            ClassificationType substitutionClassification = ClassificationBuilder.build(
                    IheConstants.CLASSIFICATION_EVENT_CODE_LIST, uuid,
                    substitutionMetaData.getSubstitutionCode(), "2.16.840.1.113883.5.1070", substitutionMetaData.getSubstitutionDisplayName());
            eot.getClassification().add(substitutionClassification);
        }

        // FormatCode
        if (isPDF) {
            eot.getClassification().add(ClassificationBuilder.build(IheConstants.FORMAT_CODE_SCHEME,
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.NODE_REPRESENTATION, "IHE PCC",
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.DISPLAY_NAME));
        } else {
            eot.getClassification().add(ClassificationBuilder.build(IheConstants.FORMAT_CODE_SCHEME,
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.NODE_REPRESENTATION,
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.CODING_SCHEME,
                    XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.DISPLAY_NAME));
        }

        // Author Person
        ClassificationType authorClassification = ClassificationBuilder.build(
                IheConstants.CLASSIFICATION_SCHEME_AUTHOR_UUID, uuid, "");
        authorClassification.getSlot().add(SlotBuilder.build(IheConstants.AUTHOR_PERSON_STR, documentMetaData.getAuthor()));
        eot.getClassification().add(authorClassification);

        return uuid;
    }
}
