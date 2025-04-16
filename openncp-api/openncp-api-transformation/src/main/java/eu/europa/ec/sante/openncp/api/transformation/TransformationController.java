package eu.europa.ec.sante.openncp.api.transformation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import eu.europa.ec.sante.openncp.common.property.Property;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMResponseStructure;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TMStatus;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TranscodeRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.domain.TranslateRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.util.Base64Util;
import eu.europa.ec.sante.openncp.core.common.tsam.error.TMError;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import java.util.*;

@RestController
public class TransformationController {

    private final Logger logger = LoggerFactory.getLogger(TransformationController.class);

    private static final String AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY = "AVAILABLE_TRANSLATION_LANGUAGES";

    private final PropertyService propertyService;

    private final CDATransformationService cdaTransformationService;

    public TransformationController(final PropertyService propertyService, CDATransformationService cdaTransformationService) {
        this.propertyService = Validate.notNull(propertyService);
        this.cdaTransformationService = cdaTransformationService;
    }

    @GetMapping("/languages")
    public Set<String> retrieveAvailableTranslationLanguages() {
        logger.info("Entering retrieveAvailableTranslationLanguages() method");
        final Property propertyEntity = propertyService.findByKeyMandatory(AVAILABLE_TRANSLATION_LANGUAGES_PROPERTY_KEY);
        var availableLanguageCodes = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(propertyEntity.getValue(), ",");

        // checking tokens
        while (st.hasMoreTokens()) {
            availableLanguageCodes.add(st.nextToken().trim());
        }
        return availableLanguageCodes;
    }

    @GetMapping(value = "/translateVS", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<String> retrieveValueSet(@RequestParam String oid, @RequestParam String targetLanguage){
        var valueSet = cdaTransformationService.translateValueSet(oid, targetLanguage);

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
        Document pivotCDA;
        try {
            pivotCDA = Base64Util.decode(translateRequest.getPivotCDA());
        } catch (Exception e) {
            TMResponseStructure tmResponseStructure = new TMResponseStructure();
            tmResponseStructure.setErrors(Collections.singletonList(TMError.BASE64_DOM_DECODING_EXCEPTION));
            tmResponseStructure.setStatus(TMStatus.ERROR);
            return ResponseEntity.badRequest().body(tmResponseStructure);
        }
        String targetLanguageCode = translateRequest.getTargetLanguageCode();
        targetLanguageCode = sanitizeString(targetLanguageCode);
        logger.info("Translating CDA document in language [{}]", targetLanguageCode);
        return ResponseEntity.ok(cdaTransformationService.translate(pivotCDA, targetLanguageCode));
    }

    @PostMapping(value = "/transcode", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<TMResponseStructure> transcodeDocument(@RequestBody TranscodeRequest transcodeRequest) {
        logger.info("Entering transcodeDocument() method");
        Document friendlyCDA;
        try {
            friendlyCDA = Base64Util.decode(transcodeRequest.getFriendlyCDA());
        } catch (Exception e) {
            TMResponseStructure tmResponseStructure = new TMResponseStructure();
            tmResponseStructure.setErrors(Collections.singletonList(TMError.BASE64_DOM_DECODING_EXCEPTION));
            tmResponseStructure.setStatus(TMStatus.ERROR);
            return ResponseEntity.badRequest().body(tmResponseStructure);
        }
        logger.info("Transcoding CDA document into PIVOT");
        return ResponseEntity.ok(cdaTransformationService.transcode(friendlyCDA));
    }

    private static String sanitizeString(String stringToSanitize) {
        return stringToSanitize != null ? stringToSanitize.replaceAll("[\n\r]", "_") : "";
    }
}
