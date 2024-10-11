package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mappings;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.StatisticsEntry;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PushBackReader;
import gov.nih.nci.evs.api.util.RrfReaders;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

/** The implementation for {@link MetaElasticLoadServiceImpl}. */
@Service
public class MetaElasticLoadServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(MetaElasticLoadServiceImpl.class);

  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;

  /** the lock file name *. */
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;

  /** download batch size *. */
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;

  /** index batch size *. */
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;

  /** file path *. */
  private File filepath;

  /** The name map. */
  private Map<String, String> nameMap = new HashMap<>();

  /** The scui cui map. */
  private Map<String, String> codeCuiMap = new HashMap<>();

  /** The rui qual map. */
  private Map<String, Set<String>> ruiQualMap = new HashMap<>();

  /** The rui qual sabs. */
  private Set<String> ruiQualSabs = new HashSet<>();

  /** The maps. */
  private Map<String, Set<Mappings>> maps = new HashMap<>();

  /** The rui inverse map. */
  private Map<String, String> ruiInverseMap = new HashMap<>();

  /** The rel inverse map. */
  private Map<String, String> relInverseMap = new HashMap<>();

  /** The rela inverse map. */
  private Map<String, String> relaInverseMap = new HashMap<>();

  /** The tty map. */
  private Map<String, String> ttyMap = new HashMap<>();

  /** The rel map. */
  private Map<String, String> relMap = new HashMap<>();

  /** The atn map. */
  private Map<String, String> atnMap = new HashMap<>();

  /** The source map. */
  private Map<String, String> sourceMap = new HashMap<>();

  /** The column map. */
  private Map<String, String> colMap = new HashMap<>();

  /** The atn set. */
  private Set<String> atnSet = new HashSet<>();

  /** The rel set. */
  private Set<String> relSet = new HashSet<>();

  /** The qual set. */
  private Map<String, Set<String>> qualMap = new HashMap<>();

  /** The history map. */
  private Map<String, Set<History>> replHistoryMap = new HashMap<>();

  private int batchSize = 0;

  /** the environment *. */
  @Autowired Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /** The elastic query service. */
  @Autowired ElasticQueryService elasticQueryService;

  /** The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the filepath.
   *
   * @return filepath
   */
  public File getFilepath() {
    return filepath;
  }

  /**
   * Sets the filepath.
   *
   * @param filepath the filepath
   */
  public void setFilepath(File filepath) {
    this.filepath = filepath;
  }

  /**
   * Cache preferred names and REL/RELA inverses.
   *
   * @throws Exception the exception
   */
  private void cacheMaps() throws Exception {

    logger.info("  START cache maps");
    int mapsetCt = 0;
    int mapCt = 0;
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader mrconso = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader mrrel = readers.getReader(RrfReaders.Keys.MRREL);
        final PushBackReader mrsat = readers.getReader(RrfReaders.Keys.MRSAT);
        final PushBackReader mrmap = readers.getReader(RrfReaders.Keys.MRMAP);
        final PushBackReader mrdoc = readers.getReader(RrfReaders.Keys.MRDOC);
        final PushBackReader mrcols = readers.getReader(RrfReaders.Keys.MRCOLS);
        final PushBackReader mrcui = readers.getReader(RrfReaders.Keys.MRCUI); ) {

      String line = null;
      final Map<String, String> codeNameMap = new HashMap<>();
      final Map<String, Mappings> mapsetInfoMap = new HashMap<>();

      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        // CUI,LAT,TS,LUI,STT,SUI,ISPREF,AUI,SAUI,SCUI,SDUI,SAB,TTY,CODE,STR,SRL,SUPPRESS,CVF

        // Cache concept preferred names
        if (fields[2].equalsIgnoreCase("P")
            && fields[4].equalsIgnoreCase("PF")
            && fields[6].equalsIgnoreCase("Y")) {
          // Save the name map to dereference it while processing relationships
          nameMap.put(fields[0], fields[14]);
        }

        // Cache ICD10CM PT names for mapping data
        // ICD10CM/ICD10 are always target fields as of now
        if ((fields[11].equals("ICD10CM") || fields[11].equals("ICD10") || fields[11].equals("PDQ"))
            && fields[12].equals("PT")) {
          // NEED for SAB in key
          codeNameMap.put(fields[11] + fields[13], fields[14]);
        } else if ((fields[11].equals("PDQ") || fields[11].equals("NCI"))
            && fields[12].equals("PT")) {
          // no need for SAB in key
          codeNameMap.put(fields[13], fields[14]);
        }

        // Cache SNOMEDCT_US/PT SCUI -> CUI for maps
        // SNOMEDCT_US is always source field as of now
        else if (fields[11].equals("SNOMEDCT_US")
            && fields[12].equals("PT")
            && fields[16].equals("N")) {
          codeNameMap.put(fields[13], fields[14]);
          codeCuiMap.put(fields[13], fields[0]);
        }

        // Cache mapsets
        if (fields[11].equals("SNOMEDCT_US")
            && fields[12].equals("XM")
            && fields[14].contains("ICD10")) {
          // |SNOMEDCT_US_2020_09_01 to ICD10CM_2021 Mappings
          // |SNOMEDCT_US_2022_03_01 to ICD10_2016 Mappings
          final Mappings info = new Mappings();
          mapsetInfoMap.put(fields[0], info);
          info.setSource("snomedct_us"); // evsrestapi terminology
          info.setSourceTerminology("SNOMEDCT_US"); // uiLabel
          info.setSourceTerminologyVersion(
              fields[14].replaceFirst("(.*) to .*", "$1").replaceFirst("SNOMEDCT_US_", ""));
          info.setTarget(fields[14].replaceFirst(".* to ([^ ]+)_.*", "$1").toLowerCase());
          info.setTargetName(fields[14].replaceFirst(".* to ([^ ]+)_.*", "$1"));
          info.setTargetTerminology(fields[14].replaceFirst(".* to ([^ ]+)_.*", "$1"));
          info.setTargetTerminologyVersion(fields[14].replaceFirst(".* to [^ ]+_([^ ]+).*", "$1"));
          info.setMapsetCode(fields[14].replaceAll(" ", "_"));
          codeNameMap.put(fields[0], fields[14]);

        } else if (fields[11].equals("PDQ")
            && fields[12].equals("XM")
            && fields[14].contains("NCI")) {
          // PDQ_2016_07_31 to NCI_2024_06D Mappings
          final Mappings info = new Mappings();
          mapsetInfoMap.put(fields[0], info);
          info.setSource("pdq");
          info.setSourceTerminology("PDQ");
          info.setSourceTerminologyVersion(
              fields[14].replaceFirst("(.*) to .*", "$1").replaceFirst("PDQ_", ""));
          info.setTarget("nci");
          info.setTargetName("NCI");
          info.setTargetTerminology("NCI Thesaurus");
          // Compute EVSRESTAPI-style version for NCIt.
          info.setTargetTerminologyVersion(
              fields[14]
                  .replaceFirst(".* to NCI_20([^ ]+).*", "$1")
                  .replaceFirst("_", ".")
                  .toLowerCase());
          info.setMapsetCode(fields[14].replaceAll(" ", "_"));
          codeNameMap.put(fields[0], fields[14]);
        }
      }

      // Loop through map lines and cache map objects
      final Map<String, Concept> mapsetMap = new HashMap<>();
      while ((line = mrmap.readLine()) != null) {
        // MAPSETCUI,MAPSETSAB,MAPSUBSETID,MAPRANK,MAPID,MAPSID,FROMID,FROMSID,
        // FROMEXPR,FROMTYPE,FROMRULE,FROMRES,REL,RELA,TOID,TOSID,TOEXPR,TOTYPE,
        // TORULE,TORES,MAPRULE,MAPRES,MAPTYPE,MAPATN,MAPATV,CVF
        final String[] fields = line.split("\\|", -1);

        // Skip empty maps (or maps to the empty target: 100051
        if (fields[16].isEmpty() || fields[16].equals("100051")) {
          continue;
        }

        // Skip entries with descendant rules
        // OK: IFA 445518008 &#x7C; Age at onset of clinical finding (observable entity) &#x7C;
        // OK: IFA 248152002 &#x7C; Female (finding) &#x7C;
        // OK: IFA 248153007 &#x7C; Male (finding) &#x7C;

        // Change this to keep all rules
        //        if (fields[20].startsWith("IFA")
        //            && !fields[20].startsWith("IFA 445518008")
        //            && !fields[20].startsWith("IFA 248152002")
        //            && !fields[20].startsWith("IFA 248153007")) {
        //          continue;
        //        }

        final Mappings info = mapsetInfoMap.get(fields[0]);
        final Mappings map = new Mappings();
        map.setSource(info.getSource());
        map.setSourceCode(fields[8]);
        map.setSourceTerminology(info.getSourceTerminology());
        map.setSourceName(
            codeNameMap.containsKey(fields[6])
                ? codeNameMap.get(fields[6])
                : codeNameMap.get(fields[1] + fields[6]));
        map.setSourceTerminologyVersion(info.getSourceTerminologyVersion());
        map.setTargetCode(fields[16].isEmpty() || fields[16].equals("100051") ? "" : fields[16]);
        map.setTargetTermType("PT");
        if (!mapsetInfoMap.containsKey(fields[0])) {
          throw new Exception("Encountered unsupported MRMAP CUI = " + fields[0]);
        }
        map.setTarget(info.getTarget());
        map.setTargetTerminology(info.getTargetTerminology());
        map.setTargetTerminologyVersion(info.getTargetTerminologyVersion());
        map.setTargetName(
            fields[16].isEmpty() || fields[16].equals("100051")
                ? ""
                : (codeNameMap.containsKey(fields[16])
                    ? codeNameMap.get(fields[16])
                    : codeNameMap.get(info.getTargetName() + fields[16])));
        map.setType(fields[12]);
        map.setGroup(fields[2]);
        map.setRank(fields[3]);
        map.setRule(fields[20]);
        map.setMapsetCode(info.getMapsetCode());
        mapCt++;

        // Fix source name if null
        if (map.getSourceName() == null) {
          map.setSourceName("Unable to determine name, code not present");
        }

        // Fix target name if null
        if (map.getTargetName() == null) {
          map.setTargetName("Unable to determine name, code not present");
        }

        final String cui = codeCuiMap.get(fields[8]);
        // Some MRMAP entries in sample meta will not resolve to a CUI given the subset.
        if (cui != null) {
          if (!maps.containsKey(cui)) {
            maps.put(cui, new HashSet<>());
          }
          maps.get(cui).add(map);
        }

        // Handle mapsets
        if (!mapsetMap.containsKey(fields[0])) {
          List<String> terms =
              termUtils.getIndexedTerminologies(elasticQueryService).stream()
                  .map(Terminology::getTerminology)
                  .collect(Collectors.toList());
          final Concept mapset = new Concept();
          // populate the mapset details.
          mapset.setCode(info.getMapsetCode());
          mapset.setName(codeNameMap.get(fields[0]));
          mapset.setTerminology(info.getSourceTerminology().toLowerCase());
          mapset.setVersion(info.getSourceTerminologyVersion());
          // set other fields and properties as needed (to match other mapsets and needs of ui)
          mapset.getProperties().add(new Property("loader", "MetaElasticLoadServiceImpl"));
          final String mapsetUri =
              applicationProperties.getConfigBaseUri()
                  + "/mapping-"
                  + info.getSource().replaceFirst("snomedct_us", "snomed")
                  + "-"
                  + info.getTarget().replaceFirst("ncit", "nci")
                  + ".html";
          try (final InputStream is = new URL(mapsetUri).openConnection().getInputStream()) {
            final String welcomeText = IOUtils.toString(is, StandardCharsets.UTF_8);
            mapset.getProperties().add(new Property("welcomeText", welcomeText));
          } catch (Throwable t) { // read as file if no url
            try {
              if (!new File(mapsetUri).exists()) {
                throw new Exception("Unable to find welcome text for mapset = " + mapsetUri);
              }
              final String welcomeText =
                  FileUtils.readFileToString(new File(mapsetUri), StandardCharsets.UTF_8);
              mapset.getProperties().add(new Property("welcomeText", welcomeText));
            } catch (IOException ex) {
              throw new IOException(
                  "Could not find either file or uri for welcome text: " + mapsetUri); // only
              // throw
              // exception
              // if
              // both
              // fail
            }
          }
          mapset.getProperties().add(new Property("mapsetLink", null));
          mapset.getProperties().add(new Property("downloadOnly", "false"));
          mapset
              .getProperties()
              .add(
                  new Property(
                      "sourceLoaded",
                      Boolean.toString(terms.contains(info.getSourceTerminology().toLowerCase()))));
          mapset
              .getProperties()
              .add(
                  new Property(
                      "targetLoaded",
                      Boolean.toString(terms.contains(info.getTargetTerminology().toLowerCase()))));
          mapset
              .getProperties()
              .add(
                  new Property(
                      "sourceTerminology",
                      info.getSourceTerminology() != null ? info.getSource() : "not found"));
          mapset
              .getProperties()
              .add(
                  new Property(
                      "targetTerminology",
                      info.getTargetTerminology() != null ? info.getTarget() : "not found"));

          mapset
              .getProperties()
              .add(
                  new Property(
                      "sourceTerminologyVersion",
                      map.getSourceTerminologyVersion() != null
                          ? map.getSourceTerminologyVersion()
                          : "not found"));
          mapset
              .getProperties()
              .add(
                  new Property(
                      "targetTerminologyVersion",
                      map.getTargetTerminologyVersion() != null
                          ? map.getTargetTerminologyVersion()
                          : "not found"));
          mapsetMap.put(fields[0], mapset);
          mapsetCt++;
        }
        mapsetMap.get(fields[0]).getMaps().add(map);
      }

      // current snomed mapset codes
      List<String> currentMapsetCodes =
          elasticQueryService.getMapsets(new IncludeParam("properties")).stream()
              .filter(
                  concept ->
                      concept.getProperties().stream()
                          .anyMatch(
                              property ->
                                  property.getType().equals("loader")
                                      && property
                                          .getValue()
                                          .contains("MetaElasticLoadServiceImpl")))
              .map(Concept::getCode)
              .collect(Collectors.toList());
      List<String> currentMapsetVersions =
          elasticQueryService.getMapsets(new IncludeParam("properties")).stream()
              .filter(
                  concept ->
                      concept.getProperties().stream()
                          .anyMatch(
                              property ->
                                  property.getType().equals("loader")
                                      && property
                                          .getValue()
                                          .contains("MetaElasticLoadServiceImpl")))
              .map(Concept::getVersion)
              .collect(Collectors.toList());
      logger.info("currentMapsetCodes = " + currentMapsetCodes.toString());
      logger.info("currentMapsetVersions = " + currentMapsetVersions.toString());
      // all found snomed codes
      List<String> allCodes =
          mapsetMap.values().parallelStream().map(Concept::getCode).collect(Collectors.toList());
      List<String> allVersions =
          mapsetMap.values().parallelStream().map(Concept::getVersion).collect(Collectors.toList());
      logger.info("allCodes = " + allCodes.toString());
      logger.info("allVersions = " + allVersions.toString());
      // mapsets to remove (in current index and shouldn't be)
      List<String> mapsetsToRemove =
          currentMapsetCodes.stream()
              .filter(l -> !allCodes.contains(l))
              .collect(Collectors.toList());
      logger.info("mapsetsToRemove = " + mapsetsToRemove.toString());
      List<String> mapsetsToAdd =
          allCodes.stream()
              .filter(l -> !currentMapsetCodes.contains(l))
              .collect(Collectors.toList());
      logger.info("mapsetsToAdd = " + mapsetsToAdd);

      // remove old mappings by code
      for (String mapsetCode : mapsetsToRemove) {
        operationsService.delete(ElasticOperationsService.MAPSET_INDEX, mapsetCode);
        operationsService.deleteQuery(
            "mapsetCode:" + mapsetCode, ElasticOperationsService.MAPPINGS_INDEX);
      }
      for (final Concept mapset : mapsetMap.values()) {
        Collections.sort(
            mapset.getMaps(),
            new Comparator<Mappings>() {
              @Override
              public int compare(final Mappings o1, final Mappings o2) {
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
        logger.info("    Index map = " + mapset.getName());
        int i = 1;
        for (final Mappings mapToSort : mapset.getMaps()) {
          mapToSort.setSortKey(String.valueOf(1000000 + i++));
        }
        // Send 10k at at tim
        for (final List<Mappings> batch : ListUtils.partition(mapset.getMaps(), 10000)) {
          operationsService.bulkIndex(
              batch, ElasticOperationsService.MAPPINGS_INDEX, Mappings.class);
        }
        mapset.setMaps(null);
        operationsService.index(mapset, ElasticOperationsService.MAPSET_INDEX, Concept.class);
      }
      // free up memory
      mapsetMap.clear();
      mapsetInfoMap.clear();

      // read inverses from MRDOC
      while ((line = mrdoc.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        if (fields[2].equals("rel_inverse")) {
          relInverseMap.put(fields[1], fields[3]);
        } else if (fields[2].equals("rela_inverse")) {
          relaInverseMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("TTY") && fields[2].equals("expanded_form")) {
          ttyMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("REL") && fields[2].equals("expanded_form")) {
          relMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("ATN") && fields[2].equals("expanded_form")) {
          atnMap.put(fields[1], fields[3]);
        }
      }
      relaInverseMap.put("", "");

      // Column Metadata
      while ((line = mrcols.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // ATN|Attribute name||2|10.94|62|MRSAT.RRF|varchar(100)|
        colMap.put(fields[0], fields[1]);
      }

      // Handle RUI attributes
      while ((line = mrsat.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // e.g.
        // C4227882|||R114264673|RUI||AT148954088||SMQ_TERM_LEVEL|MDR|5|N||
        // Skip high volume qualifiers for relationships
        if (fields[4].equals("RUI")
            && !fields[8].equals("MODIFIER_ID")
            && !fields[8].equals("CHARACTERISTIC_TYPE_ID")) {
          final String atn = fields[8];
          final String atv = fields[10];
          if (!qualMap.containsKey(atn)) {
            qualMap.put(atn, new HashSet<>());
          }
          qualMap.get(atn).add(atv);

          if (!ruiQualMap.containsKey(fields[3])) {
            ruiQualMap.put(fields[3], new HashSet<>(4));
          }
          ruiQualMap.get(fields[3]).add(atn + "|" + atv);
          ruiQualSabs.add(fields[9]);
        }
      }

      logger.info("    ruiQualMap = " + ruiQualMap.size());
      final Map<String, String> helper = new HashMap<>();
      // Prepare parent/child relationships for getHierarchyUtils
      while ((line = mrrel.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        // Only keep inverse RUI map for sources that have qualifiers
        if (!ruiQualSabs.contains(fields[10])) {
          continue;
        }

        // for non parent/children, build inverse rui map.
        if (!fields[3].equals("PAR") && !fields[3].equals("CHD")) {
          // C4229995|A5970983|AUI|PAR|C4229995|A5963886|AUI||R91875256||MDR|MDR|||N||
          final String key = fields[1] + fields[5] + fields[3] + fields[7];
          helper.put(key, fields[8]);
          final String key2 =
              fields[5] + fields[1] + relInverseMap.get(fields[3]) + relaInverseMap.get(fields[7]);
          if (helper.containsKey(key2)) {
            ruiInverseMap.put(fields[8], helper.get(key2));
            ruiInverseMap.put(helper.get(key2), fields[8]);
            helper.remove(key);
            helper.remove(key2);
          }
        }
      }

      // Remove any entries ruiInverseMap where the inverse RUIs do not have RUI
      // qualifiers
      for (final String key : new HashSet<>(ruiInverseMap.keySet())) {
        final String value = ruiInverseMap.get(key);
        if (!ruiQualMap.containsKey(value)) {
          ruiInverseMap.remove(key);
        }
      }

      // Prepare history entries for active "replacement" concepts
      // CODE, DATE, REL (ACTION), RELA, MAPREASON, REPLACEMENT CODE, MAPINSUB
      // Action values: Broader (RB), Narrower (RN), Other Related (RO), Deleted (DEL), Removed from
      // Subset (SUBX)
      // Loop through lines until we reach "the end"
      while ((line = mrcui.readLine()) != null) {

        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        final String date = fields[1];
        final String action = fields[2];
        final String replacementCui = fields[5];
        final String inSubset = fields[6];

        // Skip things where CUI2 is not in the subset or is blank
        if (replacementCui.isEmpty() || inSubset.equals("N")) {
          continue;
        }

        // Make a history entry from this line
        final History history = new History();
        history.setCode(cui);
        // We don't know the names of retired concepts
        history.setName("Retired concept");
        history.setAction(action);
        history.setDate(date);
        history.setReplacementCode(replacementCui);
        history.setReplacementName(nameMap.get(replacementCui));

        if (!replHistoryMap.containsKey(replacementCui)) {
          replHistoryMap.put(replacementCui, new HashSet<>(5));
        }
        replHistoryMap.get(replacementCui).add(history);
      }

    } finally {
      readers.closeReaders();
    }
    logger.info("  FINISH cache maps");
    logger.info("    mapsets = " + mapsetCt);
    logger.info("    maps = " + mapCt);
    logger.info("    ruiInverseMap = " + ruiInverseMap.size());
    logger.info("    ruiQualMap = " + ruiQualMap.size());
    logger.info("    replHistoryMap = " + replHistoryMap.size());
  }

  public Boolean mappingNeedsUpdate(
      String code, String version, Map<String, String> currentMapsets) {

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
    if (!version.isEmpty()
        && currentMapVersion.isPresent()
        && !version.equals(currentMapVersion.get())) return true;
    return false;
  }

  /* see superclass */
  @Override
  public int loadConcepts(
      ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws Exception {
    logger.info("Loading Concepts (index batch size = " + INDEX_BATCH_SIZE + ")");

    // Put the mapping
    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService
          .getElasticsearchOperations()
          .indexOps(IndexCoordinates.of(terminology.getIndexName()))
          .putMapping(Concept.class);
    }

    // Cache the concept preferred names so when we resolve relationships we
    // know the "related" name
    cacheMaps();

    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader mrconso = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader mrdef = readers.getReader(RrfReaders.Keys.MRDEF);
        final PushBackReader mrsty = readers.getReader(RrfReaders.Keys.MRSTY);
        final PushBackReader mrsat = readers.getReader(RrfReaders.Keys.MRSAT);
        final PushBackReader mrrel = readers.getReader(RrfReaders.Keys.MRREL); ) {

      // Set up vars
      String line = null;
      String prevCui = null;
      Concept concept = new Concept();
      List<Concept> batch = new ArrayList<>();
      int totalConcepts = 0;

      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        // Test assumption that the file is in order (when considering
        // |)
        if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
          throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
        }

        // check if we've hit a new concept then process the other files for the
        // "prevCui"
        if (prevCui != null && !cui.equals(prevCui)) {

          // Process data
          handleDefinitions(terminology, concept, mrdef, prevCui);
          handleSemanticTypes(concept, mrsty, prevCui);
          handleAttributes(concept, mrsat, prevCui);
          handleRelationships(concept, mrrel, prevCui);
          handleMapsAndHistory(concept, prevCui);

          // There are not useful mappings in the UMLS at this point in time
          // handleMapping(concept, mrmap, prevCui);
          handleConcept(concept, batch, false, terminology.getIndexName());

          if (totalConcepts++ % 5000 == 0) {
            logger.info("    count = " + totalConcepts);
          }

          // Prep concept for the next CUI
          concept = new Concept();
        }

        // If we haven't set up the concept yet, do so now
        if (concept.getCode() == null) {
          concept.setCode(cui);
          concept.setName(nameMap.get(cui));
          concept.setNormName(ConceptUtils.normalize(nameMap.get(cui)));
          concept.setStemName(ConceptUtils.normalizeWithStemming(nameMap.get(cui)));
          concept.setTerminology(terminology.getTerminology());
          concept.setVersion(terminology.getVersion());
          // NO hierarchies for NCIM concepts, so set leaf to null
          concept.setLeaf(null);
          concept.setActive(true);
        }

        // Each line of MRCONSO is a synonym
        final Synonym sy = new Synonym();

        // Put the AUI into the URI field for the time being (to be removed
        // later)
        sy.setUri(fields[7]);
        sy.setType(fields[14].equals(concept.getName()) ? "Preferred_Name" : "Synonym");
        if (!fields[13].equals("NOCODE")) {
          sy.setCode(fields[13]);
        }
        sy.setSource(fields[11]);
        terminology.getMetadata().getSynonymSourceSet().add(fields[11]);
        sy.setTermType(fields[12]);
        terminology.getMetadata().getTermTypes().put(fields[12], ttyMap.get(fields[12]));
        sy.setName(fields[14]);
        sy.setNormName(ConceptUtils.normalize(fields[14]));
        concept.getSynonyms().add(sy);

        // Save prev cui for next round
        prevCui = cui;
      }

      // Process the final concept and all it's connected data
      handleDefinitions(terminology, concept, mrdef, prevCui);
      handleSemanticTypes(concept, mrsty, prevCui);
      handleAttributes(concept, mrsat, prevCui);
      handleRelationships(concept, mrrel, prevCui);
      handleMapsAndHistory(concept, prevCui);

      handleConcept(concept, batch, true, terminology.getIndexName());
      totalConcepts++;

      // Process any retired concepts for history
      totalConcepts += loadHistory(terminology);

      return totalConcepts;

    } finally {
      readers.closeReaders();
    }
  }

  /**
   * Handle semantic types.
   *
   * @param concept the concept
   * @param mrsty the mrsty
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleSemanticTypes(
      final Concept concept, final PushBackReader mrsty, final String prevCui) throws Exception {
    String line;
    while ((line = mrsty.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsty.push(line);
        break;
      }

      // hard code to Semantic_Type for now
      buildProperty(concept, "Semantic_Type", fields[3], null);
    }
  }

  /**
   * Handle attributes.
   *
   * @param concept the concept
   * @param mrsat the mrsat
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleAttributes(
      final Concept concept, final PushBackReader mrsat, final String prevCui) throws Exception {

    // Track seen to avoid duplicates
    final Set<String> seen = new HashSet<>();

    String line;
    while ((line = mrsat.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsat.push(line);
        break;
      }

      // CUI,LUI,SUI,METAUI,STYPE,CODE,ATUI,SATUI,ATN,SAB,ATV,SUPPRESS,CVF

      // Skip SUBSET_MEMBER
      if (fields[8].equals("SUBSET_MEMBER")) {
        continue;
      }

      // ALLOW AUI attributes
      // if (fields[4].equals("AUI")) {
      //
      // final boolean rxnormKeep =
      // fields[9].equals("RXNORM") && !fields[8].equals("RXAUI") &&
      // !fields[8].equals("RXAUI");
      // final boolean mthsplKeep = fields[9].equals("MTHSPL");
      //
      // if (!rxnormKeep && !mthsplKeep) {
      // continue;
      // }
      // }

      // ALLOW certain high-volume SNOMED attributes
      // if (fields[9].equals("SNOMEDCT_US") &&
      // (fields[8].equals("EFFECTIVE_TIME")
      // || fields[8].equals("ACTIVE") || fields[8].equals("CTV3ID")
      // || fields[8].equals("MODIFIER_ID") ||
      // fields[8].equals("CHARACTERISTIC_TYPE_ID"))) {
      // continue;
      // }

      // consider skipping MDR - SMQ*, PT_IN_VERSION

      // CUI,LUI,SUI,METAUI,STYPE,CODE,ATUI,SATUI,ATN,SAB,ATV,SUPPRESS,CVF|
      final String atn = fields[8];
      final String sab = fields[9];
      final String atv = fields[10];

      // RUI attributes handled in cacheMaps

      // Handle AUI attributes as qualifiers on synonyms
      if (fields[4].equals("AUI")) {
        // Add entry to qualifier map for metadata
        if (!qualMap.containsKey(atn)) {
          qualMap.put(atn, new HashSet<>());
        }
        qualMap.get(atn).add(atv);

        // find synonym
        final Synonym syn =
            concept.getSynonyms().stream()
                .filter(s -> s.getUri().equals(fields[3]))
                .findFirst()
                .orElse(null);
        if (syn == null) {
          throw new Exception("Synonym for attribute cannot be resolved = " + line);
        }
        syn.getQualifiers().add(new Qualifier(atn, ConceptUtils.substr(atv, 1000)));
      }

      // Otherwise handle as a concept attribute
      else if (!fields[4].equals("RUI")) {

        // De-duplicate concept attributes
        final String key = sab + atn + atv;
        if (seen.contains(key)) {
          continue;
        }
        seen.add(key);

        buildProperty(concept, atn, atv, sab);
      }
    }
  }

  /**
   * Handle building concept property.
   *
   * @param concept the concept
   * @param type the property type
   * @param value the property value
   * @param source the source
   * @return the property
   */
  private void buildProperty(
      final Concept concept, final String type, final String value, final String source) {
    final Property prop = new Property();
    prop.setValue(value);
    prop.setType(type);
    atnSet.add(type);
    if (source != null) {
      prop.setSource(source.toString());
    }
    concept.getProperties().add(prop);
  }

  /**
   * Handle definitions.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @param mrdef the mrdef
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  private void handleDefinitions(
      final Terminology terminology,
      final Concept concept,
      final PushBackReader mrdef,
      final String prevCui)
      throws Exception {
    String line;
    while ((line = mrdef.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrdef.push(line);
        break;
      }
      // handle definitions
      buildDefinition(terminology, concept, fields);
    }
  }

  /**
   * Handle statistics.
   *
   * @param terminology the terminology
   * @param sourceList the source list
   * @throws Exception the exception
   */
  private void handleStatistics(final Terminology terminology, final Set<String> sourceList)
      throws Exception {
    if (terminology.getTerminology().equals("ncim")) {
      for (String source : sourceList) {
        // create elastic object
        ElasticObject newStatsEntry = new ElasticObject("ncim-stats-" + source);

        // get source overlap stats
        String filePath = this.getFilepath() + "/stats/" + source + "/" + source + ".txt";
        // read the source overlap file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
          Map<String, List<StatisticsEntry>> sourceStatsEntry = new HashMap<>();
          List<StatisticsEntry> statsList = new ArrayList<StatisticsEntry>();

          String line;
          while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\|");
            StatisticsEntry statisticsEntry = new StatisticsEntry(parts[0], parts[1], parts[2]);
            statsList.add(statisticsEntry);
            System.out.println(line);
          }
          sourceStatsEntry.put("Source Overlap", statsList);
          newStatsEntry.setStatisticsMap(sourceStatsEntry);
          operationsService.index(
              newStatsEntry, terminology.getObjectIndexName(), ElasticObject.class);
        } catch (IOException e) {
          // Handle the file not found exception and log a warning
          logger.warn(source + " source overlap stats file not found for ncim");
        }

        // any future stats additions go here
      }
    }
  }

  /**
   * Handle building concept definition.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @param fields the fields
   * @return the definition
   */
  private void buildDefinition(
      final Terminology terminology, final Concept concept, final String[] fields) {
    final String definition = fields[5];
    final String source = fields[4];

    final Definition def = new Definition();
    def.setDefinition(definition);
    def.setSource(source);
    def.setType("DEFINITION");
    terminology.getMetadata().getDefinitionSourceSet().add(source);
    concept.getDefinitions().add(def);
  }

  /**
   * Handle relationships.
   *
   * @param concept the concept
   * @param mrrel the mrrel
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleRelationships(
      final Concept concept, final PushBackReader mrrel, final String prevCui) throws Exception {
    Set<String> seen = new HashSet<>();
    String line;
    while ((line = mrrel.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrrel.push(line);
        break;
      }

      // e.g.
      // C0000039|A13650014|AUI|RO|C0364349|A10774117|AUI|measures|R108296692||LNC|LNC|||N||
      final String rel = fields[3];
      final String rela = fields[7];
      final String fromCode = fields[4];
      final String toCode = fields[0];

      // Skip certain situations
      if (fromCode.equals(toCode)
          || rel.equals("SY")
          || rel.equals("AQ")
          || rel.equals("QB")
          || rel.equals("BRO")
          || rel.equals("BRN")
          || rel.equals("BRB")
          || rel.equals("XR")) {
        continue;
      }

      // Skip combinations already seen
      final String sab = fields[10];
      final String key = fromCode + "," + toCode + "," + rel + rela + sab;

      if (seen.contains(key)) {
        continue;
      }
      seen.add(key);

      // ALLOW AUI-AUI relationships
      // else if (fields[2].equals("AUI") && fields[6].equals("AUI")) {
      // if (!fields[10].equals("RXNORM")) {
      // continue;
      // }
      // }

      // ALLOW - SPECIAL EXCEPTION FOR SIZE
      // if (fromCode.equals("C0205447") && fields[10].startsWith("SNOMED")) {
      // continue;
      // }

      // CUI1 has parent CUI2
      if (rel.equals("PAR")) {
        buildParent(concept, fields);
      }

      // CUI1 has child CUI2
      else if (rel.equals("CHD")) {
        buildChild(concept, fields);
      }

      // Other "association" and "inverse association"
      else {

        // TEMP: skip SNOMED relationships
        // if (fields[10].equals("SNOMEDCT_US")) {
        // continue;
        // }
        buildAssociations(concept, fields);
      }
    }
  }

  /**
   * Handle maps and history.
   *
   * @param concept the concept
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleMapsAndHistory(final Concept concept, final String prevCui) throws Exception {

    // Add maps
    if (maps.containsKey(prevCui)) {
      concept.getMaps().addAll(maps.get(prevCui));
      // Clear memory as we do not need this anymore
      maps.remove(prevCui);
    }

    // Add history
    if (replHistoryMap.containsKey(prevCui)) {
      concept.getHistory().addAll(replHistoryMap.get(prevCui));
      replHistoryMap.remove(prevCui);
    }
  }

  /**
   * Handle building concept property.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the property
   * @throws Exception the exception
   */
  private void buildParent(final Concept concept, final String[] fields) throws Exception {
    final Concept parent = buildParentChildHelper(concept, fields);
    concept.getParents().add(parent);
  }

  /**
   * Builds the child.
   *
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  private void buildChild(final Concept concept, final String[] fields) throws Exception {
    final Concept child = buildParentChildHelper(concept, fields);
    concept.getChildren().add(child);
  }

  /**
   * Builds the parent child helper.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the concept
   * @throws Exception the exception
   */
  private Concept buildParentChildHelper(final Concept concept, final String[] fields)
      throws Exception {

    // C2584594|A9570455|SCUI|PAR|C0203464|A3803453|SCUI|inverse_isa|R105418833|4727926024|SNOMEDCT_US|SNOMEDCT_US||N|N||
    // final String cui1 = fields[0];
    // final String aui1 = fields[1];
    // final String stype1 = fields[2];
    // final String rel = fields[3];
    final String cui2 = fields[4];
    // final String aui2 = fields[5];
    // final String stype2 = fields[6];
    final String rela = fields[7];
    // final String rui = fields[8];
    // final String srui = fields[9];
    final String sab = fields[10];
    // final String rg = fields[12];
    // final String dir = fields[13];
    // final String suppress = fields[14];
    // final String cvf = fields[15];

    final Concept concept2 = new Concept();
    concept2.setCode(cui2);
    concept2.setName(nameMap.get(cui2));
    concept2.setTerminology(concept.getTerminology());
    concept2.setVersion(concept.getVersion());
    concept2.setLeaf(false);
    if (!rela.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("RELA", relaInverseMap.get(rela)));
    }

    // if (!rg.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("RG", rg));
    // }
    // if (!dir.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("DIR", dir));
    // }

    // if (!aui1.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("AUI1", aui1));
    // concept2.getQualifiers().add(new Qualifier("STYPE1", stype1));
    // }
    // if (!aui2.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("AUI2", aui2));
    // concept2.getQualifiers().add(new Qualifier("STYPE2", stype2));
    // }
    concept2.setSource(sab);
    // concept2.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    // concept2.getQualifiers().add(new Qualifier("SUPPRESS", suppress));

    return concept2;
  }

  /**
   * Builds the associations.
   *
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  private void buildAssociations(final Concept concept, final String[] fields) throws Exception {

    // e.g.
    // C0000039|A13650014|AUI|RO|C0364349|A10774117|AUI|measures|R108296692||LNC|LNC|||N||
    // final String cui1 = fields[0];
    final String aui1 = fields[1];
    // final String stype1 = fields[2];
    final String rel = fields[3];
    final String cui2 = fields[4];
    final String aui2 = fields[5];
    // final String stype2 = fields[6];
    final String rela = fields[7];
    // final String rui = fields[8];
    // final String srui = fields[9];
    final String sab = fields[10];
    // final String rg = fields[12];
    // final String dir = fields[13];
    // final String suppress = fields[14];
    // final String cvf = fields[15];

    // Build and add association
    final Association association = new Association();
    association.setType(relInverseMap.get(rel));
    association.setRelatedCode(cui2);
    association.setRelatedName(nameMap.get(cui2));
    association.setSource(sab);
    if (!aui1.isEmpty()) {
      // iassociation.getQualifiers().add(new Qualifier("AUI1", aui2));
      // iassociation.getQualifiers().add(new Qualifier("STYPE1", stype1));
    }
    if (!aui2.isEmpty()) {
      // iassociation.getQualifiers().add(new Qualifier("AUI2", aui2));
      // iassociation.getQualifiers().add(new Qualifier("STYPE2", stype1));
    }
    if (!rela.isEmpty()) {
      association.getQualifiers().add(new Qualifier("RELA", relaInverseMap.get(rela)));
    }

    final String inverseRui = ruiInverseMap.get(fields[8]);
    if (ruiQualMap.containsKey(inverseRui)) {
      for (final String atnatv : ruiQualMap.get(inverseRui)) {
        final String[] parts = atnatv.split("\\|");
        association
            .getQualifiers()
            .add(new Qualifier(parts[0], ConceptUtils.substr(parts[1], 1000)));
      }
    }

    // RUI and SRUI in the other direction are different.
    // iassociation.getQualifiers().add(new Qualifier("RG", rg));
    // iassociation.getQualifiers().add(new Qualifier("DIR", dir));
    // iassociation.getQualifiers().add(new Qualifier("SUPPRESS", suppress));
    concept.getAssociations().add(association);
    // Build and add an inverse association
    final Association iassociation = new Association();
    iassociation.setType(rel);
    relSet.add(rel);

    iassociation.setRelatedCode(cui2);
    iassociation.setRelatedName(nameMap.get(cui2));
    iassociation.setSource(sab);
    if (!rela.isEmpty()) {
      iassociation.getQualifiers().add(new Qualifier("RELA", rela));
    }

    // if (!rg.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("RG", rg));
    // }
    // if (!dir.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("DIR", dir));
    // }

    // if (!aui1.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("AUI1", aui1));
    // association.getQualifiers().add(new Qualifier("STYPE1", stype1));
    // }
    // if (!aui2.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("AUI2", aui1));
    // association.getQualifiers().add(new Qualifier("STYPE2", stype2));
    // }
    // association.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    // association.getQualifiers().add(new Qualifier("SUPPRESS", suppress));

    if (ruiQualMap.containsKey(inverseRui)) {
      for (final String atnatv : ruiQualMap.get(inverseRui)) {
        final String[] parts = atnatv.split("\\|");
        iassociation
            .getQualifiers()
            .add(new Qualifier(parts[0], ConceptUtils.substr(parts[1], 1000)));
      }
      ruiQualMap.remove(inverseRui);
      ruiInverseMap.remove(fields[8]);
    }

    concept.getInverseAssociations().add(iassociation);
  }

  /**
   * Handle building MRDOC property metadata.
   *
   * @param terminology the terminology
   * @param code the code
   * @param name the name
   * @return the concept
   */
  private Concept buildMetadata(
      final Terminology terminology, final String code, final String name) {
    final Concept propMeta = new Concept();
    propMeta.setCode(code);
    propMeta.setName(code);
    propMeta.setTerminology(terminology.getTerminology());
    propMeta.setVersion(terminology.getVersion());

    // property synonym info
    final Synonym propMetaSyn = new Synonym();
    propMetaSyn.setType("Preferred_Name");
    propMetaSyn.setName(code);
    // add synonym as list to property
    propMeta.getSynonyms().add(propMetaSyn);
    final Definition def = new Definition();
    def.setType("DEFINITION");
    def.setDefinition(name);
    // add definition
    propMeta.getDefinitions().add(def);

    return propMeta;
  }

  /* see superclass */
  @Override
  public void loadObjects(
      ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws Exception {

    final String indexName = terminology.getObjectIndexName();

    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);

    // Create index
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    //
    // Handle associations
    //
    final ElasticObject associations = new ElasticObject("associations");
    // MRSAT: association metadata for MRREL
    for (final String rel : relSet) {
      associations.getConcepts().add(buildMetadata(terminology, rel, relMap.get(rel)));
    }
    operationsService.index(associations, indexName, ElasticObject.class);

    // Hanlde "concept statuses" - n/a

    //
    // Handle definitionSources - n/a - handled inline
    //

    //
    // Handle definitionTypes
    //
    final ElasticObject defTypes = new ElasticObject("definitionTypes");
    defTypes.getConcepts().add(buildMetadata(terminology, "DEFINITION", "Definition"));
    operationsService.index(defTypes, indexName, ElasticObject.class);

    //
    // Handle properties
    //
    final ElasticObject properties = new ElasticObject("properties");

    // MRSAT: property metadata for MRSAT
    for (final String atn : atnSet) {
      properties.getConcepts().add(buildMetadata(terminology, atn, atnMap.get(atn)));
    }

    operationsService.index(properties, indexName, ElasticObject.class);

    //
    // Handle qualifiers
    //
    final ElasticObject qualifiers = new ElasticObject("qualifiers");

    // qualifiers to build - from relationships
    // removed for now: "AUI1", "STYPE1", "AUI2", "STYPE2", "SUPPRESS"
    for (final String col :
        new String[] {
          "RELA" // , "RG", "DIR"
        }) {
      qualifiers.getConcepts().add(buildMetadata(terminology, col, colMap.get(col)));
    }
    for (final String qual : qualMap.keySet()) {
      qualifiers.getConcepts().add(buildMetadata(terminology, qual, atnMap.get(qual)));
    }

    ConceptUtils.limitQualMap(qualMap, 1000);

    // Do this after truncating because it gets turned into a String
    qualifiers.setMap(qualMap);
    operationsService.index(qualifiers, indexName, ElasticObject.class);

    //
    // Handle roles - n/a
    //
    final ElasticObject roles = new ElasticObject("roles");
    operationsService.index(roles, indexName, ElasticObject.class);

    //
    // Handle subsets - n/a
    //
    final ElasticObject subsets = new ElasticObject("subsets");
    operationsService.index(subsets, indexName, ElasticObject.class);

    //
    // Handle synonymSources - n/a - handled inline
    //

    //
    // Handle synonymTypes
    //
    final ElasticObject syTypes = new ElasticObject("synonymTypes");
    syTypes.getConcepts().add(buildMetadata(terminology, "Preferred_Name", "Preferred name"));
    syTypes.getConcepts().add(buildMetadata(terminology, "Synonym", "Synonym"));
    operationsService.index(syTypes, indexName, ElasticObject.class);

    //
    // Handle termTypes - n/a - handled inline
    //

    // Handle stats
    handleStatistics(terminology, terminology.getMetadata().getSynonymSourceSet());
  }

  /**
   * Handle concept.
   *
   * @param concept the concept
   * @param batch the batch
   * @param flag the flag
   * @param indexName the index name
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void handleConcept(Concept concept, List<Concept> batch, boolean flag, String indexName)
      throws IOException {

    // Remove synonym "uris" as no longer needed
    concept.getSynonyms().forEach(s -> s.setUri(null));

    // Put concept lists in natural sort order
    concept.sortLists();

    batch.add(concept);

    int conceptSize = concept.toString().length();
    if (conceptSize > 10000000) {
      logger.info("    BIG concept = " + concept.getCode() + " = " + concept.toString().length());
    }
    batchSize += conceptSize;

    // Send to elasticsearch if the overall batch size > 9M
    if (flag || batchSize > 9000000) {
      // Log the bytes and number of concepts
      logger.info("    BATCH index = " + batchSize + ", " + batch.size());
      operationsService.bulkIndex(new ArrayList<>(batch), indexName, Concept.class);
      batch.clear();
      batchSize = 0;
    }
  }

  /* see superclass */
  @Override
  public Terminology getTerminology(
      ApplicationContext app,
      ElasticLoadConfig config,
      String filepath,
      String terminology,
      boolean forceDelete)
      throws Exception {
    // will eventually read and build differently
    this.setFilepath(new File(filepath));
    if (!this.getFilepath().exists()) {
      throw new Exception("Given filepath does not exist");
    }
    try (InputStream input = new FileInputStream(this.getFilepath() + "/release.dat");
        final BufferedReader in =
            new BufferedReader(new FileReader(this.getFilepath() + "/MRSAB.RRF")); ) {

      String line;
      Terminology term = new Terminology();
      while ((line = in.readLine()) != null) {
        // VCUI,RCUI,VSAB,RSAB,SON,SF,SVER,VSTART,VEND,IMETA,RMETA,SLC,SCC,SRL,
        // TFR,CFR,CXTY,TTYL,ATNL,LAT,CENC,CURVER,SABIN,SSN,SCIT
        final String[] fields = line.split("\\|", -1);
        sourceMap.put(fields[3], fields[4]);

        if (fields[3].equals("NCIMTH")) {
          Properties p = new Properties();
          p.load(input);
          term.setTerminology(terminology);
          term.setVersion(p.getProperty("umls.release.name"));
          term.setDate(p.getProperty("umls.release.date"));
          // term.setName(line.split("\\|", -1)[4]);
          term.setDescription(line.split("\\|", -1)[24]);
          term.setGraph(null);
          term.setSource(null);
          term.setTerminologyVersion(term.getTerminology() + "_" + term.getVersion());
          term.setIndexName("concept_" + term.getTerminologyVersion().toLowerCase());
          term.setLatest(true);
          term.setSparqlFlag(false);
          // if (forceDelete) {
          // logger.info(" DELETE TERMINOLOGY = " + term.getIndexName());
          // findAndDeleteTerminology(term.getIndexName());
          // }
        }
      }

      // Attempt to read the config, if anything goes wrong
      // the config file is probably not there
      try {

        // Load from config
        final JsonNode node = getMetadataAsNode(terminology.toLowerCase());
        final TerminologyMetadata metadata =
            new ObjectMapper().treeToValue(node, TerminologyMetadata.class);

        // Set term name and description
        term.setName(metadata.getUiLabel() + " " + term.getVersion());
        if (term.getDescription() == null || term.getDescription().isEmpty()) {
          term.setDescription(node.get("description").asText());
        }

        metadata.setLoader("rrf");
        metadata.setSources(sourceMap);
        metadata.setSourceCt(sourceMap.size());
        metadata.setWelcomeText(getWelcomeText(terminology.toLowerCase()));
        term.setMetadata(metadata);

      } catch (Exception e) {
        throw new Exception(
            "Unexpected error trying to load metadata = "
                + applicationProperties.getConfigBaseUri(),
            e);
      }

      return term;
    } catch (IOException ex) {
      throw new Exception("Could not load terminology ncim", ex);
    }
  }

  /**
   * Load the history files.
   *
   * @param terminology the terminology
   * @return the number of retired concepts created
   * @throws Exception the exception
   */
  private int loadHistory(final Terminology terminology) throws Exception {

    if (!terminology.getTerminology().equals("ncim")) {
      return 0;
    }

    logger.debug("  STARTING HISTORY PROCESSING (wait 5 sec for elasticsearch to finish)");
    Thread.sleep(5000);

    final List<Concept> batch = new ArrayList<>();
    int totalConcepts = 0;

    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");

    try (final PushBackReader mrcui = readers.getReader(RrfReaders.Keys.MRCUI); ) {

      String line = null;
      Concept concept = new Concept();
      String prevCui = null;

      // CODE, DATE, REL (ACTION), RELA, MAPREASON, REPLACEMENT CODE, MAPINSUB
      // Action values: Broader (RB), Narrower (RN), Other Related (RO), Deleted (DEL), Removed from
      // Subset (SUBX)
      // Loop through lines until we reach "the end"
      while ((line = mrcui.readLine()) != null) {

        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        final String date = fields[1];
        final String action = fields[2];
        final String replacementCui = fields[5];
        final String replacementInSubset = fields[6];

        // Skip things removed by Metamorphosys
        if (action.equals("SUBX")) {
          continue;
        }

        // Skip things where CUI2 is not in the subset
        if (replacementInSubset.equals("N")) {
          continue;
        }

        // Test assumption that the file is in order (when considering
        // |)
        if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
          throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
        }

        // If prev cui doesn't match, make a concept
        // check if we've hit a new concept then process the other files for the
        // "prevCui"
        if (prevCui != null && !cui.equals(prevCui)) {

          // Handle the previous concept here (and repeat outside the block)
          handleConcept(concept, batch, false, terminology.getIndexName());

          if (totalConcepts++ % 5000 == 0) {
            logger.info("    count = " + totalConcepts);
          }

          // Create new concept for this code
          concept = new Concept();
          concept.setName("Retired concept");
          concept.setNormName(ConceptUtils.normalize("Retired concept"));
          concept.setStemName(ConceptUtils.normalizeWithStemming("Retired concept"));
          concept.setCode(cui);
          concept.setTerminology(terminology.getTerminology());
          concept.setVersion(terminology.getVersion());
          // NO hierarchies for NCIM concepts, so set leaf to null
          concept.setLeaf(null);
          setConceptInactive(terminology, concept);
        }

        // Make a history entry from this line
        final History history = new History();
        history.setCode(cui);
        // We don't know the names of retired concepts
        history.setName("Retired concept");
        history.setAction(action);
        history.setDate(date);
        if (!replacementCui.isEmpty()) {
          history.setReplacementCode(replacementCui);
          history.setReplacementName(nameMap.get(replacementCui));
        }
        concept.getHistory().add(history);

        // Save prev cui for next round
        prevCui = cui;
      }

      handleConcept(concept, batch, true, terminology.getIndexName());
      totalConcepts++;

    } finally {
      readers.closeReaders();
    }

    logger.info("    totalCount = " + totalConcepts);
    return totalConcepts;
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) {
    // Don't need hierarchy utils in this indexing
    return null;
  }

  /* see superclass */
  @Override
  public Set<String> cleanStaleIndexes(final Terminology terminology) throws Exception {
    // do nothing - override superclass behavior
    return new HashSet<>(0);
  }
}
