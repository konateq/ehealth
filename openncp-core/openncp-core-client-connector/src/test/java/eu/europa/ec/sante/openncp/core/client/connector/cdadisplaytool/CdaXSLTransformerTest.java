package eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool;

import eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool.util.HtmlValidator;
import eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool.util.PdfValidator;
import eu.europa.ec.sante.openncp.core.client.connector.display.cda.CdaXSLTransformer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Ignore("Test to revise - Exclude unit test from test execution")
public class CdaXSLTransformerTest {

    private final Logger logger = LoggerFactory.getLogger(CdaXSLTransformerTest.class);

    @Test
    public void testPatientSummaryHtmlTransformation() throws Exception {
        final String inputFileName = "0000141G.xml";
        testTransformation(inputFileName, DocumentType.PS, TransformationType.USING_STANDARD_CDA_XSL);
    }

    @Test
    public void testPatientSummaryHtmlPortalTransformation() throws Exception {
        final String inputFileName = "0000141G.xml";
        testTransformation(inputFileName, DocumentType.PS, TransformationType.WITH_OUTPUT_AND_DEFINED_PATH);
    }

    @Test
    public void testPatientSummaryPdfTransformation() throws Exception {
        final String inputFileName = "0000141G.xml";
        testTransformation(inputFileName, DocumentType.PS, TransformationType.FOR_PDF);
    }

    @Test
    public void testEPrescriptionHtmlTransformation() throws Exception {
        final String inputFileName = "ep.xml";
        testTransformation(inputFileName, DocumentType.EP, TransformationType.USING_STANDARD_CDA_XSL);
    }

    @Test
    public void testEPrescriptionMultiIngredientHtmlTransformation() throws Exception {
        final String inputFileName = "ep_multi_ingredient.xml";
        testTransformation(inputFileName, DocumentType.EP, TransformationType.USING_STANDARD_CDA_XSL);
    }

    @Test
    public void testEPrescriptionSingleIngredientHtmlPortalTransformation() throws Exception {
        final String inputFileName = "ep_single_ingredient.xml";
        testTransformation(inputFileName, DocumentType.EP, TransformationType.WITH_OUTPUT_AND_DEFINED_PATH);
    }

    @Test
    public void testEPrescriptionMultiIngredientHtmlPortalTransformation() throws Exception {
        final String inputFileName = "ep_multi_ingredient.xml";
        testTransformation(inputFileName, DocumentType.EP, TransformationType.WITH_OUTPUT_AND_DEFINED_PATH);
    }

    @Test
    public void testEPrescriptionHtmlPortalTransformation() throws Exception {
        final String inputFileName = "ep.xml";
        testTransformation(inputFileName, DocumentType.EP, TransformationType.WITH_OUTPUT_AND_DEFINED_PATH);
    }

    private void testTransformation(final String inputFileName, final DocumentType documentType, final TransformationType transformationType) throws Exception {
        final String cda = readCdaDocument("/" + documentType.name() + "/" + inputFileName);
        final String transformationResult = executeXslTransformation(cda, transformationType);
        switch (transformationType) {
            case USING_STANDARD_CDA_XSL:
                new HtmlValidator().validate(cda, transformationResult);
                break;
            case FOR_PDF:
                new PdfValidator().validate(cda, transformationResult);
                break;
            case WITH_OUTPUT_AND_DEFINED_PATH:
                break;
        }
        displayTransformedFile(transformationResult, transformationType, inputFileName);
    }

    private String readCdaDocument(final String fileName) throws IOException {
        final InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        return readFromInputStream(inputStream);
    }

    private String executeXslTransformation(final String cda, final TransformationType transformationType) throws Exception {

        switch (transformationType) {
            case WITH_OUTPUT_AND_DEFINED_PATH:
                final Path path = Paths.get(System.getenv("EPSOS_PROPS_PATH"), "EpsosRepository");
                Assert.assertTrue(Files.exists(path));
                return CdaXSLTransformer.getInstance().transformWithOutputAndDefinedPath(cda, "en-GB", "", path);
            case WITH_OUTPUT_AND_USER_HOME_PATH:
                return CdaXSLTransformer.getInstance().transformWithOutputAndUserHomePath(cda, "en-GB", "");
            case USING_STANDARD_CDA_XSL:
                return CdaXSLTransformer.getInstance().transformUsingStandardCDAXsl(cda);
            case FOR_PDF:
                return CdaXSLTransformer.getInstance().transformForPDF(cda, "en-GB", false);
            case PORTAL_HTML:
                return CdaXSLTransformer.getInstance().transform(cda, "en-GB", "dispenseServlet");
            default:
                return null;
        }
    }

    private String readFromInputStream(final InputStream inputStream)
            throws IOException {
        final StringBuilder resultStringBuilder = new StringBuilder();
        try (final BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line);
            }
        }
        return resultStringBuilder.toString();
    }

    private void writeFile(final String fileName, final String content) {
        try {
            final BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write(content);
            out.close();
        } catch (final IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void displayTransformedFile(final String result, final TransformationType transformationType, final String fileName) throws IOException {
        final String fullResultFileName = "transformed_" + fileName.substring(0, fileName.lastIndexOf('.')) + '.' + transformationType.getExtension();
        writeFile(fullResultFileName, result);
        final File resultFile = new File(fullResultFileName);
        Desktop.getDesktop().browse(resultFile.toURI());
    }

    private enum TransformationType {
        WITH_OUTPUT_AND_USER_HOME_PATH("html"),
        FOR_PDF("pdf"),
        USING_STANDARD_CDA_XSL("html"),
        WITH_OUTPUT_AND_DEFINED_PATH("html"),
        PORTAL_HTML("html");

        private final String extension;

        TransformationType(final String extension) {
            this.extension = extension;
        }

        public String getExtension() {
            return extension;
        }
    }

    private enum DocumentType {
        PS,
        EP
    }
}
