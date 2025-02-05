package eu.europa.ec.sante.openncp.core.common.assertion;

import eu.europa.ec.sante.openncp.common.immutables.Domain;

import java.util.Optional;

@Domain
public interface AssertionValidationDetail {
    AssertionValidationKey getKey();

    AssertionValidationDetailStatus getStatus();

    Optional<Throwable> getError();

    Optional<String> getMessage();

    static AssertionValidationDetail passed(final AssertionValidationKey assertionValidationKey) {
       return ImmutableAssertionValidationDetail.builder()
                .key(assertionValidationKey)
                .status(AssertionValidationDetailStatus.PASSED)
                .build();
    }
}
