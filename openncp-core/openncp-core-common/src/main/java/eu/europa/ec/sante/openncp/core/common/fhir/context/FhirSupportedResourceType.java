package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import eu.europa.ec.sante.openncp.common.util.MoreCollectors;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.PatientMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.Optional;

public enum FhirSupportedResourceType {
    PATIENT(RestRequestPath.of("Patient"), CustomResource.of(PatientMyHealthEu.class)),
    COMPOSITION(RestRequestPath.of("Composition"), CustomResource.of(CompositionLabReportMyHealthEu.class)),
    SERVICE_REQUEST(RestRequestPath.of("ServiceRequest"), CustomResource.of(ServiceRequestLabMyHealthEu.class)),
    DIAGNOSTIC_REPORT(RestRequestPath.of("DiagnosticReport"), CustomResource.of(DiagnosticReportLabMyHealthEu.class)),
    BUNDLE(RestRequestPath.of("Bundle"), CustomResource.none()),
    DOCUMENT_REFERENCE(RestRequestPath.of("DocumentReference"), CustomResource.none()),
    METADATA(RestRequestPath.of("metadata"), CustomResource.none());

    private final RestRequestPath restRequestPath;
    private final CustomResource customResource;

    FhirSupportedResourceType(final RestRequestPath restRequestPath, final CustomResource customResource) {
        this.restRequestPath = Validate.notNull(restRequestPath, "restRequestPath cannot be null");
        this.customResource = Validate.notNull(customResource, "customType cannot be null");
    }

    public static Optional<FhirSupportedResourceType> ofRequestPath(final String requestPath) {
        return Arrays.stream(FhirSupportedResourceType.values())
                .filter(supportedResource -> supportedResource.getRestRequestPath().isEqualTo(requestPath))
                .findFirst();
    }

    public RestRequestPath getRestRequestPath() {
        return restRequestPath;
    }

    public CustomResource getCustomType() {
        return customResource;
    }

    public static class RestRequestPath {
        private final String restRequestPath;

        private RestRequestPath(final String restRequestPath) {
            Validate.notBlank(restRequestPath, "restRequestPath must not be blank");
            this.restRequestPath = restRequestPath;
        }

        public String getValue() {
            return restRequestPath;
        }

        public boolean isEqualTo(final String restRequestPath) {
            return this.restRequestPath.equals(restRequestPath);
        }

        public static RestRequestPath of(final String restRequestPath) {
            return new RestRequestPath(restRequestPath);
        }
    }

    public static class CustomResource {
        private final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass;
        private final String profile;

        private CustomResource(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass) {
            this.customResourceClass = customResourceClass;
            this.profile = extractProfile(customResourceClass);
        }

        private String extractProfile(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass) {
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

        public static CustomResource none() {
            return new CustomResource(null);
        }

        public static CustomResource of(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customType) {
            Validate.notNull(customType, "customType must not be blank");
            return new CustomResource(customType);
        }
    }
}
