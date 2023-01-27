# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [1.7.0.RELEASE] - 2022-05-24
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
