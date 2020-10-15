
package gov.nih.nci.evs.api.service;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.Application;

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

	/** the lock file name *. */
	@Value("${nci.evs.bulkload.lockFile}")
	private String LOCK_FILE;

	/** download batch size *. */
	@Value("${nci.evs.bulkload.downloadBatchSize}")
	private int DOWNLOAD_BATCH_SIZE;

	/** index batch size *. */
	@Value("${nci.evs.bulkload.indexBatchSize}")
	private int INDEX_BATCH_SIZE;

	/** the environment *. */
	@Autowired
	Environment env;

	/** The Elasticsearch operations service instance *. */
	@Autowired
	ElasticOperationsService operationsService;

	/**
	 * prepare command line options available.
	 *
	 * @return the options
	 */
	public static Options prepareOptions() {
		Options options = new Options();

		options.addOption("f", "forceDeleteIndex", false, "Force delete index if index already exists.");
		options.addOption("h", "help", false, "Show this help information and exit.");
		options.addOption("r", "realTime", false, "Keep for backwards compabitlity. No Effect.");
		options.addOption("t", "terminology", true, "The terminology (ex: ncit_20.02d) to load.");
		options.addOption("d", "directory", true, "load concepts from the given directory");

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
	 * the main method to trigger elasticsearch load via command line *.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Options options = prepareOptions();
		CommandLine cmd;
		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			logger.error("{}; Try -h or --help to learn more about command line options available.", e.getMessage());
			return;
		}

		if (cmd.hasOption('h')) {
			printHelp(options);
			return;
		}

		ApplicationContext app = SpringApplication.run(Application.class, new String[0]);
		ElasticLoadService loadService = null;

		try {
			if (cmd.hasOption('d')) {
				loadService = app.getBean(DirectoryElasticLoadServiceImpl.class);
			} else {
				loadService = app.getBean(StardogElasticLoadServiceImpl.class);
			}
			loadService.setUpConceptLoading(app, cmd);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			SpringApplication.exit(app);
		}

	}

	/**
	 * prepare command line options available.
	 *
	 * @return the options
	 */
}
