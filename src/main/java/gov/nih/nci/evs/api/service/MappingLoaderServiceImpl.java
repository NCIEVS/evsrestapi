
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

  public List<Map> buildMaps(String mappingData, String[] metadata) {
    List<Map> maps = new ArrayList<Map>();
    String[] mappingDataList = mappingData.split("\n");
    // welcomeText = true format
    if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\",\"");
        Map conceptToAdd = new Map();
        conceptToAdd.setSourceCode(conceptSplit[0].replace("\"", ""));
        conceptToAdd.setSourceName(conceptSplit[1]);
        conceptToAdd.setSource(conceptSplit[2]);
        conceptToAdd.setType(conceptSplit[6]);
        conceptToAdd.setRank(conceptSplit[7]);
        conceptToAdd.setTargetCode(conceptSplit[8]);
        conceptToAdd.setTargetName(conceptSplit[9]);
        conceptToAdd.setTargetTerminology(conceptSplit[10]);
        conceptToAdd.setTargetTerminologyVersion(conceptSplit[11].replace("\"", ""));
        maps.add(conceptToAdd);
      }
    }
    // mapsetLink = null + downloadOnly format
    else if (metadata[1] != null && !metadata[1].isEmpty() && !metadata[1].contains("ftp")) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\",\"");
        Map conceptToAdd = new Map();
        conceptToAdd.setSourceCode(conceptSplit[0].replace("\"", ""));
        conceptToAdd.setSourceName(conceptSplit[1]);
        conceptToAdd.setType(conceptSplit[2]);
        conceptToAdd.setTargetCode(conceptSplit[3]);
        conceptToAdd.setTargetName(conceptSplit[4]);
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

  public Boolean mappingNeedsUpdate(String code, String version, List<String> mapsetsToAdd,
    List<String> currentMapsetCodes) {
    if (!mapsetsToAdd.contains(code)) {
      return false;
    }

    // adding for first time
    if (currentMapsetCodes.isEmpty() || !currentMapsetCodes.contains(code)) {
      return true;
    }

    Optional<String> currentMapVersion =
        currentMapsetCodes.stream().filter(m -> m.equals(code)).findFirst();
    // version number while current version number is null
    if (!version.isEmpty() && currentMapVersion.isPresent() && currentMapVersion.get().isEmpty()) {
      return true;
    }
    // newer version
    if (!version.isEmpty() && currentMapVersion.isPresent()
        && version.compareTo(currentMapVersion.get()) > 0)
      return true;
    return false;
  }

  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    final String uri = applicationProperties.getConfigBaseUri();
    final String mappingUri =
        "https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/main/data/mappings/";
    final String mapsetMetadataUri = uri + "/mapsetMetadata.txt";
    List<String> allLines = new ArrayList<>(
        Arrays.asList(IOUtils.toString(new URL(mapsetMetadataUri).openConnection().getInputStream(),
            StandardCharsets.UTF_8).split("\n")));
    // skip header line
    allLines = allLines.subList(1, allLines.size());
    boolean created = operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
    if (created) {
      operationsService.getElasticsearchOperations().putMapping(
          ElasticOperationsService.MAPPING_INDEX, ElasticOperationsService.CONCEPT_TYPE,
          Concept.class);
    }
    List<String> allCodes =
        allLines.stream().map(l -> l.split(",")[0]).collect(Collectors.toList());

    List<String> currentMapsetCodes = esQueryService.getMapsets(new IncludeParam("minimal"))
        .stream().map(Concept::getCode).collect(Collectors.toList());

    List<String> mapsetsToAdd =
        allCodes.stream().filter(l -> !currentMapsetCodes.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to add = " + mapsetsToAdd.toString());

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
      if (!mappingNeedsUpdate(metadata[0], metadata[2], mapsetsToAdd, currentMapsetCodes)) {
        continue;
      }
      Concept map = new Concept();
      map.setName(metadata[0]);
      map.setCode(metadata[0]);
      if (metadata[2] != null && !metadata[2].isEmpty()) { // version numbers
        map.setVersion(metadata[2]);
      } else {
        map.setVersion(null);
      }

      // setting up metadata
      if (metadata[3] != null && !metadata[3].isEmpty() && metadata[3].length() > 1) { // welcome
                                                                                       // text
        String welcomeText =
            IOUtils.toString(new URL(uri + "/" + metadata[3]).openConnection().getInputStream(),
                StandardCharsets.UTF_8);
        map.getProperties().add(new Property("welcomeText", welcomeText));

        String mappingDataUri = mappingUri + map.getName()
            + (map.getVersion() != null ? ("_" + map.getVersion()) : "") + ".csv"; // build
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
