
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetricSearch;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.FilterCriteriaElasticFields;
import gov.nih.nci.evs.api.support.SearchCriteria;
import gov.nih.nci.evs.api.support.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Class SearchController.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Search endpoint")
public class SearchController {

  /** The Constant log. */
  private static final Logger log =
      LoggerFactory.getLogger(SearchController.class);

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The thesaurus properties. */
  @Autowired
  ThesaurusProperties thesaurusProperties;

  /** The elastic search service. */
  @Autowired
  ElasticSearchService elasticSearchService;

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Elasticsearch.
   *
   * @param filterCriteriaElasticFields the filter criteria elastic fields
   * @param bindingResult the binding result
   * @param response the response
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "${swagger.documentation.searchAPIOperation}", notes = ""
      + "# Term\n" + "\n"
      + "The **term** parameter is used to enter the term or phrase to use in the search."
      + "\n" + "# Paging\n"
      + "The **fromRecord** and **pageSize** parameter is used to control the paging of the matched concepts returned. "
      + "The default is to return the first 10 matched concepts.<br>"
      + "The fromRecord is used to specify the start record number e.g. 0 ,10, 20.<br>"
      + "The pageSize is used to specify the number of records e.g. 10, 20,100 that need to be returned from the start record number (specified by fromRecord). <br>"
      + "In the examples below the output returned has a **hits** field. The hits is an array of matched concepts.<br>"
      + "Check the **from** and **size** field returned in the output. These parameters come after the hits field."
      + "<br>" + "<br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>will return the first 10 matched concepts for the term melanoma. (say page 1)</td>"
      + "  <td><a href='api/v1/search?term=melanoma' target='_blank'>api/v1/search?term=melanoma</a></td> "
      + "  </tr>" + " <tr>"
      + "   <td>will return 10th record to 20th record. The next 10 records for the term melanoma (say page 2)</td>"
      + "  <td> <a href='api/v1/search?term=melanoma&fromRecord=10&pageSize=10' target='_blank'>api/v1/search?term=melanoma&fromRecord=10&pageSize=10<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>will return 20th record to 30th record. The next 10 records for the term melanoma from the 20th record. (say page 3)</td>"
      + "  <td><a href='api/v1/search?term=melanoma&fromRecord=20&pageSize=10' target='_blank'>api/v1/search?term=melanoma&fromRecord=20&pageSize=10</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>will return 1st record to the 100th record</td>"
      + "  <td><a href='api/v1/search?term=melanoma&fromRecord=0&pageSize=100' target='_blank'>api/v1/search?term=melanoma&fromRecord=0&pageSize=100</a></td> "
      + " </tr>" + " </table>" + "\n\n" + "# Format\n"
      + "The **format** parameter determines whether the data is returned in a raw format (format returned by elastic search server) or in a more user friendly format.<br>"
      + "If no **format** parameter is specified the default is to send the output in the raw format.<br>"
      + " Valid entries for **format** specifications are: **clean, cleanWithHighlights, raw**"
      + "<br>" + "<br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>clean format has the from, size, total (total no of record found for the match)and array of matched concepts </td>"
      + "  <td><a href='api/v1/search?term=carcinoma&format=clean' target='_blank'>api/v1/search?term=carcinoma&format=clean</a></td> "
      + "  </tr>" + " <tr>"
      + "   <td>cleanWithHighlights format has from, size, total and array of matched concepts with highlights (highlights is the text of the concept where the term/phrase was found)</td>"
      + "  <td> <a href='api/v1/search?term=metastasize&format=cleanWithHighlights' target='_blank'>api/v1/search?term=metastasize&format=cleanWithHighlights<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>raw format is the data returned by the elastic search server.</td>"
      + "  <td><a href='api/v1/search?term=metastasize&format=raw' target='_blank'>api/v1/search?term=metastasize&format=raw</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>Returns the first 100 rows in clean format</td>"
      + "  <td><a href='api/v1/search?term=melanoma&pageSize=100&format=clean' target='_blank'>api/v1/search?term=melanoma&pageSize=100&format=clean</a></td> "
      + " </tr>" + " </table>" + "\n\n" + "# Type\n"
      + "The **type** parameter determines the kind of search to perform.\n"
      + "If no **type** parameter is specified, the default is to do a contains search.<br>"
      + "All searches are case-insensitive. "
      + "Valid entries for type specifications are: **phrase, AND, OR, startswith, match, contains** <br>"
      + "Some examples below have used the cleanWithHighlights format. It can use any format or the format parameter need not be specified"
      + "<br>" + "<br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>contains search -Will find the term as a whole word or part of any word  of any property. The default is the contains search. e.g. search on carcinom instead of carcinoma</td>"
      + "  <td><a href='api/v1/search?term=carcinom&format=cleanWithHighlights' target='_blank'>api/v1/search?term=carcinom&format=cleanWithHighlights</a></td> "
      + "  </tr>" + " <tr>"
      + "   <td>phrase search - will find the concepts where the phrase exists in any of the properties</td>"
      + "  <td> <a href='api/v1/search?term=Lung%20Carcinoma&type=phrase&format=cleanWithHighlights' target='_blank'>api/v1/search?term=Lung%20Carcinoma&type=phrase&format=cleanWithHighlights<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>AND search - will find the concepts that have all the specified words in the same property or different properties of a concept</td>"
      + "  <td><a href='api/v1/search?term=lentiginous%20melanoma&type=AND' target='_blank'>api/v1/search?term=lentiginous%20melanoma&type=AND</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>OR search - will find the concepts that have the any or all words specified in any property of the concept</td>"
      + "  <td><a href='api/v1/search?term=lentiginous%20melanoma&type=OR' target='_blank'>api/v1/search?term=lentiginous%20melanoma&type=OR</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>exact Match search - will find the concepts that have the term or phrase specified matching a property value exactly</td>"
      + "  <td><a href='api/v1/search?term=Lung%20Carcinoma&type=match' target='_blank'>api/v1/search?term=Lung%20Carcinoma&type=match</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>fuzzy search - The fuzzy query uses similarity based on Levenshtein edit distance.</td>"
      + "  <td><a href='api/v1/search?term=enzymi&type=fuzzy' target='_blank'>api/v1/search?term=enzymi&type=fuzzy</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>starts with search - will find the concepts that have a property value starting with the term or phrase specified </td>"
      + "  <td><a href='api/v1/search?term=enzyme&type=startswith' target='_blank'>api/v1/search?term=enzyme&type=startswith</a></td> "
      + " </tr>" + " </table>" + "\n\n" + "# Property\n"
      + "The **property** parameter can be used to control which properties are searched.<br>"
      + "For example if *P107,P108* or *display_name, preferred_name* were passed in the **property** parameter only the "
      + "P107 (Display Name) and the P108 (Preferred_Name) properties of the concept would be searched for the specified term/phrase.<br>"
      + "Please click on the link to get a list of properties that can be searched. <a href='api/v1/propertiesList' target='_blank'>Click here.</a>"
      + "<br>" + "<br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>Will search in full_syn and preferred name properties for the term lung carcinoma as an exact match and format is cleanWithHighlights.</td>"
      + "  <td><a href='api/v1/search?term=lung%20carcinoma&type=match&format=cleanWithHighlights&property=full_syn,preferred_name' target='_blank'>api/v1/search?term=lung%20carcinoma&type=match&format=cleanWithHighlights&property=full_syn,preferred_name</a></td>"
      + "  </tr>" + " <tr>"
      + "   <td>Will search in the FDA_UNII_CODE property for the term XAV05295I5</td>"
      + "  <td> <a href='api/v1/search?term=XAV05295I5&format=cleanWithHighlights&property=fda_unii_code' target='_blank'>api/v1/search?term=XAV05295I5&format=cleanWithHighlights&property=fda_unii_code<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>Will search in the Maps_To property for the term Hydronephrosis and wil return only the maps_to property</td>"
      + "  <td><a href='api/v1/search?term=Hydronephrosis&returnProperties=maps_to&property=maps_to' target='_blank'>api/v1/search?term=Hydronephrosis&returnProperties=maps_to&property=maps_to</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>Will search in the gene_encodes_product property</td>"
      + "  <td><a href='api/v1/search?term=Phosphoinositide&property=gene_encodes_product&returnProperties=gene_encodes_product&pageSize=100' target='_blank'>api/v1/search?term=Phosphoinositide&property=gene_encodes_product&returnProperties=gene_encodes_product&pageSize=100</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>Will search in the neoplastic_status property</td>"
      + "  <td><a href='api/v1/search?term=Benign&type=match&property=neoplastic_status&returnProperties=neoplastic_status&format=cleanWithHighlights' target='_blank'>api/v1/search?term=Benign&type=match&property=neoplastic_status&returnProperties=neoplastic_status&format=cleanWithHighlights</a></td> "
      + " </tr>" + " </table>" + "\n\n" + "# Return Properties\n"
      + "The **returnProperties** pararmeter is use to restrict the properties of the matched concept that are returned.<br>"
      + "The default is to return all properties that are not null of a match concept.<br>"
      + "Valid entries for **returnProperties** specifications are: **all,short or a list of the specific properties comma separated**<br>"
      + "Please click on the link to get a list of properties that can be returned. <a href='api/v1/propertiesList' target='_blank'>Click here.</a>"
      + "<br><br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>all - Will return all the properties of the matched concepts that are not null.</td>"
      + "  <td><a href='api/v1/search?term=carcinoma&format=clean&returnProperties=all' target='_blank'>api/v1/search?term=carcinoma&format=clean&returnProperties=all</a></td>"
      + "  </tr>" + " <tr>"
      + "   <td>short - Will return a short list of properties(Code,Label,Display_Name,Preferred_Name) of the matched concepts</td>"
      + "  <td> <a href='api/v1/search?term=carcinoma&format=clean&returnProperties=short' target='_blank'>api/v1/search?term=carcinoma&format=clean&returnProperties=short<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>specific properties - will return roles and associations of the concepts. Code and Label are returned by default</td>"
      + "  <td><a href='api/v1/search?term=carcinoma&format=clean&returnProperties=roles,associations' "
      + "  target='_blank'>api/v1/search?term=carcinoma&format=clean&returnProperties=roles,associations</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>specific properties - will return roles and definition of the concepts</td>"
      + "  <td><a href='api/v1/search?term=carcinoma&format=clean&returnProperties=roles,definition' target='_blank'>api/v1/search?term=carcinoma&format=clean&returnProperties=roles,definition</a>"

      + "   </td> " + " </tr>" + " <tr>"
      + "   <td>specific properties - will return FDA_UNII_Code. The property can be specified using the label or code</td>"
      + "  <td>"
      + "<a href='api/v1/search?term=XAV05295I5&returnProperties=fda_unii_code' target='_blank'>"
      + " api/v1/search?term=XAV05295I5&returnProperties=fda_unii_code</a> <br>"
      + "<a href='api/v1/search?term=XAV05295I5&returnProperties=P319' target='_blank'>"
      + " api/v1/search?term=XAV05295I5&returnProperties=P319</a>" + "</td> "
      + " </tr>" + " </table>" + "\n"
      + "# Search a term within synonym of a concept by source and group\n"
      + "The **synonymSource** and **synonymGroup** parameter is used to search a term or phrase within the text of a synonym of the specified source and/or specified group.\n"
      + "\n\n" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>Will search for the term dsDNA in the synonyms of all the concepts where the source is NCI.</td>"
      + "  <td><a href='api/v1/search?term=dsDNA&synonymSource=NCI' target='_blank'>api/v1/search?term=dsDNA&synonymSource=NCI</a></td>"
      + "  </tr>" + " <tr>"
      + "   <td>Will search for the term dsDNA in the synonyms of all the concepts where the source is NCI and the group is SY. </td>"
      + "  <td> <a href='api/v1/search?term=dsDNA&synonymSource=NCI&synonymGroup=SY' target='_blank'>api/v1/search?term=dsDNA&synonymSource=NCI&synonymGroup=SY<a></td> "
      + " </tr>" + " <tr>"
      + "   <td>Will search for the term dsDNA in the synonyms of all the concepts where the source is NCI and the group is PT. No results will be returned since term dsDNA does not exist in the synonyms for the source NCI and group PT</td>"
      + "  <td><a href='api/v1/search?term=dsDNA&synonymSource=NCI&synonymGroup=PT' "
      + "  target='_blank'>api/v1/search?term=dsDNA&synonymSource=NCI&synonymGroup=PT</a></td> "
      + " </tr>" + " <tr>"
      + "   <td>search for term Humoral thats exists in the synonyms for same source but two different groups</td>"
      + "  <td><a href='api/v1/search?term=Humoral&synonymSource=NCI&synonymGroup=SY' target='_blank'>"
      + "      api/v1/search?term=Humoral&synonymSource=NCI&synonymGroup=SY</a><br><br>"
      + " <a href='api/v1/search?term=Humoral&synonymSource=NCI&synonymGroup=PT' target='_blank'>"
      + "  api/v1/search?term=Humoral&synonymSource=NCI&synonymGroup=PT</a></td> "
      + " </tr>" + " <tr>" + "   <td>Will search in the source NICHD</td>"
      + "  <td><a href='api/v1/search?term=dsDNA&synonymSource=NICHD' target='_blank'>"
      + " api/v1/search?term=dsDNA&synonymSource=NICHD</a></td> " + " </tr>"
      + " </table>" + "\n\n"
      + "# Search a term within definition and alt_definition of a concept by source\n"
      + "The **definitionSource** parameter is used to search a term or phrase within the text of a definition and alt_definition of the specified source."
      + "<br><br>" + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>Will search for the term Dilation in the definition and alt_definition of all the concepts where the source is NCI.</td>"
      + "  <td><a href='api/v1/search?term=Dilation&definitionSource=NCI' target='_blank'>api/v1/search?term=Dilation&definitionSource=NCI</a></td>"
      + "  </tr>" + " <tr>"
      + "   <td>Will search for the phrase 'cervical spine' in the definition and alt_definition of all the concepts where the source is CTCAE. </td>"
      + "  <td> <a href='api/v1/search?term=cervical%20spine%20joint&type=phrase&definitionSource=CTCAE' target='_blank'>api/v1/search?term=cervical%20spine%20joint&type=phrase&definitionSource=CTCAE<a></td> "
      + " </tr>" + " </table>" + "\n\n" + "# Filtering and Faceting\n"
      + "**Faceting** : The faceting of the data is done on two facets: Contributing_Source and Concept_Status. More Facets will added at a later point.\n"
      + "Check the end of the output of a search result when the format=raw : <a href='api/v1/search?term=melanoma' target='_blank'>api/v1/search?term=melanoma</a>\n"
      + "The total number of records returned are divided into various buckets. If the concept has a null Contributing_Source or Concept_Status , then they are not considered in the count.\n"
      + "<a href='api/v1/contributingSourcesList' target='_blank'>Click here for a list of valid values for Contributing_Source</a>\n"
      + "<a href='api/v1/conceptStatuesList' target='_blank'>Click here for a list of valid values for Concept_Status</a>"
      + "<br><br>"
      + "**Filtering** : The returned matched concepts can be filtered to return only concepts of a particular facet value. For e.g.concept_status = Obsolete_Concept or Contributing_Source = CTEP<br><br>"
      + "<table style='width:60%;'>" + "<tr>"
      + "  <th align='left'>Example</th>" + "  <th align='left'>URL</th> "
      + " </tr>" + " <tr>"
      + "   <td>will return matched concepts for the term melanoma (search is contains by default) but with a contributing source equal to CTEP.</td>"
      + "  <td><a href='api/v1/search?term=melanoma&contributingSource=CTEP' target='_blank'>api/v1/search?term=melanoma&contributingSource=CTEP</a></td>"
      + "  </tr>" + " <tr>"
      + "   <td>will return matched concepts for the term melanoma (search is contains by default) but with a concept status equal to Retired_Concept</td>"
      + "  <td><a href='api/v1/search?term=melanoma&conceptStatus=Retired_Concept' target='_blank'>api/v1/search?term=melanoma&conceptStatus=Retired_Concept</a></td> "
      + " </tr>" + " </table>" + "\n\n"
      + "# Search an association by a specified relationship/relationships by source or target\n"
      + "The **associationSearch** parameter controls if search happens in the associations or inverse associations.\n"
      + "**Search by source** - If the value of associationSearch is 'source', then the term is searched in the relatedConceptLabel of all the associations of all the concepts.<br><br>"
      + "Example : <br>" + "**with only the associationSearch specified**<br>"
      + "<a href='api/v1/search?term=Biological&fromRecord=0&pageSize=100&associationSearch=source' target='_blank'>api/v1/search?term=Biological&fromRecord=0&pageSize=100&associationSearch=source</a><br>"
      + "In the above the term Biological in searched in the associations for all relationships.\n"
      + " Concept C923 was returned because it had an association as below<br>"
      + "{<br>" + "relatedConceptLabel: \"Biological Agent\",<br>"
      + "relatedConceptCode: \"C307\",<br>" + "relationshipCode: \"A11\",<br>"
      + "relationship: \"Has_NICHD_Parent\"<br>" + "}<br>"
      + " Concept C43741 was returned because it had an association as below<br>"
      + "{<br>" + "relatedConceptLabel: \"Biological Process\",<br>"
      + "relatedConceptCode: \"C17828\",<br>" + "relationshipCode: \"A2\",<br>"
      + "relationship: \"Role_Has_Range\"<br>" + "}<br>" + "<br>"
      + "**with the associationSearch and relationship specified**<br>"
      + "If the value is 'source' and a relationship or relationships are specified, then the term is searched in the relatedConceptLabel of the associations with the specified relation/relations (in the relationship) of all the concepts. <br>"
      + "Please click on the link to get all the association relationships. <a href='api/v1/associationsList' target='_blank'>Click here</a><br>"
      + "<br>" + "Example : <br>"
      + "value is 'source',relationship is Has_Target , term is dna. The term is searched in the relatedConceptLabel of the associations that have the relationship = Has_Target. <br>"
      + "<a href='api/v1/search?term=dna&pageSize=100&associationSearch=source&relationship=Has_Target' target='_blank'>api/v1/search?term=dna&fromRecord=0&pageSize=100&associationSearch=source&relationship=Has_Target</a><br>"
      + "<a href='api/v1/search?term=dna&pageSize=100&associationSearch=source&relationship=A7' target='_blank'>api/v1/search?term=dna&fromRecord=0&pageSize=100&associationSearch=source&relationship=A7</a><br>"
      + "Both the examples listed above will return the same results. The relationship can be specified with the code (e.g A7)or label (e.g. Has_Target)<br>"
      + "In the above example the term dna will be searched in the associations that have the relationship as Has_Target/A7 .<br>"
      + " Concept C1464 was returned because it had an association as below:<br>"
      + "{" + "relatedConceptLabel: \"DNA\",<br>"
      + "relatedConceptCode: \"C449\",<br>" + "relationshipCode: \"A7\",<br>"
      + "relationship: \"Has_Target\"<br>" + "}<br>" + "<br>"
      + "<a href='api/v1/search?term=Biological&fromRecord=0&pageSize=100&associationSearch=source&relationship=A2,A11' target='_blank'>"
      + "api/v1/search?term=Biological&fromRecord=0&pageSize=100&associationSearch=source&relationship=A2,A11</a><br>"
      + "In the above example the association that have relation A2 or A11 or both are searched"
      + "<br><br>"
      + "**Search by target** - If the value of associationSearch is 'target', then the term is searched in the relatedConceptLabel of all the inverse associations of all the concepts.<br><br>"
      + "Example: <br>"
      + "**with the associationSearch and relationship specified**<br>"
      + "<a href='api/v1/search?term=Biological&pageSize=100&associationSearch=target&relationship=A2,A11' target='_blank'>api/v1/search?term=Biological&pageSize=100&associationSearch=target&relationship=A2,A11</a><br>"
      + "In the example above the term Biological will be searched in the inverse associations that have the relationship as A2 or A11 or both. <br>"
      + "\n\n"
      + "# Search a role by a specified relationship/relationships by source or target\n"
      + "**Search by source** -The **roleSearch** parameter controls if the search happens in the roles or inverse roles.\n"
      + "If the value of roleSearch is 'source', then the term is searched in the relatedConceptLabel of all the roles of all the concepts. <br><br>"
      + "Example : <br>" + "**with only the roleSearch specified**<br>"
      + "<a href='api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source' target='_blank'>api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source</a><br>"
      + "In the above the term Isomerase in searched in the roles.\n"
      + " Concept C923 was returned because it had an role as below <br>"
      + "{<br>" + "relatedConceptLabel: \"Biological Agent\", <br>"
      + "relatedConceptCode: \"C307\", <br>" + "relationshipCode: \"A11\", <br>"
      + "relationship: \"Has_NICHD_Parent\" <br>" + "} <br>"
      + " Concept C43741 was returned because it had an role as below <br>"
      + "{ <br>" + "relatedConceptLabel: \"Biological Process\", <br>"
      + "relatedConceptCode: \"C17828\", <br>"
      + "relationshipCode: \"A2\", <br>"
      + "relationship: \"Role_Has_Range\" <br>" + "} <br>" + "<br>"
      + "**with the roleSearch and relationship specified**<br>"
      + "If the value is 'source' and a relationship/relationships are specified, then the term is searched in the relatedConceptLabel<br>of the roles with the specified relationship. <br>"
      + "<a href='api/v1/rolesList' target='_blank' >Please click on the link to get all the role relationships</a><br><br>"
      + "Example : <br>"
      + "value is 'source',relationship is 'Gene_Product_Encoded_By_Gene',term is 'Isomerase'. The term is searched in the relatedConceptLabel of the roles that have the relationship = Gene_Product_Encoded_By_Gene.<br>"
      + "<a href='api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=Gene_Product_Encoded_By_Gene' target='_blank'>api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=Gene_Product_Encoded_By_Gene</a><br>"
      + "<a href='api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=r54' target='_blank'>api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=r54</a><br>"
      + "Both the examples listed above will return the same results. The relationship can be specified with the code (e.g r54)or label (e.g. Gene_Product_Encoded_By_Gene)"
      + "In the above example the term Isomerase will be searched in the associations that have the relationship as Gene_Product_Encoded_By_Gene/r54 .<br>"
      + " Concept C120050 was returned because it had an role as below<br>"
      + "{\n" + "relatedConceptLabel: \"Isomerase Gene\",\n"
      + "relatedConceptCode: \"C25941\",\n" + "relationshipCode: \"R54\",\n"
      + "relationship: \"Gene_Product_Encoded_By_Gene\"\n" + "}" + "<br>"
      + "<a href='api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=R54,R44' target='_blank'>"
      + "api/v1/search?term=Isomerase&fromRecord=0&pageSize=100&roleSearch=source&relationship=R54,R44</a>\n"
      + "In the above example the role that have relation R54 or R44 or both are searched"
      + "<br>" + "<br>"
      + "**Search by target** - If the value of roleSearch is 'target', then the term is searched in the relatedConceptLabel <br>of all the inverse roles of all the concepts. <br>"
      + "Example:<br>"
      + "<a href='api/v1/search?term=Triosephosphate&fromRecord=0&pageSize=100&roleSearch=target&relationship=r54' target='_blank'>api/v1/search?term=Triosephosphate&fromRecord=0&pageSize=100&roleSearch=target&relationship=r54</a><br>"
      + "In the example above the term Triosephosphate will be searched in the inverse roles that have the relationship as r54.  If a match is found, then the concept is returned."

  )
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 401, message = "You are not authorized to view the resource"),
      @ApiResponse(code = 403, message = "Accessing the resource you were trying to reach is forbidden"),
      @ApiResponse(code = 404, message = "The resource you were trying to reach is not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "associationSearch", value = "This paramter controls if the search happens in the associations or inverse associations", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "conceptStatus", value = "This parameter is a filter. The results can be filtered by the value of the concept status. <a href='api/v1/conceptStatuesList' target='_blank'>Click here for a list of values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "contributingSource", value = "This parameter is a filter. The results can be filtered by the value of the contributing source. <a href='api/v1/contributingSourcesList' target='_blank'>Click here for a list of values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "definitionSource", value = "This parameter specifies the source of a definition. When this parameter is specified the search occurs in the definition and alt_definition properties of the concepts", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "format", value = "This parameter specifies the format of the output. Valid values: clean, cleanWithHighlights, raw", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "fromRecord", value = "This parameter specifies the start record number. Used for paging the results", required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "This parameter specifies the number of records to be returned from the start record specified by the fromRecord parameter. Used for paging the results", required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "property", value = "List of comma separated properties to be searched for the given term. e.g P107,P108. <a href='api/v1/propertiesList' target='_blank'>Click here for a list of properties.</a>. The properties can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "relationship", value = "List of comma separated relationship to be searched for the given term. This parameter is used in combination with the parameters:associationSearch, roleSearch. If roleSearch, then <a href='api/v1/rolesList' target='_blank'>click here for role relationship list</a>. if associationSearch then <a href='api/v1/associationsList' target='_blank'>click here for association relationship list.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "returnProperties", value = "List of comma separated properties to be returned for the given term search. e.g P107,P108,roles. <a href='api/v1/propertiesList' target='_blank'>Click here for a list of properties</a>. The properties can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "roleSearch", value = "This paramter controls if the search happens in the roles or inverse roles", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymGroup", value = "This parameter specifies the group of a synonym. This parameter is used in combination with the parameters:synonymSource. This parameter restricts the search to occur in synonyms in the specified group.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymSource", value = "This parameter specifies the source of a synonym. This parameter restricts the search to occur in synonyms in the specified source.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "pageSize", value = "This parameter specifies the number of records to be returned from the start record specified by the fromRecord parameter. Used for paging the results", required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "term", value = "The term/phrase to be searched", required = true, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "type", value = "The type can be contains,match,startswith,phrase,AND,OR,fuzzy. If no type is specified , the search will be of the type contains.", required = false, dataType = "string", paramType = "query", defaultValue = "contains"),

  })
  @RecordMetricSearch
  @RequestMapping(method = RequestMethod.GET, value = "/elasticsearch", produces = "application/json")
  public @ResponseBody String elasticsearch(
    @ModelAttribute FilterCriteriaElasticFields filterCriteriaElasticFields,
    BindingResult bindingResult, HttpServletResponse response)
    throws IOException {
    if (bindingResult.hasErrors()) {
      log.debug("Error " + bindingResult.getObjectName());
      List<FieldError> errors = bindingResult.getFieldErrors();
      String errorMessage = "";
      for (FieldError error : errors) {
        log.debug("field name :" + error.getField());
        log.debug("Error Code :" + error.getCode());
        if (error.getCode().equalsIgnoreCase("typeMismatch")) {
          errorMessage =
              errorMessage + "Could not convert the value of the field "
                  + error.getField() + " to the expected type. Details: "
                  + error.getDefaultMessage() + ". ";
        }

      }
      int statusCode = HttpServletResponse.SC_BAD_REQUEST;
      log.error("returning status code " + statusCode + " with error message "
          + errorMessage);
      response.sendError(statusCode, errorMessage);
      return "";
    }
    String result = "";
    String queryTerm = filterCriteriaElasticFields.getTerm();
    if (queryTerm == null) {
      return null;
    }
    queryTerm = RESTUtils.escapeLuceneSpecialCharacters(queryTerm);
    filterCriteriaElasticFields.setTerm(queryTerm);
    log.debug("Term/Partial Term - " + filterCriteriaElasticFields.getTerm());
    log.debug("Type - " + filterCriteriaElasticFields.getType());
    log.debug("From Record - " + filterCriteriaElasticFields.getFromRecord());
    log.debug("Page size - " + filterCriteriaElasticFields.getPageSize());

    try {
      result = elasticSearchService.elasticsearch(filterCriteriaElasticFields);
    } catch (IOException exception) {
      int statusCode = HttpServletResponse.SC_BAD_REQUEST;
      String errorMessage = exception.getMessage();
      log.error("returning status code " + statusCode + " with error message "
          + errorMessage);
      response.sendError(statusCode, errorMessage);
    } catch (HttpClientErrorException httpClientErrorException) {
      int statusCode = httpClientErrorException.getStatusCode().value();
      String errorMessage = httpClientErrorException.getMessage();
      log.error("returning status code " + statusCode + " with error message "
          + errorMessage);
      response.sendError(statusCode, errorMessage);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      String errorMessage =
          "An error occurred in the system. Please contact the NCI help desk.";
      log.error("returning status code " + statusCode + " with error message "
          + e.getMessage());
      response.sendError(statusCode, errorMessage);
    }
    return result;
  }

  /**
   * Search within a single terminology.
   *
   * @param terminology the terminology
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get concept search results", notes = "TBD"

  )
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Comma-separated list of terminologies to search", required = true, dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "term", value = "The term, phrase, or code to be searched", required = true, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "type", value = "The match type contains, match, startswith, phrase, AND, OR, fuzzy.", required = false, dataType = "string", paramType = "query", defaultValue = "contains"),
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results", required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return", required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "conceptStatus", value = "Comma-separated list of concept status values to restrict search results to. <a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a list of NCI Thesaurus values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "property", value = "Comma-separated list of properties to search. e.g P107,P108. <a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a list of NCI Thesaurus properties.</a>.The properties can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "contributingSource", value = "Comma-separated list of contributing sources to restrict search results to. <a href='api/v1/metadata/ncit/contributingSources' target='_blank'>Click here for a list of NCI Thesaurus values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "definitionSource", value = "Comma-separated list of definition sources to restrict search results to.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymSource", value = "Comma-separated list of synonym sources to restrict search results to.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymTermGroup", value = "Comma-separated list of synonym term groups to restrict search results to. Use with \"synonymSource\".", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "inverse", value = "Used with \"associations\" or \"roles\" when true to indicate that inverse associations or roles should be searched", required = false, dataType = "string", paramType = "query", defaultValue = "false"),
      @ApiImplicitParam(name = "association", value = "Comma-separated list of associations to search. e.g A10,A215. <a href='api/v1/metadata/ncit/associations' target='_blank'>Click here for a list of NCI Thesaurus associations.</a>. The associations can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "role", value = "Comma-separated list of roles to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles' target='_blank'>Click here for a list of NCI Thesaurus roles.</a>. The roles can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = "")
  })
  @RecordMetricSearch
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/search", produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminology(
    @PathVariable(value = "terminology") final String terminology,
    @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria,
    BindingResult bindingResult) throws IOException {
    return search(new SearchCriteria(searchCriteria, terminology),
        bindingResult);
  }

  /**
   * Search.
   *
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get concept search results", notes = "TBD"

  )
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Comma-separated list of terminologies to search", required = true, dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "term", value = "The term, phrase, or code to be searched", required = true, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "type", value = "The match type contains, match, startswith, phrase, AND, OR, fuzzy.", required = false, dataType = "string", paramType = "query", defaultValue = "contains"),
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results", required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return", required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "conceptStatus", value = "Comma-separated list of concept status values to restrict search results to. <a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a list of NCI Thesaurus values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "property", value = "Comma-separated list of properties to search. e.g P107,P108. <a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a list of NCI Thesaurus properties.</a>.The properties can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "contributingSource", value = "Comma-separated list of contributing sources to restrict search results to. <a href='api/v1/metadata/ncit/contributingSources' target='_blank'>Click here for a list of NCI Thesaurus values.</a>", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "definitionSource", value = "Comma-separated list of definition sources to restrict search results to.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymSource", value = "Comma-separated list of synonym sources to restrict search results to.", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymTermGroup", value = "Comma-separated list of synonym term groups to restrict search results to. Use with \"synonymSource\".", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "inverse", value = "Used with \"associations\" or \"roles\" when true to indicate that inverse associations or roles should be searched", required = false, dataType = "string", paramType = "query", defaultValue = "false"),
      @ApiImplicitParam(name = "association", value = "Comma-separated list of associations to search. e.g A10,A215. <a href='api/v1/metadata/ncit/associations' target='_blank'>Click here for a list of NCI Thesaurus associations.</a>. The associations can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "role", value = "Comma-separated list of roles to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles' target='_blank'>Click here for a list of NCI Thesaurus roles.</a>. The roles can be specified as code or label", required = false, dataType = "string", paramType = "query", defaultValue = "")
  })
  @RecordMetricSearch
  @RequestMapping(method = RequestMethod.GET, value = "/concept/search", produces = "application/json")
  public @ResponseBody ConceptResultList search(
    @ModelAttribute SearchCriteria searchCriteria, BindingResult bindingResult)
    throws IOException {

    // Check whether or not parameter binding was successful
    if (bindingResult.hasErrors()) {
      final List<FieldError> errors = bindingResult.getFieldErrors();
      final List<String> errorMessages = new ArrayList<>();
      for (final FieldError error : errors) {
        final String errorMessage = "ERROR " + bindingResult.getObjectName()
            + " = " + error.getField() + ", " + error.getCode();
        log.error(errorMessage);
        errorMessages.add(errorMessage);
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.join("\n ", errorMessages));
    }

    if (!searchCriteria.checkRequiredFields()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Missing required field = "
              + searchCriteria.computeMissingRequiredFields());
    }
    searchCriteria.validate(thesaurusProperties);

    final String queryTerm =
        RESTUtils.escapeLuceneSpecialCharacters(searchCriteria.getTerm());
    searchCriteria.setTerm(queryTerm);
    log.debug("  Search = " + searchCriteria);

    if (searchCriteria.getTerminology().size() > 1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Search currently supports only terminology ncit");
    } else if (searchCriteria.getTerminology().size() == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Required parameter 'terminology' is missing");
    }

    try {
      final String terminology = searchCriteria.getTerminology().get(0);
      final Terminology term = TerminologyUtils
          .getTerminology(sparqlQueryManagerService, terminology);
      final String dbType =
          "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
      final IncludeParam ip = searchCriteria.computeIncludeParam();

      final ConceptResultList results =
          elasticSearchService.search(searchCriteria);

      // Look up info for all the concepts

      final List<Concept> concepts = new ArrayList<>();
      for (final Concept result : results.getConcepts()) {
        final Concept concept =
            ConceptUtils.convertConcept(sparqlQueryManagerService
                .getEvsConceptByCode(result.getCode(), dbType, ip));
        ConceptUtils.applyHighlights(concept, result.getHighlights());
        concept.setHighlights(null);
        concepts.add(concept);
      }
      results.setConcepts(concepts);

      return results;
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (IOException e) {
      log.error("Unexpected IO error", e);
      String errorMessage = e.getMessage();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);

    } catch (HttpClientErrorException e2) {
      log.error("Unexpected HTTP error", e2);
      final String errorMessage = e2.getMessage();
      throw new ResponseStatusException(e2.getStatusCode(), errorMessage);

    } catch (Exception e3) {
      log.error("Unexpected error", e3);
      final String errorMessage =
          "An error occurred in the system. Please contact the NCI help desk.";
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          errorMessage);
    }

  }
}