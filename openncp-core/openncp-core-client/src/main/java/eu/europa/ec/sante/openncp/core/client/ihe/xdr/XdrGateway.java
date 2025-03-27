package eu.europa.ec.sante.openncp.core.client.ihe.xdr;


import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.RegisteredService;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.client.ihe.IhePortTypeFactory;
import eu.europa.ec.sante.openncp.core.client.transformation.DomUtils;
import eu.europa.ec.sante.openncp.core.common.dynamicdiscovery.DynamicDiscoveryService;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.IheConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xca.XCAConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.DocumentTransformationException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XDRException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xdr.DocumentRecipientPortType;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class XdrGateway {
    //This class implementation needs to be reviewed deeply
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final Logger logger = LoggerFactory.getLogger(XdrGateway.class);
    private final CDATransformationService cdaTransformationService;
    private final ConfigurationManager configurationManager;
    private final DynamicDiscoveryService discoveryService;
    private final IhePortTypeFactory ihePortTypeFactory;


    public XdrGateway(final CDATransformationService cdaTransformationService, final ConfigurationManager configurationManager, final DynamicDiscoveryService discoveryService, final IhePortTypeFactory ihePortTypeFactory) {
        this.cdaTransformationService = Validate.notNull(cdaTransformationService, "CDATransformationService must not be null");
        this.configurationManager = Validate.notNull(configurationManager, "configurationManager must not be null");
        this.discoveryService = Validate.notNull(discoveryService, " discoveryService must not be null");
        this.ihePortTypeFactory = Validate.notNull(ihePortTypeFactory, "ihePortTypeFactory must not be null");
    }

    /**
     * Provide And Register Document Set
     *
     * @param request
     * @param countryCode
     * @param docClassCode
     * @return
     * @throws RemoteException
     */
    public RegistryResponseType provideAndRegisterDocumentSet(final XdrRequest request, final String countryCode,
                                                              final Map<AssertionType, Assertion> assertionMap, final ClassCode docClassCode)
            throws RemoteException, XDRException {

        logger.info("[XDSb Repository] XDR Request: '{}', '{}', '{}'", assertionMap.get(AssertionType.HCP).getID(), countryCode, docClassCode);
        final RegistryResponseType response;
        final String submissionSetUuid = Constants.UUID_PREFIX + UUID.randomUUID();

        String endpointReference = null;
        switch (docClassCode) {
            case ED_CLASSCODE:
            case EDD_CLASSCODE:
                endpointReference = discoveryService.getEndpointUrl(countryCode.toLowerCase(Locale.ENGLISH), RegisteredService.DISPENSATION_SERVICE);
                break;
            default:
                logger.warn("Document Class Code: '{}' not supported!!! Endpoint cannot be loaded", docClassCode);
                break;
        }
        final DocumentRecipientPortType xdrPortType = ihePortTypeFactory.createXDRPort(configurationManager, endpointReference);

        //  Retrieving XDR document from request
        Document document = null;
        try {
            document = XMLUtil.parseContent(request.getCda());
        } catch (final Exception e) {
            logger.warn("Could not parse dispense", e);
        }
        final String languageCode = document != null ? getLanguageCode(document) : XDRConstants.EXTRINSIC_OBJECT.LANGUAGE_CODE_DEFAULT_VALUE;

        final ProvideAndRegisterDocumentSetRequest registerDocumentSetRequest = objectFactory.createProvideAndRegisterDocumentSetRequest();
        registerDocumentSetRequest.setSubmitObjectsRequest(objectFactory.createSubmitObjectsRequest());
        registerDocumentSetRequest.getSubmitObjectsRequest().setRegistryObjectList(objectFactory.createRegistryObjectList());

        //  XDS Document
        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final ExtrinsicObjectType extrinsicObject = makeExtrinsicObject(request, uuid, docClassCode, languageCode);
        registerDocumentSetRequest.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables()
                .add(objectFactory.createExtrinsicObject(extrinsicObject));

        final RegistryPackageType registryPackage = prepareRegistryPackage(request, docClassCode, submissionSetUuid, assertionMap);
        registerDocumentSetRequest.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables()
                .add(objectFactory.createRegistryPackage(registryPackage));

        final ClassificationType classification = prepareClassification(submissionSetUuid);
        registerDocumentSetRequest.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables()
                .add(objectFactory.createClassification(classification));

        final AssociationType1 association = prepareAssociation(uuid, submissionSetUuid);
        registerDocumentSetRequest.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables()
                .add(objectFactory.createAssociation(association));

        // XDR Document
        final ProvideAndRegisterDocumentSetRequest.Document xdrDocument = objectFactory.createProvideAndRegisterDocumentSetRequestDocument();
        xdrDocument.setId(uuid);

        final byte[] cdaBytes = request.getCda().getBytes(StandardCharsets.UTF_8);
        try {
            /* Validate CDA epSOS Friendly */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCdaDocument(request.getCda(), NcpSide.NCP_B, docClassCode, false);
            }
            if (!docClassCode.equals(ClassCode.EDD_CLASSCODE)) {
                final TMResponseStructure tmResponseStructure = cdaTransformationService.transcode(DomUtils.byteToDocument(cdaBytes), NcpSide.NCP_B);
                final String base64EncodedDocument = tmResponseStructure.getResponseCDA();
                final byte[] transformedCda = XMLUtil.documentToByteArray(Base64Util.decode(base64EncodedDocument));
                xdrDocument.setValue(transformedCda);

            } else {
                xdrDocument.setValue(cdaBytes);
            }
            /* Validate CDA epSOS Pivot */
            if (OpenNCPValidation.isValidationEnable()) {
                OpenNCPValidation.validateCdaDocument(new String(xdrDocument.getValue(), StandardCharsets.UTF_8), NcpSide.NCP_B, docClassCode, true);
            }
        } catch (final DocumentTransformationException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
            xdrDocument.setValue(cdaBytes);
        } catch (final Exception ex) {
            logger.error(null, ex);
        }
        registerDocumentSetRequest.getDocuments().add(xdrDocument);

        response = xdrPortType.documentRecipientProvideAndRegisterDocumentSetB(registerDocumentSetRequest);

        return response;
    }

    /**
     * @param name
     * @param value
     * @return
     */
    private Slot makeSlot(final String name, final String value) {
        final Slot sl = objectFactory.createSlot();
        sl.setName(name);
        sl.setValueList(objectFactory.createValueList());
        sl.getValueList().getValues().add(value);
        return sl;
    }

    /**
     * @param classificationScheme
     * @param classifiedObject
     * @param nodeRepresentation
     * @param value
     * @param name
     * @return
     */
    private ClassificationType makeClassification(final String classificationScheme, final String classifiedObject,
                                                  final String nodeRepresentation, final String value, final String name) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var classificationType = objectFactory.createClassificationType();
        classificationType.setId(uuid);
        classificationType.setNodeRepresentation(nodeRepresentation);
        classificationType.setClassificationScheme(classificationScheme);
        classificationType.setClassifiedObject(classifiedObject);
        classificationType.getSlots().add(makeSlot("codingScheme", value));
        classificationType.setName(objectFactory.createInternationalStringType());
        classificationType.getName().getLocalizedStrings().add(objectFactory.createLocalizedString());
        classificationType.getName().getLocalizedStrings().get(0).setValue(name);
        return classificationType;
    }

    /**
     * @param classificationScheme
     * @param classifiedObject
     * @param nodeRepresentation
     * @return
     */
    private ClassificationType makeClassification0(final String classificationScheme, final String classifiedObject, final String nodeRepresentation) {
        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var classificationType = objectFactory.createClassificationType();
        classificationType.setId(uuid);
        classificationType.setNodeRepresentation(nodeRepresentation);
        classificationType.setClassificationScheme(classificationScheme);
        classificationType.setClassifiedObject(classifiedObject);
        return classificationType;
    }

    /**
     * @param value
     * @return
     */
    private InternationalStringType makeInternationalString(final String value) {
        final var internationalStringType = new InternationalStringType();
        internationalStringType.getLocalizedStrings().add(objectFactory.createLocalizedString());
        internationalStringType.getLocalizedStrings().get(0).setValue(value);
        return internationalStringType;
    }

    /**
     * @param identificationScheme
     * @param registryObject
     * @param value
     * @param name
     * @return
     */
    private ExternalIdentifierType makeExternalIdentifier(final String identificationScheme, final String registryObject,
                                                          final String value, final String name) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final ExternalIdentifierType ex = objectFactory.createExternalIdentifierType();
        ex.setId(uuid);
        ex.setIdentificationScheme(identificationScheme);
        ex.setObjectType(XDRConstants.REGREP_EXT_IDENT);
        ex.setRegistryObject(registryObject);
        ex.setValue(value);
        ex.setName(objectFactory.createInternationalStringType());
        ex.getName().getLocalizedStrings().add(objectFactory.createLocalizedString());
        ex.getName().getLocalizedStrings().get(0).setValue(name);
        return ex;
    }

    /**
     * @param request
     * @param uuid
     * @param docClassCode
     * @return
     */
    private ExtrinsicObjectType makeExtrinsicObject(final XdrRequest request, final String uuid, final ClassCode docClassCode, final String language) {
        return makeExtrinsicObject(request, uuid, docClassCode, language, Boolean.FALSE);
    }

    /**
     * @param request
     * @param uuid
     * @param docClassCode
     * @param isPDF
     * @return
     */
    private ExtrinsicObjectType makeExtrinsicObject(final XdrRequest request, final String uuid, final ClassCode docClassCode, final String language, final Boolean isPDF) {

        if (Boolean.TRUE.equals(isPDF)) {
            // A.R. isPDF unfinished...
            logger.warn("PDF document will be processed, but this is not fully supported by current implementation");
        }

        final ExtrinsicObjectType result = objectFactory.createExtrinsicObjectType();
        final PatientDemographics patient = request.getPatient();
        final var patientId = patient.getIdList().get(0);

        // Attributes handling: identifier, mimeType, objectType and Status.
        result.setId(uuid);
        result.setMimeType(Constants.MIME_TYPE);
        result.setObjectType(XCAConstants.XDS_DOC_ENTRY_CLASSIFICATION_NODE);
        result.setStatus(IheConstants.REGREP_STATUSTYPE_APPROVED);

        // rim:Slot
        result.getSlots().add(makeSlot(XDRConstants.EXTRINSIC_OBJECT.CREATION_TIME,
                DateUtil.getCurrentTimeUTC(XDRConstants.EXTRINSIC_OBJECT.DATE_FORMAT)));
        result.getSlots().add(makeSlot(XDRConstants.EXTRINSIC_OBJECT.LANGUAGE_CODE_STR, language));
        result.getSlots().add(makeSlot(XDRConstants.EXTRINSIC_OBJECT.SOURCE_PATIENT_ID, patientId.toString()));

        /*
         * Classification
         */
        // Healthcare facility code
        result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.HEALTHCAREFACILITY_CODE_SCHEME, uuid,
                request.getCountryCode(), XDRConstants.EXTRINSIC_OBJECT.HEALTHCAREFACILITY_CODE_VALUE, request.getCountryName()));

        // Practice Setting code
        result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.PRACTICE_SETTING_CODE_SCHEME, uuid,
                Constants.NOT_USED_FIELD, XDRConstants.EXTRINSIC_OBJECT.PRACTICE_SETTING_CODE_NODEREPR, Constants.NOT_USED_FIELD));

        // Confidentiality Code
        result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.CONFIDENTIALITY_CODE_SCHEME, uuid,
                XDRConstants.EXTRINSIC_OBJECT.CONFIDENTIALITY_CODE_NODEREPR, XDRConstants.EXTRINSIC_OBJECT.CONFIDENTIALITY_CODE_VALUE,
                XDRConstants.EXTRINSIC_OBJECT.CONFIDENTIALITY_CODE_STR));

        // eHDSI Class Code
        switch (docClassCode) {
            case ED_CLASSCODE:
                //  urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME, uuid,
                        ClassCode.ED_CLASSCODE.getCode(), XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_VALUE, XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_ED_STR));
                break;
            case EDD_CLASSCODE:
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME, uuid,
                        ClassCode.EDD_CLASSCODE.getCode(), XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_VALUE,
                        XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_EDD_STR));
                break;
            default:
                logger.warn("Document Class Code: '{}' not supported!!! ClassCodeScheme cannot be loaded", docClassCode);
                break;
        }

        //  eHDSI Format Code - urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d
        switch (docClassCode) {
            case ED_CLASSCODE:
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.FormatCode.FORMAT_CODE_SCHEME, uuid,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.Dispensation.EpsosPivotCoded.NODE_REPRESENTATION,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.Dispensation.EpsosPivotCoded.CODING_SCHEME,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.Dispensation.EpsosPivotCoded.DISPLAY_NAME));
                break;
            case EDD_CLASSCODE:
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.FormatCode.FORMAT_CODE_SCHEME, uuid,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.DispensationDiscard.PivotCoded.NODE_REPRESENTATION,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.DispensationDiscard.PivotCoded.CODING_SCHEME,
                        XDRConstants.EXTRINSIC_OBJECT.FormatCode.DispensationDiscard.PivotCoded.DISPLAY_NAME));
                break;
            default:
                logger.warn("Document Class Code: '{}' not supported!!! FormatCodeScheme cannot be loaded", docClassCode);
                break;
        }

        //  External Identifiers
        //  XDSDocumentEntry.PatientId
        result.getExternalIdentifiers().add(makeExternalIdentifier(XDRConstants.EXTRINSIC_OBJECT.XDSDOCENTRY_PATID_SCHEME,
                uuid, patientId.toString(), XDRConstants.EXTRINSIC_OBJECT.XDSDOCENTRY_PATID_STR));

        /* XDSDocument.UniqueId */
        result.getExternalIdentifiers().add(makeExternalIdentifier(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME,
                uuid, request.getCdaId(), XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_STR));

        // Version Info not managed, since IHE simulator does not support it, and the message is still valid at EVS.
        // result.setVersionInfo(ofRim.createVersionInfoType())
        // result.getVersionInfo().setVersionName(XDRConstants.EXTRINSIC_OBJECT.VERSION_INFO)

        // Other elements not defined in 3.4.2
        // result.setHome(Constants.HOME_COMM_ID) and result.setLid(uuid)

        // sourcePatientInfo
        final Slot slId = makeSlot(XDRConstants.EXTRINSIC_OBJECT.SRC_PATIENT_INFO_STR, "PID-3|" + patientId);
        slId.getValueList().getValues().add("PID-5|" + patient.getFamilyName() + "^" + patient.getGivenName());
        slId.getValueList().getValues().add("PID-7|" + new SimpleDateFormat("yyyyMMddkkmmss.SSSZZZZ", Locale.ENGLISH).format(patient.getBirthDate()));
        if (patient.getAdministrativeGender() != null) {
            slId.getValueList().getValues().add("PID-8|" + patient.getAdministrativeGender().getValue());
        }
        result.getSlots().add(slId);

        // eHDSI Type Code
        switch (docClassCode) {
            case ED_CLASSCODE:
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.TypeCode.TYPE_CODE_SCHEME,
                        uuid, XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensation.NODE_REPRESENTATION,
                        XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensation.CODING_SCHEME,
                        XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensation.DISPLAY_NAME));
                break;
            case EDD_CLASSCODE:
                result.getClassifications().add(makeClassification(XDRConstants.EXTRINSIC_OBJECT.TypeCode.TYPE_CODE_SCHEME,
                        uuid, XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensationDiscard.NODE_REPRESENTATION,
                        XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensationDiscard.CODING_SCHEME,
                        XDRConstants.EXTRINSIC_OBJECT.TypeCode.EDispensationDiscard.DISPLAY_NAME));
                break;
            default:
                logger.warn("Document Class Code: '{}' not supported!!! TypeCodeScheme cannot be loaded.", docClassCode);
                break;
        }
        return result;
    }

    /**
     * @param request
     * @param docClassCode
     * @param submissionSetUuid
     * @return
     */
    private RegistryPackageType prepareRegistryPackage(final XdrRequest request, final ClassCode docClassCode, final String submissionSetUuid,
                                                       final Map<AssertionType, Assertion> assertionMap) {

        final var registryPackageType = objectFactory.createRegistryPackageType();
        final var patientId = request.getPatient().getIdList().get(0);

        registryPackageType.setId(submissionSetUuid);
        registryPackageType.setObjectType(XDRConstants.REGISTRY_PACKAGE.OBJECT_TYPE_UUID);

        registryPackageType.getSlots().add(makeSlot(XDRConstants.REGISTRY_PACKAGE.SUBMISSION_TIME_STR,
                DateUtil.getDateByDateFormat(XDRConstants.REGISTRY_PACKAGE.SUBMISSION_TIME_FORMAT)));
        registryPackageType.setName(makeInternationalString(getNameFromClassCode(docClassCode)));
        registryPackageType.setDescription(makeInternationalString(getDescriptionFromClassCode(docClassCode)));

        final ClassificationType classification;
        classification = makeClassification0(XDRConstants.REGISTRY_PACKAGE.AUTHOR_CLASSIFICATION_UUID, submissionSetUuid, "");
        classification.getSlots().add(makeSlot(XDRConstants.REGISTRY_PACKAGE.AUTHOR_INSTITUTION_STR,
                getAuthorInstitution(assertionMap.get(AssertionType.HCP))));
        classification.getSlots().add(makeSlot(XDRConstants.REGISTRY_PACKAGE.AUTHOR_PERSON_STR,
                getAuthorPerson(assertionMap.get(AssertionType.HCP))));
        registryPackageType.getClassifications().add(classification);

        registryPackageType.getClassifications().add(makeClassification(XDRConstants.REGISTRY_PACKAGE.CODING_SCHEME_UUID, submissionSetUuid,
                docClassCode.getCode(), XDRConstants.REGISTRY_PACKAGE.CODING_SCHEME_VALUE, getSubmissionSetFromClassCode(docClassCode)));

        registryPackageType.getExternalIdentifiers().add(makeExternalIdentifier(XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_UNIQUEID_SCHEME,
                submissionSetUuid, request.getSubmissionSetId(), XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_UNIQUEID_STR));

        registryPackageType.getExternalIdentifiers().add(makeExternalIdentifier(XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_PATIENTID_SCHEME,
                submissionSetUuid, patientId.toString(), XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_PATIENTID_STR));

        // The XDSSubmissionSet.SourceId value should be the OID of the sender (e.g. HOME_COMMUNITY_ID),
        // according to ITI TF-3, Table 4.1-6, but not all OID prefixes are supported in IHE validator.
        registryPackageType.getExternalIdentifiers().add(makeExternalIdentifier(XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_SOURCEID_SCHEME,
                submissionSetUuid, XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_SOURCEID_VALUE,
                XDRConstants.REGISTRY_PACKAGE.XDSSUBMSET_SOURCEID_STR));
        return registryPackageType;
    }

    /**
     * @param submissionSetUuid
     * @return
     */
    private ClassificationType prepareClassification(final String submissionSetUuid) {

        final var classificationType = objectFactory.createClassificationType();
        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        classificationType.setId(uuid);
        classificationType.setClassificationNode(XDRConstants.SUBMISSION_SET_CLASSIFICATION.CLASSIFICATION_NODE_UUID);
        classificationType.setClassifiedObject(submissionSetUuid);

        return classificationType;
    }

    /**
     * @param targetObject
     * @param submissionSetUuid
     * @return
     */
    private AssociationType1 prepareAssociation(final String targetObject, final String submissionSetUuid) {

        final String uuid = Constants.UUID_PREFIX + UUID.randomUUID();
        final var associationType1 = objectFactory.createAssociationType1();
        associationType1.setId(uuid);
        associationType1.setAssociationType(XDRConstants.REGREP_HAS_MEMBER);
        associationType1.setSourceObject(submissionSetUuid);
        associationType1.setTargetObject(targetObject);
        associationType1.getSlots().add(makeSlot(XDRConstants.SUBMISSION_SET_STATUS_STR, XDRConstants.ORIGINAL_STR));

        return associationType1;
    }

    /**
     * Obtains the AuthorInstitution information, namely Name and ID from the assertion.
     * And builds an HL7 V2.5 representation of the information.
     *
     * @param clinicianAssertion the clinical assertion.
     * @return an HL7 V2.5 representation of the obtained information.
     */
    private String getAuthorInstitution(final Assertion clinicianAssertion) {

        final String result;

        var organization = "General Hospital";
        var organizationId = "1.2.3.4.5.6.7.8.9.1789.45";

        final List<AttributeStatement> attrStatements;
        final List<Attribute> attrs;

        attrStatements = clinicianAssertion.getAttributeStatements();

        if (attrStatements.size() != 1) {
            return null;
        }

        attrs = attrStatements.get(0).getAttributes();

        for (final Attribute attr : attrs) {
            if (attr.getName().equals("urn:oasis:names:tc:xspa:1.0:subject:organization")) {
                organization = Objects.requireNonNull(attr.getAttributeValues().get(0).getDOM()).getTextContent();
            }
            if (StringUtils.equals(attr.getName(), "urn:oasis:names:tc:xspa:1.0:subject:organization-id")) {
                organizationId = Objects.requireNonNull(attr.getAttributeValues().get(0).getDOM()).getTextContent();
            }
        }

        if (organizationId.startsWith(Constants.OID_PREFIX)) {
            result = organization + "^^^^^^^^^" + organizationId.split(":")[2];
        } else if (organizationId.startsWith(Constants.HL7II_PREFIX)) {
            final String[] organizationIds = organizationId.split(":");
            result = organization + "^^^^^&" + organizationIds[2] + "&ISO^^^^" + organizationIds[3];
        } else {
            result = organization + "^^^^^^^^^" + organizationId;
        }

        return result;
    }

    /**
     * Obtains the AuthorPerson information, namely the Author Identifier and Assigning AuthorityId .
     * Then builds an HL7 V2.5 representation of the information.
     *
     * @param clinicianAssertion the clinical assertion.
     * @return an HL7 V2.5 representation of the obtained information.
     */
    private String getAuthorPerson(final Assertion clinicianAssertion) {

        final String result;

        var authorIdentifier = "Welby Marcus";
        var assigningAuthorityId = "1.2.3.4.5.6.7.8.9.1789.45";

        final List<AttributeStatement> attrStatements;
        final List<Attribute> attrs;

        attrStatements = clinicianAssertion.getAttributeStatements();

        if (attrStatements.size() != 1) {
            return null;
        }
        attrs = attrStatements.get(0).getAttributes();

        for (final Attribute attr : attrs) {
            if (attr.getName().equals("urn:oasis:names:tc:xspa:1.0:subject:subject-id")) {
                authorIdentifier = Objects.requireNonNull(attr.getAttributeValues().get(0).getDOM()).getTextContent();
            }
            if (StringUtils.equals(attr.getName(), "urn:oasis:names:tc:xspa:1.0:subject:organization-id")) {
                assigningAuthorityId = Objects.requireNonNull(attr.getAttributeValues().get(0).getDOM()).getTextContent();
            }
        }

        if (assigningAuthorityId.startsWith(Constants.OID_PREFIX)) {
            result = authorIdentifier + "^^^&" + assigningAuthorityId.split(":")[2] + "&ISO";
        } else if (assigningAuthorityId.startsWith(Constants.HL7II_PREFIX)) {
            final String[] assigningAuthorityIds = assigningAuthorityId.split(":");
            result = authorIdentifier + "^^^&" + assigningAuthorityIds[2] + ":" + assigningAuthorityIds[3] + "&ISO";
        } else {
            result = authorIdentifier + "^^^&" + assigningAuthorityId + "&ISO";
        }

        return result;
    }

    /**
     * @param classCode
     * @return
     */
    private String getNameFromClassCode(final ClassCode classCode) {

        switch (classCode) {
            case ED_CLASSCODE:
                return XDRConstants.REGISTRY_PACKAGE.NAME_ED;
            case EDD_CLASSCODE:
                return "Medication Dispensation Discard";
            default:
                logger.error("Class code does not have a matching name!");
                return null;
        }
    }

    /**
     * @param classCode
     * @return
     */
    private String getDescriptionFromClassCode(final ClassCode classCode) {

        switch (classCode) {
            case ED_CLASSCODE:
                return XDRConstants.REGISTRY_PACKAGE.DESCRIPTION_ED;
            case EDD_CLASSCODE:
                return "Description of Medication Dispensation Discard";
            default:
                logger.error("Class code does not have a matching description!");
                return "Class Code does not have a matching description!";
        }
    }

    /**
     * @param classCode
     * @return
     */
    private String getSubmissionSetFromClassCode(final ClassCode classCode) {

        switch (classCode) {
            case ED_CLASSCODE:
                return XDRConstants.REGISTRY_PACKAGE.NAME_ED;
            case EDD_CLASSCODE:
                return "Medication Dispensation Discard";
            default:
                logger.error("Class Code does not have a matching submission set designation!");
                return "Class Code does not have a matching submission set designation!";
        }
    }

    /**
     * Util method retrieving the language code from a CDA document.
     *
     * @param document - CDA document as DOM object.
     * @return ISO language code of the document.
     */
    private static String getLanguageCode(final Document document) {
        final List<Node> nodeList = XMLUtil.getNodeList(document, "ClinicalDocument/languageCode");
        String languageCode = XDRConstants.EXTRINSIC_OBJECT.LANGUAGE_CODE_DEFAULT_VALUE;
        for (final Node node : nodeList) {
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getAttributes().getNamedItem("code") != null) {
                languageCode = node.getAttributes().getNamedItem("code").getTextContent();
            }
        }
        return StringUtils.trim(languageCode);
    }
}
