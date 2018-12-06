#!/usr/bin/env python

import sys
import os
import shutil
import logging
import json
import re
import requests
import time
import datetime
from requests.auth import HTTPBasicAuth
from argparse import ArgumentParser
from pathlib import Path
from dotenv import load_dotenv

from elasticsearch import Elasticsearch
from elasticsearch.exceptions import TransportError
from elasticsearch.helpers import bulk, scan, streaming_bulk, parallel_bulk

from multiprocessing.pool import Pool

env_path = Path('.')/'.env'
load_dotenv(".env")
API_ENDPOINT = os.environ.get("API_ENDPOINT")
CONCEPT_OUTPUT_DIR = os.environ.get('CONCEPT_OUTPUT_DIR')
CONCEPT_INPUT_DIR = os.environ.get('CONCEPT_INPUT_DIR')
ES_HOST = os.environ.get('ES_HOST')
ES_PORT = os.environ.get('ES_PORT')
INDEX_MAPPING_FILE = os.environ.get("INDEX_MAPPING_FILE")
LOG_DIRECTORY = os.environ.get("LOG_DIRECTORY")
MULTI_PROCESSING_POOL = int(os.environ.get("MULTI_PROCESSING_POOL"))
NAMED_GRAPH = os.environ.get("NAMED_GRAPH")
SPARQL_ENDPOINT = os.environ.get("SPARQL_ENDPOINT")
STARDOG_USERNAME = os.environ.get("STARDOG_USERNAME")
STARDOG_PASSWORD = os.environ.get("STARDOG_PASSWORD")

SPARQL_PREFIX = '''
PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>
PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX dc:<http://purl.org/dc/elements/1.1/>
PREFIX xml:<http://www.w3.org/2001/XMLSchema#>
'''

ALL_CONCEPTS_QUERY = '''
SELECT ?concept_code ?concept_label
{
    GRAPH <NAMED_GRAPH>
    {
        ?concept a owl:Class .
        ?concept rdfs:label ?concept_label .
        ?concept :NHC0 ?concept_code
    }
}
ORDER BY ?concept_code
'''

#
# run_sparql_query
#
def run_sparql_query(endpoint, named_graph, query):
    '''Generic method for running a SPARQL query.
    Checks the status code and returns results in JSON format.

    :param str endpoint: url for endpoint
    :param str named_graph: named_graph
    :param str query: SPARQL query
    :rtype: JSON
    :returns: query results in JSON format
    '''

    query = query.replace("NAMED_GRAPH", named_graph)
    sparql_query = SPARQL_PREFIX + query
    headers = {'Accept': 'application/sparql-results+json'}
    r = requests.post(endpoint,
                  headers=headers, data={"query": sparql_query},
                  auth=HTTPBasicAuth("admin", "admin"))

    if r.status_code != 200:
        sys.stderr.write("Problem Status Code: " + str(r.status_code) + "\n")
        return None

    return r.json()

def connect_elasticsearch(host="localhost", port=9200):
    es = None
    try: 
        es = Elasticsearch([{'host': host, 'port': port}])
        if es.ping():
            log.debug("Connection to ElasticSearch Successful")
            return es
        else:
            log.debug("Connection to ElasticSearch Failed")
            return None
    except:
        log.debug("Connection to ElasticSearch Failed")
        return None
    
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
        msg = "Problem Status Code: " + str(r.status_code)
        print_log(msg)
        sys.stderr.write(msg + "\n")
        sys.exit(1)
    
    output_file = open(CONCEPT_OUTPUT_DIR + conceptCode + ".json", "w")
    print(r.text, file=output_file)
    output_file.close()
    
    return r.json()

def download_concept_files(concepts, processors):
    print_log("Starting Download Process")
    shutil.rmtree(CONCEPT_OUTPUT_DIR)
    os.makedirs(CONCEPT_OUTPUT_DIR)
    download_using_parallel_processing(concepts, processors)
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
        index = index_name,
        query = {"_source": ['Code'], "query": {"match_all": {}}}
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
    concepts_in_dir = os.listdir(CONCEPT_INPUT_DIR)
    for concept in concepts_in_dir:
        concepts.append(concept.replace(".json",""))
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

if __name__ == '__main__':
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
    if es == None:
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
        download_concept_files(concepts_in_stardog, MULTI_PROCESSING_POOL)
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
            download_concept_files(concepts_in_stardog, MULTI_PROCESSING_POOL)

        concepts_in_file_system = get_concepts_from_directory()
        print_log("Number of concepts in File System: " + str(len(concepts_in_file_system)))
        print_log("Starting the Upload Process using Bulk Load")
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
            print(concept, file = f)

    with open(LOG_DIRECTORY + "concepts_only_in_es.log", "w") as f: 
        for concept in concepts_only_in_es:
            print(concept, file = f)

    if args.delete_documents:
        for concept in concepts_only_in_es:
            print_log("Deleting from ES: ",concept)
            es.delete(index=args.index_name, doc_type="concept", id=concept)

    print_log("Finished ElasticSearch Load Process")
    log_file.close()
