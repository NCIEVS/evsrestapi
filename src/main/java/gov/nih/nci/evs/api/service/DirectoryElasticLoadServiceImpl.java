
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
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

	/** The Elasticsearch operations service instance *. */
	@Autowired
	ElasticOperationsService operationsService;

	public File getFilepath() {
		return filepath;
	}

	public void setFilepath(File filepath) {
		this.filepath = filepath;
	}

	public int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy,
			CommandLine cmd) throws Exception {
		this.setFilepath(new File(cmd.getOptionValue('d')));
		if (!filepath.exists()) {
			throw new Exception("Given filepath does not exist");
		}
		try (final FileInputStream fis = new FileInputStream(this.getFilepath());
				final InputStreamReader isr = new InputStreamReader(fis);
				final BufferedReader in = new BufferedReader(isr);) {
			String line = null;
			Concept concept = new Concept();
			List<Concept> batch = new ArrayList<>();
			String prevCui = null;
			List<Synonym> synList = new ArrayList<Synonym>();
			int totalConcepts = 0;
			while ((line = in.readLine()) != null) {
				final String[] fields = line.split("\\|", -1);
				final String cui = fields[0];
				// Test assumption that the file is in order (when considering |)
				if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
					throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
				}
				// check if we've hit a new concept
				if (!cui.equals(prevCui)) {
					handleConcept(concept, batch, false, terminology.getIndexName(), synList);
					totalConcepts++;
					synList = new ArrayList<Synonym>();
					concept = new Concept();
					concept.setCode(cui);
					concept.setTerminology(terminology.getTerminology());
					concept.setVersion(terminology.getVersion());
					concept.setLeaf(false);

				}
				// find the proper name
				if (fields[2].equalsIgnoreCase("P") && fields[4].equalsIgnoreCase("PF")
						&& fields[6].equalsIgnoreCase("Y")) {
					concept.setName(fields[14]);
				}
				// build out synonym in concept
				Synonym syn = new Synonym();
				if (!fields[13].equals("NOCODE"))
					syn.setCode(fields[10]);
				syn.setSource(fields[11]);
				syn.setTermGroup(fields[12]);
				syn.setName(fields[14]);
				synList.add(syn);
				prevCui = cui;
			}
			// make sure to deal with the last concept in file
			handleConcept(concept, batch, true, terminology.getIndexName(), synList);
			totalConcepts++;
			return totalConcepts;
		}

	}

	/* see superclass */
	@Override
	public void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
			throws Exception {
		// nothing to do here yet
	}

	private void handleConcept(Concept concept, List<Concept> batch, boolean flag, String indexName,
			List<Synonym> synList) throws IOException {
		concept.setSynonyms(synList);
		batch.add(concept);
		if (flag || batch.size() == INDEX_BATCH_SIZE) {
			operationsService.bulkIndex(new ArrayList<>(batch), indexName, ElasticOperationsService.CONCEPT_TYPE,
					Concept.class);
			batch.clear();
		}
	}

	@Override
	public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config) {
		// will eventually read and build differently
		Terminology term = new Terminology();
		term.setTerminology("ncim");
		term.setVersion("202008");
		term.setTerminologyVersion(term.getTerminology() + "_" + term.getVersion());
		term.setIndexName("concept_" + term.getTerminologyVersion());
		term.setLatest(true);
		return term;
	}

	@Override
	public HierarchyUtils getHierarchyUtils(Terminology term) {
		// dont need hierarchy utils in this indexing
		return null;
	}
}
