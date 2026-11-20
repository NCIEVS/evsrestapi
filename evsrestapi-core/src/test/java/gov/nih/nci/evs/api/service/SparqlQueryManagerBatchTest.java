package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Failed, cancelled, and incomplete batches must never be returned for indexing. */
class SparqlQueryManagerBatchTest {
  private static final String EMPTY = "{\"results\":{\"bindings\":[]}}";
  private final SparqlQueryManagerServiceImpl service = new SparqlQueryManagerServiceImpl();
  private final RESTUtils rest = mock(RESTUtils.class);
  private final Terminology terminology = new Terminology();
  private final Concept concept = new Concept();
  private HierarchyUtils hierarchy;

  @BeforeEach
  void configure() {
    terminology.setTerminology("ctcae6");
    terminology.setVersion("6.0");
    terminology.setMetadata(new TerminologyMetadata());
    concept.setCode("C123");
    concept.setName("Test concept");
    hierarchy = new HierarchyUtils(terminology);
    ReflectionTestUtils.setField(hierarchy, "pathsMap", new HashMap<>());
    service.graphProperties = new GraphProperties();
    service.graphProperties.setQueryUrl("http://graph/query");
    service.graphProperties.setBatchTimeoutSeconds(1);
    service.queryBuilderService = mock(QueryBuilderService.class);
    when(service.queryBuilderService.constructPrefix(any())).thenReturn("");
    when(service.queryBuilderService.constructBatchQuery(anyString(), any(), anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ReflectionTestUtils.setField(service, "restUtils", rest);
    when(rest.runSPARQL(anyString(), eq("http://graph/query"))).thenReturn(EMPTY);
  }

  @Test
  void completeBatchWithoutAxiomsIsValid() throws Exception {
    assertEquals(1, service.getConcepts(List.of(concept), terminology, hierarchy).size());
  }

  @Test
  void workerFailuresAreReportedInsteadOfReturningPartialData() {
    when(rest.runSPARQL(eq("axioms.batch"), anyString()))
        .thenThrow(new IllegalStateException("Graph unavailable"));
    ExecutionException error =
        assertThrows(
            ExecutionException.class,
            () -> service.getConcepts(List.of(concept), terminology, hierarchy));
    assertEquals("Graph unavailable", error.getCause().getCause().getMessage());
  }

  @Test
  void timeoutCancelsWorkersAndDoesNotReturnPartialData() throws Exception {
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    blockAxioms(new CountDownLatch(1), interrupted, release);
    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(4),
          () ->
              assertThrows(
                  TimeoutException.class,
                  () -> service.getConcepts(List.of(concept), terminology, hierarchy)));
      assertTrue(interrupted.await(2, TimeUnit.SECONDS), "Timed-out workers must be cancelled");
    } finally {
      release.countDown();
    }
  }

  @Test
  void callerInterruptionCancelsWorkersAndPreservesInterruptFlag() throws Exception {
    service.graphProperties.setBatchTimeoutSeconds(60);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    blockAxioms(started, interrupted, release);
    AtomicBoolean flag = new AtomicBoolean();
    Thread caller =
        Thread.ofPlatform()
            .start(
                () -> {
                  try {
                    service.getConcepts(List.of(concept), terminology, hierarchy);
                  } catch (InterruptedException expected) {
                    flag.set(Thread.currentThread().isInterrupted());
                  } catch (Exception unexpected) {
                    throw new AssertionError(unexpected);
                  }
                });
    try {
      assertTrue(started.await(3, TimeUnit.SECONDS));
      caller.interrupt();
      caller.join(3000);
      assertFalse(caller.isAlive());
      assertTrue(flag.get());
      assertTrue(interrupted.await(2, TimeUnit.SECONDS));
    } finally {
      release.countDown();
      caller.interrupt();
      caller.join(3000);
    }
  }

  private void blockAxioms(
      CountDownLatch started, CountDownLatch interrupted, CountDownLatch release) {
    when(rest.runSPARQL(eq("axioms.batch"), anyString()))
        .thenAnswer(
            invocation -> {
              started.countDown();
              try {
                release.await(10, TimeUnit.SECONDS);
              } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Cancelled", e);
              }
              return EMPTY;
            });
  }
}
