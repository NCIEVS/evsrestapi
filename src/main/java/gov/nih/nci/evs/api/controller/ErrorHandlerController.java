package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
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

/** Handler for errors when accessing API thru browser. */
@Controller
@RequestMapping("/error")
@Hidden
public class ErrorHandlerController implements ErrorController {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerController.class);

  /** The error attributes. */
  private ErrorAttributes errorAttributes;

  /** The error attribute options to include stack trace */
  private ErrorAttributeOptions options =
      ErrorAttributeOptions.defaults().including(ErrorAttributeOptions.Include.STACK_TRACE);

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
    final Map<String, Object> body = getErrorAttributes(request, options);
    String ppBody = null;
    try {
      ppBody = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(body);
    } catch (Exception e) {
      ppBody = body.toString().replaceAll("<", "&lt;");
    }

    return String.format(
        "<html><body><h2>Error Page</h2><div>Something went wrong, "
            + "<a href=\"https://datascience.cancer.gov/about/application-support\">"
            + "please contact the NCI helpdesk</a></div><div>Status code: <b>%s</b></div>"
            + "<div>Message: <pre>%s</pre></div><body></html>",
        statusCode, ppBody);
  }

  /**
   * Handle error json.
   *
   * @param request the request
   * @return the response entity
   */
  @RequestMapping()
  @ResponseBody
  public ResponseEntity<RestException> handleErrorJson(HttpServletRequest request) {
    HttpStatus status = getStatus(request);
    if (status == HttpStatus.NO_CONTENT) {
      return new ResponseEntity<>(status);
    }
    final RestException exception = new RestException(getErrorAttributes(request, options));
    return new ResponseEntity<>(exception, status);
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
   * @param options the ErrorAttributeOptions include stack trace
   * @return the error attributes
   */
  protected Map<String, Object> getErrorAttributes(
      HttpServletRequest request, ErrorAttributeOptions options) {
    WebRequest webRequest = new ServletWebRequest(request);
    Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest, options);
    if (body.containsKey("message")) {
      try {
        final String message = body.get("message").toString();
        final StringBuilder sb = new StringBuilder();
        for (final String line : message.split("\\n")) {
          sb.append(StringEscapeUtils.escapeHtml4(line));
          sb.append("\n");
        }
        // If the message is a stack trace, then obscure it
        if (sb.toString().contains("Exception")) {
          logger.error("path = " + body.get("path").toString());
          logger.error("queryString = " + request.getQueryString());
          logger.error("message = " + sb.toString());
          body.put("message", "Unexpected error, see logs for more info");
        } else {
          // remove the trailing \n
          body.put("message", sb.toString().replaceFirst("\\n$", ""));
        }
      } catch (Exception e) {
        body.put("message", body.get("message").toString().replaceAll("<", "&lt;"));
      }
    }
    return body;
  }

  /* see superclass */
  public String getErrorPath() {
    return "/error";
  }
}
