package eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc;

import eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.model.Transaction;
import eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<Transaction> findTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Transaction getTransaction(String id) {
    return transactionRepository.findById(id)
               .orElseThrow(() -> new RuntimeException("Not Found"));
    }

    public List<String> getYears() {
        return transactionRepository.findAllYears();
    }
}
