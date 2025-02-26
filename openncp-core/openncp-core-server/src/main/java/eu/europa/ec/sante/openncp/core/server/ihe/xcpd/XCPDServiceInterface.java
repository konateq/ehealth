package eu.europa.ec.sante.openncp.core.server.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.audit.EventLog;

import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.PRPAIN201306UV02;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.soap.SOAPHeader;
import javax.xml.ws.soap.Addressing;

@WebService(serviceName = "XCPDService", //  Important: Define the service name
        portName = "XCPDPort",       // Important: Define the port name
        targetNamespace = "urn:ihe:iti:xcpd-b:2007") // Important: Match the namespace

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL) // Often required for IHE profiles
@Addressing(enabled=true, required=true) // Often required for IHE profiles
public interface XCPDServiceInterface {

    PRPAIN201306UV02 queryPatient(PRPAIN201305UV02 request, SOAPHeader soapHeader, EventLog eventLog) throws Exception;
}
