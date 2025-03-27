package eu.europa.ec.sante.openncp.core.server.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201305UV02;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.PRPAIN201306UV02;

import javax.xml.soap.SOAPHeader;

public interface XcpdServiceServerSide {

    PRPAIN201306UV02 queryPatient(PRPAIN201305UV02 request, SOAPHeader soapHeader, EventLog eventLog) throws Exception;
}
