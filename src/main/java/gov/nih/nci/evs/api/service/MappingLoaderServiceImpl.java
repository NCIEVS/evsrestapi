
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMap;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

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

  public List<ConceptMap> buildMaps(String mappingData, String[] metadata) throws Exception {
    List<ConceptMap> maps = new ArrayList<ConceptMap>();
    String[] mappingDataList = mappingData.split("\n");
    // welcomeText = true format
    if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) {
      if (mappingDataList[0].split("\t").length > 2) {
        for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
          String[] conceptSplit = conceptMap.split("\t");
          ConceptMap conceptToAdd = new ConceptMap();
          conceptToAdd
              .setSourceCode(!conceptSplit[0].replace("\"", "").isBlank() ? conceptSplit[0].replace("\"", "") : "N/A");
          conceptToAdd
              .setSourceName(!conceptSplit[1].replace("\"", "").isBlank() ? conceptSplit[1].replace("\"", "") : "N/A");
          conceptToAdd.setSource(conceptSplit[2]);
          conceptToAdd.setType(conceptSplit[6]);
          conceptToAdd.setRank(conceptSplit[7]);
          conceptToAdd
              .setTargetCode(!conceptSplit[8].replace("\"", "").isBlank() ? conceptSplit[8].replace("\"", "") : "N/A");
          conceptToAdd
              .setTargetName(!conceptSplit[9].replace("\"", "").isBlank() ? conceptSplit[9].replace("\"", "") : "N/A");
          conceptToAdd.setTargetTerminology(conceptSplit[10]);
          conceptToAdd.setTargetTerminologyVersion(conceptSplit[11].replace("\"", ""));
          maps.add(conceptToAdd);
        }
      } else if (mappingDataList[0].split("\t").length == 2) {
        for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
          String[] conceptSplit = conceptMap.split("\t");

          // Determine "source"
          final String source = metadata[0].split("_")[0];
          final Terminology sourceTerminology = termUtils.getIndexedTerminology(source.toLowerCase(), esQueryService);
          final Concept sourceConcept =
              esQueryService.getConcept(conceptSplit[0].strip(), sourceTerminology, new IncludeParam()).orElse(null);
          String sourceName = "Unable to determine name";
          if (sourceConcept != null) {
            sourceName = sourceConcept.getName();
          }

          // Determine "target" terminology
          final String target = metadata[0].split("_")[2];
          final Terminology targetTerminology = termUtils.getIndexedTerminology(target.toLowerCase(), esQueryService);
          final Concept targetConcept =
              esQueryService.getConcept(conceptSplit[1].strip(), targetTerminology, new IncludeParam()).orElse(null);
          String targetName = "Unable to determine name";
          if (targetConcept != null) {
            targetName = targetConcept.getName();
          }

          ConceptMap conceptToAdd = new ConceptMap();
          conceptToAdd.setSourceCode(conceptSplit[0].strip());
          conceptToAdd.setSourceName(sourceName);
          conceptToAdd.setSource(sourceTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setType("mapsTo");
          conceptToAdd.setRank("1");
          conceptToAdd.setTargetCode(conceptSplit[1].strip());
          conceptToAdd.setTargetName(targetName);
          conceptToAdd.setTargetTerminology(targetTerminology.getMetadata().getUiLabel().replaceAll(" ", "_"));
          conceptToAdd.setTargetTerminologyVersion(targetTerminology.getVersion());
          maps.add(conceptToAdd);
        }
      } else {
        logger.info("" + mappingDataList[0].split("\t"));
        throw new Exception("Missing data in metadata for " + metadata[0] + " line: " + mappingDataList[0]);
      }
    }
    // mapsetLink = null + downloadOnly format
    else if (metadata[1] != null && !metadata[1].isEmpty() && !metadata[1].contains("ftp")) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\",\"");
        ConceptMap conceptToAdd = new ConceptMap();
        conceptToAdd
            .setSourceCode(!conceptSplit[0].replace("\"", "").isBlank() ? conceptSplit[0].replace("\"", "") : "N/A");
        conceptToAdd
            .setSourceName(!conceptSplit[1].replace("\"", "").isBlank() ? conceptSplit[1].replace("\"", "") : "N/A");
        conceptToAdd.setType(conceptSplit[2]);
        conceptToAdd
            .setTargetCode(!conceptSplit[3].replace("\"", "").isBlank() ? conceptSplit[3].replace("\"", "") : "N/A");
        conceptToAdd
            .setTargetName(!conceptSplit[4].replace("\"", "").isBlank() ? conceptSplit[4].replace("\"", "") : "N/A");
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

  public Boolean mappingNeedsUpdate(String code, String version, Map<String, String> currentMapsets) {

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
    if (!version.isEmpty() && currentMapVersion.isPresent() && !version.equals(currentMapVersion.get()))
      return true;
    return false;
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws IOException, Exception {
    final String uri = applicationProperties.getConfigBaseUri();
    final String mappingUri = uri.replaceFirst("config/metadata", "data/mappings/");
    final String mapsetMetadataUri = uri + "/mapsetMetadata.txt";
    String rawMetadata = null;
    try (final InputStream is = new URL(mapsetMetadataUri).openConnection().getInputStream()) {
      rawMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
    }
    List<String> allLines = Arrays.asList(rawMetadata.split("\n"));
    // skip header line
    allLines = allLines.subList(1, allLines.size());
    boolean created = operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
    if (created) {
      operationsService
              .getElasticsearchOperations()
              .indexOps(IndexCoordinates.of(ElasticOperationsService.MAPPING_INDEX))
              .putMapping(Concept.class);
    }

    List<String> allCodes = new ArrayList<String>();
    for (String line : allLines) {
      if (line.split(",")[4].contains("MappingLoadServiceImpl")) {
        allCodes.add(line.split(",")[0]);
      }
    }

    // all the current codes that this deals with
    List<String> currentMapsetCodes = esQueryService.getMapsets(new IncludeParam("properties")).stream()
        .filter(concept -> concept.getProperties().stream().anyMatch(
            property -> property.getType().equals("loader") && property.getValue().contains("MappingLoadServiceImpl")))
        .map(Concept::getCode).collect(Collectors.toList());

    List<String> currentMapsetVersions = esQueryService.getMapsets(new IncludeParam("properties")).stream()
        .filter(concept -> concept.getProperties().stream().anyMatch(
            property -> property.getType().equals("loader") && property.getValue().contains("MappingLoadServiceImpl")))
        .map(Concept::getVersion).collect(Collectors.toList());

    Map<String, String> currentMapsets = IntStream.range(0, currentMapsetCodes.size()).boxed()
        .collect(Collectors.toMap(t -> currentMapsetCodes.get(t), t -> currentMapsetVersions.get(t)));

    // mapsets to add (not in current index and should be)
    List<String> mapsetsToAdd =
        allCodes.stream().filter(l -> !currentMapsetCodes.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to add = " + mapsetsToAdd.toString());

    // mapsets to remove (in current index and shouldn't be)
    List<String> mapsetsToRemove =
        currentMapsetCodes.stream().filter(l -> !allCodes.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to remove = " + mapsetsToRemove.toString());

    List<String> terms =
        termUtils.getIndexedTerminologies(esQueryService).stream().map(Terminology::getTerminology).collect(Collectors.toList());

    for (String line : allLines) { // build each mapset
      String[] metadata = line.split(",", -1);
      // remove and skip
      if (mapsetsToRemove.contains(metadata[0])) {
        logger.info("deleting " + metadata[0]);
        operationsService.delete(ElasticOperationsService.MAPPING_INDEX, metadata[0]);
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

        try (final InputStream is = new URL(uri + "/" + metadata[3]).openConnection().getInputStream()) {
          // text
          String welcomeText = IOUtils.toString(is, StandardCharsets.UTF_8);
          map.getProperties().add(new Property("welcomeText", welcomeText));
        }
        map.getProperties().add(new Property("sourceTerminology", metadata[5]));
        map.getProperties().add(new Property("sourceTerminologyVersion", metadata[6]));
        map.getProperties().add(new Property("targetTerminology", metadata[7]));
        map.getProperties().add(new Property("targetTerminologyVersion", metadata[8].replaceAll("\\s", "")));
        map.getProperties().add(new Property("sourceLoaded", Boolean.toString(terms.contains(metadata[5]))));
        map.getProperties().add(new Property("targetLoaded", Boolean.toString(terms.contains(metadata[7]))));

        String mappingDataUri =
            mappingUri + map.getName() + (map.getVersion() != null ? ("_" + map.getVersion()) : "") + ".txt"; // build
        // map
        try (final InputStream is = new URL(mappingDataUri).openConnection().getInputStream()) {
          String mappingData = IOUtils.toString(is, StandardCharsets.UTF_8);
          map.setMaps(buildMaps(mappingData, metadata));
        }

      }

      if (metadata[1] != null && !metadata[1].isEmpty()) { // download links

        map.getProperties().add(new Property("downloadOnly", "true"));
        if (metadata[1].contains("ftp")) {
          map.getProperties().add(new Property("mapsetLink", metadata[1]));
        } else {
          map.getProperties().add(new Property("mapsetLink", null));
          String mappingDataUri =
              mappingUri + map.getName() + (map.getVersion() != null ? ("_" + map.getVersion()) : "") + ".csv"; // build
          // map
          try (final InputStream is = new URL(mappingDataUri).openConnection().getInputStream()) {
            String mappingData = IOUtils.toString(is, StandardCharsets.UTF_8);
            map.setMaps(buildMaps(mappingData, metadata));
          }
        }
      } else {
        map.getProperties().add(new Property("downloadOnly", "false"));
      }
      logger.info("indexing " + metadata[0]);

      Collections.sort(map.getMaps(), new Comparator<gov.nih.nci.evs.api.model.ConceptMap>() {
        @Override
        public int compare(final gov.nih.nci.evs.api.model.ConceptMap o1,
          final gov.nih.nci.evs.api.model.ConceptMap o2) {
          // Assume maps are not null
          return (o1.getSourceName() + o1.getType() + o1.getGroup() + o1.getRank() + o1.getTargetName())
              .compareTo(o2.getSourceName() + o2.getType() + o2.getGroup() + o2.getRank() + o2.getTargetName());
        }
      });
      operationsService.index(map, ElasticOperationsService.MAPPING_INDEX, Concept.class);

    }
  }

  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws IOException, Exception {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config, String filepath, String termName,
    boolean forceDelete) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
