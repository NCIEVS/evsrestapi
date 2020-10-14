
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The implementation for {@link DirectoryElasticLoadService}.
 *
 * @author Arun
 */
@Service
public class DirectoryElasticLoadServiceImpl extends BaseLoaderService {

	/** the logger *. */
	private static final Logger logger = LoggerFactory.getLogger(DirectoryElasticLoadServiceImpl.class);

	/** the concepts download location *. */
	@Value("${nci.evs.bulkload.conceptsDir}")
	private String CONCEPTS_OUT_DIR;

	/** the lock file name *. */
	@Value("${nci.evs.bulkload.lockFile}")
	private String LOCK_FILE;

	/** download batch size *. */
	@Value("${nci.evs.bulkload.downloadBatchSize}")
	private int DOWNLOAD_BATCH_SIZE;

	/** index batch size *. */
	@Value("${nci.evs.bulkload.indexBatchSize}")
	private int INDEX_BATCH_SIZE;

	/** file path *. */
	private File filepath;

	/** the environment *. */
	@Autowired
	Environment env;

	/** The sparql query manager service. */
	@Autowired
	private SparqlQueryManagerService sparqlQueryManagerService;

	/** The Elasticsearch operations service instance *. */
	@Autowired
	ElasticOperationsService operationsService;

	public File getFilepath() {
		return filepath;
	}

	public void setFilepath(File filepath) {
		this.filepath = filepath;
	}

	public void loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
			throws Exception {
		try (final FileInputStream fis = new FileInputStream(this.getFilepath());
				final InputStreamReader isr = new InputStreamReader(fis);
				final BufferedReader in = new BufferedReader(isr);) {
			String line = null;
			Concept concept = new Concept();
			List<Concept> batch = new ArrayList<>();
			String prevCui = null;
			List<Synonym> synList = new ArrayList<Synonym>();
			while ((line = in.readLine()) != null) {
				final String[] fields = line.split("\\|", -1);
				final String cui = fields[0];
				// Test assumption that the file is in order (when considering |)
				if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
					throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
				}
				if (!cui.equals(prevCui)) {
					handleConcept(concept, batch, false, terminology.getIndexName(), synList);
					synList = new ArrayList<Synonym>();
					concept = new Concept();
					concept.setCode(cui);
					concept.setVersion(terminology.getVersion());
					concept.setLeaf(false);

				}
				if (fields[2].equalsIgnoreCase("P") && fields[4].equalsIgnoreCase("PF")
						&& fields[6].equalsIgnoreCase("Y")) {
					concept.setName(fields[14]);
				}
				// TODO: add synonym
				Synonym syn = new Synonym();
				if (!fields[13].equals("NOCODE"))
					syn.setCode(fields[10]);
				syn.setSource(fields[11]);
				syn.setTermGroup(fields[12]);
				syn.setName(fields[14]);
				synList.add(syn);
				prevCui = cui;
			}
			handleConcept(concept, batch, true, terminology.getIndexName(), synList);
		}
	}

	/* see superclass */
	@Override
	public void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
			throws Exception {
	}

	private void handleConcept(Concept concept, List<Concept> batch, boolean flag, String indexName,
			List<Synonym> synList) throws IOException {
		concept.setSynonyms(synList);
		batch.add(concept);
		if (flag || batch.size() == INDEX_BATCH_SIZE) {
			// send batch for indexing... copy batch when passing it to this --- new
			// ArrayList<>(batch);
			operationsService.bulkIndex(new ArrayList<>(batch), indexName, ElasticOperationsService.CONCEPT_TYPE,
					Concept.class);
			batch.clear();
		}
	}

	@Override
	public void setUpConceptLoading(ApplicationContext app, CommandLine cmd) throws Exception {
		try {
			ElasticLoadConfig config = buildConfig(cmd, CONCEPTS_OUT_DIR);

			if (StringUtils.isBlank(config.getTerminology())) {
				logger.error(
						"Terminology (-t or --terminology) is required! Try -h or --help to learn more about command line options available.");
				return;
			}
			Terminology term = new Terminology();
			term.setTerminology(config.getTerminology());
			term.setVersion("202008");
			term.setTerminologyVersion(term.getTerminology() + "_" + term.getVersion());
			term.setIndexName("concept_" + term.getTerminologyVersion());
			term.setLatest(true);
			// TODO: load actual details from data
			this.setFilepath(new File(cmd.getOptionValue('d')));
			if (!filepath.exists()) {
				logger.error("Given file path does not exist");
				return;
			}

			loadConcepts(config, term, null);
			loadObjects(config, term, null);
			cleanStaleIndexes();
			updateLatestFlag();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}

	}
}
