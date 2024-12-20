package eu.europa.ec.sante.openncp.core.server.nc.mock.util;


import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MimeProcessor {

    public static byte[] removeMimeHeadersAndFooters(final byte[] inputBytes) {

        final InputStream inputStream = new ByteArrayInputStream(inputBytes);

        // Parse the MIME message
        final Session session = Session.getDefaultInstance(System.getProperties());
        final MimeMessage mimeMessage;
        byte[] binaryData = null;
        try {
            mimeMessage = new MimeMessage(session, inputStream);
            // Process the MIME parts
            final Object content = mimeMessage.getContent();


            if (content instanceof SharedByteArrayInputStream) {
                final SharedByteArrayInputStream sbais = (SharedByteArrayInputStream) content;
                binaryData = sbais.readAllBytes();
            }
        } catch (final MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
        return binaryData;
    }
}
