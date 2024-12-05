package eu.europa.ec.sante.openncp.core.common.fhir.context;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class Loinc {
    public static Loinc LAB_RESULT = Loinc.of("92236-9");
    public static Loinc MEDICAL_IMAGE_STUDY = Loinc.of("18748-4");
    private final String code;

    private Loinc(String code) {
        this.code = code;
    }

    public static Loinc none() {
        return new Loinc(null);
    }

    public static Loinc of(String code) {
        Validate.notBlank(code, "The loincode cannot be blank");
        return new Loinc(code);
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loinc loinc = (Loinc) o;
        return Objects.equals(code, loinc.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("code", code)
                .toString();
    }
}
