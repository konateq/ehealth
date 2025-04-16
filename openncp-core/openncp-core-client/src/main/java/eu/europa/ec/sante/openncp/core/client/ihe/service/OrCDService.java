package eu.europa.ec.sante.openncp.core.client.ihe.service;

import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.ihe.xca.XcaGateway;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.RetrieveDocumentSetResponse;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Mathias Ghys <mathias.ghys@ext.ec.europa.eu>
 */
@Service
public class OrCDService {

    private final XcaGateway xcaGateway;

    public OrCDService(final XcaGateway xcaGateway) {
        this.xcaGateway = Validate.notNull(xcaGateway, "xcaInitGateway cannot be null");
    }

    public QueryResponse list(final PatientId pid,
                              final String countryCode,
                              final List<GenericDocumentCode> documentCodes,
                              final FilterParams filterParams,
                              final Map<AssertionType, Assertion> assertionMap) throws OpenNCPException {

        return xcaGateway.crossGatewayQuery(pid, countryCode, documentCodes, filterParams, assertionMap,
                RegisteredService.ORCD_SERVICE.getServiceName());
    }

    public RetrieveDocumentSetResponse.DocumentResponse retrieve(final XDSDocument document,
                                                                 final String homeCommunityId,
                                                                 final String countryCode,
                                                                 final String targetLanguage,
                                                                 final Map<AssertionType, Assertion> assertionMap)
            throws OpenNCPException {
        return xcaGateway.crossGatewayRetrieve(document, homeCommunityId, countryCode, targetLanguage, assertionMap, RegisteredService.ORCD_SERVICE.getServiceName());
    }
}
