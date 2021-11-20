package io.github.apimock;

import com.intuit.karate.Json;
import com.intuit.karate.JsonUtils;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.MockHandlerHook;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;
import com.intuit.karate.http.HttpUtils;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import org.openapi4j.parser.model.v3.Example;
import org.openapi4j.parser.model.v3.MediaType;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.openapi4j.parser.model.v3.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenApiExamplesHook implements MockHandlerHook {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenApiValidator4Karate openApiValidator;
    private OpenApi3 api;

    public OpenApiExamplesHook(OpenApiValidator4Karate openApiValidator) {
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
    public void onSetup(Map<Feature, ScenarioRuntime> features, Map<String, Variable> globals) {
        // TODO trasform examples before sending to karate
        // TODO seed examples
        if(api.getComponents() != null && api.getComponents() != null) {
            for (Map.Entry<String, Example> entry : api.getComponents().getExamples().entrySet()) {
                try {
                    Variable examplesVariable = new Variable(entry.getValue().getValue());
                    if(!globals.containsKey(entry.getKey())) {
                        globals.put(entry.getKey(), examplesVariable);
                    } else {
                        Variable karateVariable = globals.get(entry.getKey());
                        if(karateVariable.isList() && examplesVariable.isList()) {
                            ((List)karateVariable.getValue()).addAll(examplesVariable.getValue());
                        }
                        if(karateVariable.isMap() && examplesVariable.isMap()) {
                            ((Map)karateVariable.getValue()).putAll(examplesVariable.getValue());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error setting openapi examples {} into karate globals ({})", entry.getKey(), e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Response beforeScenario(Request req, ScenarioEngine engine) {
        // TODO consider moving this to onSetup
        if(!engine.vars.containsKey(UUID)) {
            engine.setVariable(UUID, (Function<Void, String>) this::uuid);
        }
        if(!engine.vars.containsKey(NOW)) {
            engine.setVariable(NOW, (Function<String,String>) this::now);
        }
        return null;
    }

    @Override
    public Response afterScenarioSuccess(Request req, Response response, ScenarioEngine engine) {
        // TODO trasform karate response payload
        return response;
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
            // TODO match media type from request
            Map.Entry<String, MediaType> mediaTypeEntry = oasRespose.getContentMediaTypes().entrySet().stream().findFirst().get();
            Map<String, Example> examples = mediaTypeEntry.getValue().getExamples();
            if(examples != null) {
                for (Map.Entry<String, Example> exampleEntry: examples.entrySet()) {
                    Object when = exampleEntry.getValue().getExtensions().get("x-apimock-when");
                    Map<String, String> generators = (Map<String, String>) exampleEntry.getValue().getExtensions().get("x-apimock-transform");
                    if(when != null) {
                        loadPathParams(req.getPath(), (String) operation.getExtensions().get("x-apimock-internal-path"), engine);
                        if(evalBooleanJs(engine, when.toString())) {
                            logger.debug("Found example named {} for x-apimock-when {} in openapi for operationId {}", exampleEntry.getKey(), when, operation.getOperationId());
                            Example example = exampleEntry.getValue();
                            logger.debug("Returning example in openapi for operationId {}", operation.getOperationId());
                            response = new Response(Integer.valueOf(status.toLowerCase().replaceAll("x", "0")));
                            response.setBody(processExampleBody(engine, generators, example));
                            response.setContentType(mediaTypeEntry.getKey());
                            response.setHeader("access-control-allow-origin", "*");
                        }
                        unloadPathParams(engine);
                    }
                }
            }
        }

        return response;
    }

    Pattern generatorsPattern = Pattern.compile("\\{\\{(.+)\\}\\}");
    protected String processExampleBody(ScenarioEngine engine, Map<String, String> generators, Example example) {
        Json json = Json.of(example.getValue());
        if(generators != null) {
            for (Map.Entry<String, String> entry: generators.entrySet()){
                Object replacement = evalJsAsObject(engine, entry.getValue());
                try {
                    json.set(entry.getKey(), replacement);
                } catch (Exception e) {
                    logger.error("Error replacing jsonPath: {} ({})", e.getMessage());
                }
            }
        }

        String jsonString = json.toStringPretty();
        final Matcher matcher = generatorsPattern.matcher(jsonString);
        while (matcher.find()) {
            String match = matcher.group(0);
            String script = matcher.group(1);
            logger.debug("Processing replacement for generator script: {}", script);
            String replacement = evalJsAsString(engine, script);
            jsonString = jsonString.replace(match, replacement);
        }
        return jsonString;
    }

    private void loadPathParams(String uri, String pattern, ScenarioEngine engine) {
        Map<String, String> pathParams = HttpUtils.parseUriPattern(pattern, uri);
        if (pathParams != null) {
            engine.setVariable("pathParams", pathParams);
        }
    }

    private void unloadPathParams(ScenarioEngine engine) {
        engine.setVariable("pathParams", null);
    }

    private boolean evalBooleanJs(ScenarioEngine engine, String js) {
        try {
            return engine.evalJs(js).isTrue();
        } catch (Exception e) {
            logger.error("Error evaluating boolean script: '{}' ({})", js, e.getMessage());
            return false;
        }
    }

    private String evalJsAsString(ScenarioEngine engine, String js) {
        try {
            return engine.evalJs(js).getAsString();
        } catch (Exception e) {
            logger.error("Error evaluating string script: '{}' ({})", js, e.getMessage());
            return null;
        }
    }

    private Object evalJsAsObject(ScenarioEngine engine, String js) {
        try {
            return engine.evalJs(js).getValue();
        } catch (Exception e) {
            logger.error("Error evaluating script: '{}' ({})", js, e.getMessage());
            return null;
        }
    }

    private String uuid(Void unused) {
        return java.util.UUID.randomUUID().toString();
    }

    private String now(String format) {
        Date now = new Date();
        return new SimpleDateFormat(format).format(now);
    }

    private static final String UUID = "uuid";
    private static final String NOW = "now";
}
