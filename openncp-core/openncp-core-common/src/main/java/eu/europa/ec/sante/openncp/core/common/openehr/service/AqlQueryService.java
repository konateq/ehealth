package eu.europa.ec.sante.openncp.core.common.openehr.service;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.CompositionRequest;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.TemplateRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

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

    // returns complete templates as webtemplate
    List<String> getAvailableTemplatesForPatient(TemplateRequest templateRequest, WebRequest request);

    //returns complete compositions in flat json format
    List<String> getOpenEhrCompositions(CompositionRequest compositionRequest, WebRequest request);
}
