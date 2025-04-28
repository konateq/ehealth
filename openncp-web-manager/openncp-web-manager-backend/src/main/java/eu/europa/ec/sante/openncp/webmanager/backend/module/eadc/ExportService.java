package eu.europa.ec.sante.openncp.webmanager.backend.module.eadc;

import eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.kpi.KpiStrategy;
import eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.kpi.RowIndex;
import eu.europa.ec.sante.openncp.webmanager.backend.module.eadc.persistence.model.Transaction;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class ExportService {

    public final ZoneId zoneId = ZoneId.systemDefault();

    private final String TEMPLATE_FILE = "MyHealth@EU_KPIs-Reporting_template_V1.5.xlsx";

    private final TransactionService transactionService;
    private final List<KpiStrategy> kpiRowStrategies;

    public ExportService(final TransactionService transactionService, final List<KpiStrategy> kpiRowStrategies) {
        this.transactionService = transactionService;
        this.kpiRowStrategies = kpiRowStrategies;
    }

    public byte[] export(final LocalDate fromDate, final LocalDate toDate) {
        final List<Transaction> transactions = transactionService.findTransactions(Pageable.unpaged()).getContent()
                .stream()
                .filter(t -> t.getStartTime() != null)
                .filter(t ->
                        t.getStartTime().compareTo(fromDate.atStartOfDay(zoneId).toInstant()) > 0
                                && t.getStartTime().compareTo(toDate.atStartOfDay(zoneId).toInstant()) < 0)
                .sorted(Comparator.comparing(Transaction::getStartTime))
                .collect(Collectors.toList());

        final ClassLoader classLoader = getClass().getClassLoader();
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final Workbook workbook = WorkbookFactory.create(Objects.requireNonNull(classLoader.getResource(TEMPLATE_FILE)).openStream())) {
            kpiRowStrategies.forEach(kpiStrategy -> {
                final String kpiSheetName = kpiStrategy.getKpiSheetName();
                final Sheet kpiSheet = workbook.getSheet(kpiSheetName);
                if (kpiSheet == null) {
                    throw new RuntimeException(String.format("Sheet [%s] not found in the workbook [%s]", kpiSheetName, workbook));
                }

                final RowIndex rowIndex = RowIndex.firstRowAfterHeaderRow();
                for (final Transaction transaction : kpiStrategy.filterTransactions(transactions)) {
                    final Row transactionRow = kpiSheet.createRow(rowIndex.getCurrentAndIncrement());
                    kpiStrategy.getKpiCells().forEach(kpiCell -> {
                        final Cell cell = transactionRow.createCell(kpiCell.getCellIndex().getCurrent());
                        kpiCell.writeCellValue(transaction).accept(cell);
                    });
                }
            });
            workbook.write(out);
            return out.toByteArray();
        } catch (final IOException e) {
            throw new RuntimeException("Error while exporting transactions", e);
        }
    }
}
