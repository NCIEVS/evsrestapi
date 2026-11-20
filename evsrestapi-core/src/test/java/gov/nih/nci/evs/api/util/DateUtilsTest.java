package gov.nih.nci.evs.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DateUtilsTest {
  @Test
  void preservesTerminologyDateFormats() {
    assertNull(DateUtils.convertToYYYYMMDD(null));
    assertEquals("", DateUtils.convertToYYYYMMDD(""));
    assertEquals("2023-11-01", DateUtils.convertToYYYYMMDD("202311"));
    assertEquals("2008-12-19", DateUtils.convertToYYYYMMDD("2008_12_19"));
    assertEquals("2007-02-09", DateUtils.convertToYYYYMMDD("February 9, 2007"));
    assertEquals("", DateUtils.convertToYYYYMMDD("unknown"));
  }
}
