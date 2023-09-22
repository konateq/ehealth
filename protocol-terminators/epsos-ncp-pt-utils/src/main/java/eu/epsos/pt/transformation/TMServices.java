package eu.epsos.pt.transformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.epsos.exceptions.DocumentTransformationException;
import eu.europa.ec.sante.ehdsi.constant.error.ITMTSAMError;
import eu.europa.ec.sante.ehdsi.constant.error.IheErrorCode;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.tm.domain.TMResponseStructure;
import org.apache.axis2.util.XMLUtils;
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
import org.xml.sax.SAXException;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Encapsulates all the usage of Transformation Manager for transcoding or translation processes.
 *
 * @author Marcelo Fonseca<code> - marcelo.fonseca@iuz.pt</code>
 */
public final class TMServices {

    private static final Logger LOGGER = LoggerFactory.getLogger(TMServices.class);

    private TMServices() {
    }

    /**
     * Encapsulates the TM usage, by accepting the document to translate, and the selected language.
     *
     * @param byteArray the document to translate, in a byte array form.
     * @param languageCode the language of NPC-B, where the document is being translated.
     * @return a translated document.
     * @throws DocumentTransformationException If the transformation of the CDA document producing an error not listed
     *                                         into the known potential error codes.
     */
    public static byte[] transformDocument(byte[] byteArray, String languageCode) throws DocumentTransformationException {
        TMResponseStructure tmResponse = null;
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            LOGGER.debug("TM - TRANSLATION START.");
            var mapper = new ObjectMapper();
            var node = mapper.createObjectNode();
            var pivotCDA = byteToDocument(byteArray);
            node.put("pivotCDA", getStringFromDocument(pivotCDA));
            node.put("targetLanguageCode", languageCode);
            var jsonString = node.toString();
            var entity = new StringEntity(jsonString, HTTP.UTF_8);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            var translationsAndMappingsUrl = ConfigurationManagerFactory.getConfigurationManager().getProperty("TRANSLATIONS_AND_MAPPINGS_WS_URL");
            LOGGER.info("Translations and Mappings WS URL: '{}'", translationsAndMappingsUrl);
            var postRequest = new HttpPost(translationsAndMappingsUrl + "/transcode");
            postRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            postRequest.setEntity(entity);
            try (CloseableHttpResponse response = httpclient.execute(postRequest)) {
                LOGGER.debug("HTTP statusCode : " + response.getStatusLine().getStatusCode());

                var responseEntity = response.getEntity();
                var encodingHeader = responseEntity.getContentEncoding();
                var encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                        Charsets.toCharset(encodingHeader.getValue());

                var json = EntityUtils.toString(responseEntity, encoding);
                tmResponse = mapper.readValue(json, TMResponseStructure.class);

                var resultDoc = tmResponse.getResponseCDA();
                LOGGER.debug("TM - TRANSLATION STOP");
                return XMLUtils.toOM(resultDoc.getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            if (tmResponse != null && !tmResponse.isStatusSuccess()) {
                throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC,
                        Constants.SERVER_IP,"( " + IheErrorCode.XDSRepositoryError.getCode() + "): + An error has occurred during document translation, please check the following errors:\n"
                        + processErrors(tmResponse.getErrors()));
            } else {
                throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
            }
        }
    }

    /**
     * Encapsulates the TM usage, by accepting the document to translate and transcode to the pivot format.
     *
     * @param byteArray the "friendly" document to translate/transcode, in a byte array form.
     * @return pivot document.
     * @throws DocumentTransformationException If the transformation of the CDA document producing an error not listed
     *                                         into the known potential error codes.
     */
    public static byte[] transformDocument(byte[] byteArray) throws DocumentTransformationException {
        TMResponseStructure tmResponse = null;
        try(CloseableHttpClient httpclient = HttpClients.createDefault()){
            LOGGER.debug("TM - TRANSCODING START.");
            var mapper = new ObjectMapper();
            var node = mapper.createObjectNode();
            var cdaFriendly = byteToDocument(byteArray);
            node.put("friendlyCDA", getStringFromDocument(cdaFriendly));
            var jsonString = node.toString();
            var entity = new StringEntity(jsonString, HTTP.UTF_8);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            var translationsAndMappingsUrl = ConfigurationManagerFactory.getConfigurationManager().getProperty("TRANSLATIONS_AND_MAPPINGS_WS_URL");
            LOGGER.info("Translations and Mappings WS URL: '{}'", translationsAndMappingsUrl);
            var postRequest = new HttpPost(translationsAndMappingsUrl + "/transcode");
            postRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            postRequest.setEntity(entity);
            try (CloseableHttpResponse response = httpclient.execute(postRequest)) {
                LOGGER.debug("HTTP statusCode : " + response.getStatusLine().getStatusCode());

                var responseEntity = response.getEntity();
                var encodingHeader = responseEntity.getContentEncoding();
                var encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                        Charsets.toCharset(encodingHeader.getValue());

                var json = EntityUtils.toString(responseEntity, encoding);
                tmResponse = mapper.readValue(json, TMResponseStructure.class);

                var resultDoc = tmResponse.getResponseCDA();
                LOGGER.debug("TM - TRANSCODING STOP");
                return XMLUtils.toOM(resultDoc.getDocumentElement()).toString().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
        }
    }

    /**
     * @param document CDA Document passed as byte[]
     * @return CDA Document in XML
     * @throws DocumentTransformationException If the transformation to Document failed.
     */
    public static Document byteToDocument(byte[] document) throws DocumentTransformationException {

        try {
            //Convert document byte array into a String.
            String docString = new String(document, StandardCharsets.UTF_8);
            //Parse the String into a Document object.
            return XMLUtil.parseContent(docString);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new DocumentTransformationException(OpenNCPErrorCode.ERROR_GENERIC, ex.getMessage(), ex.getMessage());
        }
    }

    /**
     * Handles errors resulted from the translation process. It currently only prints them to LOG, later to be
     * replaced with portal answer.
     *
     * @param errors List of CDA transformation errors/warnings.
     */
    private static String processErrors(List<ITMTSAMError> errors) {

        StringBuilder sb = new StringBuilder();
        sb.append("List of errors: ");

        LOGGER.debug("TRANSLATION PROCESS ERRORS:");

        for (ITMTSAMError error : errors) {
            LOGGER.debug("Error: (Code: '{}', Description: '{}'", error.getCode(), error.getDescription());
            sb.append("Error: (Code: ").append(error.getCode()).append(", Description: ").append(error.getDescription());
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String getStringFromDocument(Document doc)
    {
        try
        {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
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
