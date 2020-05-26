package gov.nih.nci.evs.api.support.es;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;

/**
 * Custom concept result mapper to extract highlights from search hits
 * 
 * @author Arun
 *
 */
public class EVSConceptResultMapper implements SearchResultMapper {
  
  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(EVSConceptResultMapper.class);
  
  /** the object mapper **/
  private ObjectMapper mapper;
  
  public EVSConceptResultMapper() {
    this.mapper = new ObjectMapper();
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
    if (response.getHits().getHits().length <= 0) {
      return new AggregatedPageImpl(Collections.emptyList(), pageable, 0L);
    }
    
    List<Concept> content = new ArrayList<Concept>();
    for (SearchHit searchHit : response.getHits()) {
      Concept concept = (Concept) mapSearchHit(searchHit, clazz);
      content.add(concept);
    }

    return new AggregatedPageImpl((List<T>) content, pageable, 
        response.getHits().getTotalHits(), response.getAggregations(), 
        response.getScrollId(), response.getHits().getMaxScore());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T mapSearchHit(SearchHit searchHit, Class<T> clazz) {
    Concept concept = null;
    try {
      concept = mapper.readValue(searchHit.getSourceAsString(), Concept.class);

      Map<String, HighlightField> highlightMap = searchHit.getHighlightFields();
      for (String key : highlightMap.keySet()) {
        HighlightField field = highlightMap.get(key);
        for (Text text : field.getFragments()) {
          String highlight = text.string();
          concept.getHighlights().put(highlight.replaceAll("<em>", "").replaceAll("</em>", ""), highlight);
        }
      }

    } catch (JsonProcessingException e) {
      logger.error(e.getMessage(), e);
    }
    
    return (T) concept;
  }
}
