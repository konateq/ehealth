package eu.europa.ec.sante.openncp.api.common.openehr;

import eu.europa.ec.sante.openncp.core.common.openehr.domain.AdhocQueryExecute;
import eu.europa.ec.sante.openncp.core.common.openehr.domain.ResultSet;
import eu.europa.ec.sante.openncp.core.common.openehr.service.AqlQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.request.WebRequest;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Renaud Subiger
 * @since 9.0
 */
@WebMvcTest(controllers = QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AqlQueryService queryService;

    @Test
    void executeAdhocQuery_validRequest_shouldReturnOk() throws Exception {
        ResultSet resultSet = new ResultSet(Collections.emptyList());

        when(queryService.executeAdhocQuery(any(AdhocQueryExecute.class), any(WebRequest.class)))
                .thenReturn(resultSet);

        mockMvc.perform(MockMvcRequestBuilders.post("/openehr/v1/query/aql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n" +
                                "  \"q\": \"SELECT e/ehr_id/value, c/context/start_time/value as startTime, obs/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude AS systolic, c/uid/value AS cid, c/name FROM EHR e CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1] CONTAINS OBSERVATION obs[openEHR-EHR-OBSERVATION.blood_pressure.v1] WHERE obs/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value/magnitude >= $systolic_bp\",\n" +
                                "  \"offset\": 0,\n" +
                                "  \"fetch\": 10,\n" +
                                "  \"query_parameters\": {\n" +
                                "    \"ehr_id\": \"7d44b88c-4199-4bad-97dc-d78268e01398\",\n" +
                                "    \"systolic_bp\": 140\n" +
                                "  }\n" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"rows\": [] }"));

        verify(queryService).executeAdhocQuery(any(AdhocQueryExecute.class), any(WebRequest.class));
    }

    @Test
    void executeAdhocQuery_invalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/openehr/v1/query/aql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest());

        verify(queryService, never()).executeAdhocQuery(any(AdhocQueryExecute.class), any(WebRequest.class));
    }
}
