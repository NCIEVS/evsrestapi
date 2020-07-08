
package gov.nih.nci.evs.api.aop;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Metric;
import gov.nih.nci.evs.api.properties.ElasticServerProperties;

/**
 * Handle record metric annotations via AOP.
 */
@Component
@Aspect
@ConditionalOnProperty(name = "nci.evs.application.metricsEnabled")
public class MetricAdvice {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(MetricAdvice.class);

  /** The elastic server properties. */
  @Autowired
  ElasticServerProperties elasticServerProperties;

  /** The mapper. */
  private static ObjectMapper mapper = initMapper();

  /**
   * Inits the mapper.
   *
   * @return the object mapper
   */
  private static ObjectMapper initMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    return mapper;
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
    final long startTime = System.currentTimeMillis();
    final Date startDate = new Date();
    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;
    final Date endDate = new Date();
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

    metric.setQueryParams(params);
    metric.setStartTime(startDate);
    metric.setEndTime(endDate);

    // get the parameters
    final RestTemplate restTemplate = new RestTemplate();
    final String metricStr = mapper.writeValueAsString(metric);

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    final HttpEntity<String> metricData = new HttpEntity<String>(metricStr, headers);
    restTemplate.postForObject(elasticServerProperties.getUrl(), metricData, String.class);
    logger.debug("metric = " + metric);
    return pjp.proceed();
  }

}
