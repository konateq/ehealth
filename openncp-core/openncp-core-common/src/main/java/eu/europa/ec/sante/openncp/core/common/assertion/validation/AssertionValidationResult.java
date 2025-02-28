package eu.europa.ec.sante.openncp.core.common.assertion.validation;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import org.apache.commons.lang3.StringUtils;

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
        return (List<String>) getFailedValidationDetails()
                .map(assertionValidationDetail -> String.format("Failed assertion [%s] for validation key [%s] with message [%s] and throwable [%s]",
                        getAssertionDetails()
                                .map(assertionDetails -> assertionDetails.getAssertionType())
                                .map(Enum::name)
                                .orElse("Unknown"),
                        assertionValidationDetail.getKey(),
                        assertionValidationDetail.getMessage().orElse(StringUtils.EMPTY),
                        assertionValidationDetail.getError().orElse(null)))
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
