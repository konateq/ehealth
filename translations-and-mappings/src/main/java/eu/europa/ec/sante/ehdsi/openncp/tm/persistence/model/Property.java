package eu.europa.ec.sante.ehdsi.openncp.tm.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EHNCP_PROPERTY")
public class Property {

    @Id
    private String name;

    private String value;

    @Column(name = "IS_SMP")
    private boolean smp;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isSmp() {
        return smp;
    }
}
