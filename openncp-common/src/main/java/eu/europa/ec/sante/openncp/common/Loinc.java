package eu.europa.ec.sante.openncp.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

public class Loinc {
    private static final String LOINC_PREFIX = "http://loinc.org/";

    private final String code;

    public static Loinc LAB_RESULT = Loinc.of("92236-9");
    public static Loinc MEDICAL_IMAGE_STUDY = Loinc.of("18748-4");

    private Loinc(final String code) {
        this.code = code;
    }

    public static Loinc none() {
        return new Loinc(null);
    }

    public static Loinc of(final String code) {
        Validate.notBlank(code, "The Loinc code cannot be blank");
        return new Loinc(code);
    }

    public String getCode() {
        return code;
    }

    public String getFhirReference() {
        return LOINC_PREFIX + code;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Loinc loinc = (Loinc) o;
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

    public boolean matches(final String loinString) {
        return StringUtils.isNotBlank(loinString) && loinString.endsWith(code);
    }
}
