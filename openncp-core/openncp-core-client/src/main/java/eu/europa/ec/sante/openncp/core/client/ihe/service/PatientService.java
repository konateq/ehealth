package eu.europa.ec.sante.openncp.core.client.ihe.service;

import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.ihe.xca.XcaInitGateway;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PatientService {

    final XcaInitGateway xcaInitGateway;

    public PatientService(final XcaInitGateway xcaInitGateway) {
        this.xcaInitGateway = Validate.notNull(xcaInitGateway, "XcaInitGateway cannot be null");
    }

    public QueryResponse list(final PatientId pid, final String countryCode, final GenericDocumentCode documentCode,
                                     final Map<AssertionType, Assertion> assertionMap) throws XCAException {

        return xcaInitGateway.crossGatewayQuery(pid, countryCode, List.of(documentCode), null, assertionMap,
                RegisteredService.PATIENT_SERVICE.getServiceName());
    }

    public RetrieveDocumentSetResponseType.DocumentResponse retrieve(final XDSDocument document,
                                                                            final String homeCommunityId,
                                                                            final String countryCode,
                                                                            final String targetLanguage,
                                                                            final Map<AssertionType, Assertion> assertionMap) throws XCAException {

        return xcaInitGateway.crossGatewayRetrieve(document, homeCommunityId, countryCode, targetLanguage, assertionMap, RegisteredService.PATIENT_SERVICE.getServiceName());
    }
}
