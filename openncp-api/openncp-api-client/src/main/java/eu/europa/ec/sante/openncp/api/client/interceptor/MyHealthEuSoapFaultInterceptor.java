package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.core.common.ihe.exception.ExceptionWithContext;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Optional;

@Component
public class MyHealthEuSoapFaultInterceptor extends AbstractSoapInterceptor {

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
