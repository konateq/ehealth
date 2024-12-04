package eu.europa.ec.sante.openncp.core.client.ihe.xdr;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XDRException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Represents a Document Source Actor, from the IHE XDR (Cross-enterprise Document Reliable Interchange) Profile.
 */
@Service
public final class XdrDocumentSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(XdrDocumentSource.class);

    private static final String ERROR_SEVERITY_ERROR = "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error";

    final XDSbRepositoryServiceInvoker xDSbRepositoryServiceInvoker;

    public XdrDocumentSource(final XDSbRepositoryServiceInvoker xDSbRepositoryServiceInvoker) {
        this.xDSbRepositoryServiceInvoker = Validate.notNull(xDSbRepositoryServiceInvoker, "XDSbRepositoryServiceInvoker cannot be null");
    }

    /**
     * Implements the necessary mechanisms to discard a medication document next to the XDR Document Recipient Actor.
     *
     * @param request     - XDR request encapsulating the CDA and it's Metadata.
     * @param countryCode - Country code of the requesting country in ISO format.
     */
    public XdrResponse discard(final XdrRequest request, final String countryCode,
                                      final Map<AssertionType, Assertion> assertionMap) throws XDRException {

        return provideAndRegisterDocSet(request, countryCode, assertionMap, ClassCode.EDD_CLASSCODE);
    }

    /**
     * Implements the necessary mechanisms to dispense a medication document next to the XDR Document Recipient Actor.
     *
     * @param request     - XDR request encapsulating the CDA and it's Metadata.
     * @param countryCode - Country code of the requesting country in ISO format.
     */
    public XdrResponse initialize(final XdrRequest request, final String countryCode,
                                         final Map<AssertionType, Assertion> assertionMap) throws XDRException {

        return provideAndRegisterDocSet(request, countryCode, assertionMap, ClassCode.ED_CLASSCODE);
    }

    /**
     * Implements the necessary mechanisms to provide and register a document next to the XDR Document Recipient Actor.
     *
     * @param request     - XDR request encapsulating the CDA and it's Metadata.
     * @param countryCode - Country code of the requesting country in ISO format.
     */
    public XdrResponse provideAndRegisterDocSet(final XdrRequest request, final String countryCode,
                                                final Map<AssertionType, Assertion> assertionMap, final ClassCode docClassCode)
            throws XDRException {

        final RegistryResponseType response;

        try {
            response = xDSbRepositoryServiceInvoker.provideAndRegisterDocumentSet(request, countryCode, assertionMap, docClassCode);
            if (response.getRegistryErrorList() != null) {
                final var registryErrorList = response.getRegistryErrorList();
                processRegistryErrors(registryErrorList);
            }
        } catch (final RemoteException e) {
            throw new XDRException(getErrorCode(docClassCode), e);
        }
        return XdrResponseDts.newInstance(response);
    }

    /**
     * Processes all the registry errors (if existing), from the XDR response.
     *
     * @param registryErrorList the Registry Error List to be processed.
     */
    private void processRegistryErrors(final RegistryErrorList registryErrorList) throws XDRException {

        if (registryErrorList == null) {
            return;
        }

        final List<RegistryError> errorList = registryErrorList.getRegistryError();
        if (errorList == null) {
            return;
        }

        var hasError = false;

        for (final RegistryError error : errorList) {
            final String errorCode = error.getErrorCode();
            final String value = error.getValue();
            final String location = error.getLocation();
            final String severity = error.getSeverity();
            final String codeContext = error.getCodeContext();

            LOGGER.error("errorCode='{}'\ncodeContext='{}'\nlocation='{}'\nseverity='{}'\n'{}'\n",
                    errorCode, codeContext, location, severity, value);

            if (StringUtils.equals(ERROR_SEVERITY_ERROR,severity)) {
                hasError = true;
            }

            final OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.getErrorCode(errorCode);
            if(openncpErrorCode == null){
                LOGGER.warn("No EHDSI error code found in the XDR response for : {}", errorCode);
            }

            if (hasError) {
                    throw new XDRException(openncpErrorCode, codeContext, location);
            }
        }
    }

    private static OpenNCPErrorCode getErrorCode(final ClassCode classCode) {
        switch (classCode){
            case ED_CLASSCODE:
                return OpenNCPErrorCode.ERROR_ED_GENERIC;
            case EDD_CLASSCODE:
                return OpenNCPErrorCode.ERROR_ED_DISCARD_FAILED;
            default:
                break;
        }
        return OpenNCPErrorCode.ERROR_GENERIC;
    }

}
