
package gov.nih.nci.evs.api.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler for errors when accessing API thru browser.
 */
@Controller
@RequestMapping("/error")
public class ErrorHandlerController implements ErrorController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerController.class);

  /** The error attributes. */
  private ErrorAttributes errorAttributes;

  /**
   * Basic error controller.
   *
   * @param errorAttributes the error attributes
   */
  public ErrorHandlerController(ErrorAttributes errorAttributes) {
    this.errorAttributes = errorAttributes;
  }

  /**
   * Handle error.
   *
   * @param request the request
   * @return the string
   */
  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String handleErrorHtml(final HttpServletRequest request) {
    final Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    final Map<String, Object> body = getErrorAttributes(request, false);
    String ppBody = null;
    try {
      ppBody = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body);
    } catch (Exception e) {
      ppBody = body.toString();
    }

    return String.format("<html><body><h2>Error Page</h2><div>Something went wrong, "
        + "<a href=\"https://datascience.cancer.gov/about/application-support\">"
        + "please contact the NCI helpdesk</a></div><div>Status code: <b>%s</b></div>"
        + "<div>Message: <pre>%s</pre></div><body></html>", statusCode, ppBody);
  }

  /**
   * Handle error json.
   *
   * @param request the request
   * @return the response entity
   */
  @RequestMapping()
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleErrorJson(HttpServletRequest request) {
    HttpStatus status = getStatus(request);
    if (status == HttpStatus.NO_CONTENT) {
      return new ResponseEntity<>(status);
    }
    Map<String, Object> body = getErrorAttributes(request, false);
    return new ResponseEntity<>(body, status);
  }

  /**
   * Returns the status.
   *
   * @param request the request
   * @return the status
   */
  protected HttpStatus getStatus(HttpServletRequest request) {
    Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
    if (statusCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    try {
      return HttpStatus.valueOf(statusCode);
    } catch (Exception ex) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Returns the error attributes.
   *
   * @param request the request
   * @param includeStackTrace the include stack trace
   * @return the error attributes
   */
  protected Map<String, Object> getErrorAttributes(HttpServletRequest request,
    boolean includeStackTrace) {
    WebRequest webRequest = new ServletWebRequest(request);
    Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest, includeStackTrace);
    if (body.containsKey("message")) {
      try {
        final String message = body.get("message").toString();
        final String messagePre = message.replaceFirst("^([a-zA-Z '\"=]+).*", "$1");
        final String messagePost = message.replaceFirst("^[a-zA-Z '\"=]+(.*)", "$1");
        body.put("message", messagePre + StringEscapeUtils.escapeHtml4(messagePost));
      } catch (Exception e) {
        body.put("message", body.get("message").toString());
      }
    }
    return body;
  }

  /* see superclass */
  @Override
  public String getErrorPath() {
    return "/error";
  }

}
