package se.sb.epsos.web.service;

import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.model.*;

import java.util.List;

public interface NcpServiceFacade {

    String about();

    void initServices(AuthenticatedUser userDetails) throws NcpServiceException;

    List<Person> queryForPatient(AuthenticatedUser userDetails, List<PatientIdVO> patientList, CountryVO country) throws NcpServiceException;

    void setTRCAssertion(TRC trc, AuthenticatedUser userDetails) throws NcpServiceException;

    List<MetaDocument> queryDocuments(Person person, String doctype, AuthenticatedUser userDetails) throws NcpServiceException;

    CdaDocument retrieveDocument(MetaDocument doc) throws NcpServiceException;

    byte[] submitDocument(Dispensation dispensation, AuthenticatedUser user, Person person, String eD_PageAsString) throws NcpServiceException;

    void bindToSession(String sessionId);
}
