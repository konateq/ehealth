package eu.europa.ec.sante.openncp.core.common.ihe.abusedetection;

import org.apache.commons.lang3.builder.ToStringBuilder;

public enum AbuseTransactionType {
    TRANSACTION_UNKNOWN("?"),
    XCPD_SERVICE_REQUEST("XCPD_SERVICE_REQUEST"),
    XCA_SERVICE_REQUEST("XCA_SERVICE_REQUEST"),
    XDR_SERVICE_REQUEST("XDR_SERVICE_REQUEST");

    private final String transaction;

    AbuseTransactionType(String transaction) {
        this.transaction = transaction;
    }

    public String getTransaction() {
        return transaction;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("transaction", transaction)
                .toString();
    }
}
