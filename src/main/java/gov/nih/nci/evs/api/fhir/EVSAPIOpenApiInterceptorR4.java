package gov.nih.nci.evs.api.fhir;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_30_40;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_43_50;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.hl7.fhir.r4.model.OperationDefinition.OperationDefinitionParameterComponent;
import org.hl7.fhir.r4.model.OperationDefinition.OperationParameterUse;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.codesystems.DataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.AlwaysValidCacheEntryValidity;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.linkbuilder.AbstractLinkBuilder;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ClassLoaderTemplateResource;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import ca.uhn.fhir.rest.server.IServerConformanceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.util.ExtensionConstants;
import ca.uhn.fhir.util.HapiExtensions;
import ca.uhn.fhir.util.UrlUtil;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * EVSAPI Api interceptor to fix header, inject header auth token. Borrowed from OpenApiInterceptor
 * and modified.
 */
public class EVSAPIOpenApiInterceptorR4 {

  /** The logger. */
  @SuppressWarnings("unused")
  private static Logger logger = LoggerFactory.getLogger(EVSAPIOpenApiInterceptorR4.class);

  /** The Constant FHIR_JSON_RESOURCE. */
  public static final String FHIR_JSON_RESOURCE = "FHIR-JSON-RESOURCE";

  /** The Constant FHIR_XML_RESOURCE. */
  public static final String FHIR_XML_RESOURCE = "FHIR-XML-RESOURCE";

  /** The Constant PAGE_SYSTEM. */
  public static final String PAGE_SYSTEM = "System Level Operations";

  /** The Constant PAGE_ALL. */
  public static final String PAGE_ALL = "All";

  /** The Constant FHIR_CONTEXT_CANONICAL. */
  public static final FhirContext FHIR_CONTEXT_CANONICAL = FhirContext.forR4();

  /** The Constant REQUEST_DETAILS. */
  public static final String REQUEST_DETAILS = "REQUEST_DETAILS";

  /** The my swagger ui version. */
  private final String mySwaggerUiVersion;

  /** The my template engine. */
  private final TemplateEngine myTemplateEngine;

  /** The my flexmark parser. */
  private final Parser myFlexmarkParser;

  /** The my flexmark renderer. */
  private final HtmlRenderer myFlexmarkRenderer;

  /** The my resource path to classpath. */
  private final Map<String, String> myResourcePathToClasspath = new HashMap<>();

  /** The my extension to content type. */
  private final Map<String, String> myExtensionToContentType = new HashMap<>();

  /** The my banner image. */
  private String myBannerImage;

  /** The my css text. */
  private String myCssText;

  /** The my use resource pages. */
  private boolean myUseResourcePages;

  /**
   * Constructor.
   */
  public EVSAPIOpenApiInterceptorR4() {

    mySwaggerUiVersion = initSwaggerUiWebJar();

    myTemplateEngine = new TemplateEngine();
    final ITemplateResolver resolver = new SwaggerUiTemplateResolver();
    myTemplateEngine.setTemplateResolver(resolver);
    final StandardDialect dialect = new StandardDialect();
    myTemplateEngine.setDialect(dialect);

    myTemplateEngine.setLinkBuilder(new TemplateLinkBuilder());

    myFlexmarkParser = Parser.builder().build();
    myFlexmarkRenderer = HtmlRenderer.builder().build();

    initResources();
  }

  /**
   * Inits the resources.
   */
  private void initResources() {
    setBannerImage("EVSAPI.png");
    setUseResourcePages(true);

    addResourcePathToClasspath("/swagger-ui/index.html", "swagger-ui/index.html");
    addResourcePathToClasspath("/swagger-ui/EVSAPI.png", "swagger-ui/EVSAPI.png");
    addResourcePathToClasspath("/swagger-ui/index.css", "swagger-ui/index.css");

    myExtensionToContentType.put(".png", "image/png");
    myExtensionToContentType.put(".css", "text/css; charset=UTF-8");
  }

  /**
   * Adds the resource path to classpath.
   *
   * @param thePath the the path
   * @param theClasspath the the classpath
   */
  protected void addResourcePathToClasspath(final String thePath, final String theClasspath) {
    myResourcePathToClasspath.put(thePath, theClasspath);
  }

  /**
   * Inits the swagger ui web jar.
   *
   * @return the string
   */
  private String initSwaggerUiWebJar() {
    final String mySwaggerUiVersion;
    final Properties props = new Properties();
    final String resourceName = "/META-INF/maven/org.webjars/swagger-ui/pom.properties";
    try (final InputStream resourceAsStream = ClasspathUtil.loadResourceAsStream(resourceName);) {
      props.load(resourceAsStream);
    } catch (final IOException e) {
      throw new ConfigurationException(Msg.code(239) + "Failed to load resource: " + resourceName);
    }
    mySwaggerUiVersion = props.getProperty("version");
    return mySwaggerUiVersion;
  }

  /**
   * Serve swagger ui.
   *
   * @param theRequest the the request
   * @param theResponse the the response
   * @param theRequestDetails the the request details
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
  public boolean serveSwaggerUi(final HttpServletRequest theRequest,
    final HttpServletResponse theResponse, final ServletRequestDetails theRequestDetails)
    throws IOException {
    final String requestPath = theRequest.getPathInfo();
    final String queryString = theRequest.getQueryString();

    if (isBlank(requestPath) || requestPath.equals("/")) {
      if (isBlank(queryString)) {
        final Set<String> highestRankedAcceptValues =
            RestfulServerUtils.parseAcceptHeaderAndReturnHighestRankedOptions(theRequest);
        if (highestRankedAcceptValues.contains(Constants.CT_HTML)) {

          String serverBase = ".";
          if (theRequestDetails.getServletRequest() != null) {
            final IServerAddressStrategy addressStrategy =
                theRequestDetails.getServer().getServerAddressStrategy();
            serverBase =
                addressStrategy.determineServerBase(theRequest.getServletContext(), theRequest);
          }
          final String redirectUrl = theResponse.encodeRedirectURL(serverBase + "/swagger-ui/");
          theResponse.sendRedirect(redirectUrl);
          theResponse.getWriter().close();
          return false;
        }
      }

      return true;
    }

    if (requestPath.startsWith("/swagger-ui/")) {

      return !handleResourceRequest(theResponse, theRequestDetails, requestPath);

    } else if (requestPath.equals("/api-docs")) {

      final OpenAPI openApi = generateOpenApi(theRequestDetails);
      final String response = Yaml.pretty(openApi);

      theResponse.setContentType("text/yaml");
      theResponse.setStatus(200);
      try (final PrintWriter writer = theResponse.getWriter();) {
        writer.write(response);
      }
      theResponse.getWriter().close();
      return false;
    }

    return true;
  }

  /**
   * Handle resource request.
   *
   * @param theResponse the the response
   * @param theRequestDetails the the request details
   * @param requestPath the request path
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected boolean handleResourceRequest(final HttpServletResponse theResponse,
    final ServletRequestDetails theRequestDetails, final String requestPath) throws IOException {
    if (requestPath.equals("/swagger-ui/") || requestPath.equals("/swagger-ui/index.html")) {
      serveSwaggerUiHtml(theRequestDetails, theResponse);
      return true;
    }

    final String resourceClasspath = myResourcePathToClasspath.get(requestPath);
    if (resourceClasspath != null) {
      theResponse.setStatus(200);

      final String extension = requestPath.substring(requestPath.lastIndexOf('.'));
      final String contentType = myExtensionToContentType.get(extension);
      assert contentType != null;
      theResponse.setContentType(contentType);
      try (InputStream resource = ClasspathUtil.loadResourceAsStream(resourceClasspath);
          final ServletOutputStream os = theResponse.getOutputStream()) {
        IOUtils.copy(resource, os);
        theResponse.getOutputStream().close();
      }
      return true;
    }

    final String resourcePath = requestPath.substring("/swagger-ui/".length());

    if (resourcePath.equals("swagger-ui-custom.css") && isNotBlank(myCssText)) {
      theResponse.setContentType("text/css");
      theResponse.setStatus(200);
      try (final PrintWriter writer = theResponse.getWriter();) {
        writer.println(myCssText);
      }
      return true;
    }

    try (
        final InputStream resource = ClasspathUtil.loadResourceAsStream(
            "/META-INF/resources/webjars/swagger-ui/" + mySwaggerUiVersion + "/" + resourcePath);
        final ServletOutputStream os = theResponse.getOutputStream()) {

      if (resourcePath.endsWith(".js") || resourcePath.endsWith(".map")) {
        theResponse.setContentType("application/javascript");
        theResponse.setStatus(200);
        IOUtils.copy(resource, os);
        theResponse.getOutputStream().close();
        return true;
      }

      if (resourcePath.endsWith(".css")) {
        theResponse.setContentType("text/css");
        theResponse.setStatus(200);
        IOUtils.copy(resource, os);
        theResponse.getOutputStream().close();
        return true;
      }

      if (resourcePath.endsWith(".html")) {
        theResponse.setContentType(Constants.CT_HTML);
        theResponse.setStatus(200);
        IOUtils.copy(resource, os);
        theResponse.getOutputStream().close();
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the trailing slash.
   *
   * @param theUrl the the url
   * @return the string
   */
  public String removeTrailingSlash(final String theUrl) {
    if (theUrl == null) {
      return null;
    }
    String url = theUrl;
    while (url != null && url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    if (url.contains("localhost")) {
      return url;
    }
    return url.replaceFirst("http", "https");
  }

  /**
   * If supplied, this field can be used to provide additional CSS text that should be loaded by the
   * swagger-ui page. The contents should be raw CSS text, e.g. <code>
   * BODY { font-size: 1.1em; }
   * </code>
   *
   * @return the css text
   */
  public String getCssText() {
    return myCssText;
  }

  /**
   * If supplied, this field can be used to provide additional CSS text that should be loaded by the
   * swagger-ui page. The contents should be raw CSS text, e.g. <code>
   * BODY { font-size: 1.1em; }
   * </code>
   *
   * @param theCssText the new css text
   */
  public void setCssText(final String theCssText) {
    myCssText = theCssText;
  }

  /**
   * Serve swagger ui html.
   *
   * @param theRequestDetails the the request details
   * @param theResponse the the response
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("unchecked")
  private void serveSwaggerUiHtml(final ServletRequestDetails theRequestDetails,
    final HttpServletResponse theResponse) throws IOException {
    final CapabilityStatement cs = getCapabilityStatement(theRequestDetails);

    final String baseUrl = removeTrailingSlash(cs.getImplementation().getUrl());
    theResponse.setStatus(200);
    theResponse.setContentType(Constants.CT_HTML);

    final HttpServletRequest servletRequest = theRequestDetails.getServletRequest();
    final ServletContext servletContext = servletRequest.getServletContext();

    final WebContext context = new WebContext(servletRequest, theResponse, servletContext);
    context.setVariable(REQUEST_DETAILS, theRequestDetails);
    context.setVariable("DESCRIPTION", cs.getImplementation().getDescription());
    context.setVariable("SERVER_NAME", cs.getSoftware().getName());
    context.setVariable("SERVER_VERSION", cs.getSoftware().getVersion());
    context.setVariable("BASE_URL", baseUrl);
    context.setVariable("BANNER_IMAGE_URL", getBannerImage());
    context.setVariable("OPENAPI_DOCS", baseUrl + "/api-docs");
    context.setVariable("FHIR_VERSION", cs.getFhirVersion().toCode());
    context.setVariable("ADDITIONAL_CSS_TEXT", getCssText());
    context.setVariable("USE_RESOURCE_PAGES", isUseResourcePages());
    context.setVariable("FHIR_VERSION_CODENAME",
        FhirVersionEnum.forVersionString(cs.getFhirVersion().toCode()).name());

    String copyright = cs.getCopyright();
    if (isNotBlank(copyright)) {
      copyright = myFlexmarkRenderer.render(myFlexmarkParser.parse(copyright));
      context.setVariable("COPYRIGHT_HTML", copyright);
    }

    final List<String> pageNames = new ArrayList<>();
    final Map<String, Integer> resourceToCount = new HashMap<>();
    cs.getRestFirstRep().getResource().stream().forEach(t -> {
      final String type = t.getType();
      pageNames.add(type);
      final Extension countExtension = t.getExtensionByUrl(ExtensionConstants.CONF_RESOURCE_COUNT);
      if (countExtension != null) {
        final IPrimitiveType<? extends Number> countExtensionValue =
            (IPrimitiveType<? extends Number>) countExtension.getValueAsPrimitive();
        if (countExtensionValue != null && countExtensionValue.hasValue()) {
          resourceToCount.put(type, countExtensionValue.getValue().intValue());
        }
      }
    });
    pageNames.sort((o1, o2) -> {
      final Integer count1 = resourceToCount.get(o1);
      final Integer count2 = resourceToCount.get(o2);
      if (count1 != null && count2 != null) {
        return count2 - count1;
      }
      if (count1 != null) {
        return -1;
      }
      if (count2 != null) {
        return 1;
      }
      return o1.compareTo(o2);
    });

    pageNames.add(0, PAGE_ALL);
    pageNames.add(1, PAGE_SYSTEM);

    context.setVariable("PAGE_NAMES", pageNames);
    context.setVariable("PAGE_NAME_TO_COUNT", resourceToCount);

    String page;
    if (isUseResourcePages()) {
      page = extractPageName(theRequestDetails, PAGE_SYSTEM);
    } else {
      page = PAGE_ALL;
    }
    context.setVariable("PAGE", page);

    populateOIDCVariables(theRequestDetails, context);

    final String outcome = myTemplateEngine.process("index.html", context);

    try (final PrintWriter writer = theResponse.getWriter();) {
      writer.write(outcome);
    }
  }

  /**
   * Populate OIDC variables.
   *
   * @param theRequestDetails the the request details
   * @param theContext the the context
   */
  protected void populateOIDCVariables(final ServletRequestDetails theRequestDetails,
    final WebContext theContext) {
    theContext.setVariable("OAUTH2_REDIRECT_URL_PROPERTY", "");
  }

  /**
   * Extract page name.
   *
   * @param theRequestDetails the the request details
   * @param theDefault the the default
   * @return the string
   */
  private String extractPageName(final ServletRequestDetails theRequestDetails,
    final String theDefault) {
    final String[] pageValues = theRequestDetails.getParameters().get("page");
    String page = null;
    if (pageValues != null && pageValues.length > 0) {
      page = pageValues[0];
    }
    if (isBlank(page)) {
      page = theDefault;
    }
    return page;
  }

  /**
   * Generate open api.
   *
   * @param theRequestDetails the the request details
   * @return the open API
   */
  protected OpenAPI generateOpenApi(final ServletRequestDetails theRequestDetails) {
    final String page = extractPageName(theRequestDetails, null);

    final CapabilityStatement cs = getCapabilityStatement(theRequestDetails);
    final FhirContext ctx = theRequestDetails.getFhirContext();

    IServerConformanceProvider<?> capabilitiesProvider = null;
    final RestfulServer restfulServer = theRequestDetails.getServer();
    if (restfulServer.getServerConformanceProvider() instanceof IServerConformanceProvider) {
      capabilitiesProvider =
          (IServerConformanceProvider<?>) restfulServer.getServerConformanceProvider();
    }

    final OpenAPI openApi = new OpenAPI();

    openApi.setInfo(new Info());
    openApi.getInfo().setDescription(cs.getDescription());
    openApi.getInfo().setTitle(cs.getSoftware().getName());
    openApi.getInfo().setVersion(cs.getSoftware().getVersion());
    openApi.getInfo().setContact(new Contact());
    openApi.getInfo().getContact().setName(cs.getContactFirstRep().getName());
    openApi.getInfo().getContact()
        .setEmail(cs.getContactFirstRep().getTelecomFirstRep().getValue());

    final Server server = new Server();
    openApi.addServersItem(server);
    final String baseUrl = removeTrailingSlash(cs.getImplementation().getUrl());
    server.setUrl(baseUrl);
    server.setDescription(cs.getSoftware().getName());

    final Paths paths = new Paths();
    openApi.setPaths(paths);

    // @SecurityScheme(name = "bearerAuth", description = "JWT authentication with bearer
    // token", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "Bearer
    // [token]")
    // Resolving showing the "Authorize" button -
    // https://github.com/hapifhir/hapi-fhir/issues/4555
    final SecurityScheme scheme = new SecurityScheme()
        .description("JWT authentication with bearer token").type(SecurityScheme.Type.HTTP)
        .scheme("bearer").bearerFormat("Bearer <JWT>").in(In.HEADER);

    ensureComponentsSchemasPopulated(openApi);
    openApi.getComponents().addSecuritySchemes("bearerAuth", scheme);
    // final SecurityRequirement securityRequirement = new SecurityRequirement();
    // securityRequirement.addList("bearerAuth");
    // openApi.security(Collections.singletonList(securityRequirement));

    if (page == null || page.equals(PAGE_SYSTEM) || page.equals(PAGE_ALL)) {
      final Tag serverTag = new Tag();
      serverTag.setName(PAGE_SYSTEM);
      serverTag.setDescription("Server-level operations");
      openApi.addTagsItem(serverTag);

      final Operation capabilitiesOperation =
          getPathItem(paths, "/{idOrUriLabel}/fhir/metadata", PathItem.HttpMethod.GET);
      // addAuthParameter(capabilitiesOperation);
      addProjectParameter(capabilitiesOperation);
      capabilitiesOperation.addTagsItem(PAGE_SYSTEM);
      capabilitiesOperation.setSummary("Fetch the server FHIR CapabilityStatement");
      addFhirResourceResponse(ctx, openApi, capabilitiesOperation, "CapabilityStatement");

      final Set<CapabilityStatement.SystemRestfulInteraction> systemInteractions =
          cs.getRestFirstRep().getInteraction().stream().map(t -> t.getCode())
              .collect(Collectors.toSet());
      // Transaction Operation
      if (systemInteractions.contains(CapabilityStatement.SystemRestfulInteraction.TRANSACTION)
          || systemInteractions.contains(CapabilityStatement.SystemRestfulInteraction.BATCH)) {
        final Operation transaction =
            getPathItem(paths, "/{idOrUriLabel}/fhir/", PathItem.HttpMethod.POST);
        transaction.addTagsItem(PAGE_SYSTEM);
        transaction.setSummary("Execute a FHIR Transaction (or FHIR Batch) Bundle");
        // addAuthParameter(transaction);
        addProjectParameter(transaction);
        addFhirResourceResponse(ctx, openApi, transaction, null);
        addFhirResourceRequestBody(openApi, transaction, ctx, null);
      }

      // System History Operation
      if (systemInteractions.contains(CapabilityStatement.SystemRestfulInteraction.HISTORYSYSTEM)) {
        final Operation systemHistory =
            getPathItem(paths, "/{idOrUriLabel}/fhir/_history", PathItem.HttpMethod.GET);
        systemHistory.addTagsItem(PAGE_SYSTEM);
        systemHistory.setSummary(
            "Fetch the resource change history across all resource types on the server");
        // addAuthParameter(systemHistory);
        addProjectParameter(systemHistory);
        addFhirResourceResponse(ctx, openApi, systemHistory, null);
      }

      // System-level Operations
      for (final CapabilityStatement.CapabilityStatementRestResourceOperationComponent nextOperation : cs
          .getRestFirstRep().getOperation()) {
        addFhirOperation(ctx, openApi, theRequestDetails, capabilitiesProvider, paths, null,
            nextOperation);
      }
    }

    for (final CapabilityStatement.CapabilityStatementRestResourceComponent nextResource : cs
        .getRestFirstRep().getResource()) {
      final String resourceType = nextResource.getType();

      if (page != null && !page.equals(resourceType) && !page.equals(PAGE_ALL)) {
        continue;
      }

      final Set<CapabilityStatement.TypeRestfulInteraction> typeRestfulInteractions =
          nextResource.getInteraction().stream().map(t -> t.getCodeElement().getValue())
              .collect(Collectors.toSet());

      final Tag resourceTag = new Tag();
      resourceTag.setName(resourceType);
      resourceTag.setDescription("The " + resourceType + " FHIR resource type");
      openApi.addTagsItem(resourceTag);

      // Instance Read
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.READ)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}", PathItem.HttpMethod.GET);
        operation.addTagsItem(resourceType);
        operation.setSummary("Read " + unCamelCase(resourceType) + " instance");
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Instance VRead
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.VREAD)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}/_history/{version_id}",
            PathItem.HttpMethod.GET);
        operation.addTagsItem(resourceType);
        operation
            .setSummary("Read " + unCamelCase(resourceType) + " instance with specific version");
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addResourceVersionIdParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Type Create
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.CREATE)) {
        final Operation operation =
            getPathItem(paths, "/{idOrUriLabel}/fhir/" + resourceType, PathItem.HttpMethod.POST);
        operation.addTagsItem(resourceType);
        operation.setSummary("Create a new " + unCamelCase(resourceType) + " instance");
        addFhirResourceRequestBody(openApi, operation, ctx,
            genericExampleSupplier(ctx, resourceType));
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Instance Update
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.UPDATE)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}", PathItem.HttpMethod.PUT);
        operation.addTagsItem(resourceType);
        operation.setSummary("Update an existing " + resourceType
            + " instance, or create using a client-assigned ID");
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addFhirResourceRequestBody(openApi, operation, ctx,
            genericExampleSupplier(ctx, resourceType));
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Type history
      if (typeRestfulInteractions
          .contains(CapabilityStatement.TypeRestfulInteraction.HISTORYTYPE)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/_history", PathItem.HttpMethod.GET);
        operation.addTagsItem(resourceType);
        operation.setSummary(
            "Fetch the resource change history for all resources of type " + resourceType);
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Instance history
      if (typeRestfulInteractions
          .contains(CapabilityStatement.TypeRestfulInteraction.HISTORYTYPE)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}/_history", PathItem.HttpMethod.GET);
        operation.addTagsItem(resourceType);
        operation.setSummary(
            "Fetch the resource change history for all resources of type " + resourceType);
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Instance Patch
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.PATCH)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}", PathItem.HttpMethod.PATCH);
        operation.addTagsItem(resourceType);
        operation.setSummary(
            "Patch a resource instance of type " + unCamelCase(resourceType) + " by ID");
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addFhirResourceRequestBody(openApi, operation, FHIR_CONTEXT_CANONICAL,
            patchExampleSupplier());
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Instance Delete
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.DELETE)) {
        final Operation operation = getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/{id}", PathItem.HttpMethod.DELETE);
        operation.addTagsItem(resourceType);
        operation.setSummary("Perform a logical delete on a resource instance");
        // addAuthParameter(operation);
        addProjectParameter(operation);
        addResourceIdParameter(operation);
        addFhirResourceResponse(ctx, openApi, operation, null);
      }

      // Search
      if (typeRestfulInteractions.contains(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE)) {
        addSearchOperation(openApi,
            getPathItem(paths, "/{idOrUriLabel}/fhir/" + resourceType, PathItem.HttpMethod.GET),
            ctx, resourceType, nextResource);
        addSearchOperation(openApi, getPathItem(paths,
            "/{idOrUriLabel}/fhir/" + resourceType + "/_search", PathItem.HttpMethod.GET), ctx,
            resourceType, nextResource);
      }

      // Resource-level Operations
      for (final CapabilityStatement.CapabilityStatementRestResourceOperationComponent nextOperation : nextResource
          .getOperation()) {
        addFhirOperation(ctx, openApi, theRequestDetails, capabilitiesProvider, paths, resourceType,
            nextOperation);
      }
    }

    return openApi;
  }

  /**
   * Adds the search operation.
   *
   * @param openApi the open api
   * @param operation the operation
   * @param ctx the ctx
   * @param resourceType the resource type
   * @param nextResource the next resource
   */
  protected void addSearchOperation(final OpenAPI openApi, final Operation operation,
    final FhirContext ctx, final String resourceType,
    final CapabilityStatement.CapabilityStatementRestResourceComponent nextResource) {
    operation.addTagsItem(resourceType);
    operation.setDescription("This is a search type");
    operation.setSummary("Search for " + unCamelCase(resourceType) + " instances");
    // addAuthParameter(operation);
    addProjectParameter(operation);
    addFhirResourceResponse(ctx, openApi, operation, null);

    for (final CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent nextSearchParam : nextResource
        .getSearchParam()) {
      final Parameter parametersItem = new Parameter();
      operation.addParametersItem(parametersItem);

      parametersItem.setName(nextSearchParam.getName());
      parametersItem.setIn("query");
      parametersItem.setDescription(nextSearchParam.getDocumentation());
      parametersItem.setStyle(Parameter.StyleEnum.SIMPLE);
    }
  }

  /**
   * Patch example supplier.
   *
   * @return the supplier
   */
  private Supplier<IBaseResource> patchExampleSupplier() {
    return () -> {
      final Parameters example = new Parameters();
      final Parameters.ParametersParameterComponent operation =
          example.addParameter().setName("operation");
      operation.addPart().setName("type").setValue(new StringType("add"));
      operation.addPart().setName("path").setValue(new StringType("Patient"));
      operation.addPart().setName("name").setValue(new StringType("birthDate"));
      operation.addPart().setName("value").setValue(new DateType("1930-01-01"));
      return example;
    };
  }

  /**
   * Adds the schema fhir resource.
   *
   * @param theOpenApi the the open api
   */
  private void addSchemaFhirResource(final OpenAPI theOpenApi) {
    ensureComponentsSchemasPopulated(theOpenApi);

    if (!theOpenApi.getComponents().getSchemas().containsKey(FHIR_JSON_RESOURCE)) {
      final ObjectSchema fhirJsonSchema = new ObjectSchema();
      fhirJsonSchema.setDescription("A FHIR resource");
      theOpenApi.getComponents().addSchemas(FHIR_JSON_RESOURCE, fhirJsonSchema);
    }

    if (!theOpenApi.getComponents().getSchemas().containsKey(FHIR_XML_RESOURCE)) {
      final ObjectSchema fhirXmlSchema = new ObjectSchema();
      fhirXmlSchema.setDescription("A FHIR resource");
      theOpenApi.getComponents().addSchemas(FHIR_XML_RESOURCE, fhirXmlSchema);
    }
  }

  /**
   * Ensure components schemas populated.
   *
   * @param theOpenApi the the open api
   */
  private void ensureComponentsSchemasPopulated(final OpenAPI theOpenApi) {
    if (theOpenApi.getComponents() == null) {
      theOpenApi.setComponents(new Components());
    }
    if (theOpenApi.getComponents().getSchemas() == null) {
      theOpenApi.getComponents().setSchemas(new LinkedHashMap<>());
    }
  }

  /**
   * Gets the capability statement.
   *
   * @param theRequestDetails the the request details
   * @return the capability statement
   */
  private CapabilityStatement getCapabilityStatement(
    final ServletRequestDetails theRequestDetails) {
    final RestfulServer restfulServer = theRequestDetails.getServer();
    final IBaseConformance versionIndependentCapabilityStatement =
        restfulServer.getCapabilityStatement(theRequestDetails);
    return toCanonicalVersion(versionIndependentCapabilityStatement);
  }

  /**
   * Adds the fhir operation.
   *
   * @param theFhirContext the the fhir context
   * @param theOpenApi the the open api
   * @param theRequestDetails the the request details
   * @param theCapabilitiesProvider the the capabilities provider
   * @param thePaths the the paths
   * @param theResourceType the the resource type
   * @param theOperation the the operation
   */
  private void addFhirOperation(final FhirContext theFhirContext, final OpenAPI theOpenApi,
    final ServletRequestDetails theRequestDetails,
    final IServerConformanceProvider<?> theCapabilitiesProvider, final Paths thePaths,
    final String theResourceType,
    final CapabilityStatement.CapabilityStatementRestResourceOperationComponent theOperation) {
    if (theCapabilitiesProvider != null) {
      final IdType definitionId = new IdType(theOperation.getDefinition());
      final IBaseResource operationDefinitionNonCanonical =
          theCapabilitiesProvider.readOperationDefinition(definitionId, theRequestDetails);
      if (operationDefinitionNonCanonical == null) {
        return;
      }

      final OperationDefinition operationDefinition =
          toCanonicalVersion(operationDefinitionNonCanonical);
      final boolean postOnly =
          operationDefinition.getAffectsState() || operationDefinition.getParameter().stream()
              .filter(p -> p.getUse().equals(OperationParameterUse.IN)).anyMatch(p -> {
                final boolean required = p.getMin() > 0;
                return required && !isPrimitive(p);
              });

      if (!postOnly) {

        // GET form for non-state-affecting operations
        if (theResourceType != null) {
          if (operationDefinition.getType()) {
            final Operation operation = getPathItem(thePaths,
                "/{idOrUriLabel}/fhir/" + theResourceType + "/$" + operationDefinition.getCode(),
                PathItem.HttpMethod.GET);
            populateOperation(theFhirContext, theOpenApi, theResourceType, operationDefinition,
                operation, true);
            operation.setSummary(operationDefinition.getCode());
            operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
                + operationDefinition.getCode());
          }
          if (operationDefinition.getInstance()) {
            final Operation operation =
                getPathItem(thePaths, "/{idOrUriLabel}/fhir/" + theResourceType + "/{id}/$"
                    + operationDefinition.getCode(), PathItem.HttpMethod.GET);
            addResourceIdParameter(operation);
            // addAuthParameter(operation);
            addProjectParameter(operation);
            populateOperation(theFhirContext, theOpenApi, theResourceType, operationDefinition,
                operation, true);
            operation.setSummary(operationDefinition.getCode());
            operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
                + operationDefinition.getCode());
          }
        } else {
          if (operationDefinition.getSystem()) {
            final Operation operation = getPathItem(thePaths,
                "/{idOrUriLabel}/fhir/$" + operationDefinition.getCode(), PathItem.HttpMethod.GET);
            populateOperation(theFhirContext, theOpenApi, null, operationDefinition, operation,
                true);
            operation.setSummary(operationDefinition.getCode());
            operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
                + operationDefinition.getCode());
          }
        }
      }

      // POST form for all operations
      if (theResourceType != null) {
        if (operationDefinition.getType()) {
          final Operation operation = getPathItem(thePaths,
              "/{idOrUriLabel}/fhir/" + theResourceType + "/$" + operationDefinition.getCode(),
              PathItem.HttpMethod.POST);
          populateOperation(theFhirContext, theOpenApi, theResourceType, operationDefinition,
              operation, false);
          operation.setSummary(operationDefinition.getCode());
          operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
              + operationDefinition.getCode());
        }
        if (operationDefinition.getInstance()) {
          final Operation operation = getPathItem(thePaths,
              "/{idOrUriLabel}/fhir/" + theResourceType + "/{id}/$" + operationDefinition.getCode(),
              PathItem.HttpMethod.POST);
          // addAuthParameter(operation);
          addProjectParameter(operation);
          addResourceIdParameter(operation);
          populateOperation(theFhirContext, theOpenApi, theResourceType, operationDefinition,
              operation, false);
          operation.setSummary(operationDefinition.getCode());
          operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
              + operationDefinition.getCode());
        }
      } else {
        if (operationDefinition.getSystem()) {
          final Operation operation = getPathItem(thePaths,
              "/{idOrUriLabel}/fhir/$" + operationDefinition.getCode(), PathItem.HttpMethod.POST);
          populateOperation(theFhirContext, theOpenApi, null, operationDefinition, operation,
              false);
          operation.setSummary(operationDefinition.getCode());
          operation.setSummary(unCamelCase(theResourceType) + " operation to perform "
              + operationDefinition.getCode());
        }
      }
    }
  }

  /** The primitive types. */
  private static List<String> primitiveTypes = Arrays.asList(DataTypes.BOOLEAN.toCode(),
      DataTypes.INTEGER.toCode(), DataTypes.STRING.toCode(), DataTypes.DECIMAL.toCode(),
      DataTypes.URI.toCode(), DataTypes.URL.toCode(), DataTypes.CANONICAL.toCode(),
      DataTypes.REFERENCE.toCode(), DataTypes.BASE64BINARY.toCode(), DataTypes.INSTANT.toCode(),
      DataTypes.DATE.toCode(), DataTypes.DATETIME.toCode(), DataTypes.TIME.toCode(),
      DataTypes.CODE.toCode(), DataTypes.CODING.toCode(), DataTypes.OID.toCode(),
      DataTypes.ID.toCode(), DataTypes.MARKDOWN.toCode(), DataTypes.UNSIGNEDINT.toCode(),
      DataTypes.POSITIVEINT.toCode(), DataTypes.UUID.toCode());

  /**
   * Checks if is primitive.
   *
   * @param parameter the parameter
   * @return true, if is primitive
   */
  private static boolean isPrimitive(final OperationDefinitionParameterComponent parameter) {
    return primitiveTypes.contains(parameter.getType());
  }

  /**
   * Populate operation.
   *
   * @param theFhirContext the the fhir context
   * @param theOpenApi the the open api
   * @param theResourceType the the resource type
   * @param theOperationDefinition the the operation definition
   * @param theOperation the the operation
   * @param theGet the the get
   */
  private void populateOperation(final FhirContext theFhirContext, final OpenAPI theOpenApi,
    final String theResourceType, final OperationDefinition theOperationDefinition,
    final Operation theOperation, final boolean theGet) {
    if (theResourceType == null) {
      theOperation.addTagsItem(PAGE_SYSTEM);
    } else {
      theOperation.addTagsItem(theResourceType);
    }
    theOperation.setSummary(theOperationDefinition.getTitle());
    theOperation.setDescription(theOperationDefinition.getDescription());
    // addAuthParameter(theOperation);
    addProjectParameter(theOperation);
    addFhirResourceResponse(theFhirContext, theOpenApi, theOperation, null);
    if (theGet) {

      for (final OperationDefinition.OperationDefinitionParameterComponent nextParameter : theOperationDefinition
          .getParameter()) {
        if ("0".equals(nextParameter.getMax())
            || !nextParameter.getUse().equals(OperationParameterUse.IN)
            || (!isPrimitive(nextParameter) && nextParameter.getMin() == 0)) {
          continue;
        }
        final Parameter parametersItem = new Parameter();
        theOperation.addParametersItem(parametersItem);

        parametersItem.setName(nextParameter.getName());
        parametersItem.setIn("query");
        parametersItem.setDescription(nextParameter.getDocumentation());
        parametersItem.setStyle(Parameter.StyleEnum.SIMPLE);
        parametersItem.setRequired(nextParameter.getMin() > 0);

        final List<Extension> exampleExtensions =
            nextParameter.getExtensionsByUrl(HapiExtensions.EXT_OP_PARAMETER_EXAMPLE_VALUE);
        if (exampleExtensions.size() == 1) {
          parametersItem
              .setExample(exampleExtensions.get(0).getValueAsPrimitive().getValueAsString());
        } else if (exampleExtensions.size() > 1) {
          for (final Extension next : exampleExtensions) {
            final String nextExample = next.getValueAsPrimitive().getValueAsString();
            parametersItem.addExample(nextExample, new Example().value(nextExample));
          }
        }
      }

    } else {

      final Parameters exampleRequestBody = new Parameters();
      for (final OperationDefinition.OperationDefinitionParameterComponent nextSearchParam : theOperationDefinition
          .getParameter()) {
        if ("0".equals(nextSearchParam.getMax())
            || !nextSearchParam.getUse().equals(OperationParameterUse.IN)) {
          continue;
        }
        final Parameters.ParametersParameterComponent param = exampleRequestBody.addParameter();
        param.setName(nextSearchParam.getName());
        final String paramType = nextSearchParam.getType();
        switch (defaultString(paramType)) {
          case "uri":
          case "url":
          case "code":
          case "string": {
            final IPrimitiveType<?> type = (IPrimitiveType<?>) FHIR_CONTEXT_CANONICAL
                .getElementDefinition(paramType).newInstance();
            type.setValueAsString("example");
            param.setValue((Type) type);
            break;
          }
          case "integer": {
            final IPrimitiveType<?> type = (IPrimitiveType<?>) FHIR_CONTEXT_CANONICAL
                .getElementDefinition(paramType).newInstance();
            type.setValueAsString("0");
            param.setValue((Type) type);
            break;
          }
          case "boolean": {
            final IPrimitiveType<?> type = (IPrimitiveType<?>) FHIR_CONTEXT_CANONICAL
                .getElementDefinition(paramType).newInstance();
            type.setValueAsString("false");
            param.setValue((Type) type);
            break;
          }
          case "CodeableConcept": {
            final CodeableConcept type = new CodeableConcept();
            type.getCodingFirstRep().setSystem("http://example.com");
            type.getCodingFirstRep().setCode("1234");
            param.setValue(type);
            break;
          }
          case "Coding": {
            final Coding type = new Coding();
            type.setSystem("http://example.com");
            type.setCode("1234");
            param.setValue(type);
            break;
          }
          case "Reference":
            final Reference reference = new Reference("example");
            param.setValue(reference);
            break;
          case "Resource":
            if (theResourceType != null) {
              if (FHIR_CONTEXT_CANONICAL.getResourceTypes().contains(theResourceType)) {
                final IBaseResource resource =
                    FHIR_CONTEXT_CANONICAL.getResourceDefinition(theResourceType).newInstance();
                resource.setId("1");
                param.setResource((Resource) resource);
              }
            }
            break;
          default:
            // do nothing
            break;
        }
      }

      final String exampleRequestBodyString = FHIR_CONTEXT_CANONICAL.newJsonParser()
          .setPrettyPrint(true).encodeResourceToString(exampleRequestBody);
      theOperation.setRequestBody(new RequestBody());
      theOperation.getRequestBody().setContent(new Content());
      final MediaType mediaType = new MediaType();
      mediaType.setExample(exampleRequestBodyString);
      mediaType.setSchema(new Schema<>().type("object").title("FHIR Resource"));
      theOperation.getRequestBody().getContent().addMediaType(Constants.CT_FHIR_JSON_NEW,
          mediaType);
    }
  }

  /**
   * Gets the path item.
   *
   * @param thePaths the the paths
   * @param thePath the the path
   * @param theMethod the the method
   * @return the path item
   */
  protected Operation getPathItem(final Paths thePaths, final String thePath,
    final PathItem.HttpMethod theMethod) {
    PathItem pathItem;

    if (thePaths.containsKey(thePath)) {
      pathItem = thePaths.get(thePath);
    } else {
      pathItem = new PathItem();
      thePaths.addPathItem(thePath, pathItem);
    }

    switch (theMethod) {
      case POST:
        assert pathItem.getPost() == null : "Have duplicate POST at path: " + thePath;
        return pathItem.post(new Operation()).getPost();
      case GET:
        assert pathItem.getGet() == null : "Have duplicate GET at path: " + thePath;
        return pathItem.get(new Operation()).getGet();
      case PUT:
        assert pathItem.getPut() == null;
        return pathItem.put(new Operation()).getPut();
      case PATCH:
        assert pathItem.getPatch() == null;
        return pathItem.patch(new Operation()).getPatch();
      case DELETE:
        assert pathItem.getDelete() == null;
        return pathItem.delete(new Operation()).getDelete();
      case HEAD:
      case OPTIONS:
      case TRACE:
      default:
        throw new IllegalStateException(Msg.code(240));
    }
  }

  /**
   * Adds the fhir resource request body.
   *
   * @param theOpenApi the the open api
   * @param theOperation the the operation
   * @param theExampleFhirContext the the example fhir context
   * @param theExampleSupplier the the example supplier
   */
  private void addFhirResourceRequestBody(final OpenAPI theOpenApi, final Operation theOperation,
    final FhirContext theExampleFhirContext, final Supplier<IBaseResource> theExampleSupplier) {
    final RequestBody requestBody = new RequestBody();
    requestBody.setContent(
        provideContentFhirResource(theOpenApi, theExampleFhirContext, theExampleSupplier));
    theOperation.setRequestBody(requestBody);
  }

  /**
   * Adds the resource version id parameter.
   *
   * @param theOperation the the operation
   */
  private void addResourceVersionIdParameter(final Operation theOperation) {
    final Parameter parameter = new Parameter();
    parameter.setName("version_id");
    parameter.setIn("path");
    parameter.setDescription("The resource version ID");
    parameter.setExample("1");
    parameter.setSchema(new Schema<>().type("string").minimum(new BigDecimal(1)));
    parameter.setStyle(Parameter.StyleEnum.SIMPLE);
    theOperation.addParametersItem(parameter);
  }

  /**
   * Adds the fhir resource response.
   *
   * @param theFhirContext the the fhir context
   * @param theOpenApi the the open api
   * @param theOperation the the operation
   * @param theResourceType the the resource type
   */
  private void addFhirResourceResponse(final FhirContext theFhirContext, final OpenAPI theOpenApi,
    final Operation theOperation, final String theResourceType) {
    theOperation.setResponses(new ApiResponses());
    final ApiResponse response200 = new ApiResponse();
    response200.setDescription("Success");
    response200.setContent(provideContentFhirResource(theOpenApi, theFhirContext,
        genericExampleSupplier(theFhirContext, theResourceType)));
    theOperation.getResponses().addApiResponse("200", response200);
  }

  /**
   * Generic example supplier.
   *
   * @param theFhirContext the the fhir context
   * @param theResourceType the the resource type
   * @return the supplier
   */
  private Supplier<IBaseResource> genericExampleSupplier(final FhirContext theFhirContext,
    final String theResourceType) {
    if (theResourceType == null) {
      return null;
    }
    return () -> {
      IBaseResource example = null;
      if (theFhirContext.getResourceTypes().contains(theResourceType)) {
        example = theFhirContext.getResourceDefinition(theResourceType).newInstance();
      }
      return example;
    };
  }

  /**
   * Provide content fhir resource.
   *
   * @param theOpenApi the the open api
   * @param theExampleFhirContext the the example fhir context
   * @param theExampleSupplier the the example supplier
   * @return the content
   */
  private Content provideContentFhirResource(final OpenAPI theOpenApi,
    final FhirContext theExampleFhirContext, final Supplier<IBaseResource> theExampleSupplier) {
    addSchemaFhirResource(theOpenApi);
    final Content retVal = new Content();

    final MediaType jsonSchema = new MediaType()
        .schema(new ObjectSchema().$ref("#/components/schemas/" + FHIR_JSON_RESOURCE));
    if (theExampleSupplier != null) {
      jsonSchema.setExample(theExampleFhirContext.newJsonParser().setPrettyPrint(true)
          .encodeResourceToString(theExampleSupplier.get()));
    }
    retVal.addMediaType(Constants.CT_FHIR_JSON_NEW, jsonSchema);

    final MediaType xmlSchema = new MediaType()
        .schema(new ObjectSchema().$ref("#/components/schemas/" + FHIR_XML_RESOURCE));
    if (theExampleSupplier != null) {
      xmlSchema.setExample(theExampleFhirContext.newXmlParser().setPrettyPrint(true)
          .encodeResourceToString(theExampleSupplier.get()));
    }
    retVal.addMediaType(Constants.CT_FHIR_XML_NEW, xmlSchema);
    return retVal;
  }

  /**
   * Adds the resource id parameter.
   *
   * @param theOperation the the operation
   */
  private void addResourceIdParameter(final Operation theOperation) {
    final Parameter parameter = new Parameter();
    parameter.setName("id");
    parameter.setIn("path");
    parameter.setDescription("The resource ID");
    parameter.setExample("123");
    parameter.setSchema(new Schema<>().type("string").minimum(new BigDecimal(1)));
    parameter.setStyle(Parameter.StyleEnum.SIMPLE);
    theOperation.addParametersItem(parameter);
  }

  /**
   * Adds the project parameter.
   *
   * @param theOperation the the operation
   */
  private void addProjectParameter(final Operation theOperation) {
    final Parameter parameter = new Parameter();
    parameter.setName("idOrUriLabel");
    parameter.setIn("path");
    parameter.setDescription("The project id or uriLabel");
    parameter.setExample("sandbox");
    parameter.setSchema(new Schema<>().type("string").minimum(new BigDecimal(1)));
    parameter.setStyle(Parameter.StyleEnum.SIMPLE);
    theOperation.addParametersItem(parameter);

    // Also add bearer auth
    theOperation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  /**
   * Adds the auth parameter.
   *
   * @param theOperation the the operation
   */
  @SuppressWarnings("unused")
  private void addAuthParameter(final Operation theOperation) {
    final Parameter parameter = new Parameter();
    parameter.setName("token");
    parameter.setIn("header");
    parameter.setDescription("the 'Authorization: Bearer <token>'");
    // parameter.setExample("sandbox");
    parameter.setSchema(new Schema<>().type("string").minimum(new BigDecimal(1)));
    parameter.setStyle(Parameter.StyleEnum.SIMPLE);
    parameter.setRequired(true);
    theOperation.addParametersItem(parameter);
  }

  /**
   * Gets the index template.
   *
   * @return the index template
   */
  protected ClassLoaderTemplateResource getIndexTemplate() {
    return new ClassLoaderTemplateResource(myResourcePathToClasspath.get("/swagger-ui/index.html"),
        StandardCharsets.UTF_8.name());
  }

  /**
   * Gets the banner image.
   *
   * @return the banner image
   */
  /* see superclass */
  public String getBannerImage() {
    return myBannerImage;
  }

  /**
   * Sets the banner image.
   *
   * @param theBannerImage the the banner image
   * @return the open api interceptor
   */
  public EVSAPIOpenApiInterceptorR4 setBannerImage(final String theBannerImage) {
    myBannerImage = StringUtils.defaultIfBlank(theBannerImage, null);
    return this;
  }

  /**
   * Checks if is use resource pages.
   *
   * @return true, if is use resource pages
   */
  public boolean isUseResourcePages() {
    return myUseResourcePages;
  }

  /**
   * Sets the use resource pages.
   *
   * @param theUseResourcePages the new use resource pages
   */
  public void setUseResourcePages(final boolean theUseResourcePages) {
    myUseResourcePages = theUseResourcePages;
  }

  /**
   * To canonical version.
   *
   * @param <T> the generic type
   * @param theNonCanonical the the non canonical
   * @return the t
   */
  @SuppressWarnings("unchecked")
  private static <T extends Resource> T toCanonicalVersion(final IBaseResource theNonCanonical) {
    IBaseResource canonical;
    if (theNonCanonical instanceof org.hl7.fhir.dstu3.model.Resource) {
      canonical = VersionConvertorFactory_30_40
          .convertResource((org.hl7.fhir.dstu3.model.Resource) theNonCanonical);
    } else if (theNonCanonical instanceof org.hl7.fhir.r5.model.Resource) {
      canonical = VersionConvertorFactory_40_50
          .convertResource((org.hl7.fhir.r5.model.Resource) theNonCanonical);
    } else if (theNonCanonical instanceof org.hl7.fhir.r4b.model.Resource) {
      final org.hl7.fhir.r5.model.Resource r5 = VersionConvertorFactory_43_50
          .convertResource((org.hl7.fhir.r4b.model.Resource) theNonCanonical);
      canonical = VersionConvertorFactory_40_50.convertResource(r5);
    } else {
      canonical = theNonCanonical;
    }
    return (T) canonical;
  }

  public static String unCamelCase(final String str) {
    // insert a space between lower & upper
    return capitalize(str.replaceAll("([a-z])([A-Z])", "$1 $2")
        // space before last upper in a sequence followed by lower
        .replaceAll("\\b([A-Z]+)([A-Z])([a-z])", "$1 $2$3"));
  }

  public static String capitalize(final String value) {
    if (isEmpty(value)) {
      return value;
    }
    return value.substring(0, 1).toUpperCase() + value.substring(1);
  }

  public static boolean isEmpty(final String str) {
    return str == null || str.isEmpty();
  }

  /**
   * SwaggerUiTemplateResolver.
   */
  private class SwaggerUiTemplateResolver implements ITemplateResolver {

    /* see superclass */
    @Override
    public String getName() {
      return getClass().getName();
    }

    /* see superclass */
    @Override
    public Integer getOrder() {
      return 0;
    }

    /* see superclass */
    @Override
    public TemplateResolution resolveTemplate(final IEngineConfiguration configuration,
      final String ownerTemplate, final String template,
      final Map<String, Object> templateResolutionAttributes) {
      final ClassLoaderTemplateResource resource = getIndexTemplate();
      final ICacheEntryValidity cacheValidity = new AlwaysValidCacheEntryValidity();
      return new TemplateResolution(resource, TemplateMode.HTML, cacheValidity);
    }
  }

  /**
   * The Class TemplateLinkBuilder.
   */
  private static class TemplateLinkBuilder extends AbstractLinkBuilder {

    /* see superclass */
    @Override
    public String buildLink(final IExpressionContext theExpressionContext, final String theBase,
      final Map<String, Object> theParameters) {

      final ServletRequestDetails requestDetails =
          (ServletRequestDetails) theExpressionContext.getVariable(REQUEST_DETAILS);

      final IServerAddressStrategy addressStrategy =
          requestDetails.getServer().getServerAddressStrategy();
      final String baseUrl = addressStrategy.determineServerBase(
          requestDetails.getServletRequest().getServletContext(),
          requestDetails.getServletRequest());

      final StringBuilder builder = new StringBuilder();
      builder.append(baseUrl);
      builder.append(theBase);
      if (!theParameters.isEmpty()) {
        builder.append("?");
        for (final Iterator<Map.Entry<String, Object>> iter =
            theParameters.entrySet().iterator(); iter.hasNext();) {
          final Map.Entry<String, Object> nextEntry = iter.next();
          builder.append(UrlUtil.escapeUrlParam(nextEntry.getKey()));
          builder.append("=");
          builder
              .append(UrlUtil.escapeUrlParam(defaultIfNull(nextEntry.getValue(), "").toString()));
          if (iter.hasNext()) {
            builder.append("&");
          }
        }
      }

      return builder.toString();
    }
  }
}
