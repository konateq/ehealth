package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "code_system")
public class CodeSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String oid;

    private String name;

    private String description;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "codeSystem", orphanRemoval = true)
    private List<CodeSystemVersion> versions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CodeSystemVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<CodeSystemVersion> versions) {
        this.versions = versions;
    }
}
