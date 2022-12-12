package eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc;

import eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.model.Transaction;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class ExportService {

    public final ZoneId zoneId = ZoneId.systemDefault();

    private final String TEMPLATE_FILE = "eHDSI_KPIs-Reporting_template_V1.1.1.xlsx";

    private final String SHEET_KPI_1_2 = "KPI-1.2";
    private final String SHEET_KPI_1_3 = "KPI-1.3";
    private final String SHEET_KPI_1_4 = "KPI-1.4";
    private final String SHEET_KPI_1_5 = "KPI-1.5";
    private final String SHEET_KPI_1_6 = "KPI-1.6";
    private final String SHEET_KPI_1_7 = "KPI-1.7";

    private final TransactionService transactionService;

    public ExportService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public byte[] export(LocalDate fromDate, LocalDate toDate) {

        List<Transaction> transactions = transactionService.findTransactions(Pageable.unpaged()).getContent();

        //Filter transactions between the dates
        List<Transaction> filteredTransactions = transactions.stream().
                filter(t -> t.getStartTime() != null).
                filter(t ->
                        t.getStartTime().compareTo(fromDate.atStartOfDay(zoneId).toInstant()) > 0
                                && t.getStartTime().compareTo(toDate.atStartOfDay(zoneId).toInstant()) < 0)
                .collect(Collectors.toList());

        ClassLoader classLoader = getClass().getClassLoader();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Workbook workbook = WorkbookFactory.create(Objects.requireNonNull(classLoader.getResource(TEMPLATE_FILE)).openStream())) {

            writeTransactions(workbook.getSheet(SHEET_KPI_1_2), getTransactionsForKPI_1_2(filteredTransactions));
            writeTransactions(workbook.getSheet(SHEET_KPI_1_3), getTransactionsForKPI_1_3(filteredTransactions));
            writeTransactions(workbook.getSheet(SHEET_KPI_1_4), getTransactionsForKPI_1_4(filteredTransactions));
            writeTransactions(workbook.getSheet(SHEET_KPI_1_5), getTransactionsForKPI_1_5(filteredTransactions));
            writeTransactions(workbook.getSheet(SHEET_KPI_1_6), getTransactionsForKPI_1_6(filteredTransactions));
            writeTransactions(workbook.getSheet(SHEET_KPI_1_7), getTransactionsForKPI_1_7(filteredTransactions));

            workbook.write(out);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void writeTransactions(Sheet sheet, List<Transaction> transactions) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        int rowCount = 0;
        for (Transaction transaction : transactions) {

            Row row = sheet.createRow(++rowCount);

            Cell cell = row.createCell(0);
            cell.setCellValue(transaction.getHomeISO());

            cell = row.createCell(1);
            cell.setCellValue(transaction.getStartTime().atZone(zoneId).getYear());

            cell = row.createCell(2);
            cell.setCellValue(transaction.getStartTime().atZone(zoneId).get(IsoFields.QUARTER_OF_YEAR));

            cell = row.createCell(3);
            cell.setCellValue(transaction.getSndISO());

            cell = row.createCell(4);
            cell.setCellValue(transaction.getReceivingISO());

            cell = row.createCell(5);
            cell.setCellValue(formatter.format(transaction.getStartTime().atZone(zoneId)));

            cell = row.createCell(6);
            cell.setCellValue(formatter.format(transaction.getEndTime().atZone(zoneId)));

            cell = row.createCell(7);
            switch (sheet.getSheetName()) {
                case SHEET_KPI_1_2:
                    cell.setCellValue(transaction.getServiceType());
                    break;
                case SHEET_KPI_1_3:
                case SHEET_KPI_1_4:
                case SHEET_KPI_1_5:
                case SHEET_KPI_1_6:
                case SHEET_KPI_1_7:
                    cell.setCellValue(transaction.getTransactionData().get(0).getValueDisplay());
                    break;
            }

        }
    }

    private List<Transaction> getTransactionsForKPI_1_2(List<Transaction> transactions) {
        List<String> transactionTypes = new ArrayList<>();

        transactionTypes.add("PATIENT_IDENTIFICATION_QUERY");
        transactionTypes.add("PATIENT_IDENTIFICATION_RESPONSE");
        transactionTypes.add("PATIENT_IDENTIFICATION_UNKNOWN");
        transactionTypes.add("DOCUMENT_LIST_QUERY");
        transactionTypes.add("DOCUMENT_LIST_RESPONSE");
        transactionTypes.add("DOCUMENT_LIST_UNKNOWN");
        transactionTypes.add("DOCUMENT_EXCHANGED_QUERY");
        transactionTypes.add("DOCUMENT_EXCHANGED_RESPONSE");
        transactionTypes.add("DOCUMENT_EXCHANGED_UNKNOWN");
        transactionTypes.add("DISPENSATION_QUERY");
        transactionTypes.add("DISPENSATION_RESPONSE");
        transactionTypes.add("DISPENSATION_UNKNOWN");
        transactionTypes.add("DISPENSATION_DISCARD_REQUEST");

        return transactions.stream().filter(
                transaction -> transactionTypes.contains(transaction.getServiceType())).collect(Collectors.toList());

    }

    private List<Transaction> getTransactionsForKPI_1_3(List<Transaction> transactions) {
        //ePrescription
        List<String> dataValues = new ArrayList<>();

        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.1");
        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.6");

        return transactions.stream().filter(transaction ->
                CollectionUtils.isNotEmpty(transaction.getTransactionData())
                        && dataValues.contains(transaction.getTransactionData().get(0).getDataValue())).collect(Collectors.toList());

    }

    private List<Transaction> getTransactionsForKPI_1_4(List<Transaction> transactions) {
        //eDispensation
        return transactions.stream().filter(transaction ->
                CollectionUtils.isNotEmpty(transaction.getTransactionData())
                        && transaction.getTransactionData().get(0).getDataValue().equals("1.3.6.1.4.1.12559.11.10.1.3.1.1.2")).collect(Collectors.toList());
    }

    private List<Transaction> getTransactionsForKPI_1_5(List<Transaction> transactions) {
        //Patient Summary

        List<String> dataValues = new ArrayList<>();

        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.3");
        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.7");

        return transactions.stream().filter(transaction ->
                CollectionUtils.isNotEmpty(transaction.getTransactionData())
                        && dataValues.contains(transaction.getTransactionData().get(0).getDataValue())).collect(Collectors.toList());
    }

    private List<Transaction> getTransactionsForKPI_1_6(List<Transaction> transactions) {
        //eDispensation discard
        return transactions.stream().filter(transaction ->
                CollectionUtils.isNotEmpty(transaction.getTransactionData())
                        && transaction.getTransactionData().get(0).getDataValue().equals("1.3.6.1.4.1.12559.11.10.1.3.1.1.2-DISCARD")).collect(Collectors.toList());
    }

    private List<Transaction> getTransactionsForKPI_1_7(List<Transaction> transactions) {

        //OrCD Document

        List<String> dataValues = new ArrayList<>();

        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.8");
        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.9");
        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.10");
        dataValues.add("1.3.6.1.4.1.12559.11.10.1.3.1.1.11");

        return transactions.stream().filter(transaction ->
                CollectionUtils.isNotEmpty(transaction.getTransactionData())
                        && dataValues.contains(transaction.getTransactionData().get(0).getDataValue())).collect(Collectors.toList());
    }
}
