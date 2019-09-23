package se.sb.epsos.web.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sb.epsos.web.model.CountryVO;
import tr.com.srdc.epsos.util.FileUtil;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class InternationalConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternationalConfigManager.class);
    private static Map<String, XMLConfiguration> config = new HashMap<>();

    public InternationalConfigManager(List<CountryVO> countryCodes) {

        super();
        String path = System.getProperty("epsos-internationalsearch-config-path");
        LOGGER.debug("International Search Configuration Path: '{}'", path);
        if (path == null) {
            final String err = "epsos-internationalsearch-config-path is not set";
            LOGGER.error(err);
            throw new IllegalArgumentException(err);
        }
        for (CountryVO countryCode : countryCodes) {

            File iSearchFile = new File(path + "/InternationalSearch_" + countryCode.getId() + ".xml");
            if (iSearchFile.exists() && iSearchFile.isFile()) {

                String xmlConfig = removeNSAndPreamble(iSearchFile);
                LOGGER.info("'{}':\n'{}'", iSearchFile.getName(), xmlConfig);
                try {
                    XMLConfiguration xmlConfiguration = new XMLConfiguration();
                    xmlConfiguration.load(new ByteArrayInputStream(xmlConfig.getBytes(StandardCharsets.UTF_8)));
                    config.put(countryCode.getId(), xmlConfiguration);
                } catch (ConfigurationException e) {
                    LOGGER.error("ConfigurationException", e);
                }
            }
        }
    }

    public static List<Properties> getProperties(String country, String prefix) {

        XMLConfiguration xmlConfig = config.get(country);
        List<Properties> propList = new ArrayList<>();
        if (xmlConfig != null) {
            Iterator<?> keys = xmlConfig.getKeys(prefix);
            while (keys.hasNext()) {
                String key = (String) keys.next();
                List<String> list = getList(country, key);
                int i = 0;
                for (String str : list) {
                    Properties props = new Properties();
                    props.put(key + i, str);
                    propList.add(props);
                    i++;
                }
            }
        }
        return propList;
    }

    public static String get(String country, String key) {

        XMLConfiguration xmlConfig = config.get(country);
        return xmlConfig.getString(key);
    }

    public static List<String> getList(String country, String key) {

        XMLConfiguration xmlConfig = config.get(country);
        if (xmlConfig == null) {
            return Collections.emptyList();
        }
        return (List<String>) (List) xmlConfig.getList(key);
    }

    private static String removeNSAndPreamble(File file) {

        String xmlString = FileUtil.readWholeFile(file);
        String xsltString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:output method=\"xml\" indent=\"yes\" encoding=\"UTF-8\"/><xsl:template match=\"/\"><xsl:copy><xsl:apply-templates/></xsl:copy></xsl:template><xsl:template match=\"@*\"><xsl:attribute name=\"{local-name()}\"><xsl:value-of select=\"current()\"/></xsl:attribute></xsl:template><xsl:template match=\"*\"><xsl:element name=\"{local-name()}\"><xsl:apply-templates select=\"@* | * | text()\"/></xsl:element></xsl:template><xsl:template match=\"text()\"><xsl:copy><xsl:value-of select=\"current()\"/></xsl:copy></xsl:template></xsl:stylesheet>";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new ByteArrayInputStream(xsltString.getBytes()));
            Transformer transformer = factory.newTransformer(xslt);
            Source xmlSrc = new StreamSource(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
            transformer.transform(xmlSrc, new StreamResult(outputStream));
            return outputStream.toString();
        } catch (TransformerException | IOException e) {
            LOGGER.error(e.getMessage());
        }
        LOGGER.error("Unable to clean Namespace and Preamble from xml source, will use unmodified xml.");
        return xmlString;
    }
}
