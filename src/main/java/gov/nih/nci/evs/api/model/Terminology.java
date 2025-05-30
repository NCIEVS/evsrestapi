package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Represents a terminology loaded into the EVSRESTAPI.
 *
 * <pre>
 *   {
 *    "terminology": "ncit",
 *    "version": "2019_11f",
 *    "name": "NCI Thesaurus 2019_11f",
 *    "terminologyVersion": "ncit_2019_11f",
 *    "latest": true,
 *    "tags": { "monthly": "true" }
 *  }
 * </pre>
 */
@Schema(description = "Represents a terminology loaded into the API")
@JsonInclude(Include.NON_EMPTY)
public class Terminology extends BaseModel implements Comparable<Terminology> {

  /** The terminology. */
  @Field(type = FieldType.Keyword)
  private String terminology;

  /** The version. */
  @Field(type = FieldType.Keyword)
  private String version;

  /** The date. */
  @Field(type = FieldType.Keyword)
  private String date;

  /** The name. */
  @Field(type = FieldType.Keyword)
  private String name;

  /** The description. */
  @Field(type = FieldType.Keyword)
  private String description;

  /** The graph. */
  @Field(type = FieldType.Keyword)
  private String graph;

  /** The graph source. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The terminology version. */
  @Field(type = FieldType.Keyword)
  private String terminologyVersion;

  /** The latest. */
  @Field(type = FieldType.Boolean)
  private Boolean latest;

  /** The tags. */
  @Field(type = FieldType.Object)
  private Map<String, String> tags;

  /** The index name for concepts. */
  @Field(type = FieldType.Keyword)
  private String indexName;

  /** The index name for generic objects. */
  @Field(type = FieldType.Keyword)
  private String objectIndexName;

  /** The metadata. */
  // TODO: we really should leave this off, but requires 4.1.RC
  // or greater (spring data elasticsearch)
  // @Field(type = FieldType.Object, enabled = false)
  @Field(type = FieldType.Object)
  private TerminologyMetadata metadata;

  /** The flag for using sparql searches. */
  private Boolean sparqlFlag;

  /** Instantiates an empty {@link Terminology}. */
  public Terminology() {
    // n/a
    this.terminology = "ncit";
  }

  /**
   * Instantiates a {@link Terminology} from the specified parameters.
   *
   * @param other the other
   */
  public Terminology(final Terminology other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Terminology other) {
    super.populateFrom(other);
    terminology = other.getTerminology();
    version = other.getVersion();
    date = other.getDate();
    name = other.getName();
    description = other.getDescription();
    graph = other.getGraph();
    source = other.getSource();
    terminologyVersion = other.getTerminologyVersion();
    latest = other.getLatest();
    tags = new HashMap<>(other.getTags());
    indexName = other.getIndexName();
    objectIndexName = other.getObjectIndexName();
    metadata = other.getMetadata();
    sparqlFlag = other.getSparqlFlag();
  }

  /**
   * Returns the terminology.
   *
   * @return the terminology
   */
  @Schema(description = "Terminology abbreviation, e.g. 'ncit'")
  public String getTerminology() {
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(final String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  @Schema(description = "Terminology version, e.g. '23.11d'")
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * Returns the date.
   *
   * @return the date
   */
  @Schema(description = "Terminology publication/release date")
  public String getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date the date
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  @Schema(description = "Terminology name")
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  @Schema(description = "Terminology description")
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Returns the graph.
   *
   * @return the graph
   */
  @Schema(description = "Name of the RDF triplestore graph if this data is backed by a triplestore")
  public String getGraph() {
    return graph;
  }

  /**
   * Sets the graph.
   *
   * @param graph the graph
   */
  public void setGraph(final String graph) {
    this.graph = graph;
  }

  /**
   * Returns the source.
   *
   * @return the source
   */
  @Schema(hidden = true)
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(final String source) {
    this.source = source;
  }

  /**
   * Returns the terminology version.
   *
   * @return the terminology version
   */
  @Schema(
      description =
          "Underscore-separated value for terminology and version"
              + " used by the API to precisely pinpoint a particular version, e.g. 'ncit_23.11d'")
  public String getTerminologyVersion() {
    if (StringUtils.isEmpty(terminologyVersion)) {
      terminologyVersion = terminology + "_" + version;
    }
    return terminologyVersion;
  }

  /**
   * Sets the terminology version.
   *
   * @param terminologyVersion the terminology version
   */
  public void setTerminologyVersion(final String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * Returns the latest.
   *
   * @return the latest
   */
  @Schema(description = "Indicates whether this is the latest version")
  public Boolean getLatest() {
    return latest;
  }

  /**
   * Sets the latest.
   *
   * @param latest the latest
   */
  public void setLatest(final Boolean latest) {
    this.latest = latest;
  }

  /**
   * Returns the metadata.
   *
   * @return the metadata
   */
  @Schema(description = "Additional terminology metadata")
  public TerminologyMetadata getMetadata() {
    return metadata;
  }

  /**
   * Sets the metadata.
   *
   * @param metadata the metadata
   */
  public void setMetadata(TerminologyMetadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Returns the tags.
   *
   * @return the tags
   */
  @Schema(description = "Additional terminology tags")
  public Map<String, String> getTags() {
    if (tags == null) {
      tags = new HashMap<>();
    }
    return tags;
  }

  /**
   * Sets the tags.
   *
   * @param tags the tags
   */
  public void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  /**
   * Returns the index name for concepts belonging to this terminology.
   *
   * @return the index name
   */
  // @Schema(hidden = true)
  @Schema(description = "for internal use")
  public String getIndexName() {
    if (StringUtils.isEmpty(indexName)) {
      indexName =
          "concept_" + getTerminologyVersion().replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }
    return indexName;
  }

  /**
   * Sets the index name for concepts belonging to this terminology.
   *
   * @param indexName the index name
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * Returns the index name for objects.
   *
   * @return the object index name
   */
  // @Schema(hidden = true)
  @Schema(description = "for internal use")
  public String getObjectIndexName() {
    if (StringUtils.isEmpty(objectIndexName)) {
      // Replace non-alphanumeric and _ chars and also lowercase
      objectIndexName =
          "evs_object_" + getTerminologyVersion().replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    }
    return objectIndexName;
  }

  /**
   * Sets the index name for objects.
   *
   * @param objectIndexName the object index name
   */
  public void setObjectIndexName(String objectIndexName) {
    this.objectIndexName = objectIndexName;
  }

  /**
   * Returns the sparql flag.
   *
   * @return the sparql flag
   */
  @Schema(description = "Indicates whether the terminology can be used with SPARQL")
  public Boolean getSparqlFlag() {
    return sparqlFlag;
  }

  /**
   * Sets the sparql flag.
   *
   * @param sparqlFlag the sparql flag
   */
  public void setSparqlFlag(Boolean sparqlFlag) {
    this.sparqlFlag = sparqlFlag;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((graph == null) ? 0 : graph.hashCode());
    result = prime * result + ((source == null) ? 0 : source.hashCode());
    result = prime * result + ((latest == null) ? 0 : latest.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((date == null) ? 0 : date.hashCode());
    result = prime * result + ((indexName == null) ? 0 : indexName.hashCode());
    result = prime * result + ((objectIndexName == null) ? 0 : objectIndexName.hashCode());
    result = prime * result + ((sparqlFlag == null) ? 0 : sparqlFlag.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Terminology other = (Terminology) obj;
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (graph == null) {
      if (other.graph != null) {
        return false;
      }
    } else if (!graph.equals(other.graph)) {
      return false;
    }
    if (source == null) {
      if (other.source != null) {
        return false;
      }
    } else if (!source.equals(other.source)) {
      return false;
    }
    if (latest == null) {
      if (other.latest != null) {
        return false;
      }
    } else if (!latest.equals(other.latest)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (terminology == null) {
      if (other.terminology != null) {
        return false;
      }
    } else if (!terminology.equals(other.terminology)) {
      return false;
    }
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null) {
        return false;
      }
    } else if (!terminologyVersion.equals(other.terminologyVersion)) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    if (date == null) {
      if (other.date != null) {
        return false;
      }
    } else if (!date.equals(other.date)) {
      return false;
    }
    if (indexName == null) {
      if (other.indexName != null) {
        return false;
      }
    } else if (!indexName.equals(other.indexName)) {
      return false;
    }
    if (objectIndexName == null) {
      if (other.objectIndexName != null) {
        return false;
      }
    } else if (!objectIndexName.equals(other.objectIndexName)) {
      return false;
    }
    if (sparqlFlag == null) {
      if (other.sparqlFlag != null) {
        return false;
      }
    } else if (!sparqlFlag.equals(other.sparqlFlag)) {
      return false;
    }
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(Terminology o) {
    return (terminology + version).compareToIgnoreCase(o.getTerminology() + o.getVersion());
  }
}
