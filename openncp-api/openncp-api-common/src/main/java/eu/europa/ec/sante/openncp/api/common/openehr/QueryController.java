package eu.europa.ec.sante.openncp.api.common.openehr;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.service.AqlQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;

/**
 * Controller for executing AQL queries.
 *
 * @author Renaud Subiger
 * @see <a href="https://specifications.openehr.org/releases/ITS-REST/Release-1.0.3/query.html">openEHR REST specifications</a>
 * @since 9.0
 */
@RestController
@RequestMapping(path = "/openehr/v1/query")
public class QueryController {

    private final AqlQueryService aqlQueryService;

    public QueryController(AqlQueryService aqlQueryService) {
        Assert.notNull(aqlQueryService, "AqlQueryService must not be null");
        this.aqlQueryService = aqlQueryService;
    }

    @PostMapping(path = "/aql")
    public ResponseEntity<ResultSet> executeAdhocQuery(@RequestBody @Valid AdhocQueryExecute queryRequest,
                                                       WebRequest request) {
        ResultSet resultSet = aqlQueryService.executeAdhocQuery(queryRequest, request);
        return ResponseEntity.ok(resultSet);
    }
}
