package eu.europa.ec.sante.openncp.core.common.fhir.transformation.translations;

import eu.europa.ec.sante.openncp.core.common.fhir.transformation.domain.TMResponseStructure;
import org.hl7.fhir.r4.model.Bundle;

public interface TranslationService {

    TMResponseStructure translate(final Bundle FHIRDocument, final String targetLanguage);
}
