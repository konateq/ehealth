package org.openhealthtools.openatna.audit.persistence.model;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Entity
@Table(name = "provisional_messages")
public class ProvisionalEntity extends PersistentEntity {

    private Long id;
    private Integer version;
    private byte[] content;


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

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProvisionalEntity that = (ProvisionalEntity) o;

        return Arrays.equals(content, that.content);
    }

    public String toString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public int hashCode() {
        return content != null ? Arrays.hashCode(content) : 0;
    }
}
