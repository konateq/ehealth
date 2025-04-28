package eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.kpi;

import eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.kpi.cells.*;
import eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.persistence.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Kpi1_2Strategy implements KpiStrategy {
    private final List<String> transactionTypes =
            List.of("PATIENT_IDENTIFICATION_QUERY",
                    "PATIENT_IDENTIFICATION_RESPONSE",
                    "PATIENT_IDENTIFICATION_UNKNOWN", // not used anymore but has been historically, best not to remove it yet
                    "DOCUMENT_LIST_QUERY",
                    "DOCUMENT_LIST_RESPONSE",
                    "DOCUMENT_LIST_UNKNOWN", // not used anymore but has been historically, best not to remove it yet
                    "DOCUMENT_EXCHANGED_QUERY",
                    "DOCUMENT_EXCHANGED_RESPONSE",
                    "DOCUMENT_EXCHANGED_UNKNOWN", // not used anymore but has been historically, best not to remove it yet
                    "DISPENSATION_QUERY",
                    "DISPENSATION_RESPONSE",
                    "DISPENSATION_UNKNOWN", // not used anymore but has been historically, best not to remove it yet
                    "FHIR_SEARCH",
                    "FHIR_READ"
            );

    @Override
    public KpiIdentifier getKpiIdentifier() {
        return KpiIdentifier.KPI_1_2;
    }

    @Override
    public List<KpiCell> getKpiCells() {
        return List.of(
                new MemberstateCell(0),
                new StartTimeYearCell(1),
                new StartTimeQuarterCell(2),
                new SenderIsoCell(3),
                new ReceiverIsoCell(4),
                new StartTimeDateTimeCell(5),
                new EndTimeDateTimeCell(6),
                new TransactionTypeCell(7),
                new ResultCell(8),
                new ErrorCell(9),
                new SenderMessageIdCell(11),
                new ReceiverMessageIdCell(12)
        );
    }

    @Override
    public List<Transaction> filterTransactions(final List<Transaction> transactions) {
        return transactions.stream().filter(
                transaction -> transactionTypes.contains(transaction.getServiceType())).collect(Collectors.toList());

    }
}
