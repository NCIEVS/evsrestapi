package gov.nih.nci.evs.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link RrfFileCopier}. */
public class RrfFileCopierUnitTest {

  /** The temp dir. */
  @TempDir Path tempDir;

  /** Test that CUI copy mode keeps all rows for a matching CUI. */
  @Test
  public void copyFilesKeepsWholeCuisInCuiMode() throws Exception {
    final Path inputDir = createInputDir();
    final Path outputDir = tempDir.resolve("output-cui");

    final RrfFileCopier copier = new RrfFileCopier();
    copier.copyFiles(inputDir.toFile(), outputDir.toFile(), Set.of("SAB"), Set.of("C0001"));

    assertThat(readLines(outputDir, "MRCONSO.RRF"))
        .containsExactly(
            "C0001|ENG|P|L1|PF|S1|Y|A1|SA1|SC1|SD1|SAB|PT|CODE1|Name one|0|N|256|",
            "C0001|ENG|P|L2|PF|S2|Y|A2|SA2|SC2|SD2|SAB|SY|CODE2|Name two|0|N|256|");
    assertThat(readLines(outputDir, "MRDEF.RRF"))
        .containsExactly(
            "C0001|A1|AT1|SAT1|SAB|Definition one|N|256|",
            "C0001|A2|AT2|SAT2|SAB|Definition two|N|256|");
  }

  /** Test that code/SAB copy mode keeps only matching atoms and atom-scoped rows. */
  @Test
  public void copyFilesByCodeSabsKeepsOnlyMatchingCodeSabRows() throws Exception {
    final Path inputDir = createInputDir();
    final Path outputDir = tempDir.resolve("output-codesab");

    final RrfFileCopier copier = new RrfFileCopier();
    copier.copyFilesByCodeSabs(
        inputDir.toFile(), outputDir.toFile(), Set.of("SAB"), Set.of("CODE1|SAB"));

    assertThat(readLines(outputDir, "MRCONSO.RRF"))
        .containsExactly("C0001|ENG|P|L1|PF|S1|Y|A1|SA1|SC1|SD1|SAB|PT|CODE1|Name one|0|N|256|");
    assertThat(readLines(outputDir, "MRDEF.RRF"))
        .containsExactly("C0001|A1|AT1|SAT1|SAB|Definition one|N|256|");
    assertThat(readLines(outputDir, "MRSAT.RRF"))
        .containsExactly(
            "C0001|L1|S1|A1|AUI|CODE1|AT1|SAT1|ATTR|SAB|Atom attr one|N|256|",
            "C0001|L1|S1||CODE|CODE1|AT3|SAT3|ATTR|SAB|Code attr one|N|256|");
    assertThat(readLines(outputDir, "MRREL.RRF"))
        .containsExactly("C0001|A1|AUI|RO|C0001|A1|AUI|related_to|R0|SR0|SAB|SAB||Y|N||");
    assertThat(readLines(outputDir, "MRHIER.RRF")).containsExactly("C0001|A1|CXN1||SAB|isa||HCD1|");
    assertThat(readLines(outputDir, "MRMAP.RRF"))
        .containsExactly("C0001|SAB|MAP1|CODE1|", "C0001|SAB|MAP2|CODE2|");
    assertThat(readLines(outputDir, "MRCUI.RRF"))
        .containsExactly("C9999|202401|DEL||||Y|", "C9998|202401|RB||reason|C0001|Y|");
  }

  /** Test that malformed RRF rows report the file, key, and line number. */
  @Test
  public void copyFilesByCodeSabsReportsMalformedRows() throws Exception {
    final Path inputDir = tempDir.resolve("malformed-input");
    final Path outputDir = tempDir.resolve("malformed-output");
    Files.createDirectories(inputDir);
    writeLines(inputDir, "MRCONSO.RRF", List.of("C0001|ENG|P|L1"));

    final RrfFileCopier copier = new RrfFileCopier();

    assertThatThrownBy(
            () ->
                copier.copyFilesByCodeSabs(
                    inputDir.toFile(), outputDir.toFile(), Set.of("SAB"), Set.of("CODE1|SAB")))
        .isInstanceOf(Exception.class)
        .hasMessageContaining("Malformed RRF row in MRCONSO.RRF (MRCONSO) at line 1")
        .hasMessageContaining("expected at least 14 fields, found 4");
  }

  /**
   * Creates an input directory with tiny RRF fixtures.
   *
   * @return the input directory
   * @throws Exception if anything goes wrong
   */
  private Path createInputDir() throws Exception {
    final Path inputDir = tempDir.resolve("input-" + System.nanoTime());
    Files.createDirectories(inputDir);

    writeLines(
        inputDir,
        "MRCONSO.RRF",
        List.of(
            "C0001|ENG|P|L1|PF|S1|Y|A1|SA1|SC1|SD1|SAB|PT|CODE1|Name one|0|N|256|",
            "C0001|ENG|P|L2|PF|S2|Y|A2|SA2|SC2|SD2|SAB|SY|CODE2|Name two|0|N|256|",
            "C0002|ENG|P|L3|PF|S3|Y|A3|SA3|SC3|SD3|SAB|PT|CODE3|Name three|0|N|256|"));
    writeLines(
        inputDir,
        "MRDEF.RRF",
        List.of(
            "C0001|A1|AT1|SAT1|SAB|Definition one|N|256|",
            "C0001|A2|AT2|SAT2|SAB|Definition two|N|256|",
            "C0002|A3|AT3|SAT3|SAB|Definition three|N|256|"));
    writeLines(
        inputDir,
        "MRSAT.RRF",
        List.of(
            "C0001|L1|S1|A1|AUI|CODE1|AT1|SAT1|ATTR|SAB|Atom attr one|N|256|",
            "C0001|L2|S2|A2|AUI|CODE2|AT2|SAT2|ATTR|SAB|Atom attr two|N|256|",
            "C0001|L1|S1||CODE|CODE1|AT3|SAT3|ATTR|SAB|Code attr one|N|256|",
            "C0001|L2|S2||CODE|CODE2|AT4|SAT4|ATTR|SAB|Code attr two|N|256|"));
    writeLines(
        inputDir,
        "MRREL.RRF",
        List.of(
            "C0001|A1|AUI|RO|C0001|A1|AUI|related_to|R0|SR0|SAB|SAB||Y|N||",
            "C0001|A1|AUI|RO|C0001|A2|AUI|related_to|R1|SR1|SAB|SAB||Y|N||",
            "C0001|A2|AUI|RO|C0002|A3|AUI|related_to|R2|SR2|SAB|SAB||Y|N||"));
    writeLines(
        inputDir,
        "MRHIER.RRF",
        List.of(
            "C0001|A1|CXN1||SAB|isa||HCD1|",
            "C0001|A2|CXN2||SAB|isa||HCD2|",
            "C0002|A3|CXN3||SAB|isa||HCD3|"));
    writeLines(
        inputDir,
        "MRMAP.RRF",
        List.of("C0001|SAB|MAP1|CODE1|", "C0001|SAB|MAP2|CODE2|", "C0002|SAB|MAP3|CODE3|"));
    writeLines(
        inputDir,
        "MRCUI.RRF",
        List.of(
            "C9999|202401|DEL||||Y|",
            "C9998|202401|RB||reason|C0001|Y|",
            "C9997|202401|RB||reason|C0002|Y|"));
    return inputDir;
  }

  /**
   * Writes lines to a fixture file.
   *
   * @param dir the directory
   * @param file the file
   * @param lines the lines
   * @throws Exception if anything goes wrong
   */
  private void writeLines(final Path dir, final String file, final List<String> lines)
      throws Exception {
    Files.write(dir.resolve(file), lines, StandardCharsets.UTF_8);
  }

  /**
   * Reads lines from an output file.
   *
   * @param dir the directory
   * @param file the file
   * @return the lines
   * @throws Exception if anything goes wrong
   */
  private List<String> readLines(final Path dir, final String file) throws Exception {
    return Files.readAllLines(dir.resolve(file), StandardCharsets.UTF_8);
  }
}
