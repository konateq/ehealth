package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Domain
public interface AssertionValidationResult {
    AssertionValidationStatus getStatus();

    Optional<AssertionDetails> getAssertionDetails();

    List<AssertionValidationDetail> getValidationDetails();

    default boolean isIgnored() {
        return getStatus() != AssertionValidationStatus.IGNORED;
    }

    default Stream<AssertionValidationDetail> getFailedValidationDetails() {
        return getValidationDetails().stream()
                .filter(assertionValidationDetail -> assertionValidationDetail.getStatus() == AssertionValidationDetailStatus.FAILED);
    }

    default List<String> getFailedValidationMessages() {
        return getFailedValidationDetails()
                .map(assertionValidationDetail -> String.format("Failed assertion validation [%s] with message [%s] and throwable [%s]", assertionValidationDetail.getKey(), assertionValidationDetail.getMessage().orElse(null), assertionValidationDetail.getError().orElse(null)))
                .collect(Collectors.toList());
    }

    static AssertionValidationResult differentAssertion(final AssertionDetails assertionDetails, final AssertionType requestedType) {
        final AssertionType actualType = assertionDetails.getAssertionType();

        return ImmutableAssertionValidationResult.builder()
                .status(AssertionValidationStatus.IGNORED)
                .assertionDetails(assertionDetails)
                .addValidationDetail(ImmutableAssertionValidationDetail.builder()
                        .key(AssertionValidationKey.DIFFERENT_ASSERTION)
                        .status(AssertionValidationDetailStatus.IGNORED)
                        .message(String.format("Requested assertion type [%s], actual assertion type [%s]", requestedType, actualType))
                        .build())
                .build();
    }


}
