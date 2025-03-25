package eu.europa.ec.sante.openncp.core.server.ihe.xca;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RetrieveDocumentSetRequest;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RetrieveDocumentSetResponse;

import javax.xml.soap.SOAPHeader;

public interface XCAService {

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @return
     * @throws Exception
     */
    AdhocQueryResponse queryDocument(AdhocQueryRequest request, SOAPHeader soapHeader, EventLog eventLog) throws Exception;

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @throws Exception
     */
    RetrieveDocumentSetResponse retrieveDocument(RetrieveDocumentSetRequest request, SOAPHeader soapHeader, EventLog eventLog) throws Exception;
}
