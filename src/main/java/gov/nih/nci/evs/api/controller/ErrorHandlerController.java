package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Handler for errors when accessing API thru browser. */
@Controller
@RequestMapping("/error")
@Hidden
public class ErrorHandlerController implements ErrorController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerController.class);

  /** The error attributes. */
  private ErrorAttributes errorAttributes;

  /** Separates the failures in a validation message. */
  private static final String VALIDATION_FAILURE_SEPARATOR = "; ";

  /** Separates a field's name from what's wrong with it in a validation message. */
  private static final String FIELD_SEPARATOR = ": ";

  /** The error attribute options to include stack trace. */
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
    final Map<String, Object> body = getErrorAttributes(request, options);
    final String statusCode = body.get("status") == null ? null : body.get("status").toString();

    // Identify the "message" field of the body and escape < to &lt;
    // This is to handle netsparker injection test
    // e.g.  /api/v1/concept/ncit/%22%3e%3ciMg%20src%3dN%20onerror%3dnetsparker(0x000009)%3
    if (body.containsKey("message")) {
      body.put(
          "message",
          body.get("message").toString().replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
    }
    String ppBody = null;
    try {
      ppBody = ThreadLocalMapper.get().writerWithDefaultPrettyPrinter().writeValueAsString(body);
    } catch (Exception e) {
      ppBody = body.toString();
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
    Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
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
    final Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest, options);

    final Throwable error = errorAttributes.getError(webRequest);
    // Validation failures name each field and what's wrong with it, since the exception's own
    // message only says validation failed. To pass any other error through, use
    // ResponseStatusException
    final String validationMessage = getValidationMessage(error);
    final boolean isValidationFailure = validationMessage != null;
    if (isValidationFailure) {
      body.put("message", validationMessage);
    } else if (error instanceof ResponseStatusException) {
      body.put("message", ((ResponseStatusException) error).getReason());
    } else {
      body.put("message", error == null ? "No message" : error.getMessage());
    }

    // This is cleanup for various situations.
    // This is likely no longer be needed (removed 202411)
    //    if (body.containsKey("message")) {
    //      try {
    //        final String message = body.get("message").toString();
    //        final StringBuilder sb = new StringBuilder();
    //        for (final String line : message.split("\\n")) {
    //          sb.append(StringEscapeUtils.escapeHtml4(line));
    //          sb.append("\n");
    //        }
    //        // If the message is a stack trace, then obscure it
    //        if (sb.toString().contains("Exception")) {
    //          logger.error("path = " + body.get("path").toString());
    //          logger.error("queryString = " + request.getQueryString());
    //          logger.error("message = " + sb.toString());
    //          body.put("message", "Unexpected error, see logs for more info");
    //        } else {
    //          // remove the trailing \n
    //          body.put("message", sb.toString().replaceFirst("\\n$", ""));
    //        }
    //      } catch (Exception e) {
    //        body.put("message", body.get("message").toString().replaceAll("<", "&lt;"));
    //      }
    //    }

    // Remove the "trace" field from HTML
    body.remove("trace");
    return body;
  }

  /**
   * Returns the error path.
   *
   * @return the error path
   */
  /**
   * Returns a message naming each parameter or field that failed validation and why, e.g.
   * "pageSize: must be less than or equal to 1000; role: must not be null", or null if the error
   * isn't a validation failure. A parameter Spring couldn't convert, e.g. an unknown include value,
   * counts as failing validation.
   *
   * @param error the error, or null
   * @return the message, or null
   */
  private static String getValidationMessage(final Throwable error) {
    if (error instanceof MethodArgumentTypeMismatchException) {
      final MethodArgumentTypeMismatchException mismatch =
          (MethodArgumentTypeMismatchException) error;
      return mismatch.getName()
          + FIELD_SEPARATOR
          + NestedExceptionUtils.getMostSpecificCause(mismatch).getMessage();
    }
    if (error instanceof MethodValidationResult) {
      return getMethodValidationMessage((MethodValidationResult) error);
    }
    if (error instanceof BindingResult) {
      return getFieldErrorsMessage(((BindingResult) error).getFieldErrors());
    }
    return null;
  }

  /**
   * Returns a message naming each request parameter or body field that failed validation. A
   * parameter is named by its parameter name; a field of the request body by its path in the body,
   * e.g. "children[0].role".
   *
   * @param result the method validation result
   * @return the message
   */
  private static String getMethodValidationMessage(final MethodValidationResult result) {
    final List<String> failures = new ArrayList<>();
    for (final ParameterValidationResult parameterResult : result.getParameterValidationResults()) {
      failures.add(getParameterValidationMessage(parameterResult));
    }
    return String.join(VALIDATION_FAILURE_SEPARATOR, failures);
  }

  /**
   * Returns a message naming what failed validation in one parameter: the fields of a body or other
   * object, or the parameter itself.
   *
   * @param parameterResult the parameter's validation result
   * @return the message
   */
  private static String getParameterValidationMessage(
      final ParameterValidationResult parameterResult) {
    if (parameterResult instanceof ParameterErrors) {
      return getFieldErrorsMessage(((ParameterErrors) parameterResult).getFieldErrors());
    }

    final String parameterName = parameterResult.getMethodParameter().getParameterName();
    final List<String> failures = new ArrayList<>();
    for (final MessageSourceResolvable failure : parameterResult.getResolvableErrors()) {
      failures.add(parameterName + FIELD_SEPARATOR + failure.getDefaultMessage());
    }
    return String.join(VALIDATION_FAILURE_SEPARATOR, failures);
  }

  /**
   * Returns a message naming each field that failed validation and why.
   *
   * @param fieldErrors the field errors
   * @return the message
   */
  private static String getFieldErrorsMessage(final List<FieldError> fieldErrors) {
    final List<String> failures = new ArrayList<>();
    for (final FieldError fieldError : fieldErrors) {
      failures.add(fieldError.getField() + FIELD_SEPARATOR + fieldError.getDefaultMessage());
    }
    return String.join(VALIDATION_FAILURE_SEPARATOR, failures);
  }

  /* see superclass */
  public String getErrorPath() {
    return "/error";
  }
}
