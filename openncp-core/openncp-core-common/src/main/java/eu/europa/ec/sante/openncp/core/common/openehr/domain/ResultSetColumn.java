package eu.europa.ec.sante.openncp.core.common.openehr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a column in a result set.
 * <p>
 * It contains the name of the column and an optional path.
 *
 * @author Renaud Subiger
 * @since 9.0
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResultSetColumn {

    @JsonProperty("name")
    private final String name;

    @JsonProperty("path")
    private String path;

    @JsonCreator
    public ResultSetColumn(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ResultSetColumn{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
