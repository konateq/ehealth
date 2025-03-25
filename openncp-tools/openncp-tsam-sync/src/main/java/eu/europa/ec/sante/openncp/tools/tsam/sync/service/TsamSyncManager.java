package eu.europa.ec.sante.openncp.tools.tsam.sync.service;

import com.ibatis.common.jdbc.ScriptRunner;
import eu.europa.ec.sante.openncp.tools.tsam.sync.config.DatabaseProperties;
import eu.europa.ec.sante.openncp.tools.tsam.sync.converter.ConceptConverter;
import eu.europa.ec.sante.openncp.tools.tsam.sync.converter.ValueSetVersionConverter;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.ValueSetVersion;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domainehealthproperty.model.Property;
import eu.europa.ec.sante.openncp.tools.tsam.sync.repository.*;
import eu.europa.ec.sante.openncp.tools.tsam.sync.config.CtsProperties;
import eu.europa.ec.sante.openncp.tools.tsam.sync.cts.CtsClient;
import eu.europa.ec.sante.openncp.tools.tsam.sync.db.DatabaseTool;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.Concept;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.Mapping;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.Designation;
import eu.europa.ec.sante.openncp.tools.tsam.sync.domainehealthproperty.service.PropertyService;
import eu.europa.ec.sante.ehdsi.termservice.web.rest.model.sync.ValueSetCatalogModel;
import eu.europa.ec.sante.ehdsi.termservice.web.rest.model.sync.ValueSetVersionModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static String databaseProduct = "";
    private static final String JDBC_EHNCP_PROPERTY = "jdbc/ehealth_ltrdb";
    private static String sqlCleanLocalDb;

    private final ValueSetVersionConverter valueSetVersionConverter;

    @Value("${pageable.page-size:500}")
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

            if(databaseProduct.isEmpty()) {
                retrieveDbProperties();
            }

            // Cleaning current LTR Database.
            logger.info("Cleaning current LTR Database");
            try {
                truncateDatabaseTables();
            } catch (Exception e) {
                logger.info("Cleaning Mappings");
                mappingRepository.deleteAll();
                logger.info("Cleaning ValueSets");
                valueSetRepository.deleteAll();
                logger.info("Cleaning CodeSystems");
                codeSystemRepository.deleteAll();
            }

            logger.info("Starting value sets synchronization");
            int index = 1;

            Property property =  new Property(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY, "");
            List<String> languagesAvailable = new ArrayList<>();

            logger.info("Catalogue value set versions : {}", catalogue.getValueSetVersions().size());

            for (ValueSetVersionModel valueSetVersionModel : catalogue.getValueSetVersions()) {

                logger.info("Value set versions : id {} - {}/{}",
                        valueSetVersionModel.getValueSet().getId(),
                        valueSetVersionModel.getValueSet().getName(), valueSetVersionModel.getValueSet().getDescription());

                ValueSetVersion valueSetVersion = valueSetVersionRepository.save(
                        Objects.requireNonNull(valueSetVersionConverter.convert(valueSetVersionModel)));
                boolean hasNext = false;
                int page = 0;
                int total = 0;
                int numberOfTotalMappings = 0;

                logger.info("{}/{}: '{}' starting...", index,
                        catalogue.getValueSetVersions().size(), valueSetVersionModel.getValueSet().getName());

                //if(!StringUtils.equals(valueSetVersionModel.getValueSet().getName(), "eHDSIDoseForm")) {
                //    continue;
                //}

                while (hasNext || page == 0) {
                    List<Concept> concepts = ctsClient.fetchConcepts(valueSetVersionModel.getValueSet().getId(),
                                    valueSetVersionModel.getVersionId(), page++, getPageSize())
                            .stream()
                            .map(conceptConverter::convert)
                            .collect(Collectors.toList());

                    logger.info("   fetch {} concepts...", concepts.size());

                    concepts.forEach(concept -> concept.addValueSetVersion(valueSetVersion));
                    for (Concept concept : concepts) {
                        numberOfTotalMappings += concept.getMappings().size();

                        logger.info("       concept {} - {} -> {} mappings found",
                                concept.getCode(), concept.getDefinition(), concept.getMappings().size());

                        for(Mapping mapping : concept.getMappings()) {
                            logger.info("           mapping source {}:{} - target {}:{} - status {}",
                                    mapping.getSource().getCode(), mapping.getSource().getDefinition(),
                                    mapping.getTarget().getCode(), mapping.getTarget().getDefinition(),
                                    mapping.getStatus());
                        }

                        for(Designation designation : concept.getDesignations()) {
                            if(!languagesAvailable.contains(designation.getLanguageCode())){
                                languagesAvailable.add(designation.getLanguageCode());
                            }
                        }
                    }
                    conceptRepository.saveAll(concepts);
                    if(languagesAvailable != null){
                        property.setValue(String.join(",", languagesAvailable));
                        if(StringUtils.isNotBlank(property.getValue()) && property.getValue().charAt(0) == ',') {
                            property.setValue(property.getValue().substring(1));
                        }
                        propertyService.save(property);
                    }

                    total += concepts.size();
                    hasNext = concepts.size() == pageSize;
                }
                logger.info("{}/{}: '{}' completed ({} concepts with '{}' mappings)", index++, catalogue.getValueSetVersions().size(),
                        valueSetVersionModel.getValueSet().getName(), total, numberOfTotalMappings);
            }

            stopWatch.stop();
            logger.info("Catalogue '{}' synchronized successfully ({} ms)", catalogue.getName(), stopWatch.getTotalTimeMillis());
        }
    }

    public Integer getPageSize(){
        return pageSize;
    }

    public void setPageSize(Integer pageSize){
        this.pageSize = pageSize;
    }

    private Connection dbConnect(String dsName) throws NamingException, SQLException {
        Context initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource) envContext.lookup(dsName);
        return ds.getConnection();
    }

    private void retrieveDbProperties()
    {
        logger.info("Retrieving db properties");
        try (Connection sqlConnection = dbConnect(TsamSyncManager.JDBC_EHNCP_PROPERTY);
             Statement stmt = sqlConnection.createStatement()) {
            databaseProduct = sqlConnection.getMetaData().getDatabaseProductName().toLowerCase();
        } catch (Exception exception) {
           // if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            //   loggerClinical.error("Cannot detect database type");
            //}
        }
        logger.info("Database is : {}", databaseProduct);
    }

    private void runSqlScript(String sqlScript) throws Exception {

        logger.info("Running sql script : {}", sqlScript);

        try (Connection sqlConnection = dbConnect(TsamSyncManager.JDBC_EHNCP_PROPERTY);
             StringReader stringReader = new StringReader(sqlScript)) {

            ScriptRunner objScriptRunner = new ScriptRunner(sqlConnection, false, true);
            objScriptRunner.setLogWriter(null);
            objScriptRunner.setErrorLogWriter(null);
            objScriptRunner.runScript(stringReader);

        } catch (Exception exception) {
            throw new Exception("The following error occurred during an SQL operation:", exception);
        }
    }

    private void truncateDatabaseTables() throws Exception {
        logger.info("Truncate database");
        runSqlScript("SET FOREIGN_KEY_CHECKS = 0;");
        runSqlScript("TRUNCATE TABLE designation;");
        runSqlScript("TRUNCATE TABLE transcoding_association;");
        runSqlScript("TRUNCATE TABLE x_concept_value_set;");
        runSqlScript("TRUNCATE TABLE code_system_concept;");
        runSqlScript("TRUNCATE TABLE code_system_version;");
        runSqlScript("TRUNCATE TABLE code_system;");
        runSqlScript("TRUNCATE TABLE value_set_version;");
        runSqlScript("TRUNCATE TABLE value_set;");
        runSqlScript("SET FOREIGN_KEY_CHECKS = 1;");
    }

}
