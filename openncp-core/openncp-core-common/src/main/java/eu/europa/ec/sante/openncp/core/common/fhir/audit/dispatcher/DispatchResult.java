package eu.europa.ec.sante.openncp.core.common.fhir.audit.dispatcher;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.apache.commons.lang3.Validate;
import org.immutables.value.Value;

import java.util.Optional;

@Domain
public interface DispatchResult {
    DispatchMetadata getDispatchingMetadata();

    Optional<String> getMessage();

    DispatchStatus getDispatchStatus();

    @Value.Default
    default DispatchError getError() {
        return DispatchError.none();
    }

    default boolean isSuccess() {
        return getDispatchStatus() == DispatchStatus.SUCCESS;

    }

    default boolean isFailure() {
        return !isSuccess();
    }

    static DispatchResult success(final DispatchMetadata dispatchMetadata) {
        Validate.notNull(dispatchMetadata, "dispatchMetadata must not be null");

        return ImmutableDispatchResult.builder()
                .dispatchingMetadata(dispatchMetadata)
                .dispatchStatus(DispatchStatus.SUCCESS)
                .build();
    }

    static DispatchResult success(final DispatchMetadata dispatchMetadata, final String message) {
        Validate.notNull(dispatchMetadata, "dispatchMetadata must not be null");
        Validate.notNull(message, "message must not be null");

        return ImmutableDispatchResult.builder()
                .dispatchingMetadata(dispatchMetadata)
                .dispatchStatus(DispatchStatus.SUCCESS)
                .message(message)
                .build();
    }

    static DispatchResult failure(final DispatchMetadata dispatchMetadata, final Throwable cause) {
        Validate.notNull(dispatchMetadata, "dispatchMetadata must not be null");
        Validate.notNull(cause, "throwable cause must not be null");

        final DispatchError dispatchError = DispatchError.of(cause);
        return ImmutableDispatchResult.builder()
                .dispatchingMetadata(dispatchMetadata)
                .dispatchStatus(DispatchStatus.FAILED)
                .error(dispatchError)
                .build();
    }

    static DispatchResult failure(final DispatchMetadata dispatchMetadata, final String errorMessage, final Throwable cause) {
        Validate.notNull(dispatchMetadata, "dispatchMetadata must not be null");
        Validate.notNull(errorMessage, "error message must not be null");
        Validate.notNull(cause, "throwable cause must not be null");

        final DispatchError dispatchError = DispatchError.of(errorMessage, cause);
        return ImmutableDispatchResult.builder()
                .dispatchingMetadata(dispatchMetadata)
                .dispatchStatus(DispatchStatus.FAILED)
                .error(dispatchError)
                .message(errorMessage)
                .build();
    }

    static DispatchResult failure(final DispatchMetadata dispatchMetadata, final String errorMessage) {
        Validate.notNull(dispatchMetadata, "dispatchMetadata must not be null");
        Validate.notNull(errorMessage, "error message must not be null");

        final DispatchError dispatchError = DispatchError.of(errorMessage);
        return ImmutableDispatchResult.builder()
                .dispatchingMetadata(dispatchMetadata)
                .dispatchStatus(DispatchStatus.FAILED)
                .error(dispatchError)
                .message(errorMessage)
                .build();
    }

    enum DispatchStatus {
        SUCCESS,
        FAILED
    }

    @Domain
    interface DispatchError {

        @Value.Default
        default String getErrorMessage() {
            return "No error message supplied.";
        }

        Optional<Throwable> getThrowable();

        static DispatchError of(final String errorMessage, final Throwable throwable) {
            Validate.notNull(errorMessage, "error message must not be null");
            Validate.notNull(throwable, "throwable must not be null");

            return ImmutableDispatchError.builder().errorMessage(errorMessage).throwable(throwable).build();
        }

        static DispatchError of(final Throwable throwable) {
            Validate.notNull(throwable, "throwable must not be null");

            return ImmutableDispatchError.builder().throwable(throwable).errorMessage(throwable.getMessage()).build();
        }

        static DispatchError of(final String errorMessage) {
            Validate.notNull(errorMessage, "error message must not be null");

            return ImmutableDispatchError.builder().errorMessage(errorMessage).build();
        }

        static DispatchError none() {
            return ImmutableDispatchError.builder().build();
        }
    }
}
