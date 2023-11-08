package eu.europa.ec.sante.ehdsi.openncp.abusedetection;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbuseDetectionServiceTest {

    @Test
    void getElapsedSecondsBetweenDateTime() {
        AbuseDetectionService ab = new AbuseDetectionService();

        LocalDateTime dtNow = new LocalDateTime(DateTimeZone.forTimeZone(TimeZone.getDefault()));
        LocalDateTime dtBeforeOneHour = dtNow.minusHours(1);
        LocalDateTime dtBeforeOneDay = dtNow.minusDays(1);
        LocalDateTime dtBeforeOneMonth = dtNow.minusDays(30);

        assertEquals (ab.getElapsedSecondsBetweenDateTime(dtBeforeOneHour, dtNow), 3600);
        assertEquals(ab.getElapsedSecondsBetweenDateTime(dtBeforeOneDay, dtNow), 86400);
        assertEquals(ab.getElapsedSecondsBetweenDateTime(dtBeforeOneMonth, dtNow), 2592000);
    }
}