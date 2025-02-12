/*package eu.europa.ec.sante.openncp.core.server.ihe.xdr;

import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.core.common.ihe.XDRServiceInterface;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;
import org.apache.axiom.soap.SOAPHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

*//**
 * XDR_ServiceSkeleton java skeleton for the axisService
 *//*
@Service("xdrServiceSkeleton")
public class XDR_ServiceSkeleton {

    @Autowired
    private XDRServiceInterface xdrServiceInterface;

    *//**
     * Auto generated method signature
     *
     * @param provideAndRegisterDocumentSetRequest
     *//*
    public RegistryResponseType documentRecipient_ProvideAndRegisterDocumentSetB(ProvideAndRegisterDocumentSetRequestType provideAndRegisterDocumentSetRequest,SOAPHeader soapHeader, EventLog eventLog) throws Exception {
        return xdrServiceInterface.saveDocument(provideAndRegisterDocumentSetRequest, soapHeader, eventLog);
    }
}*/
