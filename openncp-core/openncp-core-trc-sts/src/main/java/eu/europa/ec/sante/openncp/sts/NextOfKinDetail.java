package eu.europa.ec.sante.openncp.sts;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;

public class NextOfKinDetail {
    private String patientId;

    private String livingSubjectId;

    private String firstName;

    private String familyName;

    private String gender;

    private Date birthDate;

    private String addressStreet;

    private String addressCity;

    private String addressPostalCode;

    private String addressCountry;

    public NextOfKinDetail() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getLivingSubjectId() {
        return livingSubjectId;
    }

    public void setLivingSubjectId(final String livingSubjectId) {
        this.livingSubjectId = livingSubjectId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(final String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(final String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(final String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(final String addressCountry) {
        this.addressCountry = addressCountry;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("livingSubjectId", livingSubjectId)
                .append("firstName", firstName)
                .append("FamilyName", familyName)
                .append("gender", gender)
                .append("birthDate", birthDate)
                .append("addressStreet", addressStreet)
                .append("addressCity", addressCity)
                .append("addressPostalCode", addressPostalCode)
                .append("addressCountry", addressCountry)
                .toString();
    }
}
