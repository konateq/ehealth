package eu.europa.ec.sante.openncp.core.server.nc.mock.util;


import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class MimeProcessor {

    public static byte[] removeMimeHeadersAndFooters(final byte[] inputBytes) {

        final InputStream inputStream = new ByteArrayInputStream(inputBytes);

        // Parse the MIME message
        final Session session = Session.getDefaultInstance(System.getProperties());
        final MimeMessage mimeMessage;
        byte[] binaryData = null;
        int contentLength = 0;
        try {
            mimeMessage = new MimeMessage(session, inputStream);
            Enumeration<?> headers = mimeMessage.getAllHeaders();
            while (headers.hasMoreElements()) {
                Header header = (Header) headers.nextElement();
                if (header.getName().equalsIgnoreCase("Content-Length")) {
                    contentLength = Integer.parseInt(header.getValue());
                }
            }
            // Process the MIME parts
            final Object content = mimeMessage.getContent();

            if (content instanceof SharedByteArrayInputStream) {
                final SharedByteArrayInputStream sbais = (SharedByteArrayInputStream) content;
                binaryData = sbais.readNBytes(contentLength);
            }
        } catch (final MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
        return binaryData;
    }
}
