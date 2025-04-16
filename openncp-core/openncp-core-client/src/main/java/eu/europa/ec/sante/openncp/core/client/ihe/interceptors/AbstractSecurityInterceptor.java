package eu.europa.ec.sante.openncp.core.client.ihe.interceptors;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

public abstract class AbstractSecurityInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    protected static final String SECURITY_HEADER_KEY = "SECURITY_HEADER";

    public AbstractSecurityInterceptor(final String phase) {
        super(phase);
    }
}
