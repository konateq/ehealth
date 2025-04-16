package eu.europa.ec.sante.openncp.core.common.interceptors;

import eu.europa.ec.sante.openncp.core.common.ImmutableSecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SoapMessageInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoapMessageInterceptor.class);
    private final MessageFactory messageFactory;

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            LOGGER.error("InitializationException: '{}'", e.getMessage());
        }
    }

    public SoapMessageInterceptor() {
        super(Phase.RECEIVE);
        try {
            messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        } catch (final SOAPException e) {
            throw new RuntimeException("Could not create the SOAP message factory", e);
        }
    }

    @Override
    public void handleMessage(final SoapMessage soapMessage) throws Fault {
        // third way
        try {
            final InputStream is = soapMessage.getContent(InputStream.class);
            if (is == null) {
                return;
            }
            // Copy the stream so CXF can still use it afterward
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            is.transferTo(buffer);
            final byte[] requestBytes = buffer.toByteArray();

            // Restore the input stream for downstream interceptors else their inputstream will be empty
            soapMessage.setContent(InputStream.class, new ByteArrayInputStream(requestBytes));

            // Create SOAPMessage from the buffered bytes
            final SOAPMessage message = messageFactory.createMessage(null, new ByteArrayInputStream(requestBytes));
            message.getSOAPPart().getEnvelope().getHeader(); // Ensure it parses headers

            final SOAPHeader header = message.getSOAPHeader();
            SecurityContextProvider
                    .getSecurityContext()
                    .map(ImmutableSecurityContext::copyOf)
                    .map(immutableSecurityContext -> immutableSecurityContext.withSoapHeader(header))
                    .ifPresentOrElse(SecurityContextProvider::setAssertionContext,
                            () -> SecurityContextProvider.setAssertionContext(ImmutableSecurityContext.builder()
                                    .soapHeader(header)
                                    .build())
                    );
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
        }

    }
}
