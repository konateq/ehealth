package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domainehealthproperty.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EHNCP_PROPERTY")
public class Property {

    @Id
    @Column(name = "NAME")
    private String key;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "IS_SMP")
    private boolean smp;

    public Property() {
    }

    public Property(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSmp() {
        return smp;
    }

    public void setSmp(boolean smp) {
        this.smp = smp;
    }
}
