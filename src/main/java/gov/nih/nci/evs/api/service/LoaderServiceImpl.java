package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.Application;
import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.OpensearchLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.Set;
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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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

  /** The Opensearch operations service instance *. */
  @Autowired private OpensearchOperationsService operationsService;

  private static OpensearchOperationsService staticOperationsService;

  @PostConstruct
  public void init() {
    staticOperationsService = this.operationsService;
  }

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
  public static OpensearchLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    OpensearchLoadConfig config = new OpensearchLoadConfig();

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
   * the main method to trigger Opensearch load via command line *.
   *
   * @param args the command line arguments
   */
  @SuppressWarnings("resource")
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

    ConfigurableApplicationContext app = null;
    try {

      app = SpringApplication.run(Application.class, args);
      OpensearchLoadService loadService = null;

      // create Audit object
      final Audit termAudit = new Audit();
      termAudit.setType("reindex");
      Date startDate = new Date();
      termAudit.setStartDate(startDate);
      // which indexing object do we need to use
      if (cmd.getOptionValue("t").equals("mapping")) {
        loadService = app.getBean(MappingLoaderServiceImpl.class);
        loadService.initialize();
        loadService.loadObjects(null, null, null);
        System.exit(0);
      }
      if (cmd.hasOption('d')) {
        if (cmd.getOptionValue("t").equals("ncim")) {
          loadService = app.getBean(MetaOpensearchLoadServiceImpl.class);
        } else if (cmd.getOptionValue("t").startsWith("ncit")) {
          loadService = app.getBean(GraphOpensearchLoadServiceImpl.class);
        } else {
          loadService = app.getBean(MetaSourceOpensearchLoadServiceImpl.class);
        }
      } else if (cmd.hasOption("xr")) {
        loadService = app.getBean(GraphReportLoadServiceImpl.class);
      } else {
        loadService = app.getBean(GraphOpensearchLoadServiceImpl.class);
      }
      termAudit.setProcess(loadService.getClass().getSimpleName());

      loadService.initialize();
      final OpensearchLoadConfig config = buildConfig(cmd, CONCEPTS_OUT_DIR);
      final Terminology term =
          loadService.getTerminology(
              app,
              config,
              cmd.getOptionValue("d"),
              cmd.getOptionValue("t"),
              config.isForceDeleteIndex());
      termAudit.setTerminology(term.getTerminology());
      termAudit.setVersion(term.getVersion());
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
      final Set<String> removed = loadService.cleanStaleIndexes(term);
      loadService.updateLatestFlag(term, removed);
      termAudit.setCount(totalConcepts);
      Date endDate = new Date();
      termAudit.setEndDate(endDate);
      termAudit.setElapsedTime(endDate.getTime() - startDate.getTime());
      termAudit.setLogLevel("INFO");
      logger.info("Audit: {}", termAudit.toString());
      // only add new audit if something major has actually happened
      if (termAudit.getElapsedTime() > 10000 && totalConcepts > 0) {
        addAudit(termAudit);
        // update the metadata with the elapsed time
        term.getMetadata().setIndexRuntime(termAudit.getElapsedTime());
        logger.warn(
            "index runtime of {} ms for terminology {}",
            term.getMetadata().getIndexRuntime(),
            term.getTerminology());
      }

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      Audit.addAudit(
          staticOperationsService,
          e.getClass().getName(),
          e.getStackTrace()[0].getClassName(),
          cmd.getOptionValue("t"),
          e.getMessage(),
          "ERROR");

      // If app is null, initialization failed immediately, return nonzero code
      if (app == null) {
        System.exit(1);
      } else {
        SpringApplication.exit(app, () -> 1);
        app.close();
      }
    }

    if (app == null) {
      System.exit(0);
    } else {
      SpringApplication.exit(app, () -> 0);
      app.close();
    }
  }

  /**
   * Adds the audit.
   *
   * @param audit the audit
   * @throws Exception the exception
   */
  public static void addAudit(final Audit audit) throws Exception {
    staticOperationsService.deleteQuery(
        "terminology:" + audit.getTerminology() + " AND version:" + audit.getVersion(),
        OpensearchOperationsService.AUDIT_INDEX);
    staticOperationsService.index(audit, OpensearchOperationsService.AUDIT_INDEX, Audit.class);
  }
}
