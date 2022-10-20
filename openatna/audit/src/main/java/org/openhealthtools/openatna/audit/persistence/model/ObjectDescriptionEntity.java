package org.openhealthtools.openatna.audit.persistence.model;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "object_descriptions")
public class ObjectDescriptionEntity extends PersistentEntity {

    private Long id;
    private Integer version;
    private String mppsUids = "";
    private String accessionNumbers = "";
    private Set<SopClassEntity> sopClasses = new HashSet<>();


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

    public String getMppsUids() {
        return mppsUids;
    }

    public void setMppsUids(String mppsUids) {
        this.mppsUids = mppsUids;
    }

    public List<String> mppsUidsAsList() {
        String uids = getMppsUids();
        String[] vals = uids.split(" ");
        List<String> ret = new ArrayList<>();
        for (String val : vals) {
            if (val.length() > 0) {
                ret.add(val);
            }
        }
        return ret;
    }

    public void addMppsUid(String uid) {
        if (getMppsUids().length() == 0) {
            setMppsUids(uid);
        } else {
            setMppsUids(getMppsUids() + " " + uid);
        }
    }

    public List<String> accessionNumbersAsList() {
        String numbers = getAccessionNumbers();
        String[] vals = numbers.split(" ");
        List<String> ret = new ArrayList<>();
        for (String val : vals) {
            if (val.length() > 0) {
                ret.add(val);
            }
        }
        return ret;
    }

    public void addAccessionNumber(String num) {
        if (getAccessionNumbers().length() == 0) {
            setAccessionNumbers(num);
        } else {
            setAccessionNumbers(getAccessionNumbers() + " " + num);
        }
    }

    public String getAccessionNumbers() {
        return accessionNumbers;
    }

    public void setAccessionNumbers(String accessionNumbers) {
        this.accessionNumbers = accessionNumbers;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "descriptions_sop_classes")
    public Set<SopClassEntity> getSopClasses() {
        return sopClasses;
    }

    public void setSopClasses(Set<SopClassEntity> sopClasses) {
        this.sopClasses = sopClasses;
    }

    public void addSopClass(SopClassEntity sce) {
        getSopClasses().add(sce);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectDescriptionEntity that = (ObjectDescriptionEntity) o;

        if (!Objects.equals(accessionNumbers, that.accessionNumbers)) {
            return false;
        }
        return Objects.equals(mppsUids, that.mppsUids);
    }

    public String toString() {
        return "[" + getClass().getName() + " id=" + getId() + ", version=" + getVersion() + ", mppsUids=" + getMppsUids() +
                ", accessionNumbers=" + getAccessionNumbers() + ", SOP Classes=" + getSopClasses() + "]";
    }

    @Override
    public int hashCode() {
        int result = mppsUids != null ? mppsUids.hashCode() : 0;
        result = 31 * result + (accessionNumbers != null ? accessionNumbers.hashCode() : 0);
        return result;
    }
}
