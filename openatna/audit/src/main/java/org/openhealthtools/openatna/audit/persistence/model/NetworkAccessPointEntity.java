package org.openhealthtools.openatna.audit.persistence.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "network_access_points")
public class NetworkAccessPointEntity extends PersistentEntity {

    private static final long serialVersionUID = -1L;

    private Long id;
    private Integer version;
    private Short type;
    private String identifier;

    public NetworkAccessPointEntity() {
    }

    public NetworkAccessPointEntity(Short type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

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

    public Short getType() {
        return type;
    }

    public void setType(Short type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NetworkAccessPointEntity)) {
            return false;
        }

        NetworkAccessPointEntity that = (NetworkAccessPointEntity) o;

        if (!Objects.equals(identifier, that.identifier)) {
            return false;
        }
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "[" + getClass().getName() + " id=" + getId() + ", version=" + getVersion()
                + ", identifier=" + getIdentifier() + ", type=" + getType() + "]";
    }
}
