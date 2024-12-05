package eu.europa.ec.sante.openncp.core.common.fhir.audit.eventhandler;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Pointcut;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.FhirSupportedResourceType;
import eu.europa.ec.sante.openncp.core.common.fhir.context.RequestMatcher;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Domain
public interface AuditableEvent {
    Pointcut getPointcut();

    @Value.Default
    default Instant getTimestamp() {
        return Instant.now();
    }

    FhirContext getFhirContext();

    DispatchContext getDispatchContext();

    Optional<IBaseResource> getResource();

    Optional<Throwable> getThrowable();

    default Optional<Bundle> getResourceAsBundle() {
        return getResource()
                .filter(resource -> resource instanceof Bundle)
                .map(resource -> (Bundle) resource);
    }

    default Optional<String> getResourceType() {
        return getResource().map(IBase::fhirType);
    }

    default boolean resourceIsOfType(final String... types) {
        if (types == null || types.length == 0) {
            return false;
        }
        return resourceIsOfType(Arrays.asList(types));
    }

    default boolean resourceIsOfType(final FhirSupportedResourceType... types) {
        if (types == null || types.length == 0) {
            return false;
        }

        final List<String> supportedResourceValues = Arrays.stream(types)
                .map(FhirSupportedResourceType::getRequestMatcher)
                .map(RequestMatcher::getPath)
                .collect(Collectors.toList());
        return resourceIsOfType(supportedResourceValues);
    }

    default boolean resourceIsOfType(final List<String> types) {
        Validate.notNull(types, "types must not be null");
        if (types.isEmpty()) {
            return false;
        }
        return getResource()
                .map(IBase::fhirType)
                .map(type -> types.stream().anyMatch(type::equalsIgnoreCase))
                .orElse(false);
    }

    default Set<String> extractResourceIds() {
        return extractResourceIds((resource) -> true);
    }

    default List<IBaseResource> extractResources(final Predicate<IBaseResource> predicate) {
        Validate.notNull(predicate, "The predicate to extract resources must be null.");

        return getResource().map(resource -> {
            if (resource instanceof Bundle) {
                final Bundle bundle = (Bundle) resource;
                return bundle.getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .filter(predicate)
                        .map(filteredResource -> (IBaseResource) filteredResource)
                        .collect(Collectors.toList());
            } else {
                return List.of(resource);
            }
        }).orElse(Collections.emptyList());
    }

    default Set<String> extractResourceIds(final Predicate<IBaseResource> predicate) {
        Validate.notNull(predicate, "The predicate to extract resource id's must be null.");

        return getResource().map(resource -> {
            if (resource instanceof Bundle) {

                final Bundle bundle = (Bundle) resource;
                return bundle.getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .filter(predicate)
                        .map(Resource::getIdElement)
                        .map(idElement -> getDispatchContext().createFullyQualifiedResourceReference(idElement))
                        .collect(Collectors.toSet());
            } else {
                return Set.of(getDispatchContext().createFullyQualifiedResourceReference(resource.getIdElement()));
            }
        }).orElse(Collections.emptySet());
    }
}
