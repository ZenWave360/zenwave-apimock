package io.github.apimocks;

import com.intuit.karate.JsonUtils;
import com.intuit.karate.core.MockHandlerHook;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import org.openapi4j.parser.model.v3.Example;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OpenApiExamplesHook implements MockHandlerHook {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenApiValidator4Karate openApiValidator;
    private final OpenApi3 api;

    public OpenApiExamplesHook(OpenApiValidator4Karate openApiValidator) {
        super();
        this.openApiValidator = openApiValidator;
        this.api = openApiValidator.getApi();
    }

    @Override
    public Response noMatchingScenario(Request req, Response response, ScenarioEngine engine) {

        Operation operation = OpenApiValidator4Karate.findOperation(req.getMethod(), req.getPath(), api);
        if(operation == null) {
            logger.debug("Operation not found for {}", req.getPath());
            return response;
        }
        logger.debug("Searching examples in openapi definition for operationId {}", operation.getOperationId());
        Map<String, org.openapi4j.parser.model.v3.Response> responses = OpenApiValidator4Karate.find2xxResponses(operation);
        if(!responses.isEmpty()) {
            String status = responses.keySet().stream().findFirst().get();
            org.openapi4j.parser.model.v3.Response oasRespose = responses.get(status);
            Map.Entry<String, MediaType> mediaTypeEntry = oasRespose.getContentMediaTypes().entrySet().stream().findFirst().get();
            Map<String, Example> examples = mediaTypeEntry.getValue().getExamples();
            Example example = null;
            if(examples != null) {
                for (Map.Entry<String, Example> exampleEntry: examples.entrySet()) {
                    Object when = exampleEntry.getValue().getExtensions().get("x-apimock-when");
                    if(when != null) {
                        logger.debug("Found example named {} for x-apimock-when {} in openapi for operationId {}", exampleEntry.getKey(), when, operation.getOperationId());
                        example = exampleEntry.getValue();
                    }
                }
            }
            Object exampleValue = example != null? example.getValue() : mediaTypeEntry.getValue().getExample();
            if(example != null) {
                logger.debug("Returning example in openapi for operationId {}", operation.getOperationId());
                response = new Response(Integer.valueOf(status.toLowerCase().replaceAll("x", "0")));
                response.setBody(JsonUtils.toJson(exampleValue));
                response.setContentType(mediaTypeEntry.getKey());
                response.setHeader("access-control-allow-origin", "*");
            }
        }

        return response;
    }
}
