package io.github.apimocks;

import com.intuit.karate.JsonUtils;
import com.intuit.karate.core.MockHandlerHook;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiValidatorHook implements MockHandlerHook {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenApiValidator4Karate openApiValidator;
    private final OpenApi3 api;

    public OpenApiValidatorHook(OpenApiValidator4Karate openApiValidator) {
        super();
        this.openApiValidator = openApiValidator;
        this.api = openApiValidator.getApi();
    }

    @Override
    public Response beforeRequest(Request request) {
        Operation operation = OpenApiValidator4Karate.findOperation(request.getMethod(), request.getPath(), api);
        if(operation != null) {
            logger.debug("Validating response for operationId {}", operation.getOperationId());
            ValidationResults validationResults = openApiValidator.isValidRequest(request.getPath(), request.getMethod(), request.getBodyAsString(), OpenApiValidator4Karate.cast(request.getHeaders()), operation.getOperationId());
            if (!validationResults.isValid()) {
                Response response = new Response(400);
                response.setContentType("application/json");
                response.setHeader("access-control-allow-origin", "*");
                response.setBody(JsonUtils.toJson(validationResults));
                return response;
            }
        }
        return null;
    }

    @Override
    public Response afterScenarioSuccess(Request request, Response response, ScenarioEngine engine) {
        Operation operation = OpenApiValidator4Karate.findOperation(request.getMethod(), request.getPath(), api);

        if(operation != null) {
            logger.debug("Validating response for operationId {}", operation.getOperationId());
            ValidationResults validationResults = openApiValidator.isValidResponse(response.getBodyAsString(), OpenApiValidator4Karate.cast(response.getHeaders()), operation.getOperationId(), response.getStatus());
            if(!validationResults.isValid()) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.setHeader("access-control-allow-origin", "*");
                response.setBody(JsonUtils.toJson(validationResults));
            }
        } else {
            logger.warn("OperationId not found in openapi definition for " + request.getMethod() + " " + request.getPath());
        }

        return response;
    }
}
