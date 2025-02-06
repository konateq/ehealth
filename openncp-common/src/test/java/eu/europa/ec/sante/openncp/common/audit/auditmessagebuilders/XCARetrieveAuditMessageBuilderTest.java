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
import java.util.List;

public class XCARetrieveAuditMessageBuilderTest extends XMLTestCase {


    @Test
    public void testBuild() throws Exception {
        {
            final EventLog eventLog = new EventLog();
            eventLog.setEventType(EventType.PATIENT_SERVICE_RETRIEVE);
            eventLog.setNcpSide(NcpSide.NCP_A);
            eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_RETRIEVE);
            eventLog.setEI_EventActionCode(EventActionCode.VIEW);
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
            final XMLGregorianCalendar now = DateUtil.getDateAsXMLGregorian(new Date());
            eventLog.setEI_EventDateTime(now);
            eventLog.setSC_UserID("Service Consumer");
            eventLog.setSP_UserID("Service Provider");
            eventLog.setEventTargetParticipantObjectIds(Collections.singletonList("aHR0cHM6Ly9zbXAtdGVzdC5wdWJsaXNoZXIuZWhlYWx0aC50ZXN0YS5ldS9laGVhbHRoLXBhcnRpY2lwYW50aWQtcW5zJTNBJTNBdXJuJTNBZWhlYWx0aCUzQWF0JTNBbmNwLWlkcA=="));
            eventLog.setEM_ParticipantObjectDetail("errorMessage".getBytes());
            eventLog.setAS_AuditSourceId("42");
            eventLog.setEI_TransactionNumber("42");
            eventLog.setEventTargetAdditionalObjectId("42");
            eventLog.setHR_AlternativeUserID("HR Alternative User ID");
            eventLog.setHR_RoleID("HR Role ID");
            eventLog.setHR_UserID("HR User ID");
            eventLog.setHR_UserName("janedoe");
            eventLog.setHciIdentifier("42");
            eventLog.setMS_UserID("MS User ID");
            eventLog.setPC_RoleID("PC Role ID");
            eventLog.setPC_UserID("PC User ID");
            eventLog.setPS_ParticipantObjectIDs(List.of("PS Participant Object ID"));
            eventLog.setPT_ParticipantObjectIDs(List.of("2-1234-W7^^^&1.3.6.1.4.1.48336.1000&ISO"));
            eventLog.setQueryByParameter("Query By Parameter");
            eventLog.setReqM_ParticipantObjectDetail("AXAXAXAX".getBytes("UTF-8"));
            eventLog.setReqM_ParticipantObjectID("urn:uuid:2d1a748a-b73e-47ef-96d2-50fdb92e1c0a");

            eventLog.setSourceip("127.0.0.1");
            eventLog.setTargetip("127.0.0.1");

            final XCARetrieveAuditMessageBuilder patientRetrieveAuditMessageBuilder = new XCARetrieveAuditMessageBuilder();
            final AuditMessage generatedAuditMessage = patientRetrieveAuditMessageBuilder.build(eventLog);


            final URL url = Resources.getResource("patientserviceretrievetauditmessage.xml");
            final AuditMessage expectedAuditMessage = AuditTrailUtils.convertXMLToAuditObject(IOUtils.toInputStream(Resources.toString(url, StandardCharsets.UTF_8)));
            expectedAuditMessage.getEventIdentification().setEventDateTime(now);
            XMLUnit.setIgnoreWhitespace(true);
            assertXMLEqual(AuditTrailUtils.convertAuditObjectToXML(expectedAuditMessage), AuditTrailUtils.convertAuditObjectToXML(generatedAuditMessage));
        }

    }
}
