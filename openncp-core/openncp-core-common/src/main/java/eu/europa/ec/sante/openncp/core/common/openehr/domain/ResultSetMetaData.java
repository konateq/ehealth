package eu.europa.ec.sante.openncp.core.common.openehr.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents the metadata of a result set.
 * <p>
 * It contains information such as the href, type, schema version, creation date, etc.
 *
 * @author Renaud Subiger
 * @since 9.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultSetMetaData {

    @JsonProperty("_href")
    private String href;

    @JsonProperty("_type")
    private String type;

    @JsonProperty("_schema_version")
    private String schemaVersion;

    @JsonProperty("_created")
    private OffsetDateTime created;

    @JsonProperty("_generator")
    private String generator;

    @JsonProperty("_executed_aql")
    private String executedAql;

    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getExecutedAql() {
        return executedAql;
    }

    public void setExecutedAql(String executedAql) {
        this.executedAql = executedAql;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void addAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    @Override
    public String toString() {
        return "ResultSetMetaData{" +
                "href='" + href + '\'' +
                ", type='" + type + '\'' +
                ", schemaVersion='" + schemaVersion + '\'' +
                ", created=" + created +
                ", generator='" + generator + '\'' +
                ", executedAql='" + executedAql + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
