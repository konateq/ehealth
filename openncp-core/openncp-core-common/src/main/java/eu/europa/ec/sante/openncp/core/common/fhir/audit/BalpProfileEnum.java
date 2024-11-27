package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Coding;

import java.util.function.Supplier;

public enum BalpProfileEnum {
    BASIC_CREATE(
            "https://profiles.ihe.net/ITI/BALP/StructureDefinition/IHE.BasicAudit.Create",
            AuditEvent.AuditEventAction.C,
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110153", "Source Role ID"),
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID")),
    BASIC_READ(
            "https://profiles.ihe.net/ITI/BALP/StructureDefinition/IHE.BasicAudit.Read",
            AuditEvent.AuditEventAction.R,
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110153", "Source Role ID"),
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID")),
    PATIENT_READ(
            "https://profiles.ihe.net/ITI/BALP/StructureDefinition/IHE.BasicAudit.PatientRead",
            AuditEvent.AuditEventAction.R,
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110153", "Source Role ID"),
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID")),

    BASIC_QUERY(
            "https://profiles.ihe.net/ITI/BALP/StructureDefinition/IHE.BasicAudit.Query",
            AuditEvent.AuditEventAction.E,
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110153", "Source Role ID"),
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID")),
    PATIENT_QUERY(
            "https://profiles.ihe.net/ITI/BALP/StructureDefinition/IHE.BasicAudit.PatientQuery",
            AuditEvent.AuditEventAction.E,
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110153", "Source Role ID"),
            () -> new Coding("http://dicom.nema.org/resources/ontology/DCM", "110152", "Destination Role ID"));

    private final String myProfileUrl;
    private final AuditEvent.AuditEventAction myAction;
    private final Supplier<Coding> myAgentClientTypeCoding;
    private final Supplier<Coding> myAgentServerTypeCoding;

    BalpProfileEnum(
            final String theProfileUrl,
            final AuditEvent.AuditEventAction theAction,
            final Supplier<Coding> theAgentClientTypeCoding,
            final Supplier<Coding> theAgentServerTypeCoding) {
        myProfileUrl = theProfileUrl;
        myAction = theAction;
        myAgentClientTypeCoding = theAgentClientTypeCoding;
        myAgentServerTypeCoding = theAgentServerTypeCoding;
    }

    public Coding getAgentClientTypeCoding() {
        return myAgentClientTypeCoding.get();
    }

    public Coding getAgentServerTypeCoding() {
        return myAgentServerTypeCoding.get();
    }

    public String getProfileUrl() {
        return myProfileUrl;
    }

    public AuditEvent.AuditEventAction getAction() {
        return myAction;
    }
}
