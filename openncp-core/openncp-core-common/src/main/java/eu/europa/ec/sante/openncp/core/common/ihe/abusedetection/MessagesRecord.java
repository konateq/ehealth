package eu.europa.ec.sante.openncp.core.common.ihe.abusedetection;

import java.time.LocalDateTime;

public class MessagesRecord {
    private Long id;
    private String xml;
    private LocalDateTime eventDateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }
}
