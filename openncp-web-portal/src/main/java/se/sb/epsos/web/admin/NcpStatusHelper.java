package se.sb.epsos.web.admin;

import se.sb.epsos.web.EpsosAuthenticatedWebSession;
import se.sb.epsos.web.model.CdaDocument;
import se.sb.epsos.web.model.Person;
import se.sb.epsos.web.service.MetaDocument;
import se.sb.epsos.web.service.NcpServiceException;
import se.sb.epsos.web.service.NcpServiceFacade;

import java.util.List;

public class NcpStatusHelper {

    private NcpStatusHelper() {
    }

    public static CdaDocument getEpForPerson(NcpServiceFacade facade, Person person) throws NcpServiceException {

        List<MetaDocument> docs;
        CdaDocument ep = null;
        docs = facade.queryDocuments(person, "EP", null);
        if (!docs.isEmpty()) {
            MetaDocument epDoc = docs.get(0);
            ep = facade.retrieveDocument(epDoc);
        }
        return ep;
    }

    public static CdaDocument getPSForPerson(NcpServiceFacade facade, Person person) throws NcpServiceException {

        List<MetaDocument> docs;
        CdaDocument ps = null;
        docs = facade.queryDocuments(person, "PS", null);
        if (!docs.isEmpty()) {
            MetaDocument epDoc = docs.get(0);
            ps = facade.retrieveDocument(epDoc);
        }
        return ps;
    }

    public static void initFacade(NcpServiceFacade facade) throws NcpServiceException {
        
        EpsosAuthenticatedWebSession session = (EpsosAuthenticatedWebSession) EpsosAuthenticatedWebSession.get();
        facade.bindToSession(session.getId());
        facade.initServices(session.getUserDetails());
    }
}
