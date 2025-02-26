package eu.europa.ec.sante.openncp.core.server.ihe.xca;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import org.apache.axiom.om.OMElement;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.soap.Addressing;

@WebService(serviceName = "XCAService", //  Important: Define the service name
        portName = "XCAPort",       // Important: Define the port name
        targetNamespace = "urn:ihe:iti:xca-b:2007") // Important: Match the namespace

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL) // Often required for IHE profiles
@Addressing(enabled=true, required=true) // Often required for IHE profiles
public interface XCAServiceInterface {

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @throws Exception
     */
    AdhocQueryResponse queryDocument(AdhocQueryRequest request, SOAPHeader soapHeader, EventLog eventLog,final OMElement omElement) throws Exception;

    /**
     * @param request
     * @param soapHeader
     * @param eventLog
     * @throws Exception
     */
    void retrieveDocument(RetrieveDocumentSetRequestType request, SOAPHeader soapHeader, EventLog eventLog,final OMElement omElement) throws Exception;
}
