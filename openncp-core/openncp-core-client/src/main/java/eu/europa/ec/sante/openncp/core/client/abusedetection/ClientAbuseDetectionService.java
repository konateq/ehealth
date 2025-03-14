package eu.europa.ec.sante.openncp.core.client.abusedetection;

import com.ibatis.common.jdbc.ScriptRunner;
import eu.europa.ec.sante.openncp.common.audit.AuditTrailUtils;
import eu.europa.ec.sante.openncp.common.audit.EventType;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.core.common.ihe.abusedetection.*;
import net.RFC3881.dicom.ActiveParticipantContents;
import net.RFC3881.dicom.AuditMessage;
import net.RFC3881.dicom.ParticipantObjectIdentificationContents;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClientAbuseDetectionService implements Job {
    public static final String DESCRIPTION_ALL = "[NCP-B] Detected %d transactions within an interval of %d seconds. " +
            "This is exceeding the indicated threshold of %d transactions for the defind time interval";
    public static final String DESCRIPTION_POC = "[NCP-B] Detected %d transactions within an interval of %d seconds " +
            "from a specific Point of care. This is exceeding the indicated threshold of %d transactions for the defined interval";
    public static final String DESCRIPTION_PAT = "[NCP-B] Detected %d transactions within an interval of %d seconds " +
            "for a specific Patient. This is exceeding the indicated threshold of %d transactions for the defined interval";
    private static final int NUM_DAYS_LOOK_BACK = 3;
    private static final int ANOMALY_DESCRIPTION_SIZE = 2000;
    private static final int ANOMALY_TYPE_SIZE = 20;
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String JDBC_OPEN_ATNA = "jdbc/OPEN_ATNA";
    private static final String JDBC_EHNCP_PROPERTY = "jdbc/ConfMgr";
    private static List<AbuseEvent> abuseList = new ArrayList<>();
    private static long lastIdAnalyzed = -1;
    private final Logger logger = LoggerFactory.getLogger(ClientAbuseDetectionService.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static String databaseProduct = "";

    public ClientAbuseDetectionService() {
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        logger.info("AbuseDetectionService Job is running......");
        var scheduler = jobExecutionContext.getScheduler();

        try {
            logger.info("AbuseDetectionService Job paused");
            scheduler.pauseJob(jobExecutionContext.getJobDetail().getKey());

            if(databaseProduct.isEmpty()) {
                retrieveDbProperties();
            }

            try {
                String query;
                // If no lastId is available it starts to analyze records from n days back
                if (lastIdAnalyzed < 0) {
                    long lastFileTimeAnalyzed = LocalDateTime.now().minusDays(NUM_DAYS_LOOK_BACK).toDate().toInstant().toEpochMilli();
                    LocalDateTime localDateTime = new LocalDateTime(lastFileTimeAnalyzed, DateTimeZone.forTimeZone(TimeZone.getDefault()));
                    DateTimeFormatter dtf = DateTimeFormat.forPattern(ClientAbuseDetectionService.PATTERN);
                    String lastDateTimeFileAnalyzed = localDateTime.toString(dtf);

                    if("oracle".equals(databaseProduct)) {
                        query = "select messages.id, eventActionCode, eventDateTime, eventOutcome, messageContent, sourceAddress, " +
                                "eventId_id, code from messages inner join codes on (messages.eventId_id = codes.id) " +
                                "where codes.code IN ('ITI-55', 'ITI-38', 'ITI-39', 'ITI-41', '110152', '110153', '110106', '110107', '110112') " +
                                "and eventDateTime > TO_DATE('" + lastDateTimeFileAnalyzed + "', 'YYYY-MM-DD HH24:MI:SS') " +
                                "order by eventDateTime ASC";
                    } else {
                        query = "select messages.id, eventActionCode, eventDateTime, eventOutcome, messageContent, sourceAddress, " +
                                "eventId_id, code from messages inner join codes on (messages.eventId_id = codes.id) " +
                                "where codes.code IN ('ITI-55', 'ITI-38', 'ITI-39', 'ITI-41', '110152', '110153', '110106', '110107', '110112') " +
                                "and eventDateTime > '" + lastDateTimeFileAnalyzed + "' order by " +
                                "eventDateTime ASC;";
                    }
                } else {
                    // fetch only new records to be analyzed
                    if ("oracle".equals(databaseProduct)) {
                        query = "select messages.id, eventActionCode, eventDateTime, eventOutcome, messageContent, sourceAddress, " +
                                "eventId_id, code from messages inner join codes on (messages.eventId_id = codes.id) " +
                                "where codes.code IN ('ITI-55', 'ITI-38', 'ITI-39', 'ITI-41', '110152', '110153', '110106', '110107', '110112') " +
                                "and messages.id > " + lastIdAnalyzed + " order by id ASC";
                    } else {
                        query = "select messages.id, eventActionCode, eventDateTime, eventOutcome, messageContent, sourceAddress, " +
                                "eventId_id, code from messages inner join codes on (messages.eventId_id = codes.id) " +
                                "where codes.code IN ('ITI-55', 'ITI-38', 'ITI-39', 'ITI-41', '110152', '110153', '110106', '110107', '110112') " +
                                "and messages.id > " + lastIdAnalyzed + " order by id ASC;";
                    }
                }

                List<MessagesRecord> records = retrieveAuditEvents(query);
                if (!records.isEmpty()) {
                    records.forEach(p -> {
                        try {
                            // Read the audit message and if valid stores the corresponding id
                            AuditMessage au = readAuditString(p);
                            if (au != null) {
                                lastIdAnalyzed = p.getId();
                            }
                        } catch (JAXBException e) {
                            throw new AbuseDetectionException(e);
                        }
                    });
                    // Check for anomalies in the actual set of Audits and purge from the set those outdated
                    abuseList = checkAnomalies(abuseList);
                    logger.info("AbuseDetectionService: end of checking data");
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (SchedulerException e) {
            throw new AbuseDetectionException(e);
        } finally {
            try {
                scheduler.resumeJob(jobExecutionContext.getJobDetail().getKey());
                logger.info("AbuseDetectionService Job resumed");
            } catch (SchedulerException e) {
                logger.debug(e.getMessage());
            }
        }
    }

    private Connection dbConnect(String dsName) throws NamingException, SQLException {
        Context initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource) envContext.lookup(dsName);
        return ds.getConnection();
    }

    private void retrieveDbProperties()
    {
        try (Connection sqlConnection = dbConnect(ClientAbuseDetectionService.JDBC_EHNCP_PROPERTY);
             Statement stmt = sqlConnection.createStatement()) {

            databaseProduct = sqlConnection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (Exception exception) {
            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                loggerClinical.error("Cannot detect database type");
            }

        }
    }

    private List<MessagesRecord> retrieveAuditEvents(String sqlSelect) throws Exception {

        List<MessagesRecord> listXmlRecords = new ArrayList<>();

        try (Connection sqlConnection = dbConnect(ClientAbuseDetectionService.JDBC_OPEN_ATNA);
             Statement stmt = sqlConnection.createStatement()) {

            ResultSet resultSet = stmt.executeQuery(sqlSelect);
            while (resultSet.next()) {
                MessagesRecord record = new MessagesRecord();
                record.setId(resultSet.getLong("id"));
                if("postgresql".equals(databaseProduct)) {
                    var messageContent = resultSet.getBytes("messageContent");
                    record.setXml(new String(messageContent));
                } else {
                    // FIX Invalid column type: getString/getNString not implemented for class oracle.jdbc.driver.T4CBlobAccessor
                    Blob messageContent = resultSet.getBlob("messageContent");
                    record.setXml(new String(messageContent.getBytes(1L, (int) messageContent.length())));
                }
                record.setEventDateTime(resultSet.getTimestamp("eventDateTime").toLocalDateTime());

                if (record.getXml().startsWith("<?xml")) {
                    listXmlRecords.add(record);
                }
            }
        } catch (Exception exception) {
            throw new Exception("The following error occurred during an SQL operation:", exception);
        }
        return listXmlRecords;
    }

    private void runSqlScript(String sqlScript) throws Exception {

        try (Connection sqlConnection = dbConnect(ClientAbuseDetectionService.JDBC_EHNCP_PROPERTY);
             StringReader stringReader = new StringReader(sqlScript)) {

            ScriptRunner objScriptRunner = new ScriptRunner(sqlConnection, false, true);
            objScriptRunner.setLogWriter(null);
            objScriptRunner.setErrorLogWriter(null);
            objScriptRunner.runScript(stringReader);

        } catch (Exception exception) {
            throw new Exception("The following error occurred during an SQL operation:", exception);
        }
    }

    private boolean setAbuseErrorEvent(AbuseType abuseType, String description, int numRequests, AbuseEvent eventBegin,
                                       AbuseEvent eventEnd) {

        String eventDescription = description.substring(0, Math.min(description.length(), ANOMALY_DESCRIPTION_SIZE));

        String type = abuseType.getType();
        type = type.substring(0, Math.min(type.length(), ANOMALY_TYPE_SIZE));

        LocalDateTime now = LocalDateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault())));
        String datetime = now.toString(PATTERN);
        String eventStartDate = eventBegin.getRequestDateTime().toString(PATTERN);
        String eventEndDate = eventEnd.getRequestDateTime().toString(PATTERN);

        if (anomalyNotPresent(eventDescription, type, eventStartDate, eventEndDate)) {
            String sqlInsertStatementError;

            if("oracle".equals(databaseProduct)) {
                sqlInsertStatementError = "INSERT INTO EHNCP_ANOMALY( " +
                        "DESCRIPTION, " +
                        "TYPE, " +
                        "EVENT_DATE, " +
                        "EVENT_START_DATE, " +
                        "EVENT_END_DATE) " +
                        "VALUES (" +
                        "'" + StringUtils.replace(eventDescription, "'", "''") + "',\n" +
                        "'" + StringUtils.replace(type, "'", "''") + "',\n" +
                        "TO_DATE('" + datetime + "', 'YYYY-MM-DD HH24:MI:SS'),\n " +
                        "TO_DATE('" + eventStartDate + "', 'YYYY-MM-DD HH24:MI:SS'),\n " +
                        "TO_DATE('" + eventEndDate + "', 'YYYY-MM-DD HH24:MI:SS') " +
                        ");"; // FIX ORA-00933: SQL command not properly ended
            } else {
                sqlInsertStatementError = "INSERT INTO EHNCP_ANOMALY( " +
                        "DESCRIPTION, " +
                        "TYPE, " +
                        "EVENT_DATE, " +
                        "EVENT_START_DATE, " +
                        "EVENT_END_DATE) " +
                        "VALUES (" +
                        "'" + StringUtils.replace(eventDescription, "'", "''") + "',\n" +
                        "'" + StringUtils.replace(type, "'", "''") + "',\n" +
                        "'" + datetime + "',\n" +
                        "'" + eventStartDate + "',\n" +
                        "'" + eventEndDate + "'" +
                        ");";
            }
            try {
                this.runSqlScript(sqlInsertStatementError);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            //  Anomaly already persisted. Skipping.
            return false;
        }
        return true;
    }

    private boolean anomalyNotPresent(String description, String type, String eventStartDate, String eventEndDate) {
        int recordCount = 0;
        try (Connection sqlConnection = dbConnect(ClientAbuseDetectionService.JDBC_EHNCP_PROPERTY);
             Statement stmt = sqlConnection.createStatement()) {
                String sqlSelect = "";

                if("oracle".equals(databaseProduct)) {
                    sqlSelect = "SELECT id FROM EHNCP_ANOMALY WHERE " +
                            "DESCRIPTION = '" + StringUtils.replace(description, "'", "''") + "' AND " +
                            "TYPE = '" + StringUtils.replace(type, "'", "''") + "' AND " +
                            "EVENT_START_DATE = TO_DATE('" + eventStartDate + "', 'YYYY-MM-DD HH24:MI:SS') AND " +
                            "EVENT_END_DATE = TO_DATE('" + eventEndDate + "', 'YYYY-MM-DD HH24:MI:SS')";
                } else {
                    sqlSelect = "SELECT id FROM EHNCP_ANOMALY WHERE " +
                            "DESCRIPTION = '" + StringUtils.replace(description, "'", "''") + "' AND " +
                            "TYPE = '" + StringUtils.replace(type, "'", "''") + "' AND " +
                            "EVENT_START_DATE = '" + eventStartDate + "' AND " +
                            "EVENT_END_DATE = '" + eventEndDate + "'";
                }

                ResultSet rs = stmt.executeQuery(sqlSelect);
                while (rs.next()) {
                    recordCount++;
                }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return recordCount == 0;
    }

    /*
    private void setAbuseErrorEventOld(AbuseEvent abuseEvent) {
        EventLog eventLog = new EventLog();

        eventLog.setEI_EventActionCode(EventActionCode.CREATE);
        eventLog.setEI_EventDateTime(abuseEvent.audit.getEventIdentification().getEventDateTime());
        eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);

        eventLog.setAS_AuditSourceId(abuseEvent.audit.getAuditSourceIdentification().get(0).getAuditSourceID());

        Optional<ParticipantObjectIdentificationType> req = abuseEvent.audit.getParticipantObjectIdentification()
                .stream().filter(p -> StringUtils.equals(p.getParticipantObjectIDTypeCode().getCode(), "req")).findFirst();
        Optional<ParticipantObjectIdentificationType> rsp = abuseEvent.audit.getParticipantObjectIdentification()
                .stream().filter(p -> StringUtils.equals(p.getParticipantObjectIDTypeCode().getCode(), "rsp")).findFirst();

        eventLog.setReqM_ParticipantObjectID(req.get().getParticipantObjectID()); // getMessageID(msgContext.getEnvelope())
        eventLog.setReqM_ParticipantObjectDetail(req.get().getParticipantObjectDetail().get(0).getValue()); // msgContext.getEnvelope().getHeader().toString()".getBytes());

        Optional<AuditMessage.ActiveParticipant> cons = abuseEvent.audit.getActiveParticipant()
                .stream().filter(p -> StringUtils.equals(p.getRoleIDCode().get(0).getCode(), "ServiceConsumer")).findFirst();
        Optional<AuditMessage.ActiveParticipant> prov = abuseEvent.audit.getActiveParticipant()
                .stream().filter(p -> StringUtils.equals(p.getRoleIDCode().get(0).getCode(), "ServiceProvider")).findFirst();

        Optional<AuditMessage.ActiveParticipant> prov2 = abuseEvent.audit.getActiveParticipant()
                .stream().filter(p -> StringUtils.equals(p.getRoleIDCode().get(0).getCode(), "ServiceProvider")).findFirst();

        eventLog.setEI_TransactionName(TransactionName.ANOMALY_DETECTED);

        eventLog.setSC_UserID(cons.get().getUserID()); //clientCommonName
        eventLog.setSP_UserID(prov.get().getUserID()); //clientCommonName
        eventLog.setHR_UserID(cons.get().getUserID());
        for(ActiveParticipantType a : abuseEvent.audit.getActiveParticipant()) {
            if(XSPAFunctionalRole.containsLabel(a.getRoleIDCode().get(0).getCode())) {
                eventLog.setHR_UserID(a.getUserID());
                eventLog.setHR_RoleID(a.getRoleIDCode().get(0).getCode());
                eventLog.setHR_UserName(a.getUserName());
                eventLog.setHR_AlternativeUserID(a.getAlternativeUserID());
                break;
            }
        }

        eventLog.setSourceip(cons.get().getNetworkAccessPointID());
        eventLog.setTargetip(prov.get().getNetworkAccessPointID());

        eventLog.setResM_ParticipantObjectID(rsp.get().getParticipantObjectID()); // getMessageID(msgContext.getEnvelope())
        eventLog.setResM_ParticipantObjectDetail(rsp.get().getParticipantObjectDetail().get(0).getValue()); // msgContext.getEnvelope().getHeader().toString()".getBytes());

        eventLog.setNcpSide(NcpSide.NCP_A);
        eventLog.setEventType(EventType.ANOMALY_DETECTED);

        AuditService auditService = AuditServiceFactory.getInstance();
        auditService.write(eventLog, "", "1");
        logger.info("EventLog: '{}' generated.", eventLog.getEventType());
    }
    */

    private AuditMessage readAuditString(MessagesRecord rec) throws JAXBException {

        try {
            if (StringUtils.contains(rec.getXml(), "AuditMessage")) {
                AuditMessage au = AuditTrailUtils.convertXMLToAuditObject(new ByteArrayInputStream(rec.getXml().getBytes(StandardCharsets.UTF_8)));

                LocalDateTime dt = new LocalDateTime(au.getEventIdentification().getEventDateTime()
                        .toGregorianCalendar()
                        .toZonedDateTime()
                        .toLocalDateTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                        DateTimeZone.forTimeZone(TimeZone.getDefault()));

                boolean evtPresent = false;
                AbuseTransactionType transactionType = AbuseTransactionType.TRANSACTION_UNKNOWN;
                if (StringUtils.equals(au.getEventIdentification().getEventID().getCsdCode(),
                        EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS.getEventTypeCode().getCsdCode()) &&
                        StringUtils.equals("Cross Gateway Patient Discovery",
                                EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS.getEventTypeCode().getOriginalText()) &&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS.getEventTypeCode().getCsdCode()))) {
                    evtPresent = true;
                    transactionType = AbuseTransactionType.XCPD_SERVICE_REQUEST;
                }
                if (!evtPresent && StringUtils.equals(au.getEventIdentification().getEventID().getCsdCode(),
                        EventType.XCA_SERVICE_LIST.getEventTypeCode().getCsdCode()) &&
                        StringUtils.equals("Cross Gateway Query",
                                EventType.XCA_SERVICE_LIST.getEventTypeCode().getOriginalText()) &&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        EventType.XCA_SERVICE_LIST.getEventTypeCode().getCsdCode())) /*&&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        ClassCode.PS_CLASSCODE.getCode()))*/) {
                    evtPresent = true;
                    transactionType = AbuseTransactionType.XCA_SERVICE_REQUEST;
                }
                if (!evtPresent && StringUtils.equals(au.getEventIdentification().getEventID().getCsdCode(),
                        EventType.XDR_SERVICE_NCP_B.getEventTypeCode().getCsdCode()) &&
                        StringUtils.equals("Provide and Register Document Set-b",
                                EventType.XDR_SERVICE_NCP_B.getEventTypeCode().getOriginalText()) &&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        EventType.XDR_SERVICE_NCP_B.getEventTypeCode().getCsdCode())) /*&&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        ClassCode.EDD_CLASSCODE.getCode()))*/) {
                    evtPresent = true;
                    transactionType = AbuseTransactionType.XDR_SERVICE_REQUEST;
                }
                if (!evtPresent && StringUtils.equals(au.getEventIdentification().getEventID().getCsdCode(),
                        EventType.XCA_SERVICE_RETRIEVE_NCP_B.getEventTypeCode().getCsdCode()) &&
                        StringUtils.equals("Cross Gateway Retrieve",
                                EventType.XCA_SERVICE_RETRIEVE_NCP_B.getEventTypeCode().getOriginalText()) &&
                        au.getEventIdentification().getEventTypeCode()
                                .stream()
                                .anyMatch(c -> StringUtils.equals(c.getCsdCode(),
                                        EventType.XCA_SERVICE_RETRIEVE_NCP_B.getEventTypeCode().getCsdCode()))) {
                    evtPresent = true;
                    transactionType = AbuseTransactionType.XCA_SERVICE_REQUEST;
                }

                if (evtPresent) {

                    String joinedPoc = au.getActiveParticipant().stream()
                            .filter(ActiveParticipantContents::isUserIsRequestor)
                            .map(ActiveParticipantContents::getUserID)
                            .collect(Collectors.joining("-"));

                    String simplePoc = au.getActiveParticipant().stream()
                            .filter(auid -> auid.getAlternativeUserID() != null)
                            .filter(ActiveParticipantContents::isUserIsRequestor)
                            .map(ActiveParticipantContents::getUserID)
                            .collect(Collectors.joining());

                    String participant = au.getParticipantObjectIdentification().stream()
                            .filter(a -> a.getParticipantObjectTypeCode().equals("1") && a.getParticipantObjectTypeCodeRole().equals("1"))
                            .map(ParticipantObjectIdentificationContents::getParticipantObjectID)
                            .collect(Collectors.joining());

                    abuseList.add(
                            new AbuseEvent(au.getEventIdentification().getEventID(),
                                    simplePoc,
                                    participant,
                                    dt,
                                    rec.getId().toString(),
                                    transactionType,
                                    au)
                    );
                }
                return au;
            }
        } catch (Exception e) {
            throw new AbuseDetectionException(e);
        }

        return null;
    }

    private int getElapsedTimeBetweenEvents(List<AbuseEvent> listEvt, int beg, int end) {
        if (beg < 0 || end < 0) {
            return 0;
        }
        if (beg >= listEvt.size() || end >= listEvt.size()) {
            return 0;
        }
        LocalDateTime t1 = listEvt.get(beg).getRequestDateTime();
        LocalDateTime t2 = listEvt.get(end).getRequestDateTime();
        return getElapsedSecondsBetweenDateTime(t1, t2);
    }

    protected int getElapsedSecondsBetweenDateTime(LocalDateTime t1, LocalDateTime t2) {
        Period diff = new Period(t1, t2); // time elapsed between first and last request
        // able to calculate whole days between two dates easily
        Days days = Days.daysBetween(t1, t2);
        //return diff.toStandardSeconds().getSeconds();
        return Seconds.secondsBetween(t1, t2).getSeconds();
    }

    private List<AbuseEvent> checkAnomalies(List<AbuseEvent> list) {

        int areqr = Integer.parseInt(Constants.ABUSE_ALL_REQUEST_REFERENCE_REQUEST_PERIOD);
        int upatr = 0; // No Patient discovery on client only on server
        int upocr = Integer.parseInt(Constants.ABUSE_UNIQUE_POC_REFERENCE_REQUEST_PERIOD);

        int areqThreshold = Integer.parseInt(Constants.ABUSE_ALL_REQUEST_THRESHOLD);
        int upatThreshold = 0; // No Patient discovery on client only on server
        int upocThreshold = Integer.parseInt(Constants.ABUSE_UNIQUE_POC_REQUEST_THRESHOLD);

        if (areqr <= 0 && upatr <= 0 && upocr <= 0) { // no check
            return list;
        }

        List<AbuseEvent> sortedAllList = list.stream()
                .sorted(Comparator.comparing(AbuseEvent::getRequestDateTime))
                .collect(Collectors.toList());
        if (areqr > 0 && sortedAllList.size() > areqThreshold) { // Analyze ALL requests
            int index = 0;
            int lastValidIndex = 0;
            do {
                int tot = 0;
                int beg = 0;
                int end = 0;
                for (index = lastValidIndex; index < sortedAllList.size(); index++) {
                    beg = lastValidIndex;
                    end = index;
                    int elapsed = getElapsedTimeBetweenEvents(sortedAllList, beg, end);
                    if (elapsed > areqr) {
                        lastValidIndex = index;
                        break;
                    }
                    tot++;
                }
                if (tot > areqThreshold) {
                    if (lastValidIndex > 0 && index < sortedAllList.size()) {
                        end = lastValidIndex - 1;
                    } else {
                        end = sortedAllList.size() - 1;
                    }
                    int elapsed = getElapsedTimeBetweenEvents(sortedAllList, beg, end);
                    if (elapsed < areqr) {
                        String abuseDescription = String.format(DESCRIPTION_ALL, tot, elapsed, areqThreshold);
                        if (setAbuseErrorEvent(AbuseType.ALL, abuseDescription, tot, sortedAllList.get(beg), sortedAllList.get(end))) {
                            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                                loggerClinical.error("WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS : [Total requests: '{}' exceeding " +
                                                "threshold of: '{}' requests inside an interval of '{}' seconds] - begin event : ['{}'] end event: ['{}']",
                                        tot, areqThreshold, elapsed, sortedAllList.get(beg), sortedAllList.get(end));
                            }
                        }
                    }
                }
            } while (index < sortedAllList.size() && lastValidIndex < sortedAllList.size());
        }

        //////////////////////////////////////////////////////////////////////

        List<AbuseEvent> distinctPointOfCareIds = list.stream()
                .filter(distinctByKey(AbuseEvent::getPointOfCare))
                .collect(Collectors.toList());
        if (upocr > 0 && sortedAllList.size() > upocThreshold) { // analyze unique POC requests
            if (!distinctPointOfCareIds.isEmpty()) {
                distinctPointOfCareIds.forEach(poc -> {
                    List<AbuseEvent> sortedPocList = list.stream()
                            .filter(p -> p.getPointOfCare().equals(poc.getPointOfCare()))
                            .sorted(Comparator.comparing(AbuseEvent::getPointOfCare))
                            .sorted(Comparator.comparing(AbuseEvent::getRequestDateTime))
                            .collect(Collectors.toList());

                    Period diff = Period.ZERO;
                    int index = 0;
                    int lastValidIndex = 0;
                    do {
                        int tot = 0;
                        int beg = 0;
                        int end = 0;
                        for (index = lastValidIndex; index < sortedPocList.size(); index++) {
                            beg = lastValidIndex;
                            end = index;
                            int elapsed = getElapsedTimeBetweenEvents(sortedPocList, beg, end);
                            if (elapsed > upocr) {
                                lastValidIndex = index;
                                break;
                            }
                            tot++;
                        }
                        if (tot > upocThreshold) {
                            if (lastValidIndex > 0 && index < sortedPocList.size()) {
                                end = lastValidIndex - 1;
                            } else {
                                end = sortedPocList.size() - 1;
                            }
                            int elapsed = getElapsedTimeBetweenEvents(sortedPocList, beg, end);
                            if (elapsed < upocr) {
                                String abuseDescription = String.format(DESCRIPTION_POC, tot, elapsed, upocThreshold);
                                if (setAbuseErrorEvent(AbuseType.POC, abuseDescription, tot, sortedPocList.get(beg), sortedPocList.get(end))) {
                                    if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                                        loggerClinical.error("WARNING_SEC_UNEXPECTED_NUMBER_OF_REQUESTS_FOR_UNIQUE_POINT_OF_CARE : " +
                                                        "[Total requests: '{}' exceeding threshold of: '{}' requests inside an interval " +
                                                        "of '{}' seconds] - begin event : ['{}'] end event : ['{}']",
                                                tot, upocThreshold, elapsed,
                                                sortedPocList.get(beg), sortedPocList.get(end));
                                    }
                                }
                            }
                        }
                    } while (index < sortedPocList.size() && lastValidIndex < sortedPocList.size());
                });
            }
        }

        ///////////////////////////////////////////////////

        // No Patient discovery on client only on server

        ///////////////////////////////////////////////////

        // strip from table file older than ABUSE_ALL_REQUEST_REFERENCE_REQUEST_PERIOD
        int purgeLimit = NumberUtils.max(new int[]{areqr, upocr, upatr});
        List<AbuseEvent> ret = list.stream()
                .sorted(Comparator.comparing(AbuseEvent::getRequestDateTime))
                .filter(p -> getElapsedSecondsBetweenDateTime(
                                p.getRequestDateTime(),
                                new LocalDateTime(
                                        DateTimeZone.forTimeZone(TimeZone.getDefault())
                                )) <= purgeLimit
                        //Period.fieldDifference(p.getRequestDateTime(),
                        //        new LocalDateTime(DateTimeZone.forTimeZone(TimeZone.getDefault())))
                        //.toStandardSeconds().getSeconds() <= purgeLimit
                )
                .collect(Collectors.toList());

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            if (ret.size() < list.size()) {
                loggerClinical.info("'{}' events purged from active list, new list size; '{}'", list.size() - ret.size(), ret.size());
            } else {
                loggerClinical.info("Events in active list: '{}'", list.size());
            }
        }
        return ret;
    }
}
