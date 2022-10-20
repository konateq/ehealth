package eu.europa.ec.sante.ehdsi.openncp.gateway.module.atna.persistence.model;

import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Entity
@Table(name = "errors")
public class Error {

    @Id
    private Long id;

    private String errorMessage;

    private Instant errorTimestamp;

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] payload = new byte[0];

    private String sourceIp;

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] stackTrace = new byte[0];

    private String version;

    public Long getId() {
        return id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getErrorTimestamp() {
        return errorTimestamp;
    }

    public String getPayload() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getStackTrace() {
        return new String(stackTrace, StandardCharsets.UTF_8);
    }

    public String getVersion() {
        return version;
    }
}
