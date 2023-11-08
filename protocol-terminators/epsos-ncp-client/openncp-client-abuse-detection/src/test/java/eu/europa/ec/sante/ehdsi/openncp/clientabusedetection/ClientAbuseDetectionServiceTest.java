package eu.europa.ec.sante.ehdsi.openncp.clientabusedetection;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientAbuseDetectionServiceTest {

    @Test
    void getElapsedSecondsBetweenDateTime() {
        ClientAbuseDetectionService cab = new ClientAbuseDetectionService();

        LocalDateTime dtNow = new LocalDateTime(DateTimeZone.forTimeZone(TimeZone.getDefault()));
        LocalDateTime dtBeforeOneHour = dtNow.minusHours(1);
        LocalDateTime dtBeforeOneDay = dtNow.minusDays(1);
        LocalDateTime dtBeforeOneMonth = dtNow.minusDays(30);

        assertEquals(cab.getElapsedSecondsBetweenDateTime(dtBeforeOneHour, dtNow), 3600);
        assertEquals(cab.getElapsedSecondsBetweenDateTime(dtBeforeOneDay, dtNow), 86400);
        assertEquals(cab.getElapsedSecondsBetweenDateTime(dtBeforeOneMonth, dtNow), 2592000);
    }
}