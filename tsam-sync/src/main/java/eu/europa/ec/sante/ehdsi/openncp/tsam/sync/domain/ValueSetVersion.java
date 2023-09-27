package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "value_set_version")
@SuppressWarnings("unused")
public class ValueSetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "VERSION_NAME")
    private String name;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    private String status;

    @Column(name = "status_date")
    private LocalDateTime statusDate;

    private String description;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "VALUE_SET_ID")
    private ValueSet valueSet;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "X_CONCEPT_VALUE_SET",
            joinColumns = @JoinColumn(name = "VALUE_SET_VERSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "CODE_SYSTEM_CONCEPT_ID"))
    private List<Concept> concepts = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ValueSet getValueSet() {
        return valueSet;
    }

    public void setValueSet(ValueSet valueSet) {
        this.valueSet = valueSet;
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(LocalDateTime statusDate) {
        this.statusDate = statusDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<Concept> concepts) {
        this.concepts = concepts;
    }
}
