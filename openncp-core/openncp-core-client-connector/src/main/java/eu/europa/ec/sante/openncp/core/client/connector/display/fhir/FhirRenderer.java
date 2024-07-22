package eu.europa.ec.sante.openncp.core.client.connector.display.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4b.context.IWorkerContext;
import org.hl7.fhir.r4b.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4b.model.Bundle;
import org.hl7.fhir.r4b.renderers.BundleRenderer;
import org.hl7.fhir.r4b.renderers.utils.RenderingContext;
import org.hl7.fhir.r4b.utils.EOperationOutcome;
import org.hl7.fhir.utilities.MarkDownProcessor;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FhirRenderer {

    public XhtmlNode render(final Bundle bundle) throws IOException, EOperationOutcome {
        final FhirContext context = FhirContext.forR4B();
        final IWorkerContext workerContext = new HapiWorkerContext(context, new DefaultProfileValidationSupport(context));
        final MarkDownProcessor markDownProcessor = new MarkDownProcessor(MarkDownProcessor.Dialect.DARING_FIREBALL);
        final RenderingContext renderingContext = new RenderingContext(
                workerContext,
                markDownProcessor,
                ValidationOptions.defaults(),
                "specLink",
                "myhealtheu",
                "en",
                RenderingContext.ResourceRendererMode.TECHNICAL);
        final BundleRenderer renderer = new BundleRenderer(renderingContext);
        return renderer.render(bundle);
    }

    public static void main(final String[] args) throws IOException, EOperationOutcome {
        final FhirContext ctx = FhirContext.forR4B();
        // Instantiate a new parser
        final IParser parser = ctx.newJsonParser();
        final FhirRenderer renderer = new FhirRenderer();
        final Bundle bundle = parser.parseResource(Bundle.class, IOUtils.toString(
                renderer.getClass().getClassLoader().getResourceAsStream("fhir/bundle.json"),
                StandardCharsets.UTF_8));
        final FhirRenderer fhirRenderer = new FhirRenderer();
        System.out.println(fhirRenderer.render(bundle));
    }
}
