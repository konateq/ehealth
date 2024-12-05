package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.DiagnosticReportLabMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.PatientMyHealthEu;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.ServiceRequestLabMyHealthEu;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.util.MoreCollectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum FhirSupportedResourceType {
    PATIENT(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("Patient")
            .build(),
            CustomResource.of(PatientMyHealthEu.class)
    ),
    COMPOSITION(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("Composition")
            .build(),
            CustomResource.of(CompositionLabReportMyHealthEu.class)
    ),
    SERVICE_REQUEST(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("ServiceRequest")
            .build(), CustomResource.of(ServiceRequestLabMyHealthEu.class)
    ),
    DIAGNOSTIC_REPORT(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("DiagnosticReport")
            .build(),
            CustomResource.of(DiagnosticReportLabMyHealthEu.class)
    ),
    BUNDLE(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("Bundle")
            .build(),
            CustomResource.none()
    ),
    DOCUMENT_REFERENCE(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("DocumentReference")
            .build(),
            CustomResource.none()
    ),
    LAB_RESULT(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("DocumentReference")
            .addParameter(Parameter.of("type", Loinc.LAB_RESULT))
            .build(),
            CustomResource.none()
    ),
    MEDICAL_IMAGING(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("DocumentReference")
            .addParameter(Parameter.of("type", Loinc.MEDICAL_IMAGE_STUDY))
            .build(),
            CustomResource.none()
    ),
    METADATA(ImmutableRequest.builder()
            .method(HttpMethod.ANY)
            .path("metadata")
            .build(),
            CustomResource.none()
    );

    private final Request request;
    private final CustomResource customResource;

    FhirSupportedResourceType(final Request request, final CustomResource customResource) {
        this.request = Validate.notNull(request, "request cannot be null");
        this.customResource = Validate.notNull(customResource, "customType cannot be null");
    }
    
    public static Optional<FhirSupportedResourceType> ofRequestPath(final RequestDetails requestDetails) {
        return Arrays.stream(FhirSupportedResourceType.values())
                .filter(supportedResource -> supportedResource.getRequest().isEqualTo(requestDetails))
                .findFirst();
    }

    public Request getRequest() {
        return request;
    }

    public CustomResource getCustomType() {
        return customResource;
    }

    public static class CustomResource {
        private final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass;
        private final String profile;

        private CustomResource(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customResourceClass) {
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

        public static CustomResource none() {
            return new CustomResource(null);
        }

        public static CustomResource of(final Class<? extends eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource> customType) {
            Validate.notNull(customType, "customType must not be blank");
            return new CustomResource(customType);
        }
    }

    @Domain
    public interface Request {
        HttpMethod getMethod();

        String getPath();

        List<Parameter<?>> getParameters();

        default Optional<Parameter<?>> getParameter(final String parameterName) {
            return getParameters().stream().filter(parameter -> parameter.getName().equals(parameterName)).findFirst();
        }

        default boolean isEqualTo(final RequestDetails requestDetails) {
            final boolean isSameMethod = getMethod() == HttpMethod.ANY || getMethod().name().equals(requestDetails.getOperation());
            final boolean isSamePath = isSameMethod && getPath().equals(requestDetails.getResourceName());
            final boolean hasCorrectParam = isSamePath && getParameters().stream().allMatch(parameter ->
                    requestDetails.getParameters().entrySet().stream()
                            .anyMatch(entry -> parameter.getName().equals(entry.getKey()) && parameter.matches(entry.getValue()))
            );

            return isSameMethod && isSamePath && hasCorrectParam;
        }
    }

    public interface Parameter<T> {
        String getName();

        T getValue();

        boolean matches(String[] values);

        static StringParameter of(final String name, final String value) {
            return ImmutableStringParameter.of(name, value);
        }

        static LoincParameter of(final String name, final Loinc value) {
            return ImmutableLoincParameter.of(name, value);
        }


    }

    @Domain
    public interface StringParameter extends Parameter<String> {
        @Override
        String getName();

        @Override
        String getValue();

        @Override
        default boolean matches(final String[] values) {
            return getValue().equals(String.join(",", values));
        }
    }

    @Domain
    public interface LoincParameter extends Parameter<Loinc> {
        @Override
        String getName();

        @Override
        Loinc getValue();

        @Override
        default boolean matches(final String[] values) {
            final String joinedValues = String.join(",", values);
            return getValue().getCode().equals(joinedValues);
        }
    }

    public enum HttpMethod {
        GET, POST, ANY
    }
}
