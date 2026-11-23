package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Unit test for the {@code ancestors} choice of {@link IncludeParam}. */
public class IncludeParamUnitTest {

  /** Field name of a concept's ancestors. */
  private static final String ANCESTORS = "ancestors";

  /** Including ancestors returns them, and only then. */
  @Test
  public void testIncludeAncestors() {
    final IncludeParam include = new IncludeParam(ANCESTORS);

    assertTrue(include.isAncestors());
    assertTrue(include.hasAnyTrue());
    assertTrue(Arrays.asList(include.getIncludedFields()).contains(ANCESTORS));
    assertFalse(Arrays.asList(include.getExcludedFields()).contains(ANCESTORS));
    assertTrue(new IncludeParam(include).isAncestors());
  }

  /** The default, like descendants, leaves ancestors out. */
  @Test
  public void testMinimalExcludesAncestors() {
    final IncludeParam include = new IncludeParam((String) null);

    assertFalse(include.isAncestors());
    assertFalse(Arrays.asList(include.getIncludedFields()).contains(ANCESTORS));
    assertTrue(Arrays.asList(include.getExcludedFields()).contains(ANCESTORS));
  }

  /** "full" leaves ancestors out, as it does descendants; "*" includes them. */
  @Test
  public void testFullExcludesAndEverythingIncludesAncestors() {
    final IncludeParam full = new IncludeParam("full");
    final IncludeParam everything = new IncludeParam("*");

    assertFalse(full.isAncestors());
    assertTrue(Arrays.asList(full.getExcludedFields()).contains(ANCESTORS));
    assertTrue(everything.isAncestors());
    assertTrue(Arrays.asList(everything.getIncludedFields()).contains(ANCESTORS));
  }
}
