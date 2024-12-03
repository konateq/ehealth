package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonUtils {

    public static Map<String, Object> jsonFileToMap(final String path) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final InputStream is = JsonUtils.class.getResourceAsStream(path);

        return mapper.readValue(is, new TypeReference<>() {
        });
    }
}
