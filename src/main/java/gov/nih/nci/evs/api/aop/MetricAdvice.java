
package gov.nih.nci.evs.api.aop;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

  /** The Constant EVSRESTAPI_APPLICATION. */
  private static final String EVSRESTAPI_APPLICATION = "evsrestapi";

  /**
   * Record metric DB.
   *
   * @param pjp the pjp
   * @param recordMetricDB the record metric DB
   * @return the object
   * @throws Throwable the throwable
   */
  @Around("execution(* gov.nih.nci.evs.api.controller.*.*(..)) && @annotation(recordMetricDB)")
  private Object recordMetricDB(ProceedingJoinPoint pjp, RecordMetricDB recordMetricDB)
    throws Throwable {
    log.debug("log method having db as parameter");

    // get the request
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    String filterParams = request.getParameter("db");

    return recordMetric(pjp, request, filterParams);

  }

  /**
   * Record metric DB format.
   *
   * @param pjp the pjp
   * @param recordMetricDBFormat the record metric DB format
   * @return the object
   * @throws Throwable the throwable
   */
  @Around("execution(* gov.nih.nci.evs.api.controller.*.*(..)) && @annotation(recordMetricDBFormat)")
  private Object recordMetricDBFormat(ProceedingJoinPoint pjp,
    RecordMetricDBFormat recordMetricDBFormat) throws Throwable {
    log.debug("log method having db and format as parameter");

    // get the request
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    String filterParams = "{\"db\":\"" + request.getParameter("db") + "\",\"fmt\":\""
        + request.getParameter("fmt") + "\"}";

    return recordMetric(pjp, request, filterParams);

  }

  /**
   * Record metric.
   *
   * @param pjp the pjp
   * @param request the request
   * @param params the params
   * @return the object
   * @throws Throwable the throwable
   */
  public Object recordMetric(ProceedingJoinPoint pjp, HttpServletRequest request, String params)
    throws Throwable {

    // get the start time
    long startTime = System.currentTimeMillis();
    Date startDate = new Date();
    Object retVal = pjp.proceed();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    Date endDate = new Date();
    log.debug("durtaion = " + String.valueOf(duration));

    // get the ip address of the remote user
    ServletRequestAttributes attr =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    String userIpAddress = attr.getRequest().getRemoteAddr();
    log.debug("userIpAddress" + userIpAddress);

    String applicationName = EVSRESTAPI_APPLICATION;
    log.debug("applicationName - " + applicationName);
    Metric metric = new Metric();
    metric.setApplicationName(applicationName);
    metric.setEndPoint(request.getRequestURL().toString());
    log.debug("url -" + request.getRequestURL().toString());
    metric.setQueryParams(params);
    log.debug("params - " + params);
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    metric.setStartTime(dateFormat.format(startDate));
    metric.setEndTime(dateFormat.format(endDate));
    metric.setUsername("anonymousUser");
    log.debug("username -" + metric.getUsername());
    metric.setDuration(duration);
    metric.setRemoteIpAddress(userIpAddress);

    // get the parameters
    ObjectMapper mapper = new ObjectMapper();
    String metricStr = mapper.writeValueAsString(metric);
    log.debug("metric -" + metricStr);

    return retVal;
  }

}
