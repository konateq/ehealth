package eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool;

import eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool.exceptions.TerminologyFileNotFoundException;
import eu.europa.ec.sante.openncp.core.client.connector.cdadisplaytool.exceptions.UITransformationException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Ignore("Test to revise - Exclude unit test from test execution")
public class DisplayTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayTest.class);

    //	@Test
    private void fileTest(final String input, final TRANSFORMATION type) throws UITransformationException, IOException {

        LOGGER.info("Transforming file: " + input);

        String cda = "";
        try {
            cda = CdaXSLTransformer.getInstance().readFile(input);
        } catch (final Exception e) {
            LOGGER.error("File not found");
        }
        String out = "";
        switch (type) {
            case ForPDF:
                out = CdaXSLTransformer.getInstance().transformForPDF(cda, "fr-BE", false);
                break;
            case UsingStandardCDAXsl:
                out = CdaXSLTransformer.getInstance().transformUsingStandardCDAXsl(cda);
            case WithOutputAndDefinedPath:
                out = CdaXSLTransformer.getInstance().transformWithOutputAndDefinedPath(cda, "fr-BE", "",
                        Paths.get(System.getenv("EPSOS_PROPS_PATH"), "EpsosRepository"));
            case WithOutputAndUserHomePath:
                out = CdaXSLTransformer.getInstance().transformWithOutputAndUserHomePath(cda, "fr-BE", "");

        }
        final String filename = Paths.get(input).getFileName().toString();
        final String stripExt = filename.substring(0, filename.lastIndexOf("."));
        final String pt = Paths.get(System.getenv("EPSOS_PROPS_PATH"), "EpsosRepository", "out", stripExt + ".html").toString();
        try (final Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pt), StandardCharsets.UTF_8))) {
            writer.write(out);
        }
    }

    //	@Test
    private void folderTest(final String input, final TRANSFORMATION type) {
        final Path path = Paths.get(input);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    if (!attrs.isDirectory()) {
                        try {
                            fileTest(file.toString(), type);
                        } catch (final UITransformationException e) {
                            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage(), e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final Exception e) {
            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage(), e);
        }
    }

    @Test
    public void runFile() throws UITransformationException, IOException {
        fileTest("/Users/mathiasghys/Downloads/2-1234-W8.xml", TRANSFORMATION.WithOutputAndDefinedPath);
    }

    @Test
    public void readFile() throws TerminologyFileNotFoundException, IOException, UITransformationException {

        final String out = CdaXSLTransformer.getInstance().readFile("samples/1-5678.xml");
        final String pt = Paths.get(System.getenv("EPSOS_PROPS_PATH"), "EpsosRepository", "out", "readfile.txt").toString();
        try (final Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pt), StandardCharsets.UTF_8))) {
            writer.write(out);
        }
    }

    private enum TRANSFORMATION {
        WithOutputAndUserHomePath, ForPDF, UsingStandardCDAXsl, WithOutputAndDefinedPath
    }
}
