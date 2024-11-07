package eu.europa.ec.sante.openncp.core.server.nc.mock.xcpd.impl;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NIException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCPDErrorCode;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.AnswerNotAvailableException;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterfaceWithDemographics;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.XCPDNIException;
import eu.europa.ec.sante.openncp.core.server.nc.mock.common.NationalConnectorGateway;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Mock implementation of the PatientSearchInterface, to be replaced nationally.
 */
@Service
public class PatientSearchMockImpl extends NationalConnectorGateway implements PatientSearchInterfaceWithDemographics {

    private static final String GENDER = "administrativeGender";
    private static final String BIRTH_DATE = "birthDate";
    private static final String BIRTH_DATE_YEAR = BIRTH_DATE + ".year";
    private static final String BIRTH_DATE_MONTH = BIRTH_DATE + ".month";
    private static final String BIRTH_DATE_DAY = BIRTH_DATE + ".day";
    private static final String CITY = "city";
    private static final String COUNTRY = "country";
    private static final String EMAIL = "email";
    private static final String FAMILY_NAME = "familyName";
    private static final String GIVEN_NAME = "givenName";
    private static final String POSTAL_CODE = "postalCode";
    private static final String STREET = "street";
    private static final String TELEPHONE = "telephone";
    private final Logger logger = LoggerFactory.getLogger(PatientSearchMockImpl.class);

    PatientDemographics patientDemographics;

    @Override
    public String getPatientId(String citizenNumber) {
        if (logger.isInfoEnabled()) {
            logger.info("[National Infrastructure Mock] Get Patient ID: '{}'", citizenNumber);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PatientDemographics> getPatientDemographics(List<PatientId> idList) throws NIException {

        if (logger.isInfoEnabled()) {
            logger.info("[National Infrastructure Mock] Get Patient Demographics: '{}'", getPatientDemographics().toString());
        }
        List<PatientDemographics> result = new ArrayList<>(1);

        // Identifying mocked Patient File.
        StringBuilder patientFile = new StringBuilder();
        patientFile.append(Constants.EPSOS_PROPS_PATH).append("integration/");
        PatientId id = null;

        // Building Path File.XCPDServiceImpl
        for (PatientId patientId : idList) {
            logger.info("[National Infrastructure Mock] Searching patients with ID: '{}' and Assigning Authority: '{}'", patientId.getExtension(), patientId.getRoot());
            File rootDir = new File(patientFile + patientId.getRoot());
            //  Patient ID 999999 will throw an Exception from National Connector
            if (StringUtils.equals(patientId.getExtension(), "999999")) {
                throw new NIException(OpenNCPErrorCode.ERROR_PI_REGISTRY_NOT_AVAILABLE, "National Registry Not Available");
            }

            if (rootDir.exists()) {
                File extensionFile = new File(patientFile + patientId.getRoot() + File.separator + patientId.getExtension() + ".properties");

                if (extensionFile.exists()) {
                    patientFile.append(patientId.getRoot()).append(File.separator).append(patientId.getExtension()).append(".properties");
                    id = patientId;
                    break;
                }
            }
        }

        if (id == null) {
            logger.info("[National Infrastructure Mock] Patient with ID: '{}' not found: ", idList.get(0));
            return new ArrayList<>(0);
        }

        // Load Patient properties file.
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(patientFile.toString())) {
            logger.info("[National Infrastructure Mock] Loading Patient information from filesystem resource: '{}'", patientFile);
            properties.load(inputStream);

            PatientDemographics patient = new PatientDemographics();
            String newRoot = id.getRoot();
            id.setRoot(newRoot + ".1000");
            patient.setIdList(Collections.singletonList(id));
            patient.setAdministrativeGender(PatientDemographics.Gender.parseGender(properties.getProperty(GENDER)));
            int year = Integer.parseInt(properties.getProperty(BIRTH_DATE_YEAR));
            int month = Integer.parseInt(properties.getProperty(BIRTH_DATE_MONTH));
            int day = Integer.parseInt(properties.getProperty(BIRTH_DATE_DAY));
            Calendar birth = Calendar.getInstance();
            birth.set(Calendar.DAY_OF_MONTH, day);
            birth.set(Calendar.YEAR, year);
            birth.set(Calendar.MONTH, month);
            patient.setBirthDate(birth.getTime());
            patient.setCity(properties.getProperty(CITY));
            patient.setCountry(properties.getProperty(COUNTRY));
            patient.setEmail(properties.getProperty(EMAIL));
            patient.setFamilyName(properties.getProperty(FAMILY_NAME));
            patient.setGivenName(properties.getProperty(GIVEN_NAME));
            patient.setPostalCode(properties.getProperty(POSTAL_CODE));
            patient.setStreetAddress(properties.getProperty(STREET));
            patient.setTelephone(properties.getProperty(TELEPHONE));
            result.add(patient);

            //Create an error scenario to send two patients with the same id
            if (StringUtils.equals(idList.get(0).getExtension(), "PI_MULTIPLE_MATCHES")) {
                result.add(patient);
            }

            logger.info("[National Infrastructure Mock] Patient with ID: '{}' found.", id.getFullId());

        } catch (Exception e) {
            logger.error("[National Infrastructure Mock] Patient Not Found Exception: '{}'", e.getMessage(), e);
            throw new AnswerNotAvailableException(OpenNCPErrorCode.ERROR_PI_INTERNAL_ERROR);
        }

        return result;
    }

    public PatientDemographics getPatientDemographics() {
        return patientDemographics;
    }

    @Override
    public void setPatientDemographics(PatientDemographics patientDemographics) {

        if (logger.isInfoEnabled()) {
            logger.info("[National Infrastructure Mock] Set Patient Demographics: '{}'", patientDemographics.toString());
        }
        this.patientDemographics = patientDemographics;
    }
}
