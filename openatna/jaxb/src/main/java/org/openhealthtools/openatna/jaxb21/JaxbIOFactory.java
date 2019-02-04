package org.openhealthtools.openatna.jaxb21;

import org.apache.commons.lang3.StringUtils;
import org.openhealthtools.openatna.anom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * @author Andrew Harrison
 */
public class JaxbIOFactory implements AtnaIOFactory {

    private static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("org.openhealthtools.openatna.jaxb21");
        } catch (JAXBException e) {
            // Fatal error if the JAXBContext cannot be instantiated.
            throw new RuntimeException("Error creating JAXB context:", e);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(JaxbIOFactory.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    public AtnaMessage read(InputStream in) throws AtnaException {

        if (jaxbContext == null) {
            throw new AtnaException("Could not create JAXB Context");
        }

        try {
            Document doc = newDocument(in);
            if (doc.getDocumentElement().getTagName().equalsIgnoreCase("IHEYr4")) {
                return createProv(doc);
            }
            Unmarshaller u = jaxbContext.createUnmarshaller();
            AuditMessage a = (AuditMessage) u.unmarshal(doc);
            AtnaMessage am = createMessage(a);

            if (logger.isInfoEnabled()) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(a, bout);
                if (!StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {

                    loggerClinical.debug("Event Outcome: '{}'", am.getEventOutcome());
                }
            }

            return am;
        } catch (AtnaException e) {
            logger.error("Exception: '{}'", e.getMessage(), e);
            throw new AtnaException(e, AtnaException.AtnaError.INVALID_MESSAGE);
        } catch (Exception e) {
            throw new AtnaException(e, AtnaException.AtnaError.INVALID_MESSAGE);
        }
    }

    private Document newDocument(InputStream stream) throws IOException {

        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(stream);
        } catch (ParserConfigurationException e) {
            logger.error("ParserConfigurationException: '{}'", e.getMessage(), e);
            throw new IOException(e.getMessage());
        } catch (SAXException e) {
            logger.error("SAXException: '{}'", e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
        return doc;
    }

    private StreamResult transform(Document doc, OutputStream out) throws IOException {

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = null;
        try {
            t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        } catch (TransformerConfigurationException tce) {
            logger.error("TransformerConfigurationException: '{}'", tce.getMessage(), tce);
            assert (false);
        }
        DOMSource doms = new DOMSource(doc);
        StreamResult sr = new StreamResult(out);
        try {
            t.transform(doms, sr);
        } catch (TransformerException te) {
            logger.error("TransformerException: '{}'", te.getMessage(), te);
            throw new IOException(te.getMessage());
        }
        return sr;
    }

    private AtnaMessage createProv(Document doc) throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        transform(doc, bout);
        byte[] bytes = bout.toByteArray();
        if (logger.isDebugEnabled()) {
            logger.debug("\n{}", new String(bytes));
        }
        return new ProvisionalMessage(bytes);
    }

    public void write(AtnaMessage message, OutputStream out) throws AtnaException {

        write(message, out, true);
    }

    public void write(AtnaMessage message, OutputStream out, boolean includeDeclaration) throws AtnaException {

        if (jaxbContext == null) {
            throw new AtnaException("Could not create Jaxb Context");
        }
        if (message.getEventDateTime() == null) {
            message.setEventDateTime(new Date());
        }
        try {
            AuditMessage jmessage = createMessage(message);
            Marshaller marshaller = jaxbContext.createMarshaller();
            if (!includeDeclaration) {
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            }
            marshaller.marshal(jmessage, out);
            if (loggerClinical.isDebugEnabled() && !StringUtils.equals(System.getProperty("server.ehealth.mode"), "PROD")) {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                marshaller.marshal(jmessage, bout);
                loggerClinical.debug("Written Audit Message:\n{}", new String(bout.toByteArray()));
            }

        } catch (JAXBException e) {
            throw new AtnaException(e);
        }
    }

    private AtnaMessage createMessage(AuditMessage msg) throws AtnaException {

        EventIdentificationType evt = msg.getEventIdentification();
        if (evt == null) {
            throw new AtnaException("Message has no event");
        }
        if (evt.getEventOutcomeIndicator() == null) {
            throw new AtnaException("Message has no event outcome");
        }
        if (evt.getEventID() == null) {
            throw new AtnaException("Message has no event id");
        }
        AtnaMessage message = new AtnaMessage(createCode(AtnaCode.EVENT_ID, evt.getEventID()),
                EventOutcome.getOutcome(evt.getEventOutcome()));
        message.setEventActionCode(EventAction.getAction(evt.getEventActionCode()));
        message.setEventDateTime(evt.getEventTime());
        List<CodedValueType> eventTypes = msg.getEventIdentification().getEventTypeCode();
        for (CodedValueType type : eventTypes) {
            message.addEventTypeCode(createCode(AtnaCode.EVENT_TYPE, type));
        }
        List<ActiveParticipantType> ps = msg.getActiveParticipant();
        for (ActiveParticipantType p : ps) {
            message.addParticipant(createParticipant(p));
        }
        List<AuditSourceIdentificationType> ss = msg.getAuditSourceIdentification();
        for (AuditSourceIdentificationType s : ss) {
            message.addSource(createSource(s));
        }
        List<ParticipantObjectIdentificationType> as = msg.getParticipantObjectIdentification();
        for (ParticipantObjectIdentificationType a : as) {
            message.addObject(createObject(a));
        }
        return message;
    }

    private AuditMessage createMessage(AtnaMessage msg) throws AtnaException {

        if (msg.getEventOutcome() == null) {
            throw new AtnaException("message has no event outcome");
        }
        if (msg.getEventCode() == null) {
            throw new AtnaException("message has no event code");
        }
        AuditMessage ret = new AuditMessage();
        EventIdentificationType evt = new EventIdentificationType();
        if (msg.getEventActionCode() != null) {
            evt.setEventActionCode(msg.getEventActionCode().value());
        }
        if (msg.getEventDateTime() != null) {
            evt.setEventTime(msg.getEventDateTime());
        }
        evt.setEventID(createCode(msg.getEventCode()));
        evt.setEventOutcome(msg.getEventOutcome().value());
        List<AtnaCode> eventTypes = msg.getEventTypeCodes();

        for (AtnaCode eventType : eventTypes) {
            evt.getEventTypeCode().add(createCode(eventType));
        }
        ret.setEventIdentification(evt);

        List<AtnaMessageObject> objs = msg.getObjects();
        for (AtnaMessageObject obj : objs) {
            ret.getParticipantObjectIdentification().add(createObject(obj));
        }
        List<AtnaSource> sources = msg.getSources();
        for (AtnaSource source : sources) {
            ret.getAuditSourceIdentification().add(createSource(source));
        }
        List<AtnaMessageParticipant> parts = msg.getParticipants();
        for (AtnaMessageParticipant part : parts) {
            ret.getActiveParticipant().add(createParticipant(part));
        }
        return ret;
    }

    private AtnaMessageObject createObject(ParticipantObjectIdentificationType obj) throws AtnaException {

        logger.debug("Display NAME: '{}'", obj.getParticipantObjectIDTypeCode().displayName);
        if (obj.getParticipantObjectIDTypeCode() == null) {
            throw new AtnaException("object has no Id type code");
        }
        AtnaObject ao = new AtnaObject(obj.getParticipantObjectID(), createCode(AtnaCode.OBJECT_ID_TYPE, obj.getParticipantObjectIDTypeCode()));
        ao.setObjectName(obj.getParticipantObjectName());
        ao.setObjectSensitivity(obj.getParticipantObjectSensitivity());
        if (obj.getParticipantObjectTypeCode() != null) {
            ao.setObjectTypeCode(ObjectType.getType(obj.getParticipantObjectTypeCode()));
        }
        if (obj.getParticipantObjectTypeCodeRole() != null) {
            ao.setObjectTypeCodeRole(ObjectTypeCodeRole.getRole(obj.getParticipantObjectTypeCodeRole()));
        }
        List<ParticipantObjectDescriptionType> descs = obj.getParticipantObjectDescription();
        for (ParticipantObjectDescriptionType desc : descs) {
            ObjectDescription od = new ObjectDescription();
            List<ParticipantObjectDescriptionType.MPPS> mpps = desc.getMPPS();
            for (ParticipantObjectDescriptionType.MPPS mpp : mpps) {
                od.addMppsUid(mpp.getUID());
            }
            List<ParticipantObjectDescriptionType.Accession> accs = desc.getAccession();
            for (ParticipantObjectDescriptionType.Accession acc : accs) {
                od.addAccessionNumber(acc.getNumber());
            }
            List<ParticipantObjectDescriptionType.SOPClass> sops = desc.getSOPClass();
            for (ParticipantObjectDescriptionType.SOPClass sop : sops) {
                SopClass sc = new SopClass();
                sc.setUid(sop.getUID());
                sc.setNumberOfInstances(sop.getNumberOfInstances().intValue());
                List<ParticipantObjectDescriptionType.SOPClass.Instance> insts = sop.getInstance();
                for (ParticipantObjectDescriptionType.SOPClass.Instance inst : insts) {
                    sc.addInstanceUid(inst.getUID());
                }
                od.addSopClass(sc);
            }
            ao.addObjectDescription(od);
        }
        AtnaMessageObject ret = new AtnaMessageObject(ao);
        List<TypeValuePairType> pairs = obj.getParticipantObjectDetail();
        for (TypeValuePairType pair : pairs) {
            AtnaObjectDetail detail = new AtnaObjectDetail();
            detail.setType(pair.getType());
            detail.setValue(pair.getValue());
            ret.addObjectDetail(detail);
        }
        ret.setObjectQuery(obj.getParticipantObjectQuery());
        if (obj.getParticipantObjectDataLifeCycle() != null) {
            ret.setObjectDataLifeCycle(ObjectDataLifecycle.getLifecycle(obj.getParticipantObjectDataLifeCycle()));
        }

        return ret;
    }

    private ParticipantObjectIdentificationType createObject(AtnaMessageObject obj) throws AtnaException {

        if (obj.getObject() == null) {
            throw new AtnaException("object has no object");
        }
        if (obj.getObject().getObjectId() == null) {
            throw new AtnaException("object has no Id");
        }
        if (obj.getObject().getObjectIdTypeCode() == null) {
            throw new AtnaException("object has no Id type code");
        }
        ParticipantObjectIdentificationType ret = new ParticipantObjectIdentificationType();
        ret.setParticipantObjectID(obj.getObject().getObjectId());
        ret.setParticipantObjectIDTypeCode(createCode(obj.getObject().getObjectIdTypeCode()));
        ret.setParticipantObjectName(obj.getObject().getObjectName());
        ret.setParticipantObjectSensitivity(obj.getObject().getObjectSensitivity());
        if (obj.getObject().getObjectTypeCode() != null) {
            ret.setParticipantObjectTypeCode((short) obj.getObject().getObjectTypeCode().value());
        }
        if (obj.getObject().getObjectTypeCodeRole() != null) {
            ret.setParticipantObjectTypeCodeRole((short) obj.getObject().getObjectTypeCodeRole().value());
        }
        if (obj.getObjectDataLifeCycle() != null) {
            ret.setParticipantObjectDataLifeCycle((short) obj.getObjectDataLifeCycle().value());
        }
        ret.setParticipantObjectQuery(obj.getObjectQuery());
        List<AtnaObjectDetail> details = obj.getObjectDetails();
        for (AtnaObjectDetail detail : details) {
            TypeValuePairType pair = new TypeValuePairType();
            pair.setType(detail.getType());
            pair.setValue(detail.getValue());
            ret.getParticipantObjectDetail().add(pair);
        }
        List<ObjectDescription> descs = obj.getObject().getDescriptions();
        for (ObjectDescription desc : descs) {
            ParticipantObjectDescriptionType dt = new ParticipantObjectDescriptionType();
            List<String> uids = desc.getMppsUids();
            for (String uid : uids) {
                ParticipantObjectDescriptionType.MPPS mpps = new ParticipantObjectDescriptionType.MPPS();
                mpps.setUID(uid);
                dt.getMPPS().add(mpps);
            }
            List<String> nums = desc.getAccessionNumbers();
            for (String num : nums) {
                ParticipantObjectDescriptionType.Accession acc = new ParticipantObjectDescriptionType.Accession();
                acc.setNumber(num);
                dt.getAccession().add(acc);
            }
            List<SopClass> sops = desc.getSopClasses();
            for (SopClass sop : sops) {
                ParticipantObjectDescriptionType.SOPClass sc = new ParticipantObjectDescriptionType.SOPClass();
                sc.setNumberOfInstances(BigInteger.valueOf(sop.getNumberOfInstances()));
                sc.setUID(sop.getUid());
                List<String> insts = sop.getInstanceUids();
                for (String inst : insts) {
                    ParticipantObjectDescriptionType.SOPClass.Instance i =
                            new ParticipantObjectDescriptionType.SOPClass.Instance();
                    i.setUID(inst);
                    sc.getInstance().add(i);
                }
                dt.getSOPClass().add(sc);
            }
            ret.getParticipantObjectDescription().add(dt);
        }
        return ret;
    }

    private AuditSourceIdentificationType createSource(AtnaSource source) throws AtnaException {

        if (source.getSourceId() == null) {
            throw new AtnaException("Source has no ID");
        }
        AuditSourceIdentificationType ret = new AuditSourceIdentificationType();
        ret.setAuditSourceID(source.getSourceId());
        ret.setAuditEnterpriseSiteID(source.getEnterpriseSiteId());
        List<AtnaCode> codes = source.getSourceTypeCodes();
        for (AtnaCode code : codes) {
            ret.getAuditSourceTypeCode().add(createCode(code));
        }
        return ret;
    }

    private AtnaSource createSource(AuditSourceIdentificationType source) throws AtnaException {

        if (source.getAuditSourceID() == null) {
            throw new AtnaException("source has no Id");
        }
        AtnaSource ret = new AtnaSource(source.getAuditSourceID());
        ret.setEnterpriseSiteId(source.getAuditEnterpriseSiteID());
        List<CodedValueType> code = source.getAuditSourceTypeCode();
        for (CodedValueType type : code) {
            ret.addSourceTypeCode(createCode(AtnaCode.SOURCE_TYPE, type));
        }
        return ret;
    }

    private ActiveParticipantType createParticipant(AtnaMessageParticipant participant) throws AtnaException {

        if (participant.getParticipant() == null) {
            throw new AtnaException("participant has no participant");
        }
        if (participant.getParticipant().getUserId() == null) {
            throw new AtnaException("participant has no Id");
        }
        ActiveParticipantType ret = new ActiveParticipantType();
        ret.setUserID(participant.getParticipant().getUserId());
        ret.setUserName(participant.getParticipant().getUserName());
        ret.setAlternativeUserID(participant.getParticipant().getAlternativeUserId());
        List<AtnaCode> codes = participant.getParticipant().getRoleIDCodes();
        for (AtnaCode code : codes) {
            ret.getRoleIDCode().add(createCode(code));
        }
        ret.setNetworkAccessPointID(participant.getNetworkAccessPointId());
        if (participant.getNetworkAccessPointType() != null) {
            ret.setNetworkAccessPointTypeCode((short) participant.getNetworkAccessPointType().value());
        }
        // [Mustafa: May 8, 2012]: The following line was missing, and causing default value "true" for UserIsRequestor.
        ret.setUserIsRequestor(participant.isUserIsRequestor());

        return ret;

    }

    private AtnaMessageParticipant createParticipant(ActiveParticipantType participant) throws AtnaException {

        if (participant.getUserID() == null) {
            throw new AtnaException("Participant has no ID");
        }
        AtnaParticipant ap = new AtnaParticipant(participant.getUserID());
        ap.setUserName(participant.getUserName());
        ap.setAlternativeUserId(participant.getAlternativeUserID());
        List<CodedValueType> codes = participant.getRoleIDCode();
        for (CodedValueType code : codes) {
            ap.addRoleIDCode(createCode(AtnaCode.PARTICIPANT_ROLE_TYPE, code));
        }
        AtnaMessageParticipant ret = new AtnaMessageParticipant(ap);
        ret.setNetworkAccessPointId(participant.getNetworkAccessPointID());
        if (participant.getNetworkAccessPointTypeCode() != null) {
            ret.setNetworkAccessPointType(NetworkAccessPoint.getAccessPoint(participant.getNetworkAccessPointTypeCode()));
        }
        ret.setUserIsRequestor(participant.isUserIsRequestor());
        return ret;
    }

    private AtnaCode createCode(String type, CodedValueType code) throws AtnaException {

        if (code.getCode() == null) {
            throw new AtnaException("Code has no code");
        }
        return new AtnaCode(type, code.getCode(), code.getCodeSystem(), code.getCodeSystemName(),
                code.getDisplayName(), code.getOriginalText());
    }

    private CodedValueType createCode(AtnaCode code) throws AtnaException {

        if (code.getCode() == null) {
            throw new AtnaException("Code has no code");
        }
        CodedValueType type = new CodedValueType();
        type.setCode(code.getCode());
        type.setCodeSystem(code.getCodeSystem());
        type.setCodeSystemName(code.getCodeSystemName());
        type.setDisplayName(code.getDisplayName());
        type.setOriginalText(code.getOriginalText());
        return type;
    }
}
