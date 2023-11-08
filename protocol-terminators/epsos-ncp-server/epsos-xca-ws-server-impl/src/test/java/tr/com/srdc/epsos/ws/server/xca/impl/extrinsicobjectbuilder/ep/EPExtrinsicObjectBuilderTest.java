package tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.ep;

import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.constant.codesystem.CodeSystem;
import fi.kela.se.epsos.data.model.EPDocumentMetaData;
import fi.kela.se.epsos.data.model.EPDocumentMetaDataImpl;
import fi.kela.se.epsos.data.model.EPSOSDocumentMetaDataImpl;
import fi.kela.se.epsos.data.model.EpListParam;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import tr.com.srdc.epsos.data.model.SimpleConfidentialityEnum;
import tr.com.srdc.epsos.data.model.SubstitutionCodeEnum;
import tr.com.srdc.epsos.ws.server.xca.impl.extrinsicobjectbuilder.AbstractExtrinsicObjectBuilderTest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.Date;

@Ignore
public class EPExtrinsicObjectBuilderTest extends AbstractExtrinsicObjectBuilderTest {


    @Test
    public void testATCNormalFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var atcCode = "A10AE04";
        var atcName = "insulin glargine";
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode(atcCode)
                .setAtcName(atcName)
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setStrength("100 U/ml")
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getNodeRepresentation().equals(atcCode) &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals(CodeSystem.ATC.getOID()) &&
                    classificationType.getName().getLocalizedString().iterator().next().getValue().equals(atcName)) {
                found = true;
                var je =  OBJECT_FACTORY_RIM.createClassification(classificationType);
                System.out.println(toXml(je));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testATCNullFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var ePListParam = EPListParamBuilder.newInstance()
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setStrength("100 U/ml")
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals(CodeSystem.ATC.getOID())) {
                found = true;
            }
        }
        Assert.assertFalse(found);
    }

    @Test
    public void testDoseFormNormalFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var doseFormCode = "10219000";
        var doseFormName = "Tablet";
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setDoseFormCode(doseFormCode)
                .setDoseFormName(doseFormName)
                .setStrength("100 U/ml")
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getNodeRepresentation().equals(doseFormCode) &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals(CodeSystem.EDQM.getOID()) &&
                    classificationType.getName().getLocalizedString().iterator().next().getValue().equals(doseFormName)) {
                found = true;
                var je =  OBJECT_FACTORY_RIM.createClassification(classificationType);
                System.out.println(toXml(je));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testDoseFormNullFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setStrength("100 U/ml")
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals(CodeSystem.EDQM.getOID())) {
                found = true;
            }
        }
        Assert.assertFalse(found);
    }

    @Test
    public void testStrengthNormalFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var strength = "100 U/ml";
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setStrength(strength)
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getNodeRepresentation().equals(strength) &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals("eHDSI_Strength_CodeSystem")) {
                found = true;
                JAXBElement<ClassificationType> je =  OBJECT_FACTORY_RIM.createClassification(classificationType);
                System.out.println(toXml(je));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testStrengthNullFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setSubtitutionCode(SubstitutionCodeEnum.G)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals("eHDSI_Strength_CodeSystem")) {
                found = true;
            }
        }
        Assert.assertFalse(found);
    }

    @Test
    public void testSubstitutionCodeNormalFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var substitutionCode = SubstitutionCodeEnum.G;
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setStrength("100 U/ml")
                .setSubtitutionCode(substitutionCode)
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getNodeRepresentation().equals(substitutionCode.name()) &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals("2.16.840.1.113883.5.1070") &&
                    classificationType.getName().getLocalizedString().iterator().next().getValue().equals(substitutionCode.getDisplayName())) {
                found = true;
                JAXBElement<ClassificationType> je =  OBJECT_FACTORY_RIM.createClassification(classificationType);
                System.out.println(toXml(je));
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testSubstitutionCodeNullFlow() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var ePListParam = EPListParamBuilder.newInstance()
                .setAtcCode("A10AE04")
                .setAtcName("insulin glargine")
                .setDoseFormCode("10219000")
                .setDoseFormName("Tablet")
                .setStrength("100 U/ml")
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        boolean found = false;
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            if (classificationType.getClassificationScheme().equals("urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4") &&
                    classificationType.getNodeRepresentation().equals(SubstitutionCodeEnum.G.name()) &&
                    classificationType.getSlot().iterator().next().getValueList().getValue().iterator().next().equals("2.16.840.1.113883.5.1070") &&
                    classificationType.getName().getLocalizedString().iterator().next().getValue().equals(SubstitutionCodeEnum.G.getDisplayName())) {
                found = true;
            }
        }
        Assert.assertFalse(found);
    }

    @Test
    public void testClassifications() {
        var adHocQueryRequest = buildAdhocQueryRequest();
        var eotXML = OBJECT_FACTORY_RIM.createExtrinsicObjectType();
        var ePListParam = EPListParamBuilder.newInstance()
                .build();
        var epDocumentMetaData = buildEPDocumentMetaData(ePListParam);

        EPExtrinsicObjectBuilder.build(adHocQueryRequest, eotXML, epDocumentMetaData);
        var extrinsicObject = OBJECT_FACTORY_RIM.createExtrinsicObject(eotXML);
        for (ClassificationType classificationType: extrinsicObject.getValue().getClassification()) {
            // Ensure we are talking about coded attributes, not author attributes
            if (StringUtils.isNotEmpty(classificationType.getNodeRepresentation())) {
                // classification shall not contain other Slot than the one defined by name='codingScheme' (IHE_ITI_TF V3, 4.2.3.1.2)
                Assert.assertEquals(1, classificationType.getSlot().size());
                // Classification/slot/@name is required and fixed to 'codingScheme' (IHE_ITI_TF V3, 4.2.3.1.2)
                Assert.assertEquals("codingScheme", classificationType.getSlot().iterator().next().getName());
            }
        }
    }

    private EPDocumentMetaData buildEPDocumentMetaData(EpListParam ePListParam) {
        var epsosDocumentMetaData = new EPSOSDocumentMetaDataImpl(
                "id",
                "patientid",
                1,
                new Date(),
                ClassCode.EP_CLASSCODE,
                "repositoryId",
                "ePrescription test",
                "author",
                new EPSOSDocumentMetaDataImpl.SimpleConfidentialityMetadata(SimpleConfidentialityEnum.N),
                "en-EN",
                1000L,
                "2264d7f11d4c21f3fd4d8d093a842d765009ce72");
        return new EPDocumentMetaDataImpl(epsosDocumentMetaData, "description", ePListParam);
    }

    private String toXml(JAXBElement element) {
        try {
            JAXBContext jc = JAXBContext.newInstance(element.getValue().getClass());
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(element, baos);
            return baos.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static class EPListParamBuilder {

        private String atcCode;
        private String atcName;
        private String doseFormCode;
        private String doseFormName;
        private String strength;
        private SubstitutionCodeEnum substitutionCode;

        public static EPListParamBuilder newInstance()
        {
            return new EPListParamBuilder();
        }

        public EPListParamBuilder setAtcCode(String atcCode) {
            this.atcCode=atcCode;
            return this;
        }
        public EPListParamBuilder setAtcName(String atcName) {
            this.atcName=atcName;
            return this;
        }
        public EPListParamBuilder setDoseFormCode(String doseFormCode) {
            this.doseFormCode=doseFormCode;
            return this;
        }
        public EPListParamBuilder setDoseFormName(String doseFormName) {
            this.doseFormName=doseFormName;
            return this;
        }
        public EPListParamBuilder setStrength(String strength) {
            this.strength=strength;
            return this;
        }
        public EPListParamBuilder setSubtitutionCode(SubstitutionCodeEnum substitutionCode) {
            this.substitutionCode=substitutionCode;
            return this;
        }

        public EpListParam build() {
            return new EpListParam(
                    true,
                    atcCode,
                    atcName,
                    doseFormCode,
                    doseFormName,
                    strength,
                    substitutionCode == null ? null : new EPDocumentMetaDataImpl.SimpleSubstitutionMetadata(substitutionCode));
        }
    }
}
