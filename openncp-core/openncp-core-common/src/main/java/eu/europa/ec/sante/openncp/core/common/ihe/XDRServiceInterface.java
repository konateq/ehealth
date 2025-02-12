package eu.europa.ec.sante.openncp.core.common.ihe;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.ws.soap.Addressing;


@WebService(serviceName = "XDRService", //  Important: Define the service name
        portName = "XDRPort",       // Important: Define the port name
        targetNamespace = "urn:ihe:iti:xds-b:2007") // Important: Match the namespace

@SOAPBinding(style = Style.DOCUMENT, use = Use.LITERAL) // Often required for IHE profiles
@Addressing(enabled=true, required=true) // Often required for IHE profiles
public interface XDRServiceInterface {

    /**
     * @param request
     * @param eventLog
     * @return
     * @throws Exception
     */
    RegistryResponseType saveDocument(ProvideAndRegisterDocumentSetRequestType request,  EventLog eventLog) throws Exception;
}
