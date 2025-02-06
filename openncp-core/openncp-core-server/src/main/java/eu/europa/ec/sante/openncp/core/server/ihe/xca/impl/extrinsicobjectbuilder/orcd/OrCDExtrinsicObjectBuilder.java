package eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.orcd;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.ClassificationScheme;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.OrCDDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ClassificationType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.SlotType1;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.ClassificationBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.SlotBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class OrCDExtrinsicObjectBuilder extends AbstractExtrinsicObjectBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrCDExtrinsicObjectBuilder.class);

    /**
     * Method to build the Extrinsic object to be used for the XCA Query service for OrCD documents.
     *
     * @param request
     * @param eot
     * @param orCDDocumentMetaData
     * @return
     */
    public static String build(AdhocQueryRequest request,
                               ExtrinsicObjectType eot,
                               OrCDDocumentMetaData orCDDocumentMetaData) {

        var ofRim = new ObjectFactory();
        var uuid = Constants.UUID_PREFIX + UUID.randomUUID();

        final String title;
        final String nodeRepresentation;
        final String displayName;

        var classCode = orCDDocumentMetaData.getClassCode();
        switch (classCode) {
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                title = Constants.ORCD_HOSPITAL_DISCHARGE_REPORTS_TITLE;
                nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.NODE_REPRESENTATION;
                displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.DISPLAY_NAME;
                break;
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
                title = Constants.ORCD_LABORATORY_RESULTS_TITLE;
                nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.NODE_REPRESENTATION;
                displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.DISPLAY_NAME;
                break;
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                title = Constants.ORCD_MEDICAL_IMAGING_REPORTS_TITLE;
                nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.NODE_REPRESENTATION;
                displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PdfSourceCoded.DISPLAY_NAME;
                break;
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                title = Constants.ORCD_MEDICAL_IMAGES_TITLE;
                switch (orCDDocumentMetaData.getDocumentFileType()) {
                    case PNG:
                        nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PngSourceCoded.NODE_REPRESENTATION;
                        displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.PngSourceCoded.DISPLAY_NAME;
                        break;
                    case JPEG:
                        nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.JpegSourceCoded.NODE_REPRESENTATION;
                        displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.OrCD.JpegSourceCoded.DISPLAY_NAME;
                        break;
                    default:
                        LOGGER.error("Unsupported document file type '{}' for OrCD Medical Images", orCDDocumentMetaData.getDocumentFileType());
                        return "";
                }
                break;
            default:
                LOGGER.error("Unsupported classCode for OrCD query in OpenNCP. Requested classCode: {}", classCode);
                return "";
        }

        build(request, eot, orCDDocumentMetaData, ofRim, uuid, title);

        // Description
        eot.setDescription(ofRim.createInternationalStringType());
        eot.getDescription().getLocalizedString().add(ofRim.createLocalizedStringType());
        eot.getDescription().getLocalizedString().get(0).setValue(orCDDocumentMetaData.getDescription());

        // FormatCode
        eot.getClassification().add(ClassificationBuilder.build(ClassificationScheme.FORMAT_CODE.getUuid(),
                uuid, nodeRepresentation, "eHDSI formatCodes", displayName));

        // Service Start time (optional)
        eot.getSlot().add(SlotBuilder.build("serviceStartTime", DateUtil.getDateByDateFormat("yyyyMMddHHmmss", orCDDocumentMetaData.getServiceStartTime())));

        // Reason of hospitalisation
        var reasonOfHospitalisation = orCDDocumentMetaData.getReasonOfHospitalisation();
        if (reasonOfHospitalisation != null) {
            eot.getClassification().add(ClassificationBuilder.build("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4",
                    uuid, reasonOfHospitalisation.getCode(), reasonOfHospitalisation.getCodingScheme(), reasonOfHospitalisation.getText()));
        }

        //Authors
        for (OrCDDocumentMetaData.Author author : orCDDocumentMetaData.getAuthors()) {
            ClassificationType classificationAuthor = ClassificationBuilder.build(IheConstants.CLASSIFICATION_SCHEME_AUTHOR_UUID,
                    uuid, "");

            if (author.getAuthorPerson() != null) {
                SlotType1 authorPersonSlot = SlotBuilder.build(IheConstants.AUTHOR_PERSON_STR, author.getAuthorPerson());
                classificationAuthor.getSlot().add(authorPersonSlot);
            }

            if (author.getAuthorSpeciality() != null && !author.getAuthorSpeciality().isEmpty()) {
                SlotType1 authorSpecialtySlot = SlotBuilder.build(IheConstants.AUTHOR_SPECIALITY_STR, author.getAuthorSpeciality());
                classificationAuthor.getSlot().add(authorSpecialtySlot);
            }
            eot.getClassification().add(classificationAuthor);
        }

        return uuid;
    }
}
