package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transcoding_association")
public class Mapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="transcoding_association_id")
    private Long transcodingAssociationId = 1L;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_CONCEPT_ID")
    private Concept source;

    @ManyToOne
    @JoinColumn(name = "TARGET_CONCEPT_ID")
    private Concept target;

    private String quality;

    private String status;

    @Column(name = "status_date")
    private LocalDateTime statusDate;

    public Long getId() {
        return id;
    }


    public Concept getSource() {
        return source;
    }

    public void setSource(Concept source) {
        this.source = source;
    }

    public Concept getTarget() {
        return target;
    }

    public void setTarget(Concept target) {
        this.target = target;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
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
}
