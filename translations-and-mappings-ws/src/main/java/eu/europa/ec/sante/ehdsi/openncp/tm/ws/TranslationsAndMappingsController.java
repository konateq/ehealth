package eu.europa.ec.sante.ehdsi.openncp.tm.ws;

import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TranslateRequest;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TranscodeRequest;
import eu.europa.ec.sante.ehdsi.openncp.tm.persistence.model.Property;
import eu.europa.ec.sante.ehdsi.openncp.tm.service.ITransformationService;
import eu.europa.ec.sante.ehdsi.openncp.tm.service.PropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

@RestController
public class TranslationsAndMappingsController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY = "AVAILABLE_TRANSLATION_LANGUAGES";

    private final PropertyService propertyService;

    private final ITransformationService transformationService;

    public TranslationsAndMappingsController(PropertyService propertyService, ITransformationService transformationService) {
        this.propertyService = propertyService;
        this.transformationService = transformationService;
    }

    @GetMapping("/languages")
    public Set<String> retrieveAvailableTranslationLanguages() {
        logger.error("Entering retrieveAvailableTranslationLanguages() method");
        Property property = propertyService.getProperty(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY);
        var availableLanguageCodes = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(property.getValue(), ",");

        // checking tokens
        while (st.hasMoreTokens()) {
            availableLanguageCodes.add(st.nextToken().trim());
        }
        return availableLanguageCodes;
    }

    @PostMapping(value = "/translate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<TMResponseStructure> translateDocument(@RequestBody TranslateRequest translateRequest) {
        logger.info("Entering translateDocument() method");
        Document pivotCDA = translateRequest.getPivotCDA();
        String targetLanguageCode = translateRequest.getTargetLanguageCode();
        logger.info("Translating CDA document in language [{}]", targetLanguageCode);
        return ResponseEntity.ok(transformationService.translate(pivotCDA, targetLanguageCode));
    }

    @PostMapping(value = "/transcode", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<TMResponseStructure> transcodeDocument(@RequestBody TranscodeRequest transcodeRequest) {
        logger.info("Entering transcodeDocument() method");
        Document friendlyCDA = transcodeRequest.getFriendlyCDA();
        logger.info("Transcoding CDA document into PIVOT");
        return ResponseEntity.ok(transformationService.transcode(friendlyCDA));
    }
}
