package eu.europa.ec.sante.ehdsi.openncp.tm.ws;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.ehdsi.constant.error.TMError;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TranscodeRequest;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TranslateRequest;
import eu.europa.ec.sante.ehdsi.openncp.tm.persistence.model.Property;
import eu.europa.ec.sante.ehdsi.openncp.tm.service.ITransformationService;
import eu.europa.ec.sante.ehdsi.openncp.tm.service.PropertyService;
import eu.europa.ec.sante.ehdsi.openncp.tm.util.Base64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.*;

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

    @PostConstruct
    public void propertiesInit() {
        logger.info("propertiesInit");
    }

    @GetMapping("/languages")
    public Set<String> retrieveAvailableTranslationLanguages() {
        logger.info("Entering retrieveAvailableTranslationLanguages() method");
        Property property = propertyService.getProperty(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY);
        var availableLanguageCodes = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(property.getValue(), ",");

        // checking tokens
        while (st.hasMoreTokens()) {
            availableLanguageCodes.add(st.nextToken().trim());
        }
        return availableLanguageCodes;
    }

    @GetMapping(value = "/translateVS", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity retrieveValueSet(@RequestParam String oid, String targetLanguage){
        var valueSet = transformationService.translateValueSet(oid, targetLanguage);

        //Create a FHIR context
        FhirContext ctx = FhirContext.forR4();

        //Instantiate a new parser
        IParser parser = ctx.newJsonParser();

        return ResponseEntity.ok(Base64.getEncoder().encodeToString(parser.encodeResourceToString(valueSet).getBytes()));
    }

    @PostMapping(value = "/translate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<TMResponseStructure> translateDocument(@RequestBody TranslateRequest translateRequest) {
        logger.info("Entering translateDocument() method");
        Document pivotCDA = null;
        try {
            pivotCDA = Base64Util.decode(translateRequest.getPivotCDA());
        } catch (Exception e) {
            TMResponseStructure tmResponseStructure = new TMResponseStructure();
            tmResponseStructure.setErrors(Collections.singletonList(TMError.BASE64_DOM_DECODING_EXCEPTION));
            return ResponseEntity.badRequest().body(tmResponseStructure);
        }
        String targetLanguageCode = translateRequest.getTargetLanguageCode();
        logger.info("Translating CDA document in language [{}]", targetLanguageCode);
        return ResponseEntity.ok(transformationService.translate(pivotCDA, targetLanguageCode));
    }

    @PostMapping(value = "/transcode", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<TMResponseStructure> transcodeDocument(@RequestBody TranscodeRequest transcodeRequest) {
        logger.info("Entering transcodeDocument() method");
        Document friendlyCDA = null;
        try {
            friendlyCDA = Base64Util.decode(transcodeRequest.getFriendlyCDA());
        } catch (Exception e) {
            TMResponseStructure tmResponseStructure = new TMResponseStructure();
            tmResponseStructure.setErrors(Collections.singletonList(TMError.BASE64_DOM_DECODING_EXCEPTION));
            return ResponseEntity.badRequest().body(tmResponseStructure);
        }
        logger.info("Transcoding CDA document into PIVOT");
        return ResponseEntity.ok(transformationService.transcode(friendlyCDA));
    }
}
