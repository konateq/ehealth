package eu.europa.ec.sante.openncp.api.common.handler;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class TransformationBundleHandler implements BundleHandler {

    private final TranscodeBundleHandler transcodeBundleHandler;
    private final TranslateBundleHandler translateBundleHandler;

    public TransformationBundleHandler(final TranscodeBundleHandler transcodeBundleHandler, final TranslateBundleHandler translateBundleHandler) {
        this.transcodeBundleHandler = Validate.notNull(transcodeBundleHandler, "transcodeBundleHandler must not be null");
        this.translateBundleHandler = Validate.notNull(translateBundleHandler, "translateBundleHandler must not be null");
    }

    @Override
    public Bundle handle(final Bundle bundle, final DispatchContext dispatchContext) {
        Validate.notNull(bundle, "bundle must not be null");
        Validate.notNull(dispatchContext, "dispatchContext must not be null");

        final RestOperationTypeEnum restOperationTypeEnum = dispatchContext.getHapiRestOperationType()
                .orElseThrow(() -> new RuntimeException("The HAPI RestOperationType must be present to be able to do the transformation handling"));

        switch (dispatchContext.getNcpSide()) {
            case NCP_B:
                return (restOperationTypeEnum == RestOperationTypeEnum.CREATE)
                        ? transcodeBundleHandler.handle(bundle)
                        : translateBundleHandler.handle(bundle);
            case NCP_A:
                return (restOperationTypeEnum == RestOperationTypeEnum.CREATE)
                        ? translateBundleHandler.handle(bundle)
                        : transcodeBundleHandler.handle(bundle);
            default:
                throw new IllegalArgumentException("Unsupported NCP side " + dispatchContext.getNcpSide());
        }
    }
}
