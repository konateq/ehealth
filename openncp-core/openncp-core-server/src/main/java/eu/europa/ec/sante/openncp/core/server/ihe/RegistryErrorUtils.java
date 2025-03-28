package eu.europa.ec.sante.openncp.core.server.ihe;

import eu.europa.ec.sante.openncp.common.error.ErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.RegistryErrorSeverity;
import eu.europa.ec.sante.openncp.core.common.tsam.error.ITMTSAMError;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.ObjectFactory;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryError;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RegistryErrorList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.Optional;

public class RegistryErrorUtils {
    private static final ObjectFactory xdsObjectFactory = new ObjectFactory();

    public static void addErrorMessage(final RegistryErrorList registryErrorList, final ErrorCode errorCode, final String codeContext, final RegistryErrorSeverity severity) {
        registryErrorList.getRegistryErrors().add(createErrorMessage(errorCode.getCode(), codeContext, null, severity));
    }

    public static void addErrorMessage(final RegistryErrorList registryErrorList, final ErrorCode errorCode, final String codeContext, final Exception e, final RegistryErrorSeverity severity) {
        registryErrorList.getRegistryErrors().add(createErrorMessage(errorCode.getCode(), codeContext, Arrays.stream(Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElse(e).getStackTrace())
                .findFirst()
                .map(StackTraceElement::toString)
                .orElse(StringUtils.EMPTY), severity));
    }

    public static void addErrorMessage(final RegistryErrorList registryErrorList, final ITMTSAMError error, final String operationType, final RegistryErrorSeverity severity) {
        registryErrorList.getRegistryErrors().add(
                createErrorMessage(error.getCode(), error.getDescription(), "ECDATransformationHandler.Error." + operationType + "(" + error.getCode() + " / " + error.getDescription() + ")", severity)
        );
    }

    private static RegistryError createErrorMessage(final String errorCode, final String codeContext, final String location, final RegistryErrorSeverity severity) {

        final RegistryError registryError = xdsObjectFactory.createRegistryError();
        registryError.setErrorCode(errorCode);
        registryError.setLocation(location);
        registryError.setSeverity(severity.getText());
        registryError.setCodeContext(codeContext);
        return registryError;
    }
}
