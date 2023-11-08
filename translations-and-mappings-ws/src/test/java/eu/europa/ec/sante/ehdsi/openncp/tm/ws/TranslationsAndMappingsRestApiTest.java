package eu.europa.ec.sante.ehdsi.openncp.tm.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import eu.europa.ec.sante.ehdsi.openncp.tm.exception.TMException;
import eu.europa.ec.sante.ehdsi.openncp.tm.util.Base64Util;
import org.apache.commons.io.Charsets;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Ignore
public class TranslationsAndMappingsRestApiTest {

    @Test
    public void testLanguages() throws IOException {
        final var httpClient = HttpClientBuilder.create().build();
        final var httpGet = new HttpGet("http://localhost:9080/translations-and-mappings-ws/languages");

        var response = httpClient.execute(httpGet);
        var entity = response.getEntity();

        if (entity != null) {
            var retSrc = EntityUtils.toString(entity);
            // parsing JSON
            System.out.println(retSrc);
        }
    }


    @Test
    public void testTranslate() throws IOException, TMException {
        final var httpClient = HttpClientBuilder.create().build();
        try
        {
            //Define a postRequest request
            var postRequest = new HttpPost("http://localhost:9080/translations-and-mappings-ws/translate");

            var classloader = Thread.currentThread().getContextClassLoader();
            var is = classloader.getResourceAsStream("2-1234-W7.xml");
            var document = newDocumentFromInputStream(is);
            var languageCode = "nl-BE";

            postRequest.addHeader("Content-Type", "application/json");

            var mapper = new ObjectMapper();
            var node = mapper.createObjectNode();
            node.put("pivotCDA", Base64Util.encode(document));
            node.put("targetLanguageCode", languageCode);
            String jsonString = node.toString();
            var entity = new StringEntity(jsonString, StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            postRequest.setEntity(entity);

            //Send the request; It will immediately return the response in HttpResponse object if any
            var response = httpClient.execute(postRequest);

            //verify the valid error code first
            var statusCode = response.getStatusLine().getStatusCode();
            System.out.println("statusCode : " + statusCode);

            var responseEntity = response.getEntity();
            var encodingHeader = responseEntity.getContentEncoding();

            // you need to know the encoding to parse correctly
            var encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                    Charsets.toCharset(encodingHeader.getValue());

            // use org.apache.http.util.EntityUtils to read json as string
            var json = EntityUtils.toString(responseEntity, encoding);

            System.out.println(json);

            var tmResponseStructure = mapper.readValue(json, TMResponseStructure.class);

            System.out.println("tmResponseStructure : " + tmResponseStructure);

            Assert.assertEquals("success", tmResponseStructure.getStatus());

            var responseDocument = tmResponseStructure.getResponseCDA();
            System.out.println(getStringFromDocument(Base64Util.decode(responseDocument)));
        }
        finally
        {
            //Important: Close the connection
            httpClient.getConnectionManager().shutdown();
        }
    }

    private static Document newDocumentFromInputStream(InputStream in) {
        DocumentBuilderFactory factory;
        DocumentBuilder builder = null;
        Document ret = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        try {
            ret = builder.parse(new InputSource(in));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String getStringFromDocument(Document doc)
    {
        try
        {
            var domSource = new DOMSource(doc);
            var writer = new StringWriter();
            var result = new StreamResult(writer);
            var tf = TransformerFactory.newInstance();
            var transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch(TransformerException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

}
