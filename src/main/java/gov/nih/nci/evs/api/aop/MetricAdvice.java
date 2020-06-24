
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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Metric;

/**
 * Metric advice.
 */
@Component
@Aspect
public class MetricAdvice {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetricAdvice.class);

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
    log.info("log method having db as parameter");

    // get the request
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    Map<String, String[]> filterParams = request.getParameterMap();

    return recordMetricHelper(pjp, request, filterParams);

  }

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

    log.info("duration = " + String.valueOf(duration));
    metric.setDuration(duration);

    // get the ip address of the remote user
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

    String userIpAddress = attr.getRequest().getRemoteAddr();
    metric.setRemoteIpAddress(userIpAddress);
    log.info("userIpAddress = " + userIpAddress);

    String hostName = attr.getRequest().getRemoteHost();
    metric.setHostName(hostName);
    log.info("hostName = " + hostName);

    String url = request.getRequestURL().toString();
    metric.setEndPoint(url);
    log.info("url = " + url);
    
    metric.setQueryParams(params);
    log.info("params = " + params);
    

    metric.setStartTime(startDate);
    metric.setEndTime(endDate);

    // get the parameters
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    String metricStr = mapper.writeValueAsString(metric);
    log.info("metric = " + metricStr);

    return retVal;
  }

}
