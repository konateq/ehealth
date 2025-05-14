package eu.europa.ec.sante.openncp.core.common.openehr.service;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import org.springframework.web.context.request.WebRequest;

/**
 * Service interface for executing AQL queries.
 *
 * @author Renaud Subiger
 * @since 9.0
 */
public interface AqlQueryService {

    /**
     * Executes an ad-hoc AQL query on the underlying openEHR repository.
     *
     * @param queryRequest the AQL query request
     * @param request      the web request
     * @return the result set of the query
     */
    ResultSet executeAdhocQuery(AdhocQueryExecute queryRequest, WebRequest request);
}
