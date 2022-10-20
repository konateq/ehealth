package eu.europa.ec.sante.ehdsi.openncp.gateway.module.atna.persistence.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    private Long id;

    private String eventActionCode;

    private Instant eventDateTime;

    private String eventOutcome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Code eventId;

    @ManyToMany
    @JoinTable(
            name = "event_types_codes",
            joinColumns = @JoinColumn(name = "MessageEntity_id"),
            inverseJoinColumns = @JoinColumn(name = "eventTypeCodes_id")
    )
    private Set<Code> eventTypes = new LinkedHashSet<>();

    @OneToMany
    @JoinTable(name = "messages_mparticipants")
    private Set<MessageParticipantEntity> messageParticipants = new LinkedHashSet<>();

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] messageContent = new byte[0];

    public Long getId() {
        return id;
    }

    public String getEventActionCode() {
        return eventActionCode;
    }

    public Instant getEventDateTime() {
        return eventDateTime;
    }

    public String getEventOutcome() {
        return eventOutcome;
    }

    public Code getEventId() {
        return eventId;
    }

    public Set<Code> getEventTypes() {
        return eventTypes;
    }

    public Set<MessageParticipantEntity> getMessageParticipants() {
        return messageParticipants;
    }

    public byte[] getMessageContent() {
        return messageContent;
    }
}
