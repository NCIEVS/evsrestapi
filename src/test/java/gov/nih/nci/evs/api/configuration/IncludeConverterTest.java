package gov.nih.nci.evs.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nih.nci.evs.api.model.Include;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link IncludeConverter}, which turns each include value a client writes into an {@link
 * Include}.
 */
public class IncludeConverterTest {

  /** The converter under test. */
  private final IncludeConverter converter = new IncludeConverter();

  /** Test a value is converted from how clients write it, not the constant's name. */
  @Test
  public void testValueConvertsFromParameterValue() {
    assertEquals(Include.INVERSE_ROLES, converter.convert("inverseRoles"));
  }

  /** Test spaces around a value are ignored. */
  @Test
  public void testSpacesAroundValueAreIgnored() {
    assertEquals(Include.ROLES, converter.convert(" roles "));
  }

  /** Test an empty value turns on nothing, like minimal. */
  @Test
  public void testEmptyValueConvertsToMinimal() {
    assertEquals(Include.MINIMAL, converter.convert(""));
  }

  /** Test an unknown value fails, so Spring rejects the parameter. */
  @Test
  public void testUnknownValueThrowsError() {
    assertThrows(IllegalArgumentException.class, () -> converter.convert("rols"));
  }
}
