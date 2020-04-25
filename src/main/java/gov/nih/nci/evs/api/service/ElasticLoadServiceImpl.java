package gov.nih.nci.evs.api.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.LoadConfig;

/**
 * The implementation for {@link ElasticLoadService}
 * 
 * @author Arun
 *
 */
@Service
public class ElasticLoadServiceImpl implements ElasticLoadService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticLoadServiceImpl.class);
  
  private static final String CONCEPTS_OUT_DIR = "C:/Users/Arun/Soft/tmp/concepts/";
  
  private static final String LOCK_FILE = "DownloadSuccessfull.lck";
  
  private static final int DOWNLOAD_BATCH_SIZE = 100;
  
  private static final int LOAD_BATCH_SIZE = 100;
  
  /** The Elasticsearch operations service instance **/
  @Autowired
  ElasticOperationsService esOperations;
  
  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;
  
  @Override
  public void load(LoadConfig config, Terminology terminology) throws IOException {

    //TODO: 
    //get all concepts
    logger.info("Getting all concepts");
    List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);
    
//    logger.info("allConcepts = " + allConcepts);
    if (allConcepts != null) logger.info("all concepts size is {}", allConcepts.size());
    
    //download files from stardog in batches
    downloadConcepts(allConcepts, terminology);
    
    //load concepts to es in batches
    if (false) {
      loadConceptsFromFiles();
    }
    
    return;
    /**
    
    parser = ArgumentParser(
             prog="Bulk Load ElasticSearch",
             description="Bulk load ElasticSearch.")

    parser.add_argument(
            '--index_name',
            dest="index_name",
            required=True,
            help="Name to use for the ES index.")

    parser.add_argument(
            '--drop_index',
            dest="drop_index",
            required=False,
            default=False,
            action="store_true",
            help="If present, drops the index, then recreates the index.")

    parser.set_defaults(drop_index=False)

    parser.add_argument(
            '--download_only',
            dest="download_only",
            required=False,
            default=False,
            action="store_true",
            help="If present, downloads the files and exits.")
    parser.set_defaults(download_only=False)

    parser.add_argument(
            '--download_using_api',
            dest="download_using_api",
            required=False,
            default=False,
            action="store_true",
            help="If present, downloads the files using the REST API.")
    parser.set_defaults(download_using_api=False)

    parser.add_argument(
            '--download_continue',
            dest="download_continue",
            required=False,
            default=False,
            action="store_true",
            help="If present, restarts the download process, adding the missing files and exits.")
    parser.set_defaults(download_continue=False)

    parser.add_argument(
            '--no_download',
            dest="no_download",
            required=False,
            default=False,
            action="store_true",
            help="If present, do not download the concepts.")
    parser.set_defaults(no_download=False)

    parser.add_argument(
            '--load_real_time',
            dest="load_real_time",
            required=False,
            default=False,
            action="store_true",
            help="If present, download the concept and load in real time.")
    parser.set_defaults(load_real_time=False)

    parser.add_argument(
            '--delete_documents',
            dest="delete_documents",
            required=False,
            default=False,
            action="store_true",
            help="If present, delete documents from ES that no longer exist in Stardog.")
    parser.set_defaults(delete_documents=False)

    parser.add_argument(
        '--log',
        dest="loglevel",
        default="INFO",
        required=False,
        help="Specify the log level: DEBUG, INFO, WARNING, ERROR, CRITICAL"
    )

    args = parser.parse_args()

    #
    # Setup Logging
    #
    logging.basicConfig(format='%(levelname)s:%(module)s:%(message)s',
                        level=args.loglevel)
    log = logging.getLogger(__name__)

    #
    # ElasticSearch logging is very verbose, so turning off
    # most of the log messages
    #
    es_logger = logging.getLogger('elasticsearch')
    es_logger.setLevel(logging.ERROR)

    try: 
        log_file = open(LOG_DIRECTORY + "bulkLoadES.log", "w")
    except:
        print("Failed to open bulkLoadES.log file")
        sys.exit(1)

    print_log("Starting ElasticSearch Load Process")

    print_log("Testing connection to ElasticSearch")
    es = connect_elasticsearch(host=ES_HOST, port=ES_PORT)
    if es is None:
        print("Failed to connect to ElasticSearch, Exiting Program") 
        sys.exit(1)

    # Retrieve the Concept codes from Stardog endpoint. This
    # should be the list of concepts that should exist in
    # ElasticSearch after the loading process.
    concepts_in_stardog = get_concepts_from_stardog(
        SPARQL_ENDPOINT, NAMED_GRAPH, ALL_CONCEPTS_QUERY)
    msg = "Number of concepts in Stardog: " + str(len(concepts_in_stardog))
    print_log(msg)

    #
    # If this flag is set, download the concepts using the
    # EVS RestAPI to the file system, then quit.
    # This process takes a long time, so you may only want to
    # do this once, so you can load the same content into multiple
    # ElasticSearch instances. 
    #
    # Warning this code removes the contents of the CONCEPT_OUTPUT_DIR
    if args.download_only:
        #download_concept_files(concepts_in_stardog[:1000])
        if args.download_using_api:
            download_concept_files(concepts_in_stardog, MULTI_PROCESSING_POOL, True)
        else:
            download_concept_files_batch(concepts_in_stardog, True)
        sys.exit(1)

    if args.download_continue:
        concepts_in_stardog_set = set(concepts_in_stardog)
        concepts_in_file_system_set = set(get_concepts_from_directory())
        concepts_not_in_file_system = list(concepts_in_stardog_set - concepts_in_file_system_set)
        print_log("Number of missing files: " + str(len(concepts_not_in_file_system)))
        #download_concept_files(concepts_not_in_file_system, MULTI_PROCESSING_POOL, False)
        if args.download_using_api:
            download_concept_files(concepts_not_in_file_system, MULTI_PROCESSING_POOL, True)
        else:
            download_concept_files_batch(concepts_not_in_file_system, True)
        sys.exit(1)

    create_index(es, args.index_name, INDEX_MAPPING_FILE, args.drop_index)

    if args.load_real_time:
        print_log("Starting the Upload Process using Real Time")
        #load_concept_real_time(es, args.index_name, concepts_in_stardog[:1000])
        load_concept_real_time(es, args.index_name, concepts_in_stardog)
        print("Finished the Upload Process using Real Time")
    else:
        if args.no_download:
            print_log("No download of concepts to the file system")
        else:
            #download_concept_files(concepts_in_stardog[:200], MULTI_PROCESSING_POOL)
            #download_concept_files(concepts_in_stardog, MULTI_PROCESSING_POOL, True)
            if args.download_using_api:
                download_concept_files(concepts_in_stardog, MULTI_PROCESSING_POOL, True)
            else:
                download_concept_files_batch(concepts_in_stardog, True)

        concepts_in_file_system = get_concepts_from_directory()
        print_log("Number of concepts in File System: " + str(len(concepts_in_file_system)))
        print_log("Starting the Upload Process using Bulk Load")
        #
        # Check that the download of files was successful before continuing.
        #
        if not os.path.exists(CONCEPT_OUTPUT_DIR + "/DownloadSuccessfull.lck"):
            print_log("Bulk Load Process Failed, the DownloadSuccessfull.lck file does not exist")
            sys.exit(1)

        load_concept_bulk_file(es, args.index_name, concepts_in_file_system)
        print_log("Finished the Upload Process using Bulk Load")

    # Now check if there are concepts in ElasticSearch that need to
    # be removed. Sleep for short time to make sure ElasticSearch
    # has finished the upload refresh.
    time.sleep(60)
    concepts_in_es = get_concepts_from_es(es, args.index_name)
    print_log("Number of concepts in ES: " + str(len(concepts_in_es)))
    es_set = set(concepts_in_es)
    stardog_set = set(concepts_in_stardog)
    concepts_only_in_stardog = stardog_set - es_set
    concepts_only_in_es = es_set - stardog_set
    print_log("Concepts_only_in_stardog: " +  str(len(concepts_only_in_stardog)))
    print_log("Concepts_only_in_es: " + str(len( concepts_only_in_es)))

    with open(LOG_DIRECTORY + "concepts_only_in_stardog.log", "w") as f: 
        for concept in concepts_only_in_stardog:
            print(concept, file=f)

    with open(LOG_DIRECTORY + "concepts_only_in_es.log", "w") as f: 
        for concept in concepts_only_in_es:
            print(concept, file=f)

    if args.delete_documents:
        for concept in concepts_only_in_es:
            print_log("Deleting from ES: ",concept)
            es.delete(index=args.index_name, doc_type="concept", id=concept)

    print_log("Finished ElasticSearch Load Process")
    log_file.close()
    
    **/
  }

  //download files from stardog in batches
  private void downloadConcepts(List<Concept> allConcepts, Terminology terminology) throws IOException {
    if (CollectionUtils.isEmpty(allConcepts)) return;
    logger.info("Downloading concepts");
    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;
    
    int total = allConcepts.size();
    logger.info("  Total concepts: {}", total);
    
    
    while(start < total) {
      if (total - start <= DOWNLOAD_BATCH_SIZE) end = total;
      
      logger.info(" Processing {} to {}", start+1, end);
      
      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode()).collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);
      
      for(Concept concept: concepts) {
        String json = concept.toString();
        String filePath = CONCEPTS_OUT_DIR + concept.getCode() + ".json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(json);
        writer.close();
      }
      
      start = end;
      end = end + DOWNLOAD_BATCH_SIZE;
      
      //TODO: remove the following line -- temp code for testing
      break;
    }
    
    Files.createFile(FileSystems.getDefault().getPath(CONCEPTS_OUT_DIR, LOCK_FILE));
    logger.info("Download process complete!");
  }
  
  //load concepts to es in batches - from files
  private void loadConceptsFromFiles() throws IOException {
    File conceptsDir = new File(CONCEPTS_OUT_DIR);
    File[] files = conceptsDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".json");
        }
    });
    
    if (files == null || files.length == 0) {
      logger.info("No JSON files found to load!");
      return;
    }
    
    List<Concept> concepts = new ArrayList<>(files.length);
    ObjectMapper mapper = new ObjectMapper();
    for(File file: files) {
      byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
      Concept concept = mapper.readValue(data, Concept.class);
      concepts.add(concept);
    }
    
    esOperations.loadConcepts(concepts, ElasticOperationsService.CONCEPT_INDEX, ElasticOperationsService.CONCEPT_TYPE, true);
  }
  
  //load concepts to es in batches
  private void loadConcepts(List<Concept> concepts) throws IOException {
    esOperations.loadConcepts(concepts, ElasticOperationsService.CONCEPT_INDEX, ElasticOperationsService.CONCEPT_TYPE, true);
  }
  
  /**

def get_concepts_from_es(es, index_name):
    concept_codes = []
    res = scan(
        client=es,
        scroll='1m',
        index=index_name,
        query={"_source": ['Code'], "query": {"match_all": {}}}
    )
    for doc in res:
        concept_codes.append(doc['_source']['Code'])
    return sorted(concept_codes)


def get_concepts_from_stardog(endpoint, named_graph, query):
    results = run_sparql_query(endpoint, named_graph, query)
    concept_codes = []
    if results is None:
        print_log("No Result Returned - Problem with Query")
    else:
        for result in results['results']['bindings']:
            code = result['concept_code']['value']
            concept_codes.append(code)
    return sorted(concept_codes)


def get_concepts_from_directory():
    concepts = []
    concepts_in_dir = [ os.path.basename(p) for p in glob.glob(CONCEPT_INPUT_DIR + "*.json")]
    for concept in concepts_in_dir:
        concepts.append(concept.replace(".json", ""))
    return concepts

def load_concept_bulk_file(es, index_name, concepts):
    count = 0
    for ok, result in streaming_bulk(
        es,
        prepare_concepts_from_file(concepts, index_name),
        request_timeout=120,
        raise_on_error=False,
        chunk_size=100
    ):
        action, result = result.popitem()
        doc_id = '/%s/doc/%s' % (index_name, result['_id'])
        if not ok:
            print_log('Failed to %s document %s: %r' % (action, doc_id, result)) 
        count += 1
        if count % 100 == 0:
            print_log("  Bulk Load: " + str(count))
  **/
}
