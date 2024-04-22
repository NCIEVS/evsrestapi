package gov.nih.nci.evs.api.repository;

import gov.nih.nci.evs.api.support.es.ElasticObject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code ElasticObject} entity
 *
 * <p>Spring data creates default index (if not present) on repository bootstrapping
 *
 * @author Arun
 */
@Repository
public interface ElasticObjectRepository extends CrudRepository<ElasticObject, String> {}
