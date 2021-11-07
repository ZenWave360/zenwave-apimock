package io.github.apimocks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.openapi4j.parser.model.v3.Path;
import net.minidev.json.JSONObject;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.v3.ValidationOptions;

import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

public class OpenApiValidator4Karate {

    private final OpenApi3 api;

    private final RequestValidator validator;

    private boolean ignoreNoNullable = true;

    public OpenApiValidator4Karate(final OpenApi3 api) {
        super();
        this.api = api;
        ValidationContext vdc = new ValidationContext<>(api.getContext());
        vdc.setOption(ValidationOptions.ADDITIONAL_PROPS_RESTRICT, true);
        vdc.setFastFail(false);
        this.validator = new RequestValidator(vdc, api);
    }

    public static OpenApiValidator4Karate fromClasspathArtifactId(final String artifactId, String filename) throws Exception {
        final Pattern regex = Pattern.compile("\\/" + artifactId + "(\\/|-.+.jar)");
        final Enumeration<URL> urls = OpenApiValidator4Karate.class.getClassLoader().getResources(filename);
        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            if (regex.matcher(url.getFile()).find()) {
                return fromURL(url);
            }
        }
        throw new RuntimeException("Resource " + filename + " not found in classpath with artifactId " + artifactId);
    }

    public static OpenApiValidator4Karate fromURL(final URL url) {
        try {
            return new OpenApiValidator4Karate(new OpenApi3Parser().parse(url, false));
        } catch (ResolutionException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public static OpenApiValidator4Karate fromURL(final String url) throws MalformedURLException {
        return fromURL(new URL(url));
    }

    public static Operation findOperation(String method, String requestPath, OpenApi3 api) {
        Path path = api.getPaths().entrySet().stream()
                .filter(e -> HttpUtils.parseUriPattern(e.getKey(), requestPath) != null)
                .map(e -> e.getValue())
                .reduce((p, path2) -> { throw new IllegalStateException("More than one ID found"); })
                .orElse(null);
        return path != null? path.getOperation(method.toLowerCase()) : null;
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
        Request request = new DefaultRequest.Builder(url, Request.Method.getMethod(method))
                .headers(requestHeaders)
                .body(Body.from(requestBody))
                .build();
        final Path path = this.api.getPathItemByOperationId(operationId);

        try {
            this.validator.validate(request, path, operation);
        } catch (ValidationException e) {
            for(ValidationResults.ValidationItem item: e.results().items()) {
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
            .body(Body.from(responseBody))
            .build();
        final Path path = this.api.getPathItemByOperationId(operationId);
        final Operation operation = this.api.getOperationById(operationId);
        if (operation.getResponse(String.valueOf(status)) == null) {
            // response code is not defined
            results.add(new ValidationResult(ERROR, null, String.format("Status code %s not found for operationId %s", status, operationId)));
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
            for(ValidationResults.ValidationItem item: e.results().items()) {
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

}
