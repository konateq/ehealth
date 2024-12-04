package eu.europa.ec.sante.openncp.core.client.ihe;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.client.api.*;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.QueryDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.QueryPatientOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.RetrieveDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.SubmitDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dts.*;
import eu.europa.ec.sante.openncp.core.client.ihe.service.*;
import eu.europa.ec.sante.openncp.core.client.ihe.xdr.XdrResponse;
import eu.europa.ec.sante.openncp.core.client.logging.LoggingSlf4j;
import eu.europa.ec.sante.openncp.core.common.constants.ihe.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XDRException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("iheClientService")
public class ClientServiceImpl implements ClientService {

    private static final String UNSUPPORTED_CLASS_CODE_SCHEME_EXCEPTION = "Unsupported Class Code scheme: ";
    private static final String UNSUPPORTED_CLASS_CODE_EXCEPTION = "Unsupported Class Code: ";

    private final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final IdentificationService identificationService;
    private final PatientService patientService;
    private final OrderService orderService;
    private final OrCDService orCDService;
    private final DispensationService dispensationService;

    public ClientServiceImpl(final IdentificationService identificationService,
                             final PatientService patientService,
                             final OrderService orderService,
                             final OrCDService orCDService,
                             final DispensationService dispensationService) {
        this.identificationService = Validate.notNull(identificationService, "IdentificationService cannot be null");
        this.patientService = Validate.notNull(patientService, "PatientService cannot be null");
        this.orderService = Validate.notNull(orderService, "OrderService cannot be null");
        this.orCDService = Validate.notNull(orCDService, "OrCDService cannot be null");
        this.dispensationService = Validate.notNull(dispensationService, "DispensationService cannot be null");
    }

    @Override
    public String submitDocument(final SubmitDocumentOperation submitDocumentOperation) {
        Validate.notNull(submitDocumentOperation);

        final String methodName = "submitDocument";
        LoggingSlf4j.start(logger, methodName);
        final SubmitDocumentResponse submitDocumentResponse = objectFactory.createSubmitDocumentResponse();
        try {
            /*  create XDR request */
            final SubmitDocumentRequest submitDocumentRequest = submitDocumentOperation.getRequest();
            final EpsosDocument submitDocument = submitDocumentRequest.getDocument();
            final PatientDemographics patientDemographics = submitDocumentRequest.getPatientDemographics();
            final String countryCode = submitDocumentRequest.getCountryCode();
            final GenericDocumentCode classCode = submitDocument.getClassCode();
            final Map<AssertionType, Assertion> assertionMap = submitDocumentOperation.getAssertions();
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateHCPAssertion(assertionMap.get(AssertionType.HCP), NcpSide.NCP_B);
            }
            if (!classCode.getSchema().equals(IheConstants.CLASSCODE_SCHEME)) {
                throw new ClientConnectorException(UNSUPPORTED_CLASS_CODE_SCHEME_EXCEPTION + classCode.getSchema());
            }
            final String classCodeNode = classCode.getNodeRepresentation();
            final String nodeRepresentation = submitDocument.getFormatCode().getNodeRepresentation();
            logger.info("[Document] ClassCode: '{}' NodeRepresentation: '{}'", classCodeNode, nodeRepresentation);
            //TODO: CDA as input needs to be validated according XSD, Schematron or Validators.
            final XdrResponse response;
            final var classCodeValue = ClassCode.getByCode(classCodeNode);
            switch (classCodeValue) {

                case ED_CLASSCODE:
                    if (StringUtils.equals(nodeRepresentation, "urn:eHDSI:ed:discard:2020")) {
                        response = dispensationService.discard(submitDocument, patientDemographics, countryCode, assertionMap);
                    } else {
                        response = dispensationService.initialize(submitDocument, patientDemographics, countryCode, assertionMap);
                    }
                    break;
                case EDD_CLASSCODE:
                    response = dispensationService.discard(submitDocument, patientDemographics, countryCode, assertionMap);
                    break;
                default:
                    throw new ClientConnectorException(UNSUPPORTED_CLASS_CODE_EXCEPTION + classCodeNode);
            }
            submitDocumentResponse.setResponseStatus(response.getResponseStatus());
        } catch (final XDRException | ParseException | RuntimeException ex) {
            LoggingSlf4j.error(logger, methodName, ex);
            throw new ClientConnectorException(ex);
        }
        LoggingSlf4j.end(logger, methodName);
        return submitDocumentResponse.getResponseStatus();
    }

    @Override
    public List<EpsosDocument> queryDocuments(final QueryDocumentOperation queryDocumentOperation) {
        final String methodName = "queryDocuments";
        LoggingSlf4j.start(logger, methodName);
        final QueryDocumentsResponse queryDocumentsResponse = objectFactory.createQueryDocumentsResponse();
        try {
            final PatientId patientId = queryDocumentOperation.getRequest().getPatientId();
            final String countryCode = queryDocumentOperation.getRequest().getCountryCode();
            final List<GenericDocumentCode> documentCodes = queryDocumentOperation.getRequest().getClassCode();
            final FilterParams filterParams = queryDocumentOperation.getRequest().getFilterParams();
            final Map<AssertionType, Assertion> assertionMap = queryDocumentOperation.getAssertions();
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateHCPAssertion(assertionMap.get(AssertionType.HCP), NcpSide.NCP_B);
            }

            final QueryResponse response;
            if (documentCodes.size() == 1) {
                final String classCode = documentCodes.get(0).getNodeRepresentation();
                switch (ClassCode.getByCode(classCode)) {
                    case PS_CLASSCODE:
                        response = patientService.list(PatientIdDts.toDataModel(patientId), countryCode,
                                                       GenericDocumentCodeDts.newInstance(documentCodes.get(0)), assertionMap);
                        break;
                    case EP_CLASSCODE:
                        response = orderService.list(PatientIdDts.toDataModel(patientId), countryCode,
                                                     GenericDocumentCodeDts.newInstance(documentCodes.get(0)), assertionMap);
                        break;
                    case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                    case ORCD_LABORATORY_RESULTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                    case ORCD_MEDICAL_IMAGES_CLASSCODE:
                        response = orCDService.list(PatientIdDts.toDataModel(patientId), countryCode,
                                                    GenericDocumentCodeDts.newInstance(documentCodes), FilterParamsDts.newInstance(filterParams),
                                                    assertionMap);
                        break;
                    default:
                        throw new ClientConnectorException(UNSUPPORTED_CLASS_CODE_EXCEPTION + Arrays.toString(documentCodes.toArray()));
                }
            } else {
                if (!documentCodes.contains(ClassCode.EP_CLASSCODE.getCode()) && !documentCodes.contains(ClassCode.PS_CLASSCODE.getCode())) {
                    response = orCDService.list(PatientIdDts.toDataModel(patientId), countryCode, GenericDocumentCodeDts.newInstance(documentCodes),
                                                FilterParamsDts.newInstance(filterParams), assertionMap);
                } else {
                    throw new ClientConnectorException("Invalid combination of document codes provided: only OrCD document codes can be combined.");
                }
            }

            if (response.getDocumentAssociations() != null && !response.getDocumentAssociations().isEmpty()) {
                queryDocumentsResponse.getReturn().addAll(DocumentDts.newInstance(response.getDocumentAssociations()));
            }
        } catch (final XCAException | RuntimeException ex) {
            LoggingSlf4j.error(logger, methodName, ex);
            throw new ClientConnectorException(ex);
        }
        LoggingSlf4j.end(logger, methodName);
        return queryDocumentsResponse.getReturn();
    }

    @Override
    public EpsosDocument retrieveDocument(final RetrieveDocumentOperation retrieveDocumentOperation) {

        final String methodName = "retrieveDocument";
        LoggingSlf4j.start(logger, methodName);
        final RetrieveDocumentResponse retrieveDocumentResponse;
        try {
            final RetrieveDocumentRequest retrieveDocumentRequest = retrieveDocumentOperation.getRequest();
            final String countryCode = retrieveDocumentRequest.getCountryCode();
            final DocumentId documentId = retrieveDocumentRequest.getDocumentId();
            final String homeCommunityId = retrieveDocumentRequest.getHomeCommunityId();
            final String targetLanguage = retrieveDocumentRequest.getTargetLanguage();

            final GenericDocumentCode genericDocumentCode = retrieveDocumentRequest.getClassCode();
            final eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode documentCode = GenericDocumentCodeDts.newInstance(
                    genericDocumentCode);
            final Map<AssertionType, Assertion> assertionMap = retrieveDocumentOperation.getAssertions();
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateHCPAssertion(assertionMap.get(AssertionType.HCP), NcpSide.NCP_B);
            }

            if (!documentCode.getSchema().equals(IheConstants.CLASSCODE_SCHEME)) {
                throw new ClientConnectorException(UNSUPPORTED_CLASS_CODE_SCHEME_EXCEPTION + documentCode.getSchema());
            }

            final var xdsDocument = XdsDocumentDts.newInstance(documentId);
            xdsDocument.setClassCode(documentCode);

            logger.info("[ClientConnector retrieveDocument()] homeCommunityId: '{}' targetLanguage: '{}'", homeCommunityId, targetLanguage);
            final ClassCode classCode = ClassCode.getByCode(documentCode.getValue());
            final RetrieveDocumentSetResponseType.DocumentResponse documentResponse;
            switch (classCode) {
                case PS_CLASSCODE:
                    documentResponse = patientService.retrieve(xdsDocument, homeCommunityId, countryCode, targetLanguage, assertionMap);
                    break;
                case EP_CLASSCODE:
                    documentResponse = orderService.retrieve(xdsDocument, homeCommunityId, countryCode, targetLanguage, assertionMap);
                    break;
                case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
                case ORCD_LABORATORY_RESULTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
                case ORCD_MEDICAL_IMAGES_CLASSCODE:
                    documentResponse = orCDService.retrieve(xdsDocument, homeCommunityId, countryCode, targetLanguage, assertionMap);
                    break;
                default:
                    throw new ClientConnectorException(UNSUPPORTED_CLASS_CODE_EXCEPTION + documentCode.getValue());
            }

            retrieveDocumentResponse = RetrieveDocumentResponseDts.newInstance(documentResponse);
        } catch (final XCAException | RuntimeException ex) {
            LoggingSlf4j.error(logger, methodName, ex);
            throw new ClientConnectorException(ex);
        }
        return retrieveDocumentResponse.getReturn();
    }

    @Override
    public List<PatientDemographics> queryPatient(final QueryPatientOperation queryPatientOperation) {
        final var methodName = "queryPatient";
        LoggingSlf4j.start(logger, methodName);
        final QueryPatientResponse queryPatientResponse = objectFactory.createQueryPatientResponse();

        try {
            final PatientDemographics patientDemographics = queryPatientOperation.getRequest().getPatientDemographics();
            final String countryCode = queryPatientOperation.getRequest().getCountryCode();
            final Map<AssertionType, Assertion> assertionMap = queryPatientOperation.getAssertions();

            final List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics> patientDemographicsList =
                    identificationService.findIdentityByTraits(
                    PatientDemographicsDts.toDataModel(patientDemographics), assertionMap, countryCode);

            final List<PatientDemographics> returnedPatientDemographics = PatientDemographicsDts.fromDataModel(patientDemographicsList);
            queryPatientResponse.getReturn().addAll(returnedPatientDemographics);
        } catch (final NoPatientIdDiscoveredException | ParseException | RuntimeException ex) {
            LoggingSlf4j.error(logger, methodName, ex);
            throw new ClientConnectorException(ex);
        }
        LoggingSlf4j.end(logger, methodName);
        return queryPatientResponse.getReturn();
    }

    @Override
    public String sayHello(final String who) {
        return "Hello " + who;
    }
}
