package eu.europa.ec.sante.openncp.core.client.ihe.datamodel;

import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;

import java.util.List;
import java.util.stream.Collectors;

public class AdhocQueryRequestCreator {

    /**
     *
     */
    private AdhocQueryRequestCreator() {
    }

    /**
     * @param extension
     * @param root
     * @param documentCodes
     * @return
     */
    public static AdhocQueryRequest createAdhocQueryRequest(final String extension, final String root,
                                                            final List<GenericDocumentCode> documentCodes, final FilterParams filterParams) {


        final AdhocQueryRequest adhocQueryRequest = new AdhocQueryRequest();

        // Set AdhocQueryRequest/ResponseOption
        final ResponseOption rot = new ResponseOption();
        rot.setReturnComposedObjects(true);
        rot.setReturnType(XCAConstants.AdHocQueryRequest.RESPONSE_OPTIONS_RETURN_TYPE);
        adhocQueryRequest.setResponseOption(rot);

        // Create AdhocQueryRequest
        adhocQueryRequest.setAdhocQuery(new AdhocQueryType());
        adhocQueryRequest.getAdhocQuery().setId(XCAConstants.AdHocQueryRequest.ID);

        // Set XDSDocumentEntryPatientId Slot
        final Slot patientId = new Slot();
        patientId.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_PATIENTID_SLOT_NAME);
        final ValueList v1 = new ValueList();
        v1.getValues().add("'" + extension + "^^^&" + root + "&" + "ISO'");
        patientId.setValueList(v1);
        adhocQueryRequest.getAdhocQuery().getSlots().add(patientId);

        // Set XDSDocumentEntryStatus Slot
        final Slot entryStatus = new Slot();
        entryStatus.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_STATUS_SLOT_NAME);
        final ValueList v2 = new ValueList();
        v2.getValues().add(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_STATUS_SLOT_VALUE);
        entryStatus.setValueList(v2);
        adhocQueryRequest.getAdhocQuery().getSlots().add(entryStatus);

        // Set XDSDocumentEntryClassCode Slot
        final Slot entryClassCode = new Slot();
        entryClassCode.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_CLASSCODE_SLOT_NAME);
        final ValueList v3 = new ValueList();
        final String documentEntryClassCode = documentCodes.stream()
                .map(documentCode -> "'" + documentCode.getValue() + "^^" + documentCode.getSchema() + "'")
                .collect(Collectors.joining(",", "(", ")"));
        v3.getValues().add(documentEntryClassCode);
        entryClassCode.setValueList(v3);
        adhocQueryRequest.getAdhocQuery().getSlots().add(entryClassCode);


        //FilterParameters

        // Set XDSDocumentEntryFilterMaximumSize  Slot
        if(filterParams != null && filterParams.getMaximumSize() != null) {
            final Slot entryFilterMaximumSize = new Slot();
            entryFilterMaximumSize.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERMAXIMUMSIZE_SLOT_NAME);
            final ValueList v4 = new ValueList();
            v4.getValues().add(String.valueOf(filterParams.getMaximumSize()));
            entryFilterMaximumSize.setValueList(v4);
            adhocQueryRequest.getAdhocQuery().getSlots().add(entryFilterMaximumSize);
        }

        // Set XDSDocumentEntryFilterMaximumSize  Slot
        if(filterParams != null && filterParams.getCreatedBefore() != null) {
            final Slot entryFilterCreatedBefore = new Slot();
            entryFilterCreatedBefore.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDBEFORE_SLOT_NAME);
            final ValueList v5 = new ValueList();
            v5.getValues().add(String.valueOf(filterParams.getCreatedBefore()));
            entryFilterCreatedBefore.setValueList(v5);
            adhocQueryRequest.getAdhocQuery().getSlots().add(entryFilterCreatedBefore);
        }

        // Set XDSDocumentEntryFilterMaximumSize  Slot
        if(filterParams != null && filterParams.getCreatedAfter() != null) {
            final Slot entryFilterCreatedAfter = new Slot();
            entryFilterCreatedAfter.setName(XCAConstants.AdHocQueryRequest.XDS_DOCUMENT_ENTRY_FILTERCREATEDAFTER_SLOT_NAME);
            final ValueList v6 = new ValueList();
            v6.getValues().add(String.valueOf(filterParams.getCreatedAfter()));
            entryFilterCreatedAfter.setValueList(v6);
            adhocQueryRequest.getAdhocQuery().getSlots().add(entryFilterCreatedAfter);
        }


        return adhocQueryRequest;
    }
}
