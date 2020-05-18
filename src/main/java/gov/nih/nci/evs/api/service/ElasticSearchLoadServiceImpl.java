package gov.nih.nci.evs.api.service;

import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.support.LoadConfig;

/**
 * 
 * 
 * @author Arun
 *
 */
@Service
public class ElasticSearchLoadServiceImpl implements ElasticSearchLoadService {

  @Override
  public void load(LoadConfig config) {

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
  
  /**

def create_index(es, index_name, mapping_file, drop_index):
    # Reading in the index_mapping file
    with open(mapping_file, 'r') as infile:
        body = infile.read()
    log.debug(body)

    if es.indices.exists(index_name) and not drop_index:
        print_log("Index already exists, so it will not be recreated")
    elif es.indices.exists(index_name) and drop_index:
        print_log("Index already exists, it will be dropped and recreated")
        es.indices.delete(index=index_name)
        es.indices.create(index=index_name, body=body)
    else:
        print_log("Creating new index")
        es.indices.create(index=index_name, body=body)


def download_concept(conceptCode):
    url = API_ENDPOINT + conceptCode
    r = requests.get(url)
    if r.status_code != 200:
        msg = "Problem Status Code: " + str(r.status_code) + " Concept: " + conceptCode
        print_log(msg)
        sys.stderr.write(msg + "\n")
        sys.exit(1)

    output_file = open(CONCEPT_OUTPUT_DIR + conceptCode + ".json", "w")
    print(r.text, file=output_file)
    output_file.close()

    return r.json()

def download_concept_files_batch(concepts_list, clean):
    number_of_concepts = len(concepts_list)
    print("Number of Concepts: " + str(number_of_concepts))
    if clean:
        shutil.rmtree(CONCEPT_OUTPUT_DIR)
        os.makedirs(CONCEPT_OUTPUT_DIR)
    start = 0
    end = 1000
    while (start < number_of_concepts):
        print("Downloading concepts: " + str(start) + " to " + str(end))
        in_clause = "'" + "', '".join(concepts_list[start:end]) + "'"
        start = end
        end = end + 1000
        if end > number_of_concepts:
            end = number_of_concepts

        concepts = eu.getBulkConcepts(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        properties = eu.getAllProperties(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)

        eu.mergeConceptsAndProperties(concepts, properties)
        del properties

        axioms = eu.getAllAxioms(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndAxioms(concepts, axioms)
        del axioms

        subclasses = eu.getAllSubclasses(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndSubclasses(concepts, subclasses)
        del subclasses

        superclasses = eu.getAllSuperclasses(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndSuperclasses(concepts, superclasses)
        del superclasses

        associations = eu.getAllAssociations(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndAssociations(concepts, associations)
        del associations

        inverse_associations = eu.getAllInverseAssociations(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndInverseAssociations(concepts, inverse_associations)
        del inverse_associations

        roles = eu.getAllRoles(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndRoles(concepts, roles)
        del roles

        inverse_roles = eu.getAllInverseRoles(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndInverseRoles(concepts, inverse_roles)
        del inverse_roles

        disjoint_withs = eu.getAllDisjointWith(SPARQL_ENDPOINT, NAMED_GRAPH, in_clause)
        eu.mergeConceptsAndDisjointWith(concepts, disjoint_withs)
        del disjoint_withs

        for code, concept in concepts.items():
            eu.addAdditionalProperties(concept)
            del concept['properties']
            eu.addFullSynonyms(concept)
            eu.addDefinitions(concept)
            eu.addAltDefinitions(concept)
            eu.addMapsTo(concept)
            eu.addGO_Annotation(concept)
            del concept['axioms']
            eu.addSubclasses(concept)
            del concept['subclasses']
            eu.addSuperclasses(concept)
            del concept['superclasses']
            eu.addAssociations(concept)
            del concept['associations']
            eu.addInverseAssociations(concept)
            del concept['inverse_associations']
            eu.addRoles(concept)
            del concept['roles']
            eu.addInverseRoles(concept)
            del concept['inverse_roles']
            eu.addDisjointWith(concept)
            del concept['disjoint_with']

        for code, concept in concepts.items():
            with open(CONCEPT_OUTPUT_DIR + concept['Code'] + ".json", "w") as output_file:
                print(json.dumps(concept, indent=2, sort_keys=False), file=output_file)
                output_file.close()

    os.system('touch {}'.format(CONCEPT_OUTPUT_DIR + "/DownloadSuccessfull.lck"))
    print_log("Finished Download Process")


def download_concept_files(concepts, processors, clean):
    print_log("Starting Download Process")
    if clean:
        shutil.rmtree(CONCEPT_OUTPUT_DIR)
        os.makedirs(CONCEPT_OUTPUT_DIR)
    download_using_parallel_processing(concepts, processors)
    #
    # Create a semaphore/lock file to indicate download was successfull
    # We will check for this files existence before attemping to upload
    # into ElasticSearch
    os.system('touch {}'.format(CONCEPT_OUTPUT_DIR + "/DownloadSuccessfull.lck"))
    print_log("Finished Download Process")


def download_using_parallel_processing(concepts, processors):
    pool = Pool(processors)
    counter = 0;
    for result in pool.imap_unordered(download_concept, concepts):
        counter += 1
        if counter % 100 == 0:
          print_log("  Count: " + str(counter))


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


def prepare_concepts_from_file(concepts, index_name):
    for concept_code in concepts:
        with open(CONCEPT_INPUT_DIR + concept_code + ".json", 'r') as f:
            json_concept = json.load(f)
        yield  {
            '_op_type': "index",
            '_index': index_name,
            '_type': "concept",
            '_id': concept_code,
            '_source': json_concept
        }


def prepare_concepts_from_download(concepts, index_name):
    for concept_code in concepts:
        json_concept = download_concept(concept_code)
        yield  {
            '_op_type': "index",
            '_index': index_name,
            '_type': "concept",
            '_id': concept_code,
            '_source': json_concept
        }


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


def load_concept_real_time(es, index_name, concepts):
    count = 0
    bulk_chunks = []
    for concept_code in concepts:
        json_concept = download_concept(concept_code)
        obj = {
            '_op_type': "index",
            '_index': index_name,
            '_type': "concept",
            '_id': concept_code,
            '_source': json_concept
        }
        bulk_chunks.append(obj)
        count += 1
        if count % 100 == 0:
            for ok, result in streaming_bulk(
                es,
                bulk_chunks,
                request_timeout=120,
                raise_on_error=False,
                chunk_size=100
            ):
                action, result = result.popitem()
                doc_id = '/%s/doc/%s' % (index_name, result['_id'])
                if not ok:
                    print_log('Failed to %s document %s: %r' % (action, doc_id, result)) 

            bulk_chunks = []
            print_log("  Real Time Load: " + str(count))

    if len(bulk_chunks) > 0:
        for ok, result in streaming_bulk(
            es,
            bulk_chunks,
            request_timeout=120,
            raise_on_error=False,
            chunk_size=100
        ):
            action, result = result.popitem()
            doc_id = '/%s/doc/%s' % (index_name, result['_id'])
            if not ok:
                print_log('Failed to %s document %s: %r' % (action, doc_id, result)) 

        print_log("  Real Time Load: " +str(count))


def print_log(msg):
    global log_file
    now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(now, ": ", msg, file=sys.stdout, flush=True, sep="")
    print(now, ": ", msg, file=log_file, flush=True, sep="")
    log.debug(msg)


  **/
}
