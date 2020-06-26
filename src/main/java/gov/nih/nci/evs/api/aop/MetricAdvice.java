
package gov.nih.nci.evs.api.aop;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Metric advice.
 */
@Component
@Aspect
public class MetricAdvice {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetricAdvice.class);

  @Autowired
  ElasticServerProperties elasticServerProperties;

  /**
   * Record metric.
   *
   * @param pjp the pjp
   * @param request the request
   * @param params the params
   * @return the object
   * @throws Throwable the throwable
   */

  @Around("execution(* gov.nih.nci.evs.api.controller.*.*(..)) && @annotation(recordMetric)")
  private Object recordMetric(ProceedingJoinPoint pjp, RecordMetric recordMetric)
    throws Throwable {

    // get the request
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    Map<String, String[]> filterParams = request.getParameterMap();

    return recordMetricHelper(pjp, request, filterParams);

  }

  // unchecked conversion doesn't matter unless 6.7 somehow changes the format of the response
  @SuppressWarnings("unchecked")
  public Object recordMetricHelper(ProceedingJoinPoint pjp, HttpServletRequest request, Map<String, String[]> params)
    throws Throwable {

    // get the start time
    long startTime = System.currentTimeMillis();
    Date startDate = new Date();
    Object retVal = pjp.proceed();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    Date endDate = new Date();
    Metric metric = new Metric();

    metric.setDuration(duration);

    // get the ip address of the remote user
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

    String userIpAddress = attr.getRequest().getRemoteAddr();
    metric.setRemoteIpAddress(userIpAddress);

    String hostName = attr.getRequest().getRemoteHost();
    metric.setHostName(hostName);

    String url = request.getRequestURL().toString();
    metric.setEndPoint(url);
    
    metric.setQueryParams(params);
    metric.setStartTime(startDate);
    metric.setEndTime(endDate);

    // get the parameters
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    String metricStr = mapper.writeValueAsString(metric);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> metricData = new HttpEntity<String>(metricStr, headers);
    String elasticSearchUrl = elasticServerProperties.getUrl();
    String response = restTemplate.postForObject(elasticSearchUrl.replace("concept/_search","metrics/_doc/"), metricData, String.class);
    Map<String, Object> map=new HashMap<String, Object>();
    map = mapper.readValue(response, HashMap.class);
    log.info("metrics object id = " + map.get("_id"));
    log.info(restTemplate.getForObject(elasticSearchUrl.replace("concept/_search","metrics/_doc/") + map.get("_id"), String.class));
    return retVal;
  }

}
