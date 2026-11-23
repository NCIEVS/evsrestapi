package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Unit test for {@link IncludeParam}, built from {@link Include} values or a parameter string. */
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

  /**
   * Test every field turns on the concept field of the same name: catches an {@link Include} value
   * that {@link IncludeParam} doesn't set.
   *
   * @param field the field
   */
  @ParameterizedTest
  @EnumSource(
      value = Include.class,
      mode = Mode.EXCLUDE,
      names = {"MINIMAL", "SUMMARY", "FULL", "EVERYTHING"})
  public void testEveryFieldTurnsOnConceptField(final Include field) {
    final IncludeParam include = new IncludeParam(EnumSet.of(field));

    assertTrue(Arrays.asList(include.getIncludedFields()).contains(field.getParameterValue()));
  }

  /** Test a parameter string and the same values give the same fields. */
  @Test
  public void testParameterStringMatchesValues() {
    final IncludeParam fromString = new IncludeParam("summary,roles");
    final IncludeParam fromValues = new IncludeParam(List.of(Include.SUMMARY, Include.ROLES));

    assertEquals(
        Arrays.asList(fromValues.getIncludedFields()),
        Arrays.asList(fromString.getIncludedFields()));
  }

  /** Test spaces around a value are ignored. */
  @Test
  public void testSpacesAroundValueAreIgnored() {
    assertTrue(new IncludeParam(" roles , synonyms ").isSynonyms());
  }

  /** Test an empty value at the start is ignored, as one at the end already was. */
  @Test
  public void testLeadingEmptyValueIsIgnored() {
    assertTrue(new IncludeParam(",roles").isRoles());
  }

  /** Test an empty value between two values is ignored. */
  @Test
  public void testEmptyValueBetweenValuesIsIgnored() {
    final IncludeParam include = new IncludeParam("roles,,synonyms");

    assertTrue(include.isRoles());
    assertTrue(include.isSynonyms());
  }

  /** Test an unknown value is a bad request naming the value and the allowed values. */
  @Test
  public void testUnknownValueThrowsBadRequestNamingItAndAllowedValues() {
    final String unknownValue = "rols";

    final ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> new IncludeParam("summary," + unknownValue));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    assertTrue(error.getReason().contains(unknownValue), error.getReason());
    assertTrue(error.getReason().contains(Include.ROLES.getParameterValue()), error.getReason());
  }

  /** Test a copy keeps the mapset link, which copies used to drop. */
  @Test
  public void testCopyKeepsMapsetLink() {
    final IncludeParam include = new IncludeParam(EnumSet.of(Include.MAPSET_LINK));

    assertTrue(new IncludeParam(include).isMapsetLink());
  }
}
