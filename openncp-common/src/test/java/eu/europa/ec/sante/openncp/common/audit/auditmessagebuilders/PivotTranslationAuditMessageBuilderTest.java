package eu.europa.ec.sante.openncp.common.audit.auditmessagebuilders;

import com.google.common.io.Resources;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class PivotTranslationAuditMessageBuilderTest {

    @Test
    public void testBuild() throws Exception {

        final EventLog eventLog = new EventLog();
        eventLog.setEventType(EventType.PIVOT_TRANSLATION);
        eventLog.setNcpSide(NcpSide.NCP_B);
        eventLog.setEI_TransactionName(TransactionName.PIVOT_TRANSLATION);
        eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
        eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        final XMLGregorianCalendar now = DateUtil.getDateAsXMLGregorian(new Date());
        eventLog.setEI_EventDateTime(now);
        eventLog.setSP_UserID("ncp.lu.ehealth.testa.eu");
        eventLog.setAS_AuditSourceId("LU-1");
        eventLog.setTargetip("192.168.34.64");
        eventLog.setEventTargetParticipantObjectIds(Collections.singletonList("aHR0cHM6Ly9zbXAtdGVzdC5wdWJsaXNoZXIuZWhlYWx0aC50ZXN0YS5ldS9laGVhbHRoLXBhcnRpY2lwYW50aWQtcW5zJTNBJTNBdXJuJTNBZWhlYWx0aCUzQWF0JTNBbmNwLWlkcA=="));
        eventLog.setEM_ParticipantObjectDetail("errorMessage".getBytes());
        final PivotTranslationAuditMessageBuilder pivotTranslationAuditMessageBuilder = new PivotTranslationAuditMessageBuilder();
        final AuditMessage generatedAuditMessage = pivotTranslationAuditMessageBuilder.build(eventLog);
        final URL url = Resources.getResource("pivottranslationsauditmessage.xml");
        final AuditMessage expectedAuditMessage = AuditTrailUtils.convertXMLToAuditObject(IOUtils.toInputStream(Resources.toString(url, StandardCharsets.UTF_8)));
        expectedAuditMessage.getEventIdentification().setEventDateTime(now);
        XMLUnit.setIgnoreWhitespace(true);
        assertXMLEqual(AuditTrailUtils.convertAuditObjectToXML(expectedAuditMessage), AuditTrailUtils.convertAuditObjectToXML(generatedAuditMessage));
    }
}
