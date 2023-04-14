
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    if (!metadata[3].isEmpty()) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\t");
        Map conceptToAdd = new Map();
        conceptToAdd.setSourceCode(conceptSplit[0]);
        conceptToAdd.setSourceName(conceptSplit[1]);
        conceptToAdd.setSource(conceptSplit[2]);
        conceptToAdd.setType(conceptSplit[6]);
        conceptToAdd.setRank(conceptSplit[7]);
        conceptToAdd.setTargetCode(conceptSplit[8]);
        conceptToAdd.setTargetName(conceptSplit[9]);
        conceptToAdd.setTargetTerminology(conceptSplit[10]);
        conceptToAdd.setTargetTerminologyVersion(conceptSplit[11]);
      }
    }
    // mapsetLink = null + downloadOnly format
    else if (!metadata[1].isEmpty() && !metadata[1].contains("ftp")) {
      for (String conceptMap : Arrays.copyOfRange(mappingDataList, 1, mappingDataList.length)) {
        String[] conceptSplit = conceptMap.split("\t");
        Map conceptToAdd = new Map();
        conceptToAdd.setSourceCode(conceptSplit[0]);
        conceptToAdd.setSourceName(conceptSplit[1]);
        conceptToAdd.setType(conceptSplit[2]);
        conceptToAdd.setTargetCode(conceptSplit[3]);
        conceptToAdd.setTargetName(conceptSplit[4]);
        conceptToAdd.setTargetTermType(conceptSplit[5]);
        conceptToAdd.setTargetTerminology(conceptSplit[6]);
        conceptToAdd.setTargetTerminologyVersion(conceptSplit[7]);
      }
    }
    // ftp format = direct download and no maps
    else {
      return null;
    }

    return maps;
  }

  public Boolean mappingNeedsUpdate(String code, String version, List<String> mapsetsToAdd,
    List<Concept> currentMapsets) {
    if (!mapsetsToAdd.contains(code))
      return false;
    Optional<Concept> currentMapVersion =
        currentMapsets.stream().filter(m -> m.getCode().equals(code)).findFirst();
    // version number while current version number is null
    if (!version.isEmpty() && currentMapVersion.isPresent()
        && currentMapVersion.get().getVersion().isEmpty()) {
      return true;
    }
    // newer version
    if (!version.isEmpty() && currentMapVersion.isPresent()
        && version.compareTo(currentMapVersion.get().getVersion()) > 0)
      return true;
    return false;
  }

  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException, Exception {
    final String uri = applicationProperties.getConfigBaseUri();
    final String mappingUri =
        "https://raw.githubusercontent.com/NCIEVS/evsrestapi-operations/main/bin/data/UnitTestData/mappings/";
    boolean created = operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
    if (created) {
      operationsService.getElasticsearchOperations().putMapping(
          ElasticOperationsService.MAPPING_INDEX, ElasticOperationsService.CONCEPT_TYPE,
          Concept.class);
    }
    logger.info(System.getProperty("user.dir"));
    List<String> allLines =
        Files.readAllLines(Paths.get("src/main/resources/metadata/mapsets/mapsetMetadata.txt"));
    List<String> allCodes =
        allLines.stream().map(l -> l.split("\t")[0]).collect(Collectors.toList());

    List<Concept> currentMapsets = esQueryService.getMapsets(new IncludeParam("minimal"));
    List<String> currentMapsetCodes = esQueryService.getMapsets(new IncludeParam("minimal"))
        .stream().map(Concept::getCode).collect(Collectors.toList());

    List<String> mapsetsToAdd =
        allCodes.stream().filter(l -> !currentMapsets.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to add = " + mapsetsToAdd.toString());

    List<String> mapsetsToRemove = currentMapsetCodes.stream()
        .filter(l -> !mapsetsToAdd.contains(l)).collect(Collectors.toList());
    logger.info("Mapsets to remove = " + mapsetsToRemove.toString());
    System.exit(1);

    for (String line : allLines) { // build each mapset
      String[] metadata = line.split("\t");
      // remove and skip
      if (mapsetsToRemove.contains(metadata[0])) {
        operationsService.delete(ElasticOperationsService.MAPPING_INDEX,
            ElasticOperationsService.CONCEPT_TYPE, metadata[0]);
        continue;
      }
      // skip if no update needed
      if (!mappingNeedsUpdate(metadata[0], metadata[2], mapsetsToAdd, currentMapsets)) {
        continue;
      }
      Concept map = new Concept();
      map.setName(metadata[0]);
      map.setCode(metadata[0]);
      if (!metadata[2].isEmpty()) { // version numbers
        map.setVersion(metadata[2]);
      } else {
        map.setVersion(null);
      }

      // setting up metadata
      if (!metadata[3].isEmpty()) { // welcome text
        map.getProperties().add(new Property("welcomeText", uri + "/" + metadata[3]));
        String mappingData = mappingUri + map.getName() + "_" + map.getVersion() + ".csv"; // build
                                                                                           // map
        map.setMaps(buildMaps(mappingData, metadata));

      } else {
        map.getProperties().add(new Property("welcomeText", null));
      }

      if (!metadata[1].isEmpty()) { // download links

        map.getProperties().add(new Property("downloadOnly", "true"));
        if (metadata[1].contains("ftp")) {
          map.getProperties().add(new Property("mapsetLink", metadata[1]));
        } else {
          map.getProperties().add(new Property("mapsetLink", null));
          String mappingData = mappingUri + map.getName() + "_" + map.getVersion() + ".csv"; // build
                                                                                             // map
          map.setMaps(buildMaps(mappingData, metadata));
        }
      } else {
        map.getProperties().add(new Property("downloadOnly", "false"));
      }
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
