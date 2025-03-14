package eu.europa.ec.sante.openncp.core.common.ihe.abusedetection;

import org.apache.commons.lang3.builder.ToStringBuilder;

public enum AbuseType {
    ALL("ALL"),
    POC("POC"),
    PAT("PAT");

    private final String type;

    AbuseType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .toString();
    }
}
