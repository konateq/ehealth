package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import eu.europa.ec.sante.openncp.common.util.MoreCollectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.Optional;

public class CustomFhirResource {
    private final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass;
    private final String profile;

    private CustomFhirResource(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass) {
        this.customResourceClass = customResourceClass;
        profile = extractProfile(customResourceClass);
    }

    private static String extractProfile(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass) {
        if (customResourceClass != null) {
            return Arrays.stream(customResourceClass.getAnnotations())
                    .filter(annotation -> annotation instanceof ResourceDef)
                    .map(annotation -> (ResourceDef) annotation)
                    .map(ResourceDef::profile)
                    .collect(MoreCollectors.exactlyOne("ResourceDef", String.format("class [%s]", customResourceClass)));
        }
        return null;
    }

    public Optional<Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource>> getCustomResourceClass() {
        return Optional.ofNullable(customResourceClass);
    }

    public Optional<String> getProfile() {
        return Optional.ofNullable(profile);
    }

    public boolean isCustomResource() {
        return customResourceClass != null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("customResourceClass", customResourceClass)
                .append("profile", profile)
                .toString();
    }

    public static CustomFhirResource none() {
        return new CustomFhirResource(null);
    }

    public static CustomFhirResource of(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customType) {
        Validate.notNull(customType, "customType must not be blank");
        return new CustomFhirResource(customType);
    }
}
