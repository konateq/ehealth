package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.*;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xcpd.XCPDConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import org.apache.commons.lang3.RandomStringUtils;

import java.text.SimpleDateFormat;
import java.util.UUID;

public final class XcpdMessageBuilder {

    private XcpdMessageBuilder() {
    }

    public static PRPAIN201305UV02 build(final PatientDemographics patientDemographics, final String dstHomeCommunityId) {
        /*
         * PRPAIN201305UV02 Patient Demographic request creation
         */
        final PRPAIN201305UV02 request = new PRPAIN201305UV02();

        final ObjectFactory of = new ObjectFactory();

        // Set ITS_VERSION element
        request.setITSVersion(XCPDConstants.ITS_VERSION);

        // Set id of the message
        request.setId(of.createII());
        request.getId().setRoot(UUID.randomUUID().toString());
        request.getId().setExtension(RandomStringUtils.randomNumeric(10));

        // Set creationTime
        request.setCreationTime(of.createTS());
        request.getCreationTime().setValue(DateUtil.getCurrentTimeUTC());

        // Set versionCode
        request.setVersionCode(of.createCS());
        request.getVersionCode().setCode(XCPDConstants.HL7_VERSION);

        // Set interactionId
        request.setInteractionId(of.createII());
        request.getInteractionId().setRoot(XCPDConstants.INTERACTION_IDS_NAMESPACE);
        request.getInteractionId().setExtension(XCPDConstants.PATIENT_DISCOVERY_REQUEST);

        // Set processingCode
        request.setProcessingCode(of.createCS());
        request.getProcessingCode().setCode(XCPDConstants.PROCESSING_CODE);

        // Set processingModeCode
        request.setProcessingModeCode(of.createCS());
        request.getProcessingModeCode().setCode(XCPDConstants.PROCESSING_MODE_CODE);

        // Set acceptAckCode
        request.setAcceptAckCode(of.createCS());
        request.getAcceptAckCode().setCode(XCPDConstants.ACCEPT_ACK_CODE);

        // Create receiver
        final MCCIMT000100UV01Receiver receiver = new MCCIMT000100UV01Receiver();
        request.getReceiver().add(receiver);
        final CommunicationFunctionType rTypeCode = CommunicationFunctionType.RCV;
        request.getReceiver().get(0).setTypeCode(rTypeCode);

        // Create receiver/device
        final MCCIMT000100UV01Device rDevice = new MCCIMT000100UV01Device();
        request.getReceiver().get(0).setDevice(rDevice);
        final EntityClassDevice rClassCode = EntityClassDevice.DEV;
        request.getReceiver().get(0).getDevice().setClassCode(rClassCode);
        request.getReceiver().get(0).getDevice().setDeterminerCode(XCPDConstants.DETERMINER_CODE_INSTANCE);

        // Set receiver/device/id
        request.getReceiver().get(0).getDevice().getId().add(of.createII());
        request.getReceiver().get(0).getDevice().getId().get(0).setRoot(dstHomeCommunityId);

        // Create receiver/device/asAgent
        final MCCIMT000100UV01Agent rAgent = new MCCIMT000100UV01Agent();
        request.getReceiver().get(0).getDevice().setAsAgent(of.createMCCIMT000100UV01DeviceAsAgent(rAgent));
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().getClassCode().add(XCPDConstants.CLASS_CODE_AGNT);

        // Create receiver/device/asAgent/representedOrganization
        final MCCIMT000100UV01Organization rOrganization = new MCCIMT000100UV01Organization();
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().setRepresentedOrganization(of.createMCCIMT000100UV01AgentRepresentedOrganization(rOrganization));
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().setClassCode(XCPDConstants.CLASS_CODE_ORG);
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().setDeterminerCode(XCPDConstants.DETERMINER_CODE_INSTANCE);

        // Set receiver/device/asAgent/representedOrganization/id
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().add(of.createII());
        request.getReceiver().get(0).getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().get(0).setRoot(dstHomeCommunityId);

        // Create sender
        final MCCIMT000100UV01Sender sender = new MCCIMT000100UV01Sender();
        request.setSender(sender);
        final CommunicationFunctionType sTypeCode = CommunicationFunctionType.SND;
        request.getSender().setTypeCode(sTypeCode);

        // Create sender/device
        final MCCIMT000100UV01Device sDevice = new MCCIMT000100UV01Device();
        request.getSender().setDevice(sDevice);
        final EntityClassDevice sClassCode = EntityClassDevice.DEV;
        request.getSender().getDevice().setClassCode(sClassCode);
        request.getSender().getDevice().setDeterminerCode(XCPDConstants.DETERMINER_CODE_INSTANCE);

        // Set sender/device/id
        request.getSender().getDevice().getId().add(of.createII());
        request.getSender().getDevice().getId().get(0).setRoot(Constants.HOME_COMM_ID);

        // Create sender/device/asAgent
        final MCCIMT000100UV01Agent sAgent = new MCCIMT000100UV01Agent();
        request.getSender().getDevice().setAsAgent(of.createMCCIMT000100UV01DeviceAsAgent(sAgent));
        request.getSender().getDevice().getAsAgent().getValue().getClassCode().add(XCPDConstants.CLASS_CODE_AGNT);

        // Create sender/device/asAgent/representedOrganization
        final MCCIMT000100UV01Organization sOrganization = new MCCIMT000100UV01Organization();
        request.getSender().getDevice().getAsAgent().getValue().setRepresentedOrganization(of.createMCCIMT000100UV01AgentRepresentedOrganization(sOrganization));
        request.getSender().getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().setClassCode(XCPDConstants.CLASS_CODE_ORG);
        request.getSender().getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().setDeterminerCode(XCPDConstants.DETERMINER_CODE_INSTANCE);

        // Set sender/device/asAgent/representedOrganization/id
        request.getSender().getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().add(of.createII());
        request.getSender().getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue().getId().get(0).setRoot(Constants.HOME_COMM_ID);

        // Create controlActProcess
        final PRPAIN201305UV02QUQIMT021001UV01ControlActProcess controlActProcess = new PRPAIN201305UV02QUQIMT021001UV01ControlActProcess();
        request.setControlActProcess(controlActProcess);
        final ActClassControlAct cClassCode = ActClassControlAct.CACT;
        request.getControlActProcess().setClassCode(cClassCode);
        final XActMoodIntentEvent moodCode = XActMoodIntentEvent.EVN;
        request.getControlActProcess().setMoodCode(moodCode);

        // Set controlActProcess/code
        request.getControlActProcess().setCode(of.createCD());
        request.getControlActProcess().getCode().setCode(XCPDConstants.CONTROL_ACT_PROCESS.CODE);
        request.getControlActProcess().getCode().setCodeSystemName(XCPDConstants.INTERACTION_IDS_NAMESPACE);

        // Create controlActProcess/authorOrPerformer
        final QUQIMT021001UV01AuthorOrPerformer aop = new QUQIMT021001UV01AuthorOrPerformer();
        request.getControlActProcess().getAuthorOrPerformer().add(aop);
        final XParticipationAuthorPerformer typeCode = XParticipationAuthorPerformer.AUT;
        request.getControlActProcess().getAuthorOrPerformer().get(0).setTypeCode(typeCode);

        // Set controlActProcess/authorOrPerformer/assignedPerson
        final COCTMT090100UV01AssignedPerson ap = new COCTMT090100UV01AssignedPerson();
        request.getControlActProcess().getAuthorOrPerformer().get(0).setAssignedPerson(of.createMFMIMT700711UV01AuthorOrPerformerAssignedPerson(ap));
        request.getControlActProcess().getAuthorOrPerformer().get(0).getAssignedPerson().getValue().setClassCode(XCPDConstants.CLASS_CODE_ASSIGNED);

        // Create controlActProcess/queryByParameter
        final PRPAMT201306UV02QueryByParameter queryByParameter = of.createPRPAMT201306UV02QueryByParameter();
        request.getControlActProcess().setQueryByParameter(of.createPRPAIN201306UV02MFMIMT700711UV01ControlActProcessQueryByParameter(queryByParameter));

        // Set controlActProcess/queryByParameter/queryId
        request.getControlActProcess().getQueryByParameter().getValue().setQueryId(of.createII());
        request.getControlActProcess().getQueryByParameter().getValue().getQueryId().setRoot(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_ID_ROOT);

        // Set controlActProcess/queryByParameter/statusCode
        request.getControlActProcess().getQueryByParameter().getValue().setStatusCode(of.createCS());
        request.getControlActProcess().getQueryByParameter().getValue().getStatusCode().setCode(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_STATUS_CODE);

        // Set controlActProcess/queryByParameter/responseModalityCode
        request.getControlActProcess().getQueryByParameter().getValue().setResponseModalityCode(of.createCS());
        request.getControlActProcess().getQueryByParameter().getValue().getResponseModalityCode().setCode(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_RESPONSE_MODALITY_CODE);

        // Set controlActProcess/queryByParameter/responsePriorityCode
        request.getControlActProcess().getQueryByParameter().getValue().setResponsePriorityCode(of.createCS());
        request.getControlActProcess().getQueryByParameter().getValue().getResponsePriorityCode().setCode(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_RESPONSE_PRIORITY_CODE);

        // Set controlActProcess/queryByParameter/matchCriterionList
        final PRPAMT201306UV02MatchCriterionList matchCriterionList = new PRPAMT201306UV02MatchCriterionList();
        request.getControlActProcess().getQueryByParameter().getValue().setMatchCriterionList(of.createPRPAMT201306UV02QueryByParameterMatchCriterionList(matchCriterionList));

        // Create controlActProcess/queryByParameter/parameterList
        final PRPAMT201306UV02ParameterList parameterList = new PRPAMT201306UV02ParameterList();
        request.getControlActProcess().getQueryByParameter().getValue().setParameterList(parameterList);

        if (patientDemographics.getAdministrativeGender() != null) {
            // Create controlActProcess/queryByParameter/parameterList/livingSubjectAdministrativeGender
            final PRPAMT201306UV02LivingSubjectAdministrativeGender administrativeGender = new PRPAMT201306UV02LivingSubjectAdministrativeGender();
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectAdministrativeGender().add(administrativeGender);

            // Set controlActProcess/queryByParameter/parameterList/livingSubjectAdministrativeGender/value
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectAdministrativeGender().get(0).getValue().add(of.createCE());
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectAdministrativeGender().get(0).getValue().get(0).setCode(patientDemographics.getAdministrativeGender().toString());

            // Set controlActProcess/queryByParameter/parameterList/livingSubjectAdministrativeGender/semanticsText
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectAdministrativeGender().get(0).setSemanticsText(of.createST());

            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectAdministrativeGender().get(0).getSemanticsText().getContent().add(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_ADMINISTRATIVE_GENDER_SEMANTICS);
        }

        if (patientDemographics.getBirthDate() != null) {

            final SimpleDateFormat birthTimeFormat = new SimpleDateFormat(DateUtil.DATE_FORMAT);
            // Create controlActProcess/queryByParameter/parameterList/livingSubjectBirthTime
            final PRPAMT201306UV02LivingSubjectBirthTime birthTime = new PRPAMT201306UV02LivingSubjectBirthTime();
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectBirthTime().add(birthTime);

            // Set controlActProcess/queryByParameter/parameterList/livingSubjectBirthTime/value
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectBirthTime().get(0).getValue().add(of.createIVLTS());
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectBirthTime().get(0).getValue().get(0).setValue(birthTimeFormat.format(patientDemographics.getBirthDate()));

            // Set controlActProcess/queryByParameter/parameterList/livingSubjectBirthTime/semanticsText
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectBirthTime().get(0).setSemanticsText(of.createST());
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectBirthTime().get(0).getSemanticsText().getContent().add(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_BIRTHDATE_SEMANTICS);
        }

        if (patientDemographics.getIdList() != null) {

            for (final PatientId patId : patientDemographics.getIdList()) {

                // Create controlActProcess/queryByParameter/parameterList/livingSubjectId
                final PRPAMT201306UV02LivingSubjectId id = new PRPAMT201306UV02LivingSubjectId();
                // Set controlActProcess/queryByParameter/parameterList/livingSubjectId/value
                final II ii = of.createII();
                ii.setRoot(patId.getRoot());
                ii.setExtension(patId.getExtension());
                id.getValue().add(ii);
                // Set controlActProcess/queryByParameter/parameterList/livingSubjectId/semanticsText
                id.setSemanticsText(of.createST());
                id.getSemanticsText().getContent().add(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_LIVING_SUBJECT_ID_SEMANTICS);

                request.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId().add(id);
            }
        }

        if ((patientDemographics.getFamilyName() != null) || (patientDemographics.getGivenName() != null)) {
            // Set controlActProcess/queryByParameter/parameterList/livingSubjectName
            final PRPAMT201306UV02LivingSubjectName name = new PRPAMT201306UV02LivingSubjectName();
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectName().add(name);

            // Create controlActProcess/queryByParameter/parameterList/livingSubjectName/value
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectName().get(0).getValue().add(of.createEN());

            if (patientDemographics.getFamilyName() != null) {
                // Set controlActProcess/queryByParameter/parameterList/livingSubjectName/value/family
                final EnFamily family = of.createEnFamily();
                family.getContent().add(patientDemographics.getFamilyName());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getLivingSubjectName().get(0).getValue().get(0).getContent().add(of.createENFamily(family));
            }
            if (patientDemographics.getGivenName() != null) {
                // Set controlActProcess/queryByParameter/parameterList/livingSubjectName/value/given
                final EnGiven given = of.createEnGiven();
                given.getContent().add(patientDemographics.getGivenName());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getLivingSubjectName().get(0).getValue().get(0).getContent().add(of.createENGiven(given));
            }

            // Set controlActProcess/queryByParameter/parameterList/livingSubjectName/semanticsText
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectName().get(0).setSemanticsText(of.createST());
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectName().get(0).getSemanticsText()
                    .getContent().add(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_LIVING_SUBJECT_NAME_SEMANTICS);
        }

        if ((patientDemographics.getCity() != null) || (patientDemographics.getCountry() != null)
                || (patientDemographics.getPostalCode() != null) || (patientDemographics.getStreetAddress() != null)) {

            // Create controlActProcess/queryByParameter/parameterList/patientAddress
            final PRPAMT201306UV02PatientAddress address = new PRPAMT201306UV02PatientAddress();
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList().getPatientAddress().add(address);

            // Create controlActProcess/queryByParameter/parameterList/patientAddress/value
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList().getPatientAddress().get(0).getValue().add(of.createAD());

            if (patientDemographics.getStreetAddress() != null) {
                // Set controlActProcess/queryByParameter/parameterList/patientAddress/value/streetName
                final AdxpStreetName street = of.createAdxpStreetName();
                street.getContent().add(patientDemographics.getStreetAddress());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getPatientAddress().get(0).getValue().get(0).getContent().add(of.createADStreetName(street));
            }
            if (patientDemographics.getCity() != null) {
                // Set controlActProcess/queryByParameter/parameterList/patientAddress/value/city
                final AdxpCity city = of.createAdxpCity();
                city.getContent().add(patientDemographics.getCity());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getPatientAddress().get(0).getValue().get(0).getContent().add(of.createADCity(city));
            }
            if (patientDemographics.getCountry() != null) {
                // Set controlActProcess/queryByParameter/parameterList/patientAddress/value/country
                final AdxpCountry country = of.createAdxpCountry();
                country.getContent().add(patientDemographics.getCountry());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getPatientAddress().get(0).getValue().get(0).getContent().add(of.createADCountry(country));
            }
            if (patientDemographics.getPostalCode() != null) {
                // Set controlActProcess/queryByParameter/parameterList/patientAddress/value/postalCode
                final AdxpPostalCode postalCode = of.createAdxpPostalCode();
                postalCode.getContent().add(patientDemographics.getPostalCode());
                request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                        .getPatientAddress().get(0).getValue().get(0).getContent().add(of.createADPostalCode(postalCode));
            }

            // Set controlActProcess/queryByParameter/parameterList/patientAddress/semanticsText
            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getPatientAddress().get(0).setSemanticsText(of.createST());

            request.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getPatientAddress().get(0).getSemanticsText().getContent().add(XCPDConstants.CONTROL_ACT_PROCESS.QUERY_BY_PARAMETER_PATIENT_ADDRESS_SEMANTICS);
        }

        return request;
    }
}
