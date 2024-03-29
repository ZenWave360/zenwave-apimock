package io.github.apimock;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.intuit.karate.StringUtils;
import com.intuit.karate.core.MockHandlerHook;
import com.intuit.karate.http.HttpUtils;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.core.validation.ValidationResult;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.model.impl.Body;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.openapi4j.operation.validator.validation.RequestValidator;
//import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import net.minidev.json.JSONObject;
import org.openapi4j.parser.model.v3.Server;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.v3.ValidationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

/**
 *
 *  @author ivangsa
 */
public class OpenApiValidator4Karate {

    private static Logger logger = LoggerFactory.getLogger(OpenApiValidator4Karate.class);

    private OpenApi3 api;

    private RequestValidator validator;

    private boolean ignoreNoNullable = true;

    public OpenApiValidator4Karate(final OpenApi3 api) {
        super();
        this.api = api;
        if(this.api.getServers() == null) {
            this.api.setServers(new ArrayList<>());
        }
        this.api.getServers().add(new Server().setUrl("http://localhost")); //validate also without contextPath
        ValidationContext vdc = new ValidationContext<>(api.getContext());
        vdc.setOption(ValidationOptions.ADDITIONAL_PROPS_RESTRICT, true);
        vdc.setFastFail(false);
        this.validator = new RequestValidator(vdc, api);
    }

    public static OpenApiValidator4Karate fromURL(final URL ...urls) {
        try {
            OpenApi3 api = new OpenApi3Parser().parse(urls);
            if(api.getExtensions() == null) {
                api.setExtensions(new HashMap<>());
            }
            api.getExtensions().put("x-apimock-internal-urls", urls);
            return new OpenApiValidator4Karate(api);
        } catch (ResolutionException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        URL[] urls = (URL[]) this.api.getExtensions().get("x-apimock-internal-urls");
        logger.debug("Loading {} from {}", this.getClass().getSimpleName(), urls);
        if(urls != null) {
            OpenApiValidator4Karate reloaded = fromURL(urls);
            this.api = reloaded.api;
            this.validator = reloaded.validator;
            this.ignoreNoNullable = reloaded.ignoreNoNullable;
        }
    }

    public static Operation findOperation(String method, String requestPath, OpenApi3 api) {
        List<Map.Entry<String, Path>> paths = api.getPaths().entrySet().stream()
                .filter(e -> HttpUtils.parseUriPattern(e.getKey(), requestPath) != null ||HttpUtils.parseUriPattern(e.getKey(), "/" + requestPath) != null)
                .collect(Collectors.toList());
        Path path = paths.size() == 1?
                paths.get(0).getValue() :
                paths.stream().filter(e -> e.getKey().equals(requestPath) || e.getKey().equals(fixUrl(requestPath))).map(e -> e.getValue()).findFirst().orElse(null);

        Operation operation = path != null? path.getOperation(method.toLowerCase()) : null;
        if(operation != null) {
            if(operation.getExtensions() == null) {
                operation.setExtensions(new HashMap<>());
            }
            operation.getExtensions().put("x-apimock-internal-path", paths.get(0).getKey());
        }
        return operation;
    }

    public static Map<String, org.openapi4j.parser.model.v3.Response> find2xxResponses(Operation operation) {
        return operation.getResponses().entrySet().stream()
                .filter(e -> e.getKey().startsWith("2"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    public static Map<String, Collection<String>> cast(Map responseHeaders) {
        return (Map<String, Collection<String>>)responseHeaders;
    }

    public OpenApi3 getApi() {
        return api;
    }

    public ValidationResults isValidRequest(
            final String url,
            final String method,
            final String requestBody,
            final Map<String, Collection<String>> requestHeaders,
            final String operationId) {
        final Operation operation = this.api.getOperationById(operationId);
        ValidationResults results = new ValidationResults();
        Request request = new DefaultRequest.Builder(fixUrl(url), Request.Method.getMethod(method))
                .headers(requestHeaders)
                .body(requestBody != null? Body.from(requestBody) : null)
                .build();
        final Path path = this.api.getPathItemByOperationId(operationId);

        try {
            this.validator.validate(request, path, operation);
        } catch (ValidationException e) {
            for(org.openapi4j.core.validation.ValidationResults.ValidationItem item: e.results().items()) {
                // skip "nullable: false" errors as they are not correctly used in java apis
                if (!Integer.valueOf(1021).equals(item.code()) || !ignoreNoNullable) {
                    results.add(item);
                }
            }
        }
        return results;
    }

    public ValidationResults isValidResponse(
            final String responseBody,
            final Map<String, Collection<String>> responseHeaders,
            final String operationId,
            final int status) {
        ValidationResults results = new ValidationResults();
        final Response response = new DefaultResponse.Builder(status)
            .headers(responseHeaders)
            .body(responseBody != null? Body.from(responseBody) : null)
            .build();
        final Path path = this.api.getPathItemByOperationId(operationId);
        final Operation operation = this.api.getOperationById(operationId);
        if (operation.getResponse(String.valueOf(status)) == null) {
            // response code is not defined
            results.add(String.format("Status code %s not found for operationId %s", status, operationId));
            return results;
        }
        final Map<String, MediaType> mediaTypes = this.getMediaTypes(operation, response.getStatus());
        if ((mediaTypes == null) || mediaTypes.isEmpty()) {
            // ignore errors for codes with no specific response body in openapi.yml
            return results;
        }

        try {
            this.validator.validate(response, path, operation);
        } catch (ValidationException e) {
            for(org.openapi4j.core.validation.ValidationResults.ValidationItem item: e.results().items()) {
                // skip "nullable: false" errors as they are not correctly used in java apis
                if (!Integer.valueOf(1021).equals(item.code()) || !ignoreNoNullable) {
                    results.add(item);
                }
            }
        }
        return results;
    }

    protected Map<String, MediaType> getMediaTypes(final Operation operation, final int status) {
        org.openapi4j.parser.model.v3.Response response = operation.getResponse(String.valueOf(status));
        if (response.getRef() != null) {
            response = this.api.getComponents()
                .getResponse(response.getRef().substring(response.getRef().lastIndexOf('/') + 1));
        }
        return response.getContentMediaTypes();
    }

    private static String fixUrl(String url) {
        url = url.startsWith("/")? url : "/" + url;;
        try {
            return URLEncoder.encode(url, "UTF8").replaceAll("%2F", "/");
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException UTF8 encoding request path",  e);
            return url;
        }
    }

    public static class ValidationResults {

        final List<StringUtils.Pair> items = new ArrayList<>();

        public boolean isValid() {
            return items.isEmpty();
        }

        void add(org.openapi4j.core.validation.ValidationResults.ValidationItem item) {
            items.add(new StringUtils.Pair(item.dataCrumbs(), item.message()));
        }

        void add(String message) {
            items.add(new StringUtils.Pair("", message));
        }
    }
}
