package eu.europa.ec.sante.openncp.core.server.ihe.xdr;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ProvideAndRegisterDocumentSetRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryResponseType;

import javax.xml.soap.SOAPHeader;

public interface XdrServiceServerSide {

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    RegistryResponseType saveDocument(ProvideAndRegisterDocumentSetRequest request, SOAPHeader soapHeader, EventLog eventLog) throws Exception;
}
