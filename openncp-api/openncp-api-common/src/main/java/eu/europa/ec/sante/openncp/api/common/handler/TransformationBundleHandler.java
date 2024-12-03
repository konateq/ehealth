package eu.europa.ec.sante.openncp.api.common.handler;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.r4.model.Bundle;

public class TransformationBundleHandler implements BundleHandler {

    private final TranscodeBundleHandler transcodeBundleHandler;
    private final TranslateBundleHandler translateBundleHandler;

    public TransformationBundleHandler(TranscodeBundleHandler transcodeBundleHandler, TranslateBundleHandler translateBundleHandler) {
        this.transcodeBundleHandler = Validate.notNull(transcodeBundleHandler, "transcodeBundleHandler must not be null");
        this.translateBundleHandler = Validate.notNull(translateBundleHandler, "translateBundleHandler must not be null");
    }

    @Override
    public Bundle handle(Bundle bundle, DispatchContext dispatchContext) {
        switch (dispatchContext.getNcpSide()) {
            case NCP_B:
                return  (dispatchContext.getRestOperationType() == RestOperationTypeEnum.CREATE)
                        ? transcodeBundleHandler.handle(bundle)
                        : translateBundleHandler.handle(bundle);
            case NCP_A:
                return  (dispatchContext.getRestOperationType() == RestOperationTypeEnum.CREATE)
                        ? translateBundleHandler.handle(bundle)
                        : transcodeBundleHandler.handle(bundle);
            default:
                throw new IllegalArgumentException("Unsupported NCP side " + dispatchContext.getNcpSide());
        }
    }
}
