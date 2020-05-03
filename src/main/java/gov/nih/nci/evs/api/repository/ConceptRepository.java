package gov.nih.nci.evs.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import gov.nih.nci.evs.api.model.Concept;

@Repository
public interface ConceptRepository extends ElasticsearchRepository<Concept, String> {
  @Query("{\"query_string\" : {\"query\" : \"?0\", \"default_operator\" : \"AND\"}}")
  Page<Concept> searchTopLevelOnly(String q, Pageable pageable);
  
  @Query("{" + 
      "    \"query\": {" + 
      "        \"bool\": {" + 
      "            \"should\": [" + 
      "                {" + 
      "                    \"query_string\": {" + 
      "                        \"query\": \"melanoma astrocytoma\"," + 
      "                        \"default_operator\": \"AND\"" + 
      "                    }" + 
      "                }," + 
      "                {" + 
      "                    \"nested\": {" + 
      "                        \"query\": {" + 
      "                            \"query_string\": {" + 
      "                                \"query\": \"melanoma astrocytoma\"," + 
      "                                \"default_operator\": \"AND\"" + 
      "                            }" + 
      "                        }," + 
      "                        \"path\": \"properties\"" + 
      "                    }" + 
      "                }," + 
      "                {" + 
      "                    \"nested\": {" + 
      "                        \"query\": {" + 
      "                            \"query_string\": {" + 
      "                                \"query\": \"melanoma astrocytoma\"," + 
      "                                \"default_operator\": \"AND\"" + 
      "                            }" + 
      "                        }," + 
      "                        \"path\": \"synonyms\"" + 
      "                    }" + 
      "                }," + 
      "                {" + 
      "                    \"nested\": {" + 
      "                        \"query\": {" + 
      "                            \"query_string\": {" + 
      "                                \"query\": \"melanoma astrocytoma\"," + 
      "                                \"default_operator\": \"AND\"" + 
      "                            }" + 
      "                        }," + 
      "                        \"path\": \"definitions\"" + 
      "                    }" + 
      "                }" + 
      "            ]" + 
      "        }" + 
      "    }" + 
      "}")
  Page<Concept> search(String q, Pageable pageable);
}
