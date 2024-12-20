package eu.europa.ec.sante.openncp.core.server.nc.mock.util;


import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MimeProcessor {

    public static byte[] removeMimeHeadersAndFooters(byte[] inputBytes) {

        InputStream inputStream = new ByteArrayInputStream(inputBytes);

        // Parse the MIME message
        Session session = Session.getDefaultInstance(System.getProperties());
        MimeMessage mimeMessage;
        byte[] binaryData = null;
        try {
            mimeMessage = new MimeMessage(session, inputStream);
            // Process the MIME parts
            Object content = mimeMessage.getContent();


            if (content instanceof SharedByteArrayInputStream) {
                SharedByteArrayInputStream sbais = (SharedByteArrayInputStream) content;
                binaryData = sbais.readAllBytes();
            }
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
        return binaryData;
    }
}
