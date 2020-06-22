package gov.nih.nci.evs.api.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import gov.nih.nci.evs.api.support.es.ElasticObject;

/**
 * Repository for {@code ElasticObject} entity
 * 
 * Spring data creates default index (if not present) on repository bootstrapping
 * 
 * @author Arun
 *
 */
@Repository
public interface ElasticObjectRepository extends CrudRepository<ElasticObject, String> {

}
