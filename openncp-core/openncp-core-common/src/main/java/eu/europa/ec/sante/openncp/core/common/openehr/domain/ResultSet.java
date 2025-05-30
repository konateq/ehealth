package eu.europa.ec.sante.openncp.core.common.openehr.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a result set from an AQL query.
 * <p>
 * It contains metadata about the result set, including the columns and rows of data.
 *
 * @author Renaud Subiger
 * @since 9.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultSet {

    @JsonProperty("meta")
    private ResultSetMetaData meta;

    @JsonProperty("name")
    private String name;

    @JsonProperty("q")
    private String query;

    @JsonProperty("columns")
    private final List<ResultSetColumn> columns = new ArrayList<>();

    @JsonProperty("rows")
    private final List<List<Object>> rows = new ArrayList<>();

    @JsonCreator
    public ResultSet(@JsonProperty("rows") List<List<Object>> rows) {
        this.rows.addAll(rows);
    }

    public ResultSetMetaData getMeta() {
        return meta;
    }

    public void setMeta(ResultSetMetaData meta) {
        this.meta = meta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<ResultSetColumn> getColumns() {
        return columns;
    }

    public void addColumn(ResultSetColumn column) {
        columns.add(column);
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "ResultSet{" +
                "meta=" + meta +
                ", name='" + name + '\'' +
                ", query='" + query + '\'' +
                ", columns=" + columns +
                ", rows=" + rows +
                '}';
    }
}
