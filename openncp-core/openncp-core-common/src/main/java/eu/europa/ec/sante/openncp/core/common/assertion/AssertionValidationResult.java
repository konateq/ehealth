package eu.europa.ec.sante.openncp.core.common.assertion;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Domain
public interface AssertionValidationResult {
    AssertionValidationStatus getStatus();

    AssertionDetails getAssertionDetails();

    List<AssertionValidationDetail> getValidationDetails();

    default boolean isCorrectType() {
        return getStatus() != AssertionValidationStatus.DIFFERENT_TYPE;
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

    static AssertionValidationResult forDifferentAssertionType(final AssertionDetails assertionDetails) {
        return ImmutableAssertionValidationResult.builder()
                .status(AssertionValidationStatus.DIFFERENT_TYPE)
                .assertionDetails(assertionDetails)
                .build();
    }


}
