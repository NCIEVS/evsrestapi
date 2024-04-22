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
        if (args.length != 4) {
          throw new Exception("Usage: ... rrfSample <inputPath> <list file> <terminology>");
        }
        final String inputPath = args[1];
        final String listFile = args[2];
        final String terminology = args[3];
        // Generate subset to local directory
        SamplingApplication.rrfSample(
            "admin",
            inputPath,
            listFile,
            Arrays.asList(terminology.split(",")).stream().collect(Collectors.toSet()));
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

    final RrfSampleGenerator generator = new RrfSampleGenerator();
    generator.setTerminologies(terminologies);
    generator.setCuisFile(listFile);
    generator.setInputPath(inputPath);
    generator.setKeepDescendants(false);
    // generator.setDistanceOne(true);
    generator.compute();
  }
}
