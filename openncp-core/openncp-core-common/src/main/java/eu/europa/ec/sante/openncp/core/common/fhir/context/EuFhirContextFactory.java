package eu.europa.ec.sante.openncp.core.common.fhir.context;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.ParserOptions;
import ca.uhn.fhir.parser.LenientErrorHandler;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CustomResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class EuFhirContextFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EuFhirContextFactory.class);

    static public FhirContext createFhirContext() {
        final ParserOptions parserOptions = new ParserOptions();
        parserOptions.setOverrideResourceIdWithBundleEntryFullUrl(false);

        final FhirContext ctx = FhirContext.forR4();
        ctx.setParserOptions(parserOptions);
        ctx.setParserErrorHandler(new LenientErrorHandler());

        Arrays.stream(FhirSupportedResourceType.values()).forEach(fhirSupportedResourceType -> {
            final FhirSupportedResourceType.CustomResource customType = fhirSupportedResourceType.getCustomType();
            if (customType.isCustomResource()) {
                final Class<? extends CustomResource> resourceClass = customType.getCustomResourceClass().orElseThrow();
                final String resourceProfile = customType.getProfile().orElseThrow();
                LOGGER.info("Registering default FHIR type [{}] for profile [{}]", resourceClass.getSimpleName(), resourceProfile);
                ctx.setDefaultTypeForProfile(resourceProfile, resourceClass);
            }
        });

        return ctx;
    }
}
