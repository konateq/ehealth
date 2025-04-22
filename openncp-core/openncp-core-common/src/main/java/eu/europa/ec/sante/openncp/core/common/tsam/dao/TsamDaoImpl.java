package eu.europa.ec.sante.openncp.core.common.tsam.dao;

import eu.europa.ec.sante.openncp.core.common.tsam.CodeConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.RetrievedConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.TSAMException;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.*;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TSAMError;
import eu.europa.ec.sante.openncp.core.common.tsam.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TsamDaoImpl implements TsamDao {

    private final Logger logger = LoggerFactory.getLogger(TsamDaoImpl.class);
    private static final String CURRENT_STATUS = "current";

    private static final String VALID_STATUS = "valid";
    private final CodeSystemConceptRepository codeSystemConceptRepository;

    private final CodeSystemVersionRepository codeSystemVersionRepository;

    private final CodeSystemRepository codeSystemRepository;

    private final DesignationRepository designationRepository;

    private final ValueSetRepository valueSetRepository;

    private final ValueSetVersionRepository valueSetVersionRepository;

    private final TranscodingAssociationRepository transcodingAssociationRepository;

    public TsamDaoImpl(final CodeSystemConceptRepository codeSystemConceptRepository,
                       final CodeSystemVersionRepository codeSystemVersionRepository,
                       final CodeSystemRepository codeSystemRepository,
                       final DesignationRepository designationRepository,
                       final ValueSetRepository valueSetRepository,
                       final ValueSetVersionRepository valueSetVersionRepository,
                       final TranscodingAssociationRepository transcodingAssociationRepository) {
        this.codeSystemConceptRepository = codeSystemConceptRepository;
        this.codeSystemVersionRepository = codeSystemVersionRepository;
        this.codeSystemRepository = codeSystemRepository;
        this.designationRepository = designationRepository;
        this.valueSetRepository = valueSetRepository;
        this.valueSetVersionRepository = valueSetVersionRepository;
        this.transcodingAssociationRepository = transcodingAssociationRepository;
    }

    /**
     * @param codeSystemConcept Code system concept
     * @param valueSetOid OID of the Value set
     * @param valueSetVersion name of ValueSetVersion
     * @return
     */
    @Override
    public boolean valueSetMatches(final CodeSystemConcept codeSystemConcept, final String valueSetOid, final String valueSetVersion) {
        logger.debug("[TsamDao] valueSetMatches('{}', '{}', '{}')", codeSystemConcept.getCode(), valueSetOid, valueSetVersion);
        if (valueSetOid == null) {
            return false;
        }
        final List<ValueSetVersion> result = valueSetRepository.findByOidAndConcepts(valueSetOid, codeSystemConcept.getId());
        if (null == valueSetVersion) {
            return result.size() > 0;
        } else {
            boolean match = false;
            for (final ValueSetVersion version : result) {
                if (valueSetVersion.equals(version.getVersionName())) {
                    match = true;
                    break;
                }
            }
            return match;
        }
    }

    @Override
    public CodeSystemConcept getTargetConcept(final CodeSystemConcept sourceConcept) throws TSAMException {
        logger.debug("[TsamDao] getTargetConcept('{}')", sourceConcept.getCode());
        final List<TranscodingAssociation> transcodingAssociations = transcodingAssociationRepository.findTranscodingAssociationsBySourceConcept(sourceConcept);

        if (transcodingAssociations.isEmpty()) {
            return null;
        }
        CodeSystemConcept target = null;
        for (final TranscodingAssociation association : transcodingAssociations) {
            target = association.getTargedConcept();
            final String status = association.getStatus();
            if (status == null || !status.equalsIgnoreCase(VALID_STATUS)) {
            } else break;
        }
        if (target == null) {
            throw new TSAMException(TSAMError.ERROR_TRANSCODING_INVALID);
        }
        return target;
    }

    @Override
    public Optional<CodeSystemConcept> getConcept(final String code, final CodeSystemVersion codeSystemVersion) throws TSAMException {
        logger.debug("[TsamDao] method getConcept('{}', '{}')", code, codeSystemVersion.getFullName());
        return codeSystemConceptRepository.findByCodeAndCodeSystemVersionId(code, codeSystemVersion.getId());
    }

    @Override
    public Optional<CodeSystemConcept> getConceptByCodeSystemVersionIds(final String code, final List<Long> codeSystemVersionIds) throws TSAMException {
        logger.debug("--> method CodeSystemConcept getConceptByCodeSystemVersionIds('{}', '{}')", code, codeSystemVersionIds.size());

        final var concepts = new ArrayList<CodeSystemConcept>();
        for (final Long codeSystemVersionId : codeSystemVersionIds) {
            if (codeSystemConceptRepository.findByCodeAndCodeSystemVersionId(code, codeSystemVersionId).isPresent()) {
                concepts.add(codeSystemConceptRepository.findByCodeAndCodeSystemVersionId(code, codeSystemVersionId).get());
            }
        }
        //TODO Consider logic to distinguish what codes are required

        // if more concepts are found, try to pick current one
        if (concepts.size() > 1) {
            for (final CodeSystemConcept concept : concepts) {
                if (StringUtils.equalsIgnoreCase(CURRENT_STATUS, concept.getStatus())) {
                    return Optional.of(concept);
                }
            }
        }
        return concepts.stream().findFirst();
    }

    @Override
    public CodeSystemVersion getVersion(final String version, final CodeSystem codeSystem) throws TSAMException {
        return codeSystemVersionRepository.findByLocalNameAndCodeSystem(version, codeSystem).orElseThrow(() -> new TSAMException(TSAMError.ERROR_CODE_SYSTEM_VERSION_NOTFOUND));
    }

    @Override
    public CodeSystem getCodeSystem(final CodeConcept codeConcept) throws TSAMException {
        final CodeSystem system;
        if (codeConcept.getCodeSystemOid().isPresent()) {
            final String oid = codeConcept.getCodeSystemOid().get();
            system = codeSystemRepository.findByOid(oid).orElseThrow(() -> new TSAMException(TSAMError.ERROR_CODE_SYSTEM_NOTFOUND, oid));
        } else if (codeConcept.getCodeSystemUrl().isPresent()){
            final String url = codeConcept.getCodeSystemUrl().get();
            system = codeSystemRepository.findByUrl(url).orElseThrow(() -> new TSAMException(TSAMError.ERROR_CODE_SYSTEM_NOTFOUND, url));
        } else {
            throw new TSAMException(TSAMError.ERROR_OID_OR_URL_MUST_BE_PROVIDED_TO_FIND_CODE_SYSTEM);
        }
        return system;
    }

    @Override
    public List<Long> getCodeSystemVersionIds(final CodeConcept codeConcept) {
        if (codeConcept.getCodeSystemOid().isPresent()) {
            return codeSystemVersionRepository.findByOid(codeConcept.getCodeSystemOid().get());
        } else if (codeConcept.getCodeSystemUrl().isPresent()) {
            return codeSystemVersionRepository.findByUrl(codeConcept.getCodeSystemUrl().get());
        }
        return new ArrayList<>();
    }

    @Override
    public List<RetrievedConcept> getConcepts(String valueSetOid, String valueSetVersionName, String language) {
        valueSetOid = sanitizeString(valueSetOid);
        valueSetVersionName = sanitizeString(valueSetVersionName);
        language = sanitizeString(language);
        logger.debug("[TsamDao] getConcepts('{}', '{}', '{}')", valueSetOid, valueSetVersionName, language);
        final ValueSetVersion valueSetVersion;
        if (!valueSetVersionName.isEmpty()) {
            valueSetVersion = valueSetVersionRepository.findValueSetVersionByDescriptionAndValueSetOid(valueSetVersionName, valueSetOid);
        } else {
            valueSetVersion = valueSetVersionRepository.findValueSetVersionByStatusAndValueSetOid(CURRENT_STATUS, valueSetOid);
        }
        if (valueSetVersion == null) {
            return new ArrayList<>();
        }

        final List<CodeSystemConcept> codeSystemConcepts = codeSystemConceptRepository.findCodeSystemConceptsByValueSetVersionsIdOrderByIdAsc(valueSetVersion.getId());

        final List<Designation> designations = designationRepository.findByValueSetVersionId(valueSetVersion.getId());

        final List<RetrievedConcept> result = new ArrayList<>();
        RetrievedConcept retrievedConcept;

        long id1;
        long id2;
        int j = 0;
        for (final CodeSystemConcept concept : codeSystemConcepts) {
            retrievedConcept = new RetrievedConcept(concept);
            result.add(retrievedConcept);
            id1 = concept.getId();
            while (j < codeSystemConcepts.size()) {
                final Designation designation = designations.get(j);
                id2 = designation.getConcept().getId();
                if (id1 < id2) {
                    break;
                } else if (id1 == id2) {
                    retrievedConcept.setLanguage(designation.getLanguageCode());
                    retrievedConcept.setDesignation(designation.getDesignation());
                    j++;
                    break;
                } else {
                    j++;
                }
            }
        }

        return result;
    }

    @Override
    public List<Designation> getSourceDesignation(final CodeSystemConcept target) throws TSAMException {
        logger.debug("[TsamDao] getSourceDesignation('{}')", target.getCode());
        final List result;
        try {
            result = getDesignation(target, "en-GB");
        } catch (final TSAMException e) {
            if (e.getReason() == TSAMError.ERROR_DESIGNATION_NOTFOUND) {
                throw new TSAMException(TSAMError.ERROR_DESIGNATION_NOTFOUND);
            } else {
                throw e;
            }
        }
        return result;
    }

    public List<Designation> getDesignation(final CodeSystemConcept codeSystemConcept, final String lang) throws TSAMException {
        logger.debug("[TsamDao] getDesignation('{}', '{}')", codeSystemConcept.getCode(), lang);
        List<Designation> designations = codeSystemConceptRepository.findDesignationByIdAndDesignationLanguageCode(codeSystemConcept.getId(), lang);

        if (designations.isEmpty()) {
            throw new TSAMException(TSAMError.ERROR_DESIGNATION_NOTFOUND);
        }

        final List<Designation> filter = new ArrayList<>();

        for (final Designation designation : designations) {
            if (CURRENT_STATUS.equalsIgnoreCase(designation.getStatus())) {
                filter.add(designation);
            }
        }

        if (filter.isEmpty()) {
            throw new TSAMException(TSAMError.ERROR_NO_CURRENT_DESIGNATIONS);
        }

        designations = filter;

        // sort designations by preferred flag, put preferred on top
        if (designations.size() > 1) {
            designations.sort(new Comparator<Designation>() {
                public int compare(final Designation o1, final Designation o2) {
                    if (Boolean.TRUE.equals(o1.isPreferred())) {
                        return -1;
                    } else if (Boolean.TRUE.equals(o2.isPreferred())) {
                        return 1;
                    }
                    return 0;
                }

                @Override
                public boolean equals(final Object obj) {
                    return super.equals(obj);
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            });
        }

        return designations;
    }

    public List<String> getLtrLanguages() {
        return designationRepository.findAllAvailableLanguageCodes();
    }

    private static @NotNull String sanitizeString(final String stringToSanitize) {
        return stringToSanitize != null ? stringToSanitize.replaceAll("[\n\r]", "_") : "";
    }
}
