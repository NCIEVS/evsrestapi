
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link LoaderService}.
 *
 * @author Arun
 */
@Service
public class MappingLoaderServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(MappingLoaderServiceImpl.class);

  /** the environment *. */
  @Autowired
  Environment env;

  @Autowired
  ApplicationProperties applicationProperties;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticQueryService esQueryService;

  /** The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  public List<Map> buildMaps(String mappingData, String[] metadata) throws Exception {
    List<Map> maps = new ArrayList<Map>();
    String[] mappingDataList = mappingData.split("\n");
    // welcomeText = true format
    if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) {
      if (mappingDataList[0].split("\t").length > 2) {
        for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
          String[] conceptSplit = conceptMap.split("\t");
          Map conceptToAdd = new Map();
          conceptToAdd.setSourceCode(!conceptSplit[0].replace("\"", "").isBlank()
              ? conceptSplit[0].replace("\"", "") : "None");
          conceptToAdd.setSourceName(!conceptSplit[1].replace("\"", "").isBlank()
              ? conceptSplit[1].replace("\"", "") : "None");
          conceptToAdd.setSource(conceptSplit[2]);
          conceptToAdd.setType(conceptSplit[6]);
          conceptToAdd.setRank(conceptSplit[7]);
          conceptToAdd.setTargetCode(!conceptSplit[8].replace("\"", "").isBlank()
              ? conceptSplit[8].replace("\"", "") : "None");
          conceptToAdd.setTargetName(!conceptSplit[9].replace("\"", "").isBlank()
              ? conceptSplit[9].replace("\"", "") : "None");
          conceptToAdd.setTargetTerminology(conceptSplit[10]);
          conceptToAdd.setTargetTerminologyVersion(conceptSplit[11].replace("\"", ""));
          maps.add(conceptToAdd);
        }
      } else if (mappingDataList[0].split("\t").length == 2) {
        for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
          String[] conceptSplit = conceptMap.split("\t");

          // Determine "source"
          final String source = metadata[0].split("_")[0];
          final Terminology sourceTerminology =
              termUtils.getTerminology(source.toLowerCase(), true);
          final Concept sourceConcept = esQueryService
              .getConcept(conceptSplit[0].strip(), sourceTerminology, new IncludeParam())
              .orElse(null);
          String sourceName = "Unable to determine name";
          if (sourceConcept != null) {
            sourceName = sourceConcept.getName();
          }

          // Determine "target" terminology
          final String target = metadata[0].split("_")[2];
          final Terminology targetTerminology =
              termUtils.getTerminology(target.toLowerCase(), true);
          final Concept targetConcept = esQueryService
              .getConcept(conceptSplit[1].strip(), targetTerminology, new IncludeParam())
              .orElse(null);
          String targetName = "Unable to determine name";
          if (targetConcept != null) {
            targetName = targetConcept.getName();
          }

          Map conceptToAdd = new Map();
          conceptToAdd.setSourceCode(conceptSplit[0].strip());
          conceptToAdd.setSourceName(sourceName);
          conceptToAdd.setSource(sourceTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setType("mapsTo");
          conceptToAdd.setRank("1");
          conceptToAdd.setTargetCode(conceptSplit[1].strip());
          conceptToAdd.setTargetName(targetName);
          conceptToAdd.setTargetTerminology(
              targetTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setTargetTerminologyVersion(targetTerminology.getVersion());
          maps.add(conceptToAdd);
        }
      } else {
        logger.info("" + mappingDataList[0].split("\t"));
        throw new Exception(
            "Missing data in metadata for " + metadata[0] + " line: " + mappingDataList[0]);
      }
    }
    // mapsetLink = null + downloadOnly format
    else if (metadata[1] != null && !metadata[1].isEmpty() && !metadata[1].contains("ftp")) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\",\"");
        Map conceptToAdd = new Map();
        conceptToAdd.setSourceCode(!conceptSplit[0].replace("\"", "").isBlank()
            ? conceptSplit[0].replace("\"", "") : "None");
        conceptToAdd.setSourceName(!conceptSplit[1].replace("\"", "").isBlank()
            ? conceptSplit[1].replace("\"", "") : "None");
        conceptToAdd.setType(conceptSplit[2]);
        conceptToAdd.setTargetCode(!conceptSplit[3].replace("\"", "").isBlank()
            ? conceptSplit[3].replace("\"", "") : "None");
        conceptToAdd.setTargetName(!conceptSplit[4].replace("\"", "").isBlank()
            ? conceptSplit[4].replace("\"", "") : "None");
        conceptToAdd.setTargetTermType(conceptSplit[5]);
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

  public Boolean mappingNeedsUpdate(String code, String version,
    java.util.Map<String, String> currentMapsets) {

    // adding for first time
    if (currentMapsets.keySet().isEmpty() || !currentMapsets.keySet().contains(code)) {
      return true;
    }

    Optional<String> currentMapVersion = Optional.ofNullable(currentMapsets.get(code));
    // version number while current version number is null
    if (!version.isEmpty() && currentMapVersion.isPresent() && currentMapVersion.get().isEmpty()) {
      return true;
    }
    // different version
    if (!version.isEmpty() && currentMapVersion.isPresent()
        && !version.equals(currentMapVersion.get()))
      return true;
    return false;
  }

  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    final String uri = applicationProperties.getConfigBaseUri();
    final String mappingUri = uri.replaceFirst("config/metadata", "data/mappings/");
    final String mapsetMetadataUri = uri + "/mapsetMetadata.txt";
    String rawMetadata = IOUtils.toString(
        new URL(mapsetMetadataUri).openConnection().getInputStream(), StandardCharsets.UTF_8);
    List<String> allLines = Arrays.asList(rawMetadata.split("\n"));
    // skip header line
    allLines = allLines.subList(1, allLines.size());
    boolean created = operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
    if (created) {
      operationsService.getElasticsearchOperations().putMapping(
          ElasticOperationsService.MAPPING_INDEX, ElasticOperationsService.CONCEPT_TYPE,
          Concept.class);
    }

    List<String> allCodes = new ArrayList<String>();
    for (String line : allLines) {
      if (line.split(",")[4].contains("MappingLoadServiceImpl")) {
        allCodes.add(line.split(",")[0]);
      }
    }

    // all the current codes that this deals with
    List<String> currentMapsetCodes =
        esQueryService.getMapsets(new IncludeParam("properties")).stream()
            .filter(concept -> concept.getProperties().stream()
                .anyMatch(property -> property.getType().equals("loader")
                    && property.getValue().contains("MappingLoadServiceImpl")))
            .map(Concept::getCode).collect(Collectors.toList());

    List<String> currentMapsetVersions =
        esQueryService.getMapsets(new IncludeParam("properties")).stream()
            .filter(concept -> concept.getProperties().stream()
                .anyMatch(property -> property.getType().equals("loader")
                    && property.getValue().contains("MappingLoadServiceImpl")))
            .map(Concept::getVersion).collect(Collectors.toList());

    java.util.Map<String, String> currentMapsets =
        IntStream.range(0, currentMapsetCodes.size()).boxed().collect(
            Collectors.toMap(t -> currentMapsetCodes.get(t), t -> currentMapsetVersions.get(t)));

    // mapsets to add (not in current index and should be)
    List<String> mapsetsToAdd =
        allCodes.stream().filter(l -> !currentMapsetCodes.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to add = " + mapsetsToAdd.toString());

    // mapsets to remove (in current index and shouldn't be)
    List<String> mapsetsToRemove =
        currentMapsetCodes.stream().filter(l -> !allCodes.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to remove = " + mapsetsToRemove.toString());

    for (String line : allLines) { // build each mapset
      String[] metadata = line.split(",", -1);
      // remove and skip
      if (mapsetsToRemove.contains(metadata[0])) {
        logger.info("deleting " + metadata[0]);
        operationsService.delete(ElasticOperationsService.MAPPING_INDEX,
            ElasticOperationsService.CONCEPT_TYPE, metadata[0]);
        continue;
      }
      // skip if no update needed
      if (!mappingNeedsUpdate(metadata[0], metadata[2], currentMapsets)) {
        continue;
      } else if (!mapsetsToAdd.contains(metadata[0])) {
        logger.info(metadata[0] + " needs update to version: " + metadata[2]);
      }
      Concept map = new Concept();
      map.setName(metadata[0]);
      map.setCode(metadata[0]);
      if (metadata[2] != null && !metadata[2].isEmpty()) { // version numbers
        map.setVersion(metadata[2]);
      } else {
        map.setVersion(null);
      }
      map.getProperties().add(new Property("loader", "MappingLoadServiceImpl"));

      // setting up metadata
      if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) { // welcome
                                                                                       // text
        String welcomeText =
            IOUtils.toString(new URL(uri + "/" + metadata[3]).openConnection().getInputStream(),
                StandardCharsets.UTF_8);
        map.getProperties().add(new Property("welcomeText", welcomeText));

        String mappingDataUri = mappingUri + map.getName()
            + (map.getVersion() != null ? ("_" + map.getVersion()) : "") + ".txt"; // build
        // map
        String mappingData = IOUtils.toString(
            new URL(mappingDataUri).openConnection().getInputStream(), StandardCharsets.UTF_8);
        map.setMaps(buildMaps(mappingData, metadata));

      }

      if (metadata[1] != null && !metadata[1].isEmpty()) { // download links

        map.getProperties().add(new Property("downloadOnly", "true"));
        if (metadata[1].contains("ftp")) {
          map.getProperties().add(new Property("mapsetLink", metadata[1]));
        } else {
          map.getProperties().add(new Property("mapsetLink", null));
          String mappingDataUri = mappingUri + map.getName()
              + (map.getVersion() != null ? ("_" + map.getVersion()) : "") + ".csv"; // build
          // map
          String mappingData = IOUtils.toString(
              new URL(mappingDataUri).openConnection().getInputStream(), StandardCharsets.UTF_8);
          map.setMaps(buildMaps(mappingData, metadata));
        }
      } else {
        map.getProperties().add(new Property("downloadOnly", "false"));
      }
      logger.info("indexing " + metadata[0]);

      Collections.sort(map.getMaps(), new Comparator<gov.nih.nci.evs.api.model.Map>() {
        @Override
        public int compare(final gov.nih.nci.evs.api.model.Map o1,
          final gov.nih.nci.evs.api.model.Map o2) {
          // Assume maps are not null
          return (o1.getSourceName() + o1.getType() + o1.getGroup() + o1.getRank()
              + o1.getTargetName())
              .compareTo(o2.getSourceName() + o2.getType() + o2.getGroup() + o2.getRank()
                  + o2.getTargetName());
        }
      });
      operationsService.index(map, ElasticOperationsService.MAPPING_INDEX,
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);

    }
  }

  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config,
    String filepath, String termName, boolean forceDelete) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
