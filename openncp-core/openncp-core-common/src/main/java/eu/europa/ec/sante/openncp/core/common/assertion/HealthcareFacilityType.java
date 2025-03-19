package eu.europa.ec.sante.openncp.core.common.assertion;

/**
 * From WP3.4 Deliverable D.3.4.2
 * Type of HCPO
 * FriendlyName:  eHealth DSI Healthcare Facility Type
 * Name:  urn:ehdsi:names:subject:healthcare-facility-type
 */
public enum HealthcareFacilityType {

    HOSPITAL("Hospital"),
    RESIDENT_PHYSICIAN("Resident Physician"),
    PHARMACY("Pharmacy"),
    OTHER("Other");

    private final String facilityType;

    HealthcareFacilityType(final String facilityType) {
        this.facilityType = facilityType;
    }

    @Override
    public String toString() {
        return facilityType;
    }
}
