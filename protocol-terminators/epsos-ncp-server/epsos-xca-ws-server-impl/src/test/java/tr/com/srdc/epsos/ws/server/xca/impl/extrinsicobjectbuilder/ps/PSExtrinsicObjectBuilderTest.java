package tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.ps;

import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import fi.kela.se.epsos.data.model.EPSOSDocumentMetaDataImpl;
import fi.kela.se.epsos.data.model.PSDocumentMetaData;
import fi.kela.se.epsos.data.model.PSDocumentMetaDataImpl;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import tr.com.srdc.epsos.data.model.SimpleConfidentialityEnum;
import tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilderTest;

import java.util.Date;

@Ignore
public class PSExtrinsicObjectBuilderTest extends AbstractExtrinsicObjectBuilderTest {

    @Test
    public void testSize() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();

        PSExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, buildPSDocumentMetaData(), false);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        var found = false;
        for (SlotType1 slot: extrinsicObject.getValue().getSlot()) {
            if (slot.getName().equals("size") &&
                    slot.getValueList().getValue().iterator().next().equals("1000")) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testHash() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();

        PSExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, buildPSDocumentMetaData(), false);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        var found = false;
        for (SlotType1 slot: extrinsicObject.getValue().getSlot()) {
            if (slot.getName().equals("hash") &&
                    slot.getValueList().getValue().iterator().next().equals("2264d7f11d4c21f3fd4d8d093a842d765009ce72")) {
                found = true;
            }
        }
        Assert.assertTrue(found);
    }

    private PSDocumentMetaData buildPSDocumentMetaData() {
        var epsosDocumentMetaData = new EPSOSDocumentMetaDataImpl(
                "id",
                "patientid",
                1,
                new Date(),
                ClassCode.PS_CLASSCODE,
                "repositoryId",
                "patient summary test",
                "author",
                new EPSOSDocumentMetaDataImpl.SimpleConfidentialityMetadata(SimpleConfidentialityEnum.N),
                "en-EN",
                1000L,
                "2264d7f11d4c21f3fd4d8d093a842d765009ce72");
        return new PSDocumentMetaDataImpl(epsosDocumentMetaData);
    }
}
