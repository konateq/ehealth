package tr.com.srdc.epsos.ws.server.xca.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.epsos.exceptions.DocumentTransformationException;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import org.apache.commons.io.Charsets;
import org.apache.hc.core5.http.ContentType;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class TranslationsAndMappingsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationsAndMappingsClient.class);

    public static TMResponseStructure translate(Document cdaPivot, String targetLanguage) throws DocumentTransformationException {
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            LOGGER.debug("TM - TRANSLATION START.");
            var mapper = new ObjectMapper();
            var node = mapper.createObjectNode();
            node.put("pivotCDA", getStringFromDocument(cdaPivot));
            node.put("targetLanguageCode", targetLanguage);
            var jsonString = node.toString();
            var entity = new StringEntity(jsonString, HTTP.UTF_8);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            var postRequest = new HttpPost(getTranslationsAndMappingsWsUrl() + "/translate");
            postRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            postRequest.setEntity(entity);
            try (CloseableHttpResponse response = httpclient.execute(postRequest)) {
                LOGGER.debug("HTTP statusCode : " + response.getStatusLine().getStatusCode());

                var responseEntity = response.getEntity();
                var encodingHeader = responseEntity.getContentEncoding();
                var encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                        Charsets.toCharset(encodingHeader.getValue());

                var json = EntityUtils.toString(responseEntity, encoding);
                var tmResponse = mapper.readValue(json, TMResponseStructure.class);

                LOGGER.debug("TM - TRANSLATION STOP");
                return tmResponse;
            }
        } catch (Exception ex) {
            throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
        }
    }

    public static TMResponseStructure transcode(Document cdaFriendly) throws DocumentTransformationException {
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            LOGGER.debug("TM - TRANSCODING START.");
            var mapper = new ObjectMapper();
            var node = mapper.createObjectNode();
            node.put("friendlyCDA", getStringFromDocument(cdaFriendly));
            var jsonString = node.toString();
            var entity = new StringEntity(jsonString, HTTP.UTF_8);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            var postRequest = new HttpPost(getTranslationsAndMappingsWsUrl() + "/transcode");
            postRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            postRequest.setEntity(entity);
            try (CloseableHttpResponse response = httpclient.execute(postRequest)) {
                LOGGER.debug("HTTP statusCode : " + response.getStatusLine().getStatusCode());

                var responseEntity = response.getEntity();
                var encodingHeader = responseEntity.getContentEncoding();
                var encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                        Charsets.toCharset(encodingHeader.getValue());

                var json = EntityUtils.toString(responseEntity, encoding);
                var tmResponse = mapper.readValue(json, TMResponseStructure.class);

                LOGGER.debug("TM - TRANSCODING STOP");
                return tmResponse;
            }
        } catch (Exception ex) {
            throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
        }
    }

    private static String getStringFromDocument(Document doc) throws TransformerException {
        var domSource = new DOMSource(doc);
        var writer = new StringWriter();
        var result = new StreamResult(writer);
        var tf = TransformerFactory.newInstance();
        var transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(domSource, result);
        return writer.toString();
    }

    private static String getTranslationsAndMappingsWsUrl() {
        var translationsAndMappingsUrl = ConfigurationManagerFactory.getConfigurationManager().getProperty("TRANSLATIONS_AND_MAPPINGS_WS_URL");
        LOGGER.info("Translations and Mappings WS URL: '{}'", translationsAndMappingsUrl);
        return translationsAndMappingsUrl;
    }
}
