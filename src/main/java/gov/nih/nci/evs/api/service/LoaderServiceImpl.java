package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.Application;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * The implementation for {@link LoaderService}.
 *
 * @author Arun
 */
@Service
public class LoaderServiceImpl {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(LoaderServiceImpl.class);

  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.conceptsDir}")
  private static String CONCEPTS_OUT_DIR;

  /** the environment *. */
  @Autowired Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /**
   * prepare command line options available.
   *
   * @return the options
   */
  public static Options prepareOptions() {
    Options options = new Options();

    options.addOption(
        "f", "forceDeleteIndex", false, "Force delete index if index already exists.");
    options.addOption("h", "help", false, "Show this help information and exit.");
    options.addOption("r", "realTime", false, "Keep for backwards compabitlity. No Effect.");
    options.addOption("t", "terminology", true, "The terminology (ex: ncit_20.02d) to load.");
    options.addOption("d", "directory", true, "Load concepts from the given directory");
    options.addOption(
        "xc",
        "skipConcepts",
        false,
        "Skip loading concepts, just clean stale terminologies, metadata, and update latest flags");
    options.addOption(
        "xm",
        "skipMetadata",
        false,
        "Skip loading metadata, just clean stale terminologies concepts, and update latest flags");
    options.addOption(
        "xl",
        "skipLoad",
        false,
        "Skip loading data, just clean stale terminologies and update latest flags");
    options.addOption("xr", "report", false, "Compute and return a report instead of loading data");

    return options;
  }

  /**
   * print command line help information.
   *
   * @param options the options available
   */
  public static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar $DIR/evsrestapi-*.jar", options);
    return;
  }

  /**
   * build config object from command line options.
   *
   * @param cmd the command line object
   * @param defaultLocation the default download location to use
   * @return the config object
   */
  public static ElasticLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    ElasticLoadConfig config = new ElasticLoadConfig();

    config.setTerminology(cmd.getOptionValue('t'));
    config.setForceDeleteIndex(cmd.hasOption('f'));
    if (cmd.hasOption('d')) {
      String location = cmd.getOptionValue('d');
      if (StringUtils.isBlank(location)) {
        logger.error("Location is empty!");
      }
      if (!location.endsWith("/")) {
        location += "/";
      }
      logger.info("location - {}", location);
      config.setLocation(location);
    } else {
      config.setLocation(defaultLocation);
    }

    return config;
  }

  /**
   * the main method to trigger elasticsearch load via command line *.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    Options options = prepareOptions();
    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger.error(
          "{}; Try -h or --help to learn more about command line options available.",
          e.getMessage());
      return;
    }

    if (cmd.hasOption('h')) {
      printHelp(options);
      return;
    }

    @SuppressWarnings("resource")
    ApplicationContext app = SpringApplication.run(Application.class, new String[0]);
    ElasticLoadService loadService = null;

    try {
      // which indexing object do we need to use
      if (cmd.getOptionValue("t").equals("mapping")) {
        loadService = app.getBean(MappingLoaderServiceImpl.class);
        loadService.loadObjects(null, null, null);
        System.exit(0);
      }
      if (cmd.hasOption('d')) {
        if (cmd.getOptionValue("t").equals("ncim")) {
          loadService = app.getBean(MetaElasticLoadServiceImpl.class);
        } else if (cmd.getOptionValue("t").startsWith("ncit")) {
          loadService = app.getBean(StardogElasticLoadServiceImpl.class);
        } else {
          loadService = app.getBean(MetaSourceElasticLoadServiceImpl.class);
        }
      } else if (cmd.hasOption("xr")) {
        loadService = app.getBean(StardogReportLoadServiceImpl.class);
      } else {
        loadService = app.getBean(StardogElasticLoadServiceImpl.class);
      }

      loadService.initialize();
      final ElasticLoadConfig config = buildConfig(cmd, CONCEPTS_OUT_DIR);
      final Terminology term =
          loadService.getTerminology(
              app,
              config,
              cmd.getOptionValue("d"),
              cmd.getOptionValue("t"),
              config.isForceDeleteIndex());
      final HierarchyUtils hierarchy = loadService.getHierarchyUtils(term);
      int totalConcepts = 0;
      if (!cmd.hasOption("xl")) {
        if (!cmd.hasOption("xc")) {
          totalConcepts = loadService.loadConcepts(config, term, hierarchy);
          loadService.checkLoadStatus(totalConcepts, term);
        }
        if (!cmd.hasOption("xm")) {
          // Give load objects a chance to update terminology metadata
          loadService.loadObjects(config, term, hierarchy);
          loadService.loadIndexMetadata(totalConcepts, term);
        }
      }
      loadService.cleanStaleIndexes(term);
      loadService.updateLatestFlag(term);

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      int exitCode =
          SpringApplication.exit(
              app,
              new ExitCodeGenerator() {
                @Override
                public int getExitCode() {
                  // return the error code
                  logger.info("Exit code 1");
                  return 1;
                }
              });
      System.exit(exitCode);
    }

    int exitCode =
        SpringApplication.exit(
            app,
            new ExitCodeGenerator() {
              @Override
              public int getExitCode() {
                // return the error code
                logger.info("Exit code 0");
                return 0;
              }
            });
    System.exit(exitCode);
  }

  /**
   * prepare command line options available.
   *
   * @return the options
   */
}
