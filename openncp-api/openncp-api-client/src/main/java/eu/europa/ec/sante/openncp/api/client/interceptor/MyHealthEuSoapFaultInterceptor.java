package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.core.common.ihe.exception.ExceptionWithContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.phase.Phase;
import org.opensaml.xmlsec.signature.Q;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Optional;

@Component
public class MyHealthEuSoapFaultInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyHealthEuSoapFaultInterceptor.class);

    public MyHealthEuSoapFaultInterceptor() {
        super(Phase.PREPARE_SEND);
        addAfter(FaultOutInterceptor.class.getName()); // Ensure it runs after standard fault handling
    }

    @Override
    public void handleMessage(final SoapMessage message) throws Fault {
        final Exception ex = message.getContent(Exception.class);
        if (ex instanceof Fault) {
            final Fault fault = (Fault) ex;
            getFirstExceptionWithContext(fault.getCause(), 10).ifPresent(exceptionWithContext -> {
                final SoapFault soapFault = SoapFault.createFault(fault, message.getVersion());
                //from the POV of the connector, NCP-B acts as a server in context of a SOAP request!
                soapFault.setFaultCode(Fault.FAULT_CODE_SERVER);
                soapFault.addSubCode(new QName(exceptionWithContext.getErrorCode().getCode()));
                try{
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

                    factory.setNamespaceAware(true);

                    // Create a DocumentBuilder
                    DocumentBuilder builder = factory.newDocumentBuilder();

                    // Create a new Document
                    Document document = builder.newDocument();

                    Element element = document.createElementNS("http://clientconnector.protocolterminator.openncp.epsos/","soapenv:" + exceptionWithContext.getClass().getSimpleName());
                    element.setNodeValue(exceptionWithContext.getMessage());

                    soapFault.setDetail(element);
                }catch (Exception e){
                    LOGGER.error("Error creating detail element: {} ", e.getMessage() );
                }


                //replace the Fault content with a proper populated SoapFault
                message.setContent(Exception.class, soapFault);
            });
        }
    }

    private Optional<ExceptionWithContext> getFirstExceptionWithContext(final Throwable exception, final int maxDepth) {
        if (exception == null || maxDepth <= 0) {
            return Optional.empty();
        }

        if (exception instanceof ExceptionWithContext) {
            return Optional.of((ExceptionWithContext) exception);
        } else {
            return getFirstExceptionWithContext(exception.getCause(), maxDepth - 1);
        }
    }
}
