
package gov.nih.nci.evs.api.aop;

import java.io.File;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import gov.nih.nci.evs.api.model.GeoIP;
import gov.nih.nci.evs.api.model.Location;
import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;
import gov.nih.nci.evs.api.service.ElasticOperationsService;

/**
 * Handle record metric annotations via AOP.
 */
@Component
@Aspect
@ConditionalOnProperty(name = "nci.evs.application.metricsEnabled")
public class MetricAdvice {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(MetricAdvice.class);

  /** the geoIP location database */
  DatabaseReader dbReader = null;


  /** The elastic server properties. */
  @Autowired
  ElasticServerProperties elasticServerProperties;

  /** The operations service. */
  @Autowired
  ElasticOperationsService operationsService;

  @PostConstruct
  public void postInit() throws Exception {
    dbReader = new DatabaseReader.Builder(new File("GeoLite2-City.mmdb")).build();
  }

  /**
   * Record metric.
   *
   * @param pjp the pjp
   * @param recordMetric the record metric
   * @return the object
   * @throws Throwable the throwable
   */

  @Around("execution(* gov.nih.nci.evs.api.controller.*.*(..)) && @annotation(recordMetric)")
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
  public Object recordMetricHelper(final ProceedingJoinPoint pjp, final HttpServletRequest request,
    final Map<String, String[]> params) throws Throwable {

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

    final String userIpAddress = "2601:205:c380:2ef0:3105:58fa:23f5:efe2";
    // final String userIpAddress = attr.getRequest().getRemoteAddr();
    metric.setRemoteIpAddress(userIpAddress);

    final String hostName = attr.getRequest().getRemoteHost();
    metric.setHostName(hostName);

    final String url = request.getRequestURL().toString();
    metric.setEndPoint(url);

    try {
      CityResponse response = dbReader.city(InetAddress.getByName(userIpAddress));
      GeoIP geoip = new GeoIP(response.getCity().getName(),
                              response.getCountry().getName(),
                              response.getLeastSpecificSubdivision().getName(),
                              response.getContinent().getName(),
                              new Location(response.getLocation().getLatitude().toString(),
                              response.getLocation().getLongitude().toString()));
      metric.setGeoip(geoip);
    } catch (Exception e) {
      logger.warn("GeoIP could not find IP");
    }

    metric.setQueryParams(params);
    metric.setStartTime(startDate);
    metric.setEndTime(endDate);

    // get the parameters
    operationsService.loadMetric(metric,
        "metrics-" + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + "-"
            + String.valueOf(Calendar.getInstance().get(Calendar.MONTH)));

    logger.debug("metric = " + metric);
    return retval;
  }

}
