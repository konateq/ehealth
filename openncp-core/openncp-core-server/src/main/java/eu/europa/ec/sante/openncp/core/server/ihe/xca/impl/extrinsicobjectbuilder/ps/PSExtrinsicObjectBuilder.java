package eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.ps;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.ClassificationScheme;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;

import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.ClassificationBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.impl.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PSExtrinsicObjectBuilder extends AbstractExtrinsicObjectBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PSExtrinsicObjectBuilder.class);


    public static String build(AdhocQueryRequest request, ExtrinsicObjectType eot, EPSOSDocumentMetaData documentMetaData, boolean isPDF) {

        var ofRim = new ObjectFactory();

        String uuid = Constants.UUID_PREFIX + UUID.randomUUID();

        final String title;
        final String nodeRepresentation;
        final String displayName;

        switch (documentMetaData.getClassCode()) {

            case PS_CLASSCODE:
                title = Constants.PS_TITLE;
                nodeRepresentation = XCAConstants.EXTRINSIC_OBJECT.FormatCode.PatientSummary.EpsosPivotCoded.NODE_REPRESENTATION;
                displayName = XCAConstants.EXTRINSIC_OBJECT.FormatCode.PatientSummary.EpsosPivotCoded.DISPLAY_NAME;
                break;
            default:
                LOGGER.error("Unsupported classCode for query in OpenNCP. Requested document classCode: {}", documentMetaData.getClassCode());
                return "";
        }

        build(request, eot, documentMetaData, ofRim, uuid, title);

        // Description (optional)
        eot.setDescription(ofRim.createInternationalStringType());
        eot.getDescription().getLocalizedString().add(ofRim.createLocalizedStringType());
        if (isPDF) {
            eot.getDescription().getLocalizedString().get(0)
                    .setValue("The " + title + " document (CDA L1 / PDF body) for patient " + trimDocumentEntryPatientId(getDocumentEntryPatientId(request)));
        } else {
            eot.getDescription().getLocalizedString().get(0)
                    .setValue("The " + title + " document (CDA L3 / Structured body) for patient " + trimDocumentEntryPatientId(getDocumentEntryPatientId(request)));
        }

        // FormatCode
        if (isPDF) {
            eot.getClassification().add(ClassificationBuilder.build(ClassificationScheme.FORMAT_CODE.getUuid(),
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.NODE_REPRESENTATION,
                    "IHE PCC", XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.DISPLAY_NAME));
        } else {
            eot.getClassification().add(ClassificationBuilder.build(ClassificationScheme.FORMAT_CODE.getUuid(),
                    uuid, nodeRepresentation, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.CODING_SCHEME, displayName));
        }

        return uuid;
    }

    private static String trimDocumentEntryPatientId(String patientId) {

        if (patientId.contains("^^^")) {
            return patientId.substring(0, patientId.indexOf("^^^"));
        }
        return patientId;
    }
}
