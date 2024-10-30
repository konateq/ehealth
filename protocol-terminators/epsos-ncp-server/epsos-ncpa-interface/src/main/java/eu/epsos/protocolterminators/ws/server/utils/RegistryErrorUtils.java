package eu.epsos.protocolterminators.ws.server.utils;

import java.util.Arrays;
import java.util.Optional;

import eu.epsos.protocolterminators.ws.server.common.RegistryErrorSeverity;
import eu.europa.ec.sante.ehdsi.constant.error.ErrorCode;
import eu.europa.ec.sante.ehdsi.constant.error.ITMTSAMError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class RegistryErrorUtils {

    private static final OMFactory omFactory = OMAbstractFactory.getOMFactory();
    private static final ObjectFactory ofRs = new ObjectFactory();

    public static void addErrorMessage(RegistryErrorList registryErrorList, ErrorCode errorCode, String codeContext, RegistryErrorSeverity severity) {
        registryErrorList.getRegistryError().add(createErrorMessage(errorCode.getCode(), codeContext, null, severity));
    }

    public static void addErrorMessage(RegistryErrorList registryErrorList, ErrorCode errorCode, String codeContext, Exception e, RegistryErrorSeverity severity) {
        registryErrorList.getRegistryError().add(createErrorMessage(errorCode.getCode(), codeContext, Arrays.stream(Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElse(e).getStackTrace())
                .findFirst()
                .map(StackTraceElement::toString)
                .orElse(StringUtils.EMPTY), severity));
    }

    public static void addErrorOMMessage(OMNamespace ons, OMElement registryErrorList, ErrorCode errorCode, String codeContext, RegistryErrorSeverity severity) {
        registryErrorList.addChild(createErrorOMMessage(ons, errorCode.getCode(), codeContext, null, severity));
    }

    public static void addErrorOMMessage(OMNamespace ons, OMElement registryErrorList, ErrorCode errorCode, String codeContext, Exception e, RegistryErrorSeverity severity) {
        registryErrorList.addChild(createErrorOMMessage(ons, errorCode.getCode(), codeContext, Arrays.stream(Optional.ofNullable(ExceptionUtils.getRootCause(e)).orElse(e).getStackTrace())
                .findFirst()
                .map(StackTraceElement::toString)
                .orElse(StringUtils.EMPTY), severity));
    }

    public static void addErrorOMMessage(OMNamespace ons, OMElement registryErrorList, ITMTSAMError error, String operationType, RegistryErrorSeverity severity) {
        registryErrorList.addChild(createErrorOMMessage(ons,
                error.getCode(),
                error.getDescription(),
                "ECDATransformationHandler.Error." + operationType + "(" + error.getCode() + " / " + error.getDescription() + ")",
                severity));
    }

    private static RegistryError createErrorMessage(String errorCode, String codeContext, String location, RegistryErrorSeverity severity) {

        var registryError = ofRs.createRegistryError();
        registryError.setErrorCode(errorCode);
        registryError.setLocation(location);
        registryError.setSeverity(severity.getText());
        registryError.setCodeContext(codeContext);
        return registryError;
    }

    private static OMElement createErrorOMMessage(OMNamespace ons, String errorCode, String codeContext, String location, RegistryErrorSeverity severity) {

        var registryError = omFactory.createOMElement("RegistryError", ons);
        registryError.addAttribute(omFactory.createOMAttribute("codeContext", null, codeContext));
        registryError.addAttribute(omFactory.createOMAttribute("errorCode", null, errorCode));
        String aux = severity != null ? severity.getText() : null;
        registryError.addAttribute(omFactory.createOMAttribute("severity", null, aux));
        registryError.addAttribute(omFactory.createOMAttribute("location", null, location));
        return registryError;
    }
}
