package eu.europa.ec.sante.openncp.core.common.ihe.util;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import javax.xml.namespace.QName;
import java.util.List;

class MustUnderstandInterceptor extends AbstractSoapInterceptor {

    public MustUnderstandInterceptor() {
        super(Phase.RECEIVE);
    }


    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        List<Header> headers = message.getHeaders();
        for (Header header : headers) {
            QName name = header.getName();

        }

    }
}
