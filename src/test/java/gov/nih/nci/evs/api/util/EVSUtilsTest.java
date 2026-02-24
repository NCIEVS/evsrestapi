package gov.nih.nci.evs.api.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

public class EVSUtilsTest {

  @Test
  public void testGetValueFromFile() throws Exception {
    File tempFile = File.createTempFile("test", ".txt");
    tempFile.deleteOnExit();
    Files.write(tempFile.toPath(), "Hello World".getBytes());

    String uri = tempFile.toURI().toString();
    String content = EVSUtils.getValueFromFile(uri);
    assertNotNull(content);
    assertTrue(content.contains("Hello World"));
  }
}
