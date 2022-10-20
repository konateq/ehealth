package org.openhealthtools.openatna.audit.persistence.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sop_classes")
public class SopClassEntity extends PersistentEntity {

    private Long id;
    private Integer version;

    private String sopId;
    private Integer numberOfInstances = 0;
    private String instanceUids = "";

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

    public String getSopId() {
        return sopId;
    }

    public void setSopId(String sopId) {
        this.sopId = sopId;
    }

    public Integer getNumberOfInstances() {
        return numberOfInstances;
    }

    public void setNumberOfInstances(Integer numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public String getInstanceUids() {
        return instanceUids;
    }

    public void setInstanceUids(String instanceUids) {
        this.instanceUids = instanceUids;
    }

    public List<String> instanceUidsAsList() {
        String uids = getInstanceUids();
        String[] vals = uids.split(" ");
        List<String> ret = new ArrayList<>();
        for (String val : vals) {
            if (val.length() > 0) {
                ret.add(val);
            }
        }
        return ret;
    }

    public void addInstanceUid(String uid) {
        if (getInstanceUids().length() == 0) {
            setInstanceUids(uid);
        } else {
            setInstanceUids(getInstanceUids() + " " + uid);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SopClassEntity that = (SopClassEntity) o;

        if (!Objects.equals(instanceUids, that.instanceUids)) {
            return false;
        }
        if (!Objects.equals(numberOfInstances, that.numberOfInstances)) {
            return false;
        }
        return Objects.equals(sopId, that.sopId);
    }

    public String toString() {
        return "[" + getClass().getName() +
                " id=" +
                getId() +
                ", version=" +
                getVersion() +
                ", instanceUids=" +
                getInstanceUids() +
                ", numberOfInstances=" +
                getNumberOfInstances() +
                ", sopId=" +
                getSopId() +
                "]";

    }

    @Override
    public int hashCode() {
        int result = sopId != null ? sopId.hashCode() : 0;
        result = 31 * result + (numberOfInstances != null ? numberOfInstances.hashCode() : 0);
        result = 31 * result + (instanceUids != null ? instanceUids.hashCode() : 0);
        return result;
    }
}
