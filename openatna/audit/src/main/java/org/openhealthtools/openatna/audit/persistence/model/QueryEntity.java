package org.openhealthtools.openatna.audit.persistence.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Objects;

public class QueryEntity extends PersistentEntity {

    private Long id;
    private Integer version;
    private String name;
    private String query;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        QueryEntity that = (QueryEntity) o;

        if (!Objects.equals(name, that.name)) {
            return false;
        }
        return Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "[" + getClass().getName() + " id=" + getId() + ", version=" + getVersion() + ", name=" + getName() + ", query=" + getQuery() + "]";
    }
}
