package eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.ps;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.ClassificationScheme;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.EPSOSDocumentMetaData;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ExtrinsicObjectType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.ClassificationBuilder;
import eu.europa.ec.sante.openncp.core.server.ihe.xca.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class PSExtrinsicObjectBuilder extends AbstractExtrinsicObjectBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PSExtrinsicObjectBuilder.class);


    public static String build(final AdhocQueryRequest request, final ExtrinsicObjectType eot, final EPSOSDocumentMetaData documentMetaData, final boolean isPDF) {

        final var ofRim = new ObjectFactory();

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();

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
        eot.getDescription().getLocalizedStrings().add(ofRim.createLocalizedString());
        if (isPDF) {
            eot.getDescription().getLocalizedStrings().get(0)
                    .setValue("The " + title + " document (CDA L1 / PDF body) for patient " + trimDocumentEntryPatientId(getDocumentEntryPatientId(request)));
        } else {
            eot.getDescription().getLocalizedStrings().get(0)
                    .setValue("The " + title + " document (CDA L3 / Structured body) for patient " + trimDocumentEntryPatientId(getDocumentEntryPatientId(request)));
        }

        // FormatCode
        if (isPDF) {
            eot.getClassifications().add(ClassificationBuilder.build(ClassificationScheme.FORMAT_CODE.getUuid(),
                    uuid, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.NODE_REPRESENTATION,
                    "IHE PCC", XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.PdfSourceCoded.DISPLAY_NAME));
        } else {
            eot.getClassifications().add(ClassificationBuilder.build(ClassificationScheme.FORMAT_CODE.getUuid(),
                    uuid, nodeRepresentation, XCAConstants.EXTRINSIC_OBJECT.FormatCode.EPrescription.EpsosPivotCoded.CODING_SCHEME, displayName));
        }

        return uuid;
    }

    private static String trimDocumentEntryPatientId(final String patientId) {

        if (patientId.contains("^^^")) {
            return patientId.substring(0, patientId.indexOf("^^^"));
        }
        return patientId;
    }
}
