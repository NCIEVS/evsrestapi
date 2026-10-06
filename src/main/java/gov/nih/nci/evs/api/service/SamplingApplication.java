package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.Application;
import gov.nih.nci.evs.api.util.RrfSampleGenerator;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/** Entry point for gradle tasks. */
@Service
public class SamplingApplication {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(SamplingApplication.class);

  /**
   * Application entry point.
   *
   * @param args the command line arguments
   */
  @SuppressWarnings("resource")
  public static void main(final String[] args) {
    // It is important to pass "args" here so it has a value below
    final ApplicationContext app = SpringApplication.run(Application.class, args);
    logger.debug("SAMPLE APPLICATION START ");
    try {

      final String command = args[0];

      // Sample RRF for a terminology
      if (command.equals("rrfSample")) {
        if (args.length != 4 && args.length != 5) {
          throw new Exception(
              "Usage: ... rrfSample <inputPath> <list file> <terminology> [codesab|cui]");
        }
        final String inputPath = args[1];
        final String listFile = args[2];
        final String terminology = args[3];
        final String copyMode = args.length == 5 ? args[4] : "codesab";
        if (!"codesab".equalsIgnoreCase(copyMode) && !"cui".equalsIgnoreCase(copyMode)) {
          throw new Exception("Copy mode must be codesab or cui: " + copyMode);
        }
        // Generate subset to local directory
        SamplingApplication.rrfSample(
            "admin",
            inputPath,
            listFile,
            Arrays.asList(terminology.split(",")).stream().collect(Collectors.toSet()),
            "cui".equalsIgnoreCase(copyMode));
      }

    } catch (final Throwable t) {
      logger.error("Unexpected error", t);
      final int exitCode =
          SpringApplication.exit(
              app,
              new ExitCodeGenerator() {
                @Override
                public int getExitCode() {
                  // return the error code
                  return 1;
                }
              });
      System.exit(exitCode);
    }

    final int exitCode =
        SpringApplication.exit(
            app,
            new ExitCodeGenerator() {

              /**
               * Returns the exit code.
               *
               * @return the exit code
               */
              @Override
              public int getExitCode() {
                // return the error code
                return 0;
              }
            });
    System.exit(exitCode);
  }

  /**
   * Rrf sample.
   *
   * @param username the username
   * @param inputPath the input path
   * @param listFile the list file
   * @param terminologies the terminologies
   * @throws Exception the exception
   */
  public static void rrfSample(
      final String username,
      final String inputPath,
      final String listFile,
      final Set<String> terminologies)
      throws Exception {
    rrfSample(username, inputPath, listFile, terminologies, false);
  }

  /**
   * Rrf sample.
   *
   * @param username the username
   * @param inputPath the input path
   * @param listFile the list file
   * @param terminologies the terminologies
   * @param cuiMode the CUI copy mode
   * @throws Exception the exception
   */
  public static void rrfSample(
      final String username,
      final String inputPath,
      final String listFile,
      final Set<String> terminologies,
      final boolean cuiMode)
      throws Exception {

    final RrfSampleGenerator generator = new RrfSampleGenerator();
    generator.setTerminologies(terminologies);
    generator.setCuisFile(listFile);
    generator.setInputPath(inputPath);
    generator.setKeepDescendants(false);
    generator.setCuiMode(cuiMode);
    // generator.setDistanceOne(true);
    generator.compute();
  }
}
