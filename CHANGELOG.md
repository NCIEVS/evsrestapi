# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).
## [1.6.0-SNAPSHOT] - 2022-05-24
### Added
- Added MetaSourceElasticLoadServiceImpl to support loads of individual terminologies from NCIM data (and in particular starting with MDR)
- Added "uiLabel" to terminology metadata to support human readable (friendly) name for terminologies in EVS Explore

### Changed
- API endpoints and model object fields called "termGroup" or "xxxTermGroup" were renamed to "termType" and "xxxTermType". This represents a breaking change for existing users.
- Improvements to the MetaElasticLoadServiceImpl loader to better manage relationships and relationship qualifiers loaded from NCIM data

## [1.5.0-SNAPSHOT and prior]
### No change log for earlier releases
