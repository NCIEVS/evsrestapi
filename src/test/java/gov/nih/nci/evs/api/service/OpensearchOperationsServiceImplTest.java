package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

/** Checks that writes without entity IDs can be replayed safely. */
class OpensearchOperationsServiceImplTest {

  private final OpenSearchOperations operations = mock(OpenSearchOperations.class);
  private final OpensearchOperationsServiceImpl service = new OpensearchOperationsServiceImpl();

  @BeforeEach
  void setUp() {
    service.operations = operations;
  }

  @Test
  void auditIndexGetsStableRequestId() throws Exception {
    service.index(new Audit(), OpensearchOperationsService.AUDIT_INDEX, Audit.class);

    ArgumentCaptor<IndexQuery> query = ArgumentCaptor.forClass(IndexQuery.class);
    verify(operations).index(query.capture(), any(IndexCoordinates.class));
    assertNotNull(query.getValue().getId());
  }

  @Test
  void metricIndexGetsStableRequestId() throws Exception {
    service.loadMetric(new Metric(), "test_metric");

    ArgumentCaptor<IndexQuery> query = ArgumentCaptor.forClass(IndexQuery.class);
    verify(operations).index(query.capture(), any(IndexCoordinates.class));
    assertNotNull(query.getValue().getId());
  }
}
