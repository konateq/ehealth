package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.service;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.config.CtsProperties;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.config.DatabaseProperties;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.converter.ConceptConverter;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.converter.ValueSetVersionConverter;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.cts.CtsClient;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.db.DatabaseTool;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.Concept;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.Designation;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domainehealthproperty.model.Property;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.ValueSetVersion;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domainehealthproperty.service.PropertyService;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.repository.*;
import eu.europa.ec.sante.ehdsi.termservice.web.rest.model.sync.ValueSetCatalogModel;
import eu.europa.ec.sante.ehdsi.termservice.web.rest.model.sync.ValueSetVersionModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TsamSyncManager {

    private final Logger logger = LoggerFactory.getLogger(TsamSyncManager.class);
    private final DatabaseTool databaseTool;
    private final CtsProperties ctsProperties;
    private final CtsClient ctsClient;
    private final DatabaseProperties databaseProperties;
    private final CodeSystemRepository codeSystemRepository;
    private final ConceptRepository conceptRepository;
    private final ValueSetRepository valueSetRepository;
    private final ValueSetVersionRepository valueSetVersionRepository;
    private final MappingRepository mappingRepository;
    private final ConceptConverter conceptConverter;
    private final PropertyService propertyService;

    private final ValueSetVersionConverter valueSetVersionConverter;

    @Value("${pageable.page-size:250}")
    private Integer pageSize;

    private static final String AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY = "AVAILABLE_TRANSLATION_LANGUAGES";

    @SuppressWarnings("squid:S00107")
    public TsamSyncManager(DatabaseTool databaseTool, CtsProperties ctsProperties, CtsClient ctsClient,
                           DatabaseProperties databaseProperties, CodeSystemRepository codeSystemRepository,
                           ConceptRepository conceptRepository, MappingRepository mappingRepository,
                           ValueSetRepository valueSetRepository, ValueSetVersionRepository valueSetVersionRepository,
                           ConceptConverter conceptConverter, PropertyService propertyService, ValueSetVersionConverter valueSetVersionConverter) {
        this.databaseTool = databaseTool;
        this.ctsProperties = ctsProperties;
        this.ctsClient = ctsClient;
        this.databaseProperties = databaseProperties;
        this.codeSystemRepository = codeSystemRepository;
        this.conceptRepository = conceptRepository;
        this.mappingRepository = mappingRepository;
        this.valueSetRepository = valueSetRepository;
        this.valueSetVersionRepository = valueSetVersionRepository;
        this.conceptConverter = conceptConverter;
        this.propertyService = propertyService;
        this.valueSetVersionConverter = valueSetVersionConverter;
    }

    @Transactional
    public void synchronize() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.info("Authenticating user '{}'", ctsProperties.getUsername());
        ctsClient.authenticate();
        logger.info("User '{}' authenticated successfully to the Central Terminology Services ({})", ctsProperties.getUsername(), ctsProperties.getUrl());

        logger.info("Checking for updates");
        Optional<ValueSetCatalogModel> opt = ctsClient.fetchCatalogue();
        if (opt.isEmpty()) {
            logger.info("Nothing to synchronize");
        } else {
            ValueSetCatalogModel catalogue = opt.get();
            logger.info("Catalogue '{}' retrieved from the server", catalogue.getName());

            if (databaseProperties.isBackup()) {
                logger.info("Starting database backup process");
                String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
                date = StringUtils.replace(date, ":", "-");
                databaseTool.backup("backup_" + date + ".sql");
                logger.info("Database backup operation completed");
            } else {
                logger.warn("Database backup is disabled (Property 'tsam-sync.database.backup' is set to 'false')");
            }

            // Cleaning current LTR Database.
            mappingRepository.deleteAll();
            valueSetRepository.deleteAll();
            codeSystemRepository.deleteAll();

            logger.info("Starting value sets synchronization");
            int index = 1;

            for (ValueSetVersionModel valueSetVersionModel : catalogue.getValueSetVersions()) {

                ValueSetVersion valueSetVersion = valueSetVersionRepository.save(
                        Objects.requireNonNull(valueSetVersionConverter.convert(valueSetVersionModel)));
                boolean hasNext = false;
                int page = 0;
                int total = 0;
                int numberOfMapping = 0;
                Property property = propertyService.getProperty(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY);
                List<String> languagesAvailable = null;
                while (hasNext || page == 0) {
                    List<Concept> concepts = ctsClient.fetchConcepts(valueSetVersionModel.getValueSet().getId(),
                                    valueSetVersionModel.getVersionId(), page++, getPageSize())
                            .stream()
                            .map(conceptConverter::convert)
                            .collect(Collectors.toList());
                    concepts.forEach(concept -> concept.addValueSetVersion(valueSetVersion));
                    for (Concept concept : concepts) {
                        numberOfMapping += concept.getMappings().size();
                        if(property == null){
                            property = new Property(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY, "");
                        }
                        if(property.getValue() == null){
                            property.setValue("");
                        }

                        String[] split = property.getValue().split(",");
                        languagesAvailable = Arrays.stream(split).collect(Collectors.toList());
                        for(Designation designation : concept.getDesignations()){
                            if(!languagesAvailable.contains(designation.getLanguageCode())){
                                languagesAvailable.add(designation.getLanguageCode());
                            }
                        }
                    }
                    conceptRepository.saveAll(concepts);
                    if(languagesAvailable != null){
                        property.setValue(String.join(",", languagesAvailable));
                        if(StringUtils.isNotBlank(property.getValue()) && property.getValue().charAt(0)==','){
                            property.setValue(property.getValue().substring(1));
                        }
                        propertyService.save(property);
                    }

                    total += concepts.size();
                    hasNext = concepts.size() == pageSize;
                }
                logger.info("{}/{}: '{}' completed ({} concepts with '{}' mappings)", index++, catalogue.getValueSetVersions().size(),
                        valueSetVersionModel.getValueSet().getName(), total, numberOfMapping);
            }

            stopWatch.stop();
            logger.info("Catalogue '{}' synchronized successfully ({} ms)", catalogue.getName(), stopWatch.getTotalTimeMillis());
        }
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getLanguageCode(Concept concept, List<String> languageAvailable){
        return "";
    }
}
