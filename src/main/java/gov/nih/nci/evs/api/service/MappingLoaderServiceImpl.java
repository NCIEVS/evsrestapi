package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.OpensearchLoadConfig;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.tomcat.util.buf.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * The implementation for {@link BaseLoaderService}.
 *
 * @author Arun
 */
@Service
public class MappingLoaderServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(MappingLoaderServiceImpl.class);

  /** the environment *. */
  @Autowired Environment env;

  /** The application properties. */
  @Autowired ApplicationProperties applicationProperties;

  /** The Opensearch operations service instance *. */
  @Autowired OpensearchOperationsService operationsService;

  /** The Opensearch operations service instance *. */
  @Autowired OpensearchQueryService osQueryService;

  /** The terminology utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * Builds the maps.
   *
   * @param mappingData the mapping data
   * @param metadata the metadata
   * @return the list
   * @throws Exception the exception
   */
  public List<Mapping> buildMaps(final String mappingData, final String[] metadata)
      throws Exception {
    final List<Mapping> maps = new ArrayList<>();
    final String[] mappingDataList = mappingData.split("\n");

    if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) {
      // Support for ICD10-MDR mappings and similar
      if (mappingDataList[0].split("\t").length > 2) {
        for (final String conceptMap :mappingDataList) {
          // Skip header
          if (conceptMap.contains("Source Code")) {
            continue;
          }

          final String[] conceptSplit = conceptMap.split("\t");
          final Mapping conceptToAdd = new Mapping();
          conceptToAdd.setMapsetCode(metadata[0]);
          conceptToAdd.setSourceCode(
              !conceptSplit[0].replace("\"", "").isBlank()
                  ? conceptSplit[0].replace("\"", "")
                  : "N/A");
          conceptToAdd.setSource(metadata[5]);
          conceptToAdd.setSourceName(
              !conceptSplit[1].replace("\"", "").isBlank()
                  ? conceptSplit[1].replace("\"", "")
                  : "N/A");
          conceptToAdd.setSourceTerminology(conceptSplit[2]);
          conceptToAdd.setType(conceptSplit[6]);
          conceptToAdd.setRank(conceptSplit[7]);
          conceptToAdd.setTarget(metadata[7]);
          conceptToAdd.setTargetCode(
              !conceptSplit[8].replace("\"", "").isBlank()
                  ? conceptSplit[8].replace("\"", "")
                  : "N/A");
          conceptToAdd.setTargetName(
              !conceptSplit[9].replace("\"", "").isBlank()
                  ? conceptSplit[9].replace("\"", "")
                  : "N/A");
          conceptToAdd.setTargetTerminology(conceptSplit[10]);
          conceptToAdd.setTargetTerminologyVersion(conceptSplit[11].replace("\"", ""));
          maps.add(conceptToAdd);
        }
      }
      // Support for NCIT-HGNC maps and others regularly updated by NCI
      else if (mappingDataList[0].split("\t").length == 2) {
        for (final String conceptMap :mappingDataList) {
          // Skip header
          if (conceptMap.contains("Source Code")) {
            continue;
          }
          final String[] conceptSplit = conceptMap.split("\t");

          // Determine "source"
          final String source = metadata[0].split("_")[0];
          final Terminology sourceTerminology =
              termUtils.getIndexedTerminology(source.toLowerCase(), osQueryService, true);
          final Concept sourceConcept =
              osQueryService
                  .getConcept(conceptSplit[0].strip(), sourceTerminology, new IncludeParam())
                  .orElse(null);
          String sourceName = "Unable to determine name";
          if (sourceConcept != null) {
            sourceName = sourceConcept.getName();
          }

          // Determine "target" terminology
          final String target = metadata[0].split("_")[2];
          final Terminology targetTerminology =
              termUtils.getIndexedTerminology(target.toLowerCase(), osQueryService, true);
          final Concept targetConcept =
              osQueryService
                  .getConcept(conceptSplit[1].strip(), targetTerminology, new IncludeParam())
                  .orElse(null);
          String targetName = "Unable to determine name";
          if (targetConcept != null) {
            targetName = targetConcept.getName();
          }

          final Mapping conceptToAdd = new Mapping();
          conceptToAdd.setMapsetCode(metadata[0]);
          conceptToAdd.setSourceCode(conceptSplit[0].strip());
          conceptToAdd.setSourceName(sourceName);
          conceptToAdd.setSource(metadata[5]);
          conceptToAdd.setSourceTerminology(
              sourceTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setType("mapsTo");
          conceptToAdd.setRank("1");
          conceptToAdd.setTargetCode(conceptSplit[1].strip());
          conceptToAdd.setTargetName(targetName);
          conceptToAdd.setTarget(metadata[7]);
          conceptToAdd.setTargetTerminology(
              targetTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setTargetTerminologyVersion(targetTerminology.getVersion());
          maps.add(conceptToAdd);
        }
      } else {
        logger.info("  line = " + Arrays.asList(mappingDataList[0].split("\t")).toString());
        throw new Exception(
            "Missing data in metadata for " + metadata[0] + " line: " + mappingDataList[0]);
      }
    }
    // mapsetLink = null + downloadOnly format
    else if (metadata[1] != null && !metadata[1].isEmpty() && !metadata[1].contains("ftp")) {
      for (final String conceptMap :
          Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        final String[] conceptSplit = conceptMap.split("\",\"");
        final Mapping conceptToAdd = new Mapping();
        conceptToAdd.setMapsetCode(metadata[0]);
        conceptToAdd.setSourceCode(
            !conceptSplit[0].replace("\"", "").isBlank()
                ? conceptSplit[0].replace("\"", "")
                : "N/A");
        conceptToAdd.setSourceName(
            !conceptSplit[1].replace("\"", "").isBlank()
                ? conceptSplit[1].replace("\"", "")
                : "N/A");
        conceptToAdd.setType(conceptSplit[2]);
        conceptToAdd.setTargetCode(
            !conceptSplit[3].replace("\"", "").isBlank()
                ? conceptSplit[3].replace("\"", "")
                : "N/A");
        conceptToAdd.setTargetName(
            !conceptSplit[4].replace("\"", "").isBlank()
                ? conceptSplit[4].replace("\"", "")
                : "N/A");
        conceptToAdd.setTargetTermType(conceptSplit[5]);
        conceptToAdd.setTarget(conceptSplit[6]);
        conceptToAdd.setTargetTerminology(conceptSplit[6]);
        conceptToAdd.setTargetTerminologyVersion(conceptSplit[7].replace("\"", ""));
        maps.add(conceptToAdd);
      }
    }
    // ftp format = direct download and no maps
    else {
      return null;
    }
    return maps;
  }

  /**
   * Mapping needs update.
   *
   * @param code the code
   * @param version the version
   * @param currentMapsets the current mapsets
   * @return the boolean
   */
  public Boolean mappingNeedsUpdate(
      final String code, final String version, final Map<String, String> currentMapsets) {

    // adding for first time
    if (currentMapsets.keySet().isEmpty() || !currentMapsets.keySet().contains(code)) {
      return true;
    }

    final Optional<String> currentMapVersion = Optional.ofNullable(currentMapsets.get(code));
    // version number while current version number is null
    if (!version.isEmpty() && currentMapVersion.isPresent() && currentMapVersion.get().isEmpty()) {
      return true;
    }
    // different version
    if (!version.isEmpty()
        && currentMapVersion.isPresent()
        && !version.equals(currentMapVersion.get())) {
      return true;
    }
    return false;
  }

  /* see superclass */
  @Override
  public void loadObjects(
      final OpensearchLoadConfig config,
      final Terminology terminology,
      final HierarchyUtils hierarchy)
      throws Exception {

    // Get mapset metadata without the header line
    final String uri = applicationProperties.getConfigBaseUri();
    final String mappingUri = uri.replaceFirst("config/metadata", "data/mappings/");
    final String mapsetMetadataUri = uri + "/mapsetMetadata.txt";
    logger.info("  Download mapset metadata = " + mapsetMetadataUri);
    final String rawMetadata =
        StringUtils.join(EVSUtils.getValueFromFile(mapsetMetadataUri, "mapsetMetadataUri"), '\n');
    List<String> allLines = Arrays.asList(rawMetadata.split("\n"));
    allLines = allLines.subList(1, allLines.size());

    // Find all mapset codes from metadata that are handled by this loader
    final List<String> allCodes = new ArrayList<String>();
    for (final String line : allLines) {
      if (line.split(",")[4].contains("MappingLoadServiceImpl")) {
        allCodes.add(line.split(",")[0]);
      }
    }

    // Build mapset code -> version map
    final List<String> currentMapsetCodes =
        osQueryService.getMapsets(new IncludeParam("properties")).stream()
            .filter(
                concept ->
                    concept.getProperties().stream()
                        .anyMatch(
                            property ->
                                property.getType().equals("loader")
                                    && property.getValue().contains("MappingLoadServiceImpl")))
            .map(Concept::getCode)
            .collect(Collectors.toList());

    final List<String> currentMapsetVersions =
        osQueryService.getMapsets(new IncludeParam("properties")).stream()
            .filter(
                concept ->
                    concept.getProperties().stream()
                        .anyMatch(
                            property ->
                                property.getType().equals("loader")
                                    && property.getValue().contains("MappingLoadServiceImpl")))
            .map(Concept::getVersion)
            .collect(Collectors.toList());

    final Map<String, String> mapsetVersionMap =
        IntStream.range(0, currentMapsetCodes.size())
            .boxed()
            .collect(
                Collectors.toMap(
                    t -> currentMapsetCodes.get(t), t -> currentMapsetVersions.get(t)));

    // mapsets to add (not in current index and should be)
    final List<String> mapsetsToAdd =
        allCodes.stream().filter(l -> !currentMapsetCodes.contains(l)).collect(Collectors.toList());
    logger.info("  Mapsets to add = " + mapsetsToAdd);

    // mapsets to remove (in current index and shouldn't be)
    final List<String> mapsetsToRemove =
        currentMapsetCodes.stream().filter(l -> !allCodes.contains(l)).collect(Collectors.toList());
    logger.info("  Mapsets to remove = " + mapsetsToRemove);

    // remove all in mapsetsToRemove
    for (final String code : mapsetsToRemove) {
      logger.info("  Delete mapset (and mappings) = " + code);
      // delete the mapset and all mappings
      operationsService.delete(OpensearchOperationsService.MAPSET_INDEX, code);
      operationsService.deleteQuery(
          "mapsetCode:" + code, OpensearchOperationsService.MAPPINGS_INDEX);
    }

    final List<String> terms =
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .map(Terminology::getTerminology)
            .collect(Collectors.toList());

    for (final String line : allLines) {

      // build each mapset
      final String[] metadata = line.split(",", -1);
      final String code = metadata[0];
      final String version = metadata[2];

      // skip if no update needed
      if (!mappingNeedsUpdate(code, version, mapsetVersionMap)) {
        logger.info("  SKIP current mapset = " + code + " " + version);
        continue;
      } else if (!mapsetsToAdd.contains(code)) {
        logger.info("  Update mapset to version = " + code + " " + version);
        // No need to delete from MAPSET_INDEX because the index call below
        // will just replace/update the mapset to the new version
        logger.info("    delete old version maps");
        operationsService.deleteQuery(
            "mapsetCode:" + code, OpensearchOperationsService.MAPPINGS_INDEX);
      } else {
        logger.info("  Add mapset = " + code + " " + version);
      }

      // Create the mapset concept and set metadata
      final Concept map = new Concept();
      map.setName(code);
      map.setCode(code);
      // version numbers
      if (version != null && !version.isEmpty()) {
        map.setVersion(version);
        map.getProperties().add(new Property("date", FhirUtility.convertToYYYYMMDD(version)));
      } else {
        map.setVersion(null);
      }
      map.getProperties().add(new Property("loader", "MappingLoadServiceImpl"));

      // Get the welcome text
      if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) {
        final String welcomeText =
            String.join("\n", EVSUtils.getValueFromFile(uri + "/" + metadata[3], "welcomeText"));

        // Configure source/target terminology and version
        map.getProperties().add(new Property("welcomeText", welcomeText));
        map.getProperties().add(new Property("sourceTerminology", metadata[5]));
        map.getProperties().add(new Property("sourceTerminologyVersion", metadata[6]));
        map.getProperties().add(new Property("targetTerminology", metadata[7]));
        map.getProperties()
            .add(new Property("targetTerminologyVersion", metadata[8].replaceAll("\\s", "")));
        map.getProperties()
            .add(new Property("sourceLoaded", Boolean.toString(terms.contains(metadata[5]))));
        map.getProperties()
            .add(new Property("targetLoaded", Boolean.toString(terms.contains(metadata[7]))));

        // Get the map data and build the maps
        final String mappingDataUri =
            mappingUri
                + map.getName()
                + (map.getVersion() != null ? ("_" + map.getVersion()) : "")
                + ".txt";
        final String mappingData =
            StringUtils.join(EVSUtils.getValueFromFile(mappingDataUri, "mappingDataUri"), '\n');
        map.setMaps(buildMaps(mappingData, metadata));
      }

      // download links
      if (metadata[1] != null && !metadata[1].isEmpty()) {

        map.getProperties().add(new Property("downloadOnly", "true"));
        if (metadata[1].contains("ftp")) {
          map.getProperties().add(new Property("mapsetLink", metadata[1]));
        } else {
          map.getProperties().add(new Property("mapsetLink", null));
          // build map
          final String mappingDataUri =
              mappingUri
                  + map.getName()
                  + (map.getVersion() != null ? ("_" + map.getVersion()) : "")
                  + ".csv";

          final String mappingData =
              StringUtils.join(EVSUtils.getValueFromFile(mappingDataUri, "mappingDataUri"), '\n');
          map.setMaps(buildMaps(mappingData, metadata));
        }
      } else {
        map.getProperties().add(new Property("downloadOnly", "false"));
      }
      logger.info("  INDEX mapset (and mappings) = " + code + " " + version);

      // Sort maps (e.g. mostly for SNOMEDCT maps which have multiple groups)
      Collections.sort(
          map.getMaps(),
          new Comparator<Mapping>() {
            @Override
            public int compare(final Mapping o1, final Mapping o2) {
              // Assume maps are not null
              return (o1.getSourceName()
                      + o1.getType()
                      + o1.getGroup()
                      + o1.getRank()
                      + o1.getTargetName())
                  .compareTo(
                      o2.getSourceName()
                          + o2.getType()
                          + o2.getGroup()
                          + o2.getRank()
                          + o2.getTargetName());
            }
          });
      int i = 1;
      for (final Mapping mapToSort : map.getMaps()) {
        mapToSort.setSortKey(String.valueOf(1000000 + i++));
      }
      operationsService.bulkIndex(
          map.getMaps(), OpensearchOperationsService.MAPPINGS_INDEX, Mapping.class);
      map.setMaps(null);
      operationsService.index(map, OpensearchOperationsService.MAPSET_INDEX, Concept.class);
    }
  }

  /* see superclass */
  @Override
  public int loadConcepts(
      final OpensearchLoadConfig config,
      final Terminology terminology,
      final HierarchyUtils hierarchy,
      Map<String, List<Map<String, String>>> historyMap)
      throws IOException, Exception {
    // n/a
    return 0;
  }

  /* see superclass */
  @Override
  public Terminology getTerminology(
      final ApplicationContext app,
      final OpensearchLoadConfig config,
      final String filepath,
      final String termName,
      final boolean forceDelete)
      throws Exception {
    // n/a
    return null;
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(final Terminology term) throws Exception {
    // n/a
    return null;
  }
}
