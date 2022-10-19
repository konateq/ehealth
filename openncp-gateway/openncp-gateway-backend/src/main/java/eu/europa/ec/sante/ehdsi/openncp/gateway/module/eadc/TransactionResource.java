package eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc;

import eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/eadc")
public class TransactionResource {

    private final Logger logger = LoggerFactory.getLogger(TransactionResource.class);
    private final ExportService exportService;
    private final TransactionService transactionService;

    public TransactionResource(TransactionService transactionService, ExportService exportService) {
        this.transactionService = transactionService;
        this.exportService = exportService;
    }

    @GetMapping(path = "/transactions")
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("[API] Listing eADC Transactions");
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<Transaction> page = transactionService.findTransactions(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping(path = "/transactions/{id}")
    public ResponseEntity<Transaction> getTransactions(@PathVariable("id")  String id) {
        logger.info("[API] Retrieving Transaction: '{}'", id);
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping(path = "/transactions/exportFromTo")
    public ResponseEntity<ByteArrayResource> getMathResume(@RequestParam("fromDate")
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                           @RequestParam("toDate")
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        logger.info("[API] Exporting KPIs report:");
        ByteArrayResource resource = new ByteArrayResource(exportService.export(fromDate, toDate));
        return ResponseEntity.ok()
                .contentLength(resource.contentLength())
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(resource);
    }

    @GetMapping(path = "/transactions/getYears")
    public ResponseEntity<List<String>> getTransactionsYears() {
        logger.info("[API] Retrieving Years");
        List<String> years = new ArrayList<>();
        years = transactionService.getYears();
        return ResponseEntity.ok(years);
    }
}
