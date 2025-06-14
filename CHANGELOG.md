# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [2.2.0.RELEASE] - 2025-06-10
### Changed
- FHIR improvements (improved param support, improved swagger, improved metadata, more tests)
- Improvements to computed childhood neoplasm subsets
- Improved handling of complex defintions
- Improved handling of NCI Thesaurus cumulative history

## [2.1.1.RELEASE] - 2025-04-30
### Changed
- Load childhood neoplasm data from a renamed file and include subsetLink data
- Fix the query for deleting mappings when fixing the mapping version.

## [2.1.0.RELEASE] - 2025-03-18
### Added
- Added support for Childhood Malignant Neoplasm Subset.
### Changed
- Backend migration from Stardog to Apache Jena for data loading and indexing
- Updates to the EVSRESTAPI SDK examples.

## [2.0.0.RELEASE] - 2025-01-28
### Added
- FHIR R5 terminology endpoints for CodeSystem, ValueSet, and ConceptMap
- Added vulnerability scanning
- Added endpoint for URL mapping from terms browser to evs explore
- Add sending email to term form handling
### Changed
- Upgrade backend to Spring Boot 3 (and J17), Upgrade tests to Junit 5
- FHIR R4 improvements and alignment with spec
- More consistent error handling
- Fixes to swagger
- Improvements to SPARQL query handling
- Improvements to backend handling of "mappings" to use a separate index for mappings

## [1.10.0.RELEASE] - 2024-07-22
### Added
- FHIR R4 terminology endpoints for CodeSystem, ValueSet, and ConceptMap
- Additional search endpoints to support Sparql querying (of rdf-loaded terminologies)
- Support for additional terminologies (both RRF and RDF) and corresponding tests
- Backend support for term form submission
### Changed
- Upgrade to Java17
- Upgrade to Elasticsearch 7.10+
- Improvements to elasticsearch "mappings" for model objects
- More work to factor out configuration info - notably: calculation of "terminology" values and sparql prefixes
- Fix bugs related terminology content

## [1.9.0.RELEASE] - 2024-02-01
### Added
- Added "concept.active" flag
- Implement MedDRA license check
- Implement loading of NCIm "source statistics" data
### Changed
- Improvements to loader scripts for logging and return values
- Changes to support new terminologies (MED-RT, CanMED, CTCAE5, UMLS Semantic Network)
- Fixes to sample testing to properly verify (and fixes to RRF sampling data for hierarchies)
- Cleanup of swagger to align with exactly what data to expose (and reconcile evsrestapi-client-SDK)
- Disable internal metrics recording
- Add stemming to concept indexes/search
- Improvements to handling/versioning of maps (including support for simpler data representation)
- Fix paging bugs

## [1.8.1.RELEASE] - 2023-10-31
### Added
- Swagger update for global header

## [1.8.0.RELEASE] - 2023-05-31
### Added
- Sampling QA for all terminologies loaded into the dev and deploy environments
1.9.0-RC-master- Support for additional RRF terminolologies, including ICD10CM, ICD9CM.
- New mapset endpoints for interacting with cross-terminology mapping data
- Support for loading cross-terminology mapping data sets
- New history endpoints for interacting with terminology history information
- New "history" field of Concept objects to represent individual history elements for a concept
- New subsets endpoints for accessing subset/value set data independenty of the metadata endpoints
- Some endpoints now have a "limit" parameter that causes less information to be loaded and allowing for a "load more later" approach
### Changed
- Metadata for remodeled properties is now being included with information about how it is remodeled.
- Limit page size to 10 for terminologies with license restrictions - to discourage bulk downloading
- Update to swagger libraries and some tuning of documentation
- Various bug fixes to searching, loading of qualifiers with URL values, and subset/member performance

## [1.7.2.RELEASE] - 2023-03-31
### Changed
- Fixed logic of /subsets call to avoid returning Publish_Value_Set=No

## [1.7.1.RELEASE] - 2023-03-15
### Changed
- Fixed performance of content /pathToRoot, /pathFromRoot, and /pathToAncestor calls and added paging params
- Fixed performance of metadata/{terminology}/subsets call to more efficiently return all ncit subsets

## [1.7.0.RELEASE] - 2023-01-13
### Added
- Added an additional "value" parameter that works with "property" and can also work with "term" 
- Added "has terminology hierarchy" info to terminology metadata to allow setting hierarchy links for terms that have a valid hierarchy
- Loading/Indexing of the Chebi terminologyI.
- Loading/Indexing of the HGNC terminology.
- Loading/Indexing of the GO terminology.
- Add API endpoint for terminology "welcome text"
- Extended "subset" parameter in search calls to accept a wildcard to indicate any subset
- Support sortBy parameter in search API
- Created a QA harness to leverage the sampling files to perform QA.
### Changed
 - Improvement to role restriction query, it now uses a single query that finds all relevant paths.
 - Added limit on concept code/subtree results for quick retrieval of data by UI while allowing "full" data by default

## [1.6.0.RELEASE] - 2022-05-24
### Added
- Added MetaSourceElasticLoadServiceImpl to support loads of individual terminologies from NCIM data (and in particular starting with MDR)
- Added "uiLabel" to terminology metadata to support human readable (friendly) name for terminologies in EVS Explore

### Changed
- API endpoints and model object fields called "termGroup" or "xxxTermGroup" were renamed to "termType" and "xxxTermType". This represents a breaking change for existing users.
- Improvements to the MetaElasticLoadServiceImpl loader to better manage relationships and relationship qualifiers loaded from NCIM data

## [1.5.0-SNAPSHOT and prior]
### No change log for earlier releases
