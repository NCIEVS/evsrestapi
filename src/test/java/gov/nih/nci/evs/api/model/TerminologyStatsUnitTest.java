package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import org.junit.jupiter.api.Test;

/** Unit test for {@link TerminologyStats}. */
public class TerminologyStatsUnitTest {

  /**
   * Test concept stat aggregation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRecordConcept() throws Exception {
    final TerminologyStats stats = new TerminologyStats();
    stats.setTerminology("ncit");
    stats.setVersion("25.01d");

    final Concept active = new Concept("ncit", "C1", "Concept 1");
    active.setActive(true);
    active.getSynonyms().add(new Synonym());
    active.getSynonyms().add(new Synonym());
    active.getDefinitions().add(new Definition());
    active.getProperties().add(new Property("P1", "V1"));
    active.getParents().add(new Concept("PARENT"));
    active.getChildren().add(new Concept("CHILD"));
    active.getAssociations().add(new Association());
    active.getInverseAssociations().add(new Association());
    active.getRoles().add(new Role());
    active.getInverseRoles().add(new Role());
    active.getMaps().add(new Mapping());

    final Concept inactive = new Concept();
    inactive.setActive(false);
    inactive.getProperties().add(new Property("P2", "V2"));

    stats.recordConcept(active);
    stats.recordConcept(inactive);

    assertFalse(stats.isEmpty());
    assertEquals(2, stats.getConceptCount());
    assertEquals(1, stats.getCodeCount());
    assertEquals(1, stats.getActiveConceptCount());
    assertEquals(1, stats.getInactiveConceptCount());
    assertEquals(2, stats.getSynonymCount());
    assertEquals(1, stats.getDefinitionCount());
    assertEquals(2, stats.getPropertyCount());
    assertEquals(1, stats.getParentReferenceCount());
    assertEquals(1, stats.getChildReferenceCount());
    assertEquals(1, stats.getAssociationCount());
    assertEquals(1, stats.getInverseAssociationCount());
    assertEquals(1, stats.getRoleCount());
    assertEquals(1, stats.getInverseRoleCount());
    assertEquals(1, stats.getMapCount());
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final TerminologyStats stats = new TerminologyStats();
    stats.setTerminology("ncit");
    stats.setVersion("25.01d");
    stats.setConceptCount(2);
    stats.getHierarchy().setMaxParents(new TerminologyStats.CodeCount("C1", "Concept 1", 3));

    final String json = ThreadLocalMapper.get().writeValueAsString(stats);
    final TerminologyStats copy = ThreadLocalMapper.get().readValue(json, TerminologyStats.class);
    assertEquals(stats, copy);
  }
}
