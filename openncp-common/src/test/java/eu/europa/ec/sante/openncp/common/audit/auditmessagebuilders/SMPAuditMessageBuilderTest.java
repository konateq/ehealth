package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import com.google.common.io.Resources;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

public class SMPAuditMessageBuilderTest extends XMLTestCase {

    @Test
    public void testBuild() throws Exception {

        final EventLog eventLog = new EventLog();
        eventLog.setEventType(EventType.SMP_QUERY);
        eventLog.setNcpSide(NcpSide.NCP_A);
        eventLog.setEI_TransactionName(TransactionName.SMP_QUERY);
        eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        final XMLGregorianCalendar now = DateUtil.getDateAsXMLGregorian(new Date());
        eventLog.setEI_EventDateTime(now);
        eventLog.setSC_UserID("ncp-ppt.pt.ehealth.testa.eu");
        eventLog.setSP_UserID("smp-test.publisher.ehealth.testa.eu");
        eventLog.setAS_AuditSourceId("PT-1");
        eventLog.setSourceip("10.200.137.161");
        eventLog.setTargetip("smp-cert-auth-test.publisher.ehealth.testa.eu");
        eventLog.setEventTargetParticipantObjectIds(Collections.singletonList("aHR0cHM6Ly9zbXAtdGVzdC5wdWJsaXNoZXIuZWhlYWx0aC50ZXN0YS5ldS9laGVhbHRoLXBhcnRpY2lwYW50aWQtcW5zJTNBJTNBdXJuJTNBZWhlYWx0aCUzQWF0JTNBbmNwLWlkcA=="));
        eventLog.setEM_ParticipantObjectDetail("errorMessage".getBytes());
        final SMPAuditMessageBuilder smpAuditMessageBuilder = new SMPAuditMessageBuilder();
        final AuditMessage generatedAuditMessage = smpAuditMessageBuilder.build(eventLog);
        final URL url = Resources.getResource("smpqueryauditmessage.xml");
        final AuditMessage expectedAuditMessage = AuditTrailUtils.convertXMLToAuditObject(IOUtils.toInputStream(Resources.toString(url, StandardCharsets.UTF_8)));
        expectedAuditMessage.getEventIdentification().setEventDateTime(now);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(AuditTrailUtils.convertAuditObjectToXML(expectedAuditMessage), AuditTrailUtils.convertAuditObjectToXML(generatedAuditMessage));
    }

    @Test
    public void testBuildIncludingError() throws Exception {

        final EventLog eventLog = new EventLog();
        eventLog.setEventType(EventType.SMP_QUERY);
        eventLog.setNcpSide(NcpSide.NCP_A);
        eventLog.setEI_TransactionName(TransactionName.SMP_QUERY);
        eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        final XMLGregorianCalendar now = DateUtil.getDateAsXMLGregorian(new Date());
        eventLog.setEI_EventDateTime(now);
        eventLog.setSC_UserID("ncp-ppt.pt.ehealth.testa.eu");
        eventLog.setSP_UserID("smp-test.publisher.ehealth.testa.eu");
        eventLog.setAS_AuditSourceId("PT-1");
        eventLog.setSourceip("10.200.137.161");
        eventLog.setTargetip("smp-cert-auth-test.publisher.ehealth.testa.eu");
        eventLog.setEventTargetParticipantObjectIds(Collections.singletonList("aHR0cHM6Ly9zbXAtdGVzdC5wdWJsaXNoZXIuZWhlYWx0aC50ZXN0YS5ldS9laGVhbHRoLXBhcnRpY2lwYW50aWQtcW5zJTNBJTNBdXJuJTNBZWhlYWx0aCUzQWF0JTNBbmNwLWlkcA=="));
        eventLog.setEM_ParticipantObjectID("aHR0cHM6Ly9zbXAtY2VydC1hdXRoLXRlc3QucHVibGlzaGVyLmVoZWFsdGgudGVzdGEuZXUvZWhlYWx0aC1wYXJ0aWNpcGFudGlkLXFuczo6dXJuOmVoZWFsdGg6cHQ6bmNwLWlkcC9zZXJ2aWNlcy9laGVhbHRoLXJlc2lkLXFuczo6dXJuOmVoZWFsdGg6cGF0aWVudGlkZW50aWZpY2F0aW9uYW5kYXV0aGVudGljYXRpb246OnhjcGQ6OmNyb3NzZ2F0ZXdheXBhdGllbnRkaXNjb3ZlcnklMjMlMjNpdGktNTU=");
        eventLog.setEM_ParticipantObjectDetail("errorMessage".getBytes());
        final SMPAuditMessageBuilder smpAuditMessageBuilder = new SMPAuditMessageBuilder();
        final AuditMessage generatedAuditMessage = smpAuditMessageBuilder.build(eventLog);
        final URL url = Resources.getResource("smpqueryauditmessagewitherror.xml");
        final AuditMessage expectedAuditMessage = AuditTrailUtils.convertXMLToAuditObject(IOUtils.toInputStream(Resources.toString(url, StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        expectedAuditMessage.getEventIdentification().setEventDateTime(now);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(AuditTrailUtils.convertAuditObjectToXML(expectedAuditMessage), AuditTrailUtils.convertAuditObjectToXML(generatedAuditMessage));
    }
}
