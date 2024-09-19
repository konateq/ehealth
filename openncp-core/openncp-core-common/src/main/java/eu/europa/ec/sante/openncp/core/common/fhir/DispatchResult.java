package eu.europa.ec.sante.openncp.core.common.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.r4.model.Bundle;

public class DispatchResult<T> {
    private T result;

    public DispatchResult(T result) {
        this.result = result;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public static DispatchResult<Bundle> of(final Bundle bundleResult) {
        return new DispatchResult<>(bundleResult);
    }

    public static DispatchResult<MethodOutcome> of(final MethodOutcome methodOutcome) {
        return new DispatchResult<>(methodOutcome);
    }
}
