package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

/** Tests {@link Include}, the values the include parameter accepts. */
public class IncludeUnitTest {

  /** The lists full leaves out because they're large. */
  private static final Set<Include> LARGE_FIELDS =
      EnumSet.of(Include.DESCENDANTS, Include.ANCESTORS, Include.PATHS, Include.EXTENSIONS);

  // ---------------------------------------------------------------------------------------------
  // Parameter values: what clients write
  // ---------------------------------------------------------------------------------------------

  /**
   * Test every value is found by the parameter value clients write.
   *
   * @param include the include value
   */
  @ParameterizedTest
  @EnumSource(Include.class)
  public void testEveryValueIsFoundByItsParameterValue(final Include include) {
    assertEquals(include, Include.getByParameterValue(include.getParameterValue()));
  }

  /** Test an unknown value fails, naming it and the allowed values. */
  @Test
  public void testUnknownParameterValueThrowsErrorNamingItAndAllowedValues() {
    final String unknownValue = "rols";

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class, () -> Include.getByParameterValue(unknownValue));

    assertTrue(error.getMessage().contains(unknownValue), error.getMessage());
    assertTrue(error.getMessage().contains(Include.ROLES.getParameterValue()), error.getMessage());
  }

  /** Test a value in the wrong case isn't found: parameter values are case-sensitive. */
  @Test
  public void testParameterValueInWrongCaseThrowsError() {
    final String wrongCaseValue = "ROLES";

    assertThrows(IllegalArgumentException.class, () -> Include.getByParameterValue(wrongCaseValue));
  }

  /** Test values are joined into one parameter as a client would write it, in the order given. */
  @Test
  public void testValuesJoinIntoParameterInOrder() {
    final List<Include> includes = List.of(Include.SUMMARY, Include.ROLES);

    assertEquals("summary,roles", Include.getParameter(includes));
  }

  /**
   * Test a value is written to JSON as its parameter value, which is also how the API documentation
   * lists it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSerializesAsParameterValue() throws Exception {
    assertEquals(
        "\"inverseRoles\"", ThreadLocalMapper.get().writeValueAsString(Include.INVERSE_ROLES));
  }

  // ---------------------------------------------------------------------------------------------
  // Fields: what each value turns on
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a field turns on only itself.
   *
   * @param field the field
   */
  @ParameterizedTest
  @EnumSource(
      value = Include.class,
      mode = Mode.EXCLUDE,
      names = {"MINIMAL", "SUMMARY", "FULL", "EVERYTHING"})
  public void testFieldTurnsOnOnlyItself(final Include field) {
    assertTrue(field.getIsField());
    assertEquals(EnumSet.of(field), field.getFields());
  }

  /** Test minimal turns on no fields. */
  @Test
  public void testMinimalTurnsOnNoFields() {
    assertTrue(Include.MINIMAL.getFields().isEmpty());
  }

  /** Test summary turns on synonyms, definitions, and properties. */
  @Test
  public void testSummaryTurnsOnSynonymsDefinitionsAndProperties() {
    assertEquals(
        EnumSet.of(Include.SYNONYMS, Include.DEFINITIONS, Include.PROPERTIES),
        Include.SUMMARY.getFields());
  }

  /** Test everything turns on every field. */
  @Test
  public void testEverythingTurnsOnEveryField() {
    final Set<Include> everyField = EnumSet.noneOf(Include.class);
    Arrays.stream(Include.values()).filter(Include::getIsField).forEach(everyField::add);

    assertEquals(everyField, Include.EVERYTHING.getFields());
  }

  /** Test full turns on every field except the large lists. */
  @Test
  public void testFullTurnsOnEveryFieldExceptLargeLists() {
    final Set<Include> everyFieldExceptLarge = Include.EVERYTHING.getFields();
    everyFieldExceptLarge.removeAll(LARGE_FIELDS);

    assertEquals(everyFieldExceptLarge, Include.FULL.getFields());
  }

  /**
   * Test a shortcut isn't a field.
   *
   * @param shortcut the shortcut
   */
  @ParameterizedTest
  @EnumSource(
      value = Include.class,
      names = {"MINIMAL", "SUMMARY", "FULL", "EVERYTHING"})
  public void testShortcutIsNotField(final Include shortcut) {
    assertFalse(shortcut.getIsField());
  }
}
