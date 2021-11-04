package io.github.apimock;

import com.intuit.karate.JsonUtils;
import com.intuit.karate.core.MockHandlerHook;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *  @author ivangsa
 */
public class OpenApiValidatorHook implements MockHandlerHook {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenApiValidator4Karate openApiValidator;
    private OpenApi3 api;

    public OpenApiValidatorHook(OpenApiValidator4Karate openApiValidator) {
        super();
        this.openApiValidator = openApiValidator;
        this.api = openApiValidator.getApi();
    }

    @Override
    public void reload() {
        openApiValidator.reload();
        this.api = openApiValidator.getApi();
    }

    @Override
    public Response beforeRequest(Request request) {
        Operation operation = OpenApiValidator4Karate.findOperation(request.getMethod(), request.getPath(), api);
        if(operation != null) {
            logger.debug("Validating request for operationId {}", operation.getOperationId());
            OpenApiValidator4Karate.ValidationResults validationResults = openApiValidator.isValidRequest(request.getPath(), request.getMethod(), request.getBodyAsString(), OpenApiValidator4Karate.cast(request.getHeaders()), operation.getOperationId());
            if (!validationResults.isValid()) {
                Response response = new Response(400);
                response.setContentType("application/json");
                response.setHeader("access-control-allow-origin", "*");
                response.setBody(toJson("request", validationResults));
                return response;
            }
        } else {
            logger.warn("OperationId not found in openapi definition for " + request.getMethod() + " " + request.getPath());
        }
        return null;
    }

    @Override
    public Response afterScenarioSuccess(Request request, Response response, ScenarioEngine engine) {
        Operation operation = OpenApiValidator4Karate.findOperation(request.getMethod(), request.getPath(), api);

        if(operation != null) {
            logger.debug("Validating response for operationId {}", operation.getOperationId());
            OpenApiValidator4Karate.ValidationResults validationResults = openApiValidator.isValidResponse(response.getBodyAsString(), OpenApiValidator4Karate.cast(response.getHeaders()), operation.getOperationId(), response.getStatus());
            if(!validationResults.isValid()) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.setHeader("access-control-allow-origin", "*");
                response.setBody(toJson("response", validationResults));
            }
        } else {
            logger.warn("OperationId not found in openapi definition for " + request.getMethod() + " " + request.getPath());
        }

        return response;
    }

    public String toJson(String type, OpenApiValidator4Karate.ValidationResults validationResults) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        Map<String, List<String>> validations = new HashMap<>();
        validationResults.items.forEach(item -> {
            if(!validations.containsKey(item.left)) {
                validations.put(item.left, new ArrayList<>());
            }
            validations.get(item.left).add(item.right);
        });
        map.put("errors", validations);
        return JsonUtils.toJson(map);
    }
}
