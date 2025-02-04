package gov.nih.nci.evs.api.aop;

import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Handle record metric annotations via AOP. */
@Component
@Aspect
@ConditionalOnProperty(name = "nci.evs.application.metricsEnabled")
public class MetricAdvice {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(MetricAdvice.class);

  /** the geoIP location database. */
  // DatabaseReader dbReader = null;

  /** the metrics db path. */
  @Autowired ApplicationProperties applicationProperties;

  /** The elastic server properties. */
  @Autowired ElasticServerProperties elasticServerProperties;

  /** The operations service. */
  @Autowired ElasticOperationsService operationsService;

  /** The database found. */
  boolean databaseFound;

  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {

    // NO longer do this
    /**
     * File file = new File(applicationProperties.getMetricsDir() + "/GeoLite2-City.mmdb");
     * this.databaseFound = file.exists(); if (databaseFound) { dbReader = new
     * DatabaseReader.Builder(file).build(); } else { logger.warn("GeoLite Database was not found =
     * " + applicationProperties.getMetricsDir()); }
     *
     * <p>String indexName = "metrics-" + String.valueOf(Calendar.getInstance().get(Calendar.YEAR))
     * + "-" + String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);
     *
     * <p>boolean result = operationsService.createIndex(indexName, false); if (result) {
     * operationsService.getOpenSearchOperations().putMapping(indexName,
     * ElasticOperationsService.METRIC_TYPE, Metric.class); }
     */
  }

  /**
   * Record metric.
   *
   * @param pjp the pjp
   * @param recordMetric the record metric
   * @return the object
   * @throws Throwable the throwable
   */
  // @Around("execution(* gov.nih.nci.evs.api.controller.*.*(..)) && @annotation(recordMetric)")
  @SuppressWarnings("unused")
  private Object recordMetric(final ProceedingJoinPoint pjp, final RecordMetric recordMetric)
      throws Throwable {

    // get the request
    final HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    return recordMetricHelper(pjp, request, request.getParameterMap());
  }

  /**
   * Record metric helper.
   *
   * @param pjp the pjp
   * @param request the request
   * @param params the params
   * @return the object
   * @throws Throwable the throwable
   */
  public Object recordMetricHelper(
      final ProceedingJoinPoint pjp,
      final HttpServletRequest request,
      final Map<String, String[]> params)
      throws Throwable {

    // get the start time
    final Date startDate = new Date();
    Object retval = pjp.proceed();

    final Date endDate = new Date();
    final long duration = endDate.getTime() - startDate.getTime();
    final Metric metric = new Metric();

    metric.setDuration(duration);

    // get the ip address of the remote user
    final ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

    final String userIpAddress = attr.getRequest().getRemoteAddr();
    metric.setRemoteIpAddress(userIpAddress);

    final String hostName = attr.getRequest().getRemoteHost();
    metric.setHostName(hostName);

    final String url = request.getRequestURL().toString();
    metric.setEndPoint(url);

    // NO LONGER USE
    //    if (this.databaseFound) {
    //      logger.info("database found: " + this.databaseFound);
    //      try {
    //        CityResponse response = dbReader.city(InetAddress.getByName(userIpAddress));
    //        metric.setGeoPoint(new GeoPoint(response.getLocation().getLatitude(),
    // response.getLocation().getLongitude()));
    //      } catch (Exception e) {
    //        logger.warn("GeoPoint could not find IP");
    //      }
    //    }

    metric.setQueryParams(params);
    metric.setStartTime(startDate);
    metric.setEndTime(endDate);

    // get the parameters
    operationsService.loadMetric(
        metric,
        "metrics-"
            + String.valueOf(Calendar.getInstance().get(Calendar.YEAR))
            + "-"
            + String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1));

    logger.debug("metric = " + metric);
    return retval;
  }
}
