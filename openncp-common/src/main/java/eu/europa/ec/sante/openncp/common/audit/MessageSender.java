package eu.europa.ec.sante.openncp.common.audit;

import eu.europa.ec.sante.openncp.common.audit.serialization.AuditLogSerializer;
import eu.europa.ec.sante.openncp.common.audit.ssl.AuthSSLSocketFactory;
import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import eu.europa.ec.sante.openncp.common.audit.utils.SerializableMessage;
import eu.europa.ec.sante.openncp.common.audit.utils.Utils;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import net.RFC3881.dicom.AuditMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Thread for sending the messages to the syslog repository. Each message is being sent using a different thread.
 * If a message can't be sent immediately, it tries for a time interval.
 */
public class MessageSender {

    private static final String AUDIT_REPOSITORY_URL = "audit.repository.url";
    private static final String AUDIT_REPOSITORY_PORT = "audit.repository.port";
    private static final String TRUSTSTORE = "TRUSTSTORE_PATH";
    private static final String KEY_ALIAS = "NCP_SIG_PRIVATEKEY_ALIAS";
    private static final String[] enabledProtocols = {"TLSv1.2"};
    private final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private AuditLogSerializer auditLogSerializer;
    private String facility;
    private String severity;

    /**
     * @param auditLogSerializer
     * @param auditmessage
     * @param facility
     * @param severity
     */
    public void send(final AuditLogSerializer auditLogSerializer, final AuditMessage auditmessage, final String facility, final String severity) {

        logger.info("[Audit Service] Message Sender Start...");
        boolean sent = false;
        this.auditLogSerializer = auditLogSerializer;
        this.facility = facility;
        this.severity = severity;

        try {
            if (auditmessage.getEventIdentification() != null && auditmessage.getEventIdentification().getEventTypeCode() != null) {
                logger.info("Try to construct the Audit Message type: '{}'", auditmessage.getEventIdentification().getEventTypeCode().get(0).getCsdCode());
            } else {
                logger.info("Try to construct the Audit Message type: '{}'", "N/A");
            }
            final String auditMessage = AuditTrailUtils.constructMessage(auditmessage, true);
            if (StringUtils.isNotBlank(auditMessage)) {
                long timeout = Long.parseLong(Utils.getProperty("audit.time.to.try", "60000", true));
                boolean timeouted;
                logger.debug("Try to send the message for '{}' msec", timeout);
                timeout += System.currentTimeMillis();

                do {

                    sent = sendMessage(auditMessage, facility, severity);
                    timeouted = System.currentTimeMillis() > timeout;
                    if (!sent && !timeouted) {
                        Utils.sleep(1000);
                    }
                } while (!sent && !timeouted);

                if (timeouted) {
                    logger.info("The time set to OpenNCP properties in order to retry sending the audit has passed");
                }
            }
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            logger.error("InterruptedException: '{}'", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } finally {
            if (!sent) {
                if (auditLogSerializer != null) {
                    auditLogSerializer.writeObjectToFile(new SerializableMessage(auditmessage, facility, severity));
                } else {
                    logger.info("Failed to send backup audit message to OpenATNA. Retry later.");
                }
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This class is responsible for sending the audit message to the repository. Creates UDP logs for every step.
     *
     * @param auditMessage
     * @param facility
     * @param severity
     * @return true/false depending on the success of sending the message
     */
    private boolean sendMessage(final String auditMessage, final String facility, final String severity) {

        final SSLSocket sslsocket;
        boolean sent = false;
        final String facsev = facility + severity;

        try {
            sslsocket = createAuditSecuredSocket();
        } catch (final IOException e) {
            logger.error("IOException: Cannot contact Secured Audit Server '{}'", e.getMessage());
            return false;
        }
        try (final BufferedOutputStream outputStream = new BufferedOutputStream(sslsocket.getOutputStream())) {

            //  Set header AuditLogSerializer of syslog message.
            String hostName = sslsocket.getLocalAddress().getHostName();
            if (!sslsocket.getLocalAddress().isLinkLocalAddress() && !sslsocket.getLocalAddress().isLoopbackAddress()
                    && (sslsocket.getLocalAddress() instanceof Inet4Address)) {
                hostName = IPUtil.getPrivateServerIp();
            }
            logger.info("Syslog Server hostname: '{}'", hostName);
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            final Date now = new Date();
            final StringBuilder nowStr = new StringBuilder(dateFormat.format(now));
            if (nowStr.charAt(4) == '0') {
                nowStr.setCharAt(4, ' ');
            }
            final String header = "<" + facsev + ">1 " + nowStr + " " + hostName + " - - - - ";

            //  Set body of syslog message.
            final int length = header.getBytes().length + 3 + auditMessage.getBytes().length;
            outputStream.write((length + " ").getBytes());
            outputStream.write(header.getBytes());

            //  Set the bom for UTF-8
            outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            outputStream.flush();

            //  Write the Syslog message to repository
            outputStream.write(auditMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            sent = true;

        } catch (final Exception e) {
            logger.error("Error sending message: '{}'", e.getMessage(), e);

        } finally {

            try {
                // Closing Secured Socket
                logger.info("Closing SSL Socket");
                sslsocket.close();
            } catch (final IOException e) {
                logger.warn("Unable to close SSLSocket", e);
            }
        }
        return sent;
    }

    /**
     * @return
     * @throws IOException
     */
    private SSLSocket createAuditSecuredSocket() throws IOException {

        logger.info("Initialization SSLSocket...");
        final String host = ConfigurationManagerFactory.getConfigurationManager().getProperty(AUDIT_REPOSITORY_URL);
        final int port = Integer.parseInt(ConfigurationManagerFactory.getConfigurationManager().getProperty(AUDIT_REPOSITORY_PORT));

        final File u = new File(ConfigurationManagerFactory.getConfigurationManager().getProperty(TRUSTSTORE));
        final KeystoreDetails trust = KeystoreDetails.of(u.toString(),
                ConfigurationManagerFactory.getConfigurationManager().getProperty(Configuration.TRUSTSTORE_PWD.getValue()),
                ConfigurationManagerFactory.getConfigurationManager().getProperty(KEY_ALIAS));
        final File uu = new File(ConfigurationManagerFactory.getConfigurationManager().getProperty(Configuration.TLS_KEYSTORE_FILE.getValue()));
        final KeystoreDetails key = KeystoreDetails.of(uu.toString(),
                ConfigurationManagerFactory.getConfigurationManager().getProperty(Configuration.TLS_KEYSTORE_PWD.getValue()),
                ConfigurationManagerFactory.getConfigurationManager().getProperty(Configuration.TLS_PRIVATE_KEY_ALIAS.getValue()),
                ConfigurationManagerFactory.getConfigurationManager().getProperty(Configuration.TLS_PRIVATE_KEY_PWD.getValue()));
        final AuthSSLSocketFactory authSSLSocketFactory = new AuthSSLSocketFactory(key, trust);
        final SSLSocket sslsocket = (SSLSocket) authSSLSocketFactory.createSecureSocket(host, port);
        sslsocket.setEnabledProtocols(enabledProtocols);
        final String[] suites = sslsocket.getSupportedCipherSuites();
        sslsocket.setEnabledCipherSuites(suites);

        return sslsocket;
    }
}
