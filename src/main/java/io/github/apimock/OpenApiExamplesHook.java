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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *  @author ivangsa
 */
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
        if(!globals.containsKey(UUID)) {
            globals.put(UUID, new Variable((Supplier<String>) this::uuid));
        }
        if(!globals.containsKey(SEQUENCE_NEXT)) {
            globals.put(SEQUENCE_NEXT, new Variable((Supplier<Integer>) this::sequenceNext));
        }
        if(!globals.containsKey(NOW)) {
            globals.put(NOW, new Variable((Function<String,String>) this::now));
        }
        if(!globals.containsKey(DATE)) {
            globals.put(DATE, new Variable((BiFunction<String, String, String>) this::date));
        }

        if(api.getComponents() != null && api.getComponents().getExamples() != null) {
            ScenarioEngine engine = new ScenarioEngine(features.values().stream().findFirst().get(), new HashMap<>(globals));
            engine.init();
            for (Example example : api.getComponents().getExamples().values()) {
                String karateVar = (String) firstNotNull(example.getExtensions(), Collections.emptyMap()).get("x-apimock-karate-var");
                if(isNotEmpty(karateVar)) {
                    Object seeds = firstNotNull(firstNotNull(example.getExtensions(), Collections.emptyMap()).get("x-apimock-seed"), 1);
                    Map<String, Object> seedsMap = seeds instanceof Integer? defaultRootSeed((Integer) seeds): (Map<String, Object>) seeds;
                    Object seededExample = seed(example.getValue(), seedsMap);

                    try {
                        Map<String, String> transforms = (Map) firstNotNull(example.getExtensions(), Collections.emptyMap()).get("x-apimock-transform");
                        String json = processObjectDynamicProperties(engine, transforms, seededExample);
                        Variable exampleVariable = new Variable(Json.of(json).value());
                        addExamplesVariableToKarateGlobals(globals, karateVar, exampleVariable);
                    } catch (Exception e) {
                        logger.error("Error setting openapi examples {} into karate globals ({})", karateVar, e.getMessage(), e);
                    }
                }
            }
        }
    }

    private Map<String, Object> defaultRootSeed(Integer seed) {
        Map<String, Object> seedMap = new HashMap<>();
        seedMap.put("$", seed);
        return seedMap;
    }

    private Object seed(Object value, Map<String, Object> seedsMap) {
        Json json = Json.of(value);
        for (Map.Entry<String, Object> seedEntry : seedsMap.entrySet()) {
            int seed = (Integer) seedEntry.getValue();
            if(seed == 1) {
                continue;
            }
            String seedPath = String.valueOf(seedEntry.getKey());
            Object inner = json.get(seedPath);
            Object seeded = seedValue(inner, seed);
            json = replace(json, seedPath, seeded);
        }
        return json.get("$");
    }


    private List seedValue(Object value, int seed) {
        List seeded = new ArrayList();
        for (int i = 0; i < seed; i++) {
            if(value instanceof List) {
                seeded.addAll((List) JsonUtils.deepCopy(value));
            } else {
                seeded.add(JsonUtils.deepCopy(value));
            }
        }
        return seeded;
    }

    private Json replace(Json json, String path, Object replacement) {
        if("$".equals(path)) {
            return Json.of(replacement);
        }
        json.set(path, replacement);
        return json;
    }

    private void addExamplesVariableToKarateGlobals(Map<String, Variable> globals, String karateVar, Variable examplesVariable) {
        if(!globals.containsKey(karateVar)) {
            globals.put(karateVar, examplesVariable);
        } else {
            Variable karateVariable = globals.get(karateVar);
            if(karateVariable.isList()) {
                if(examplesVariable.isList()) {
                    ((List)karateVariable.getValue()).addAll(examplesVariable.getValue());
                } else {
                    ((List)karateVariable.getValue()).add(examplesVariable.getValue());
                }
            }
            if(karateVariable.isMap() && examplesVariable.isMap()) {
                ((Map)karateVariable.getValue()).putAll(examplesVariable.getValue());
            }
        }
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
        loadPathParams(req.getPath(), (String) operation.getExtensions().get("x-apimock-internal-path"), engine);

        if(!responses.isEmpty()) {
            String status = responses.keySet().stream().findFirst().get();
            org.openapi4j.parser.model.v3.Response oasRespose = responses.get(status);
            // match media type from request
            String contentType = getContentType(req);
            Map.Entry<String, MediaType> mediaTypeEntry = oasRespose.getContentMediaTypes().entrySet().stream()
                    .filter(e -> e.getKey().startsWith(contentType))
                    .findFirst().orElse(new AbstractMap.SimpleEntry("", new MediaType()));

            if(mediaTypeEntry.getValue().getExamples() == null && mediaTypeEntry.getValue().getExample() != null) {
                logger.debug("Returning default example in openapi for operationId {}", operation.getOperationId());
                response = new Response(Integer.valueOf(status.toLowerCase().replaceAll("x", "0")));
                response.setBody(processObjectDynamicProperties(engine, null, mediaTypeEntry.getValue().getExample()));
                response.setContentType(mediaTypeEntry.getKey());
                response.setHeader("access-control-allow-origin", "*");
                unloadPathParams(engine);
                return response;
            }

            for (Map.Entry<String, Example> exampleEntry: mediaTypeEntry.getValue().getExamples().entrySet()) {
                Map<String, Object> extensions = exampleEntry.getValue().getExtensions();
                if(extensions == null) {
                    continue;
                }
                Object when = extensions.get("x-apimock-when");
                Map<String, String> generators = (Map<String, String>) extensions.get("x-apimock-transform");
                if(when != null) {
                    if(evalBooleanJs(engine, when.toString())) {
                        logger.debug("Found example[{}] for x-apimock-when {} in openapi for operationId {}", exampleEntry.getKey(), when, operation.getOperationId());
                        Example example = exampleEntry.getValue();
                        logger.debug("Returning example in openapi for operationId {}", operation.getOperationId());
                        response = new Response(Integer.valueOf(status.toLowerCase().replaceAll("x", "0")));
                        response.setBody(processObjectDynamicProperties(engine, generators, example.getValue()));
                        response.setContentType(mediaTypeEntry.getKey());
                        response.setHeader("access-control-allow-origin", "*");
                        break;
                    }
                }
            }
        }

        unloadPathParams(engine);
        return response;
    }

    private String getContentType(Request req) {
        String contentType = firstNotNull(req.getContentType(), "application/json");
        return contentType.contains(";")? contentType.substring(0, contentType.indexOf(";")) : contentType;
    }

    protected void evaluateJsAndReplacePath(ScenarioEngine engine, Json json, String path, String js) {
        Object replacement = evalJsAsObject(engine, js);
        try {
            if (replacement != null) {
                json.set(path, replacement);
            }
        } catch (Exception e) {
            logger.error("Error replacing jsonPath: {} ({})", path, e.getMessage());
        }
    }

    Pattern generatorsPattern = Pattern.compile("\\{\\{(.+)\\}\\}");
    protected String processObjectDynamicProperties(ScenarioEngine engine, Map<String, String> generators, Object value) {
        if(value == null) {
            return null;
        }
        Json json = Json.of(value);
        if(generators != null) {
            for (Map.Entry<String, String> entry: generators.entrySet()){
                if(entry.getKey().startsWith("$[*]") && json.isArray()) {
                    List list = json.asList();
                    for(int i = 0; i < list.size(); i++) {
                        evaluateJsAndReplacePath(engine, json, entry.getKey().replace("$[*]", "$[" + i + "]"), entry.getValue());
                    }
                } else {
                    evaluateJsAndReplacePath(engine, json, entry.getKey(), entry.getValue());
                }
            }
        }

        String jsonString = json.toStringPretty();
        final Matcher matcher = generatorsPattern.matcher(jsonString);
        while (matcher.find()) {
            String match = matcher.group(0);
            String script = matcher.group(1);
            logger.trace("Processing inline replacement for script: {}", script);
            String replacement = evalJsAsString(engine, script);
            if(replacement != null) {
                jsonString = jsonString.replace(match, replacement);
            }
        }
        return JsonUtils.toStrictJson(jsonString);
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
            Object result = engine.evalJs(js).getValue();
            return result != null? result : "";
        } catch (Exception e) {
            logger.error("Error evaluating script: '{}' ({})", js, e.getMessage());
            return null;
        }
    }

    private String uuid() {
        return java.util.UUID.randomUUID().toString();
    }

    private int sequenceNext = 0;
    private int sequenceNext() {
        return sequenceNext++;
    }

    private String now(String format) {
        Date now = new Date();
        return new SimpleDateFormat(format).format(now);
    }

    private String date(String format, String intervalExpression) {
        int length = intervalExpression.length();
        String intervalString = intervalExpression.trim().substring(0, length - 1);
        String range = intervalExpression.trim().substring(length);
        int amount = Integer.parseInt(intervalString);
        int field = Calendar.DATE;
        if(range.equalsIgnoreCase("d")) {
            field = Calendar.DATE;
        }
        if(range.equalsIgnoreCase("h")) {
            field = Calendar.HOUR;
        }
        if(range.equalsIgnoreCase("s")) {
            field = Calendar.SECOND;
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(field, amount);
        return new SimpleDateFormat(format).format(calendar.getTime());
    }

    private <T> T firstNotNull(T one, T two) {
        return one != null? one : two;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().equals("");
    }

    private static final String UUID = "uuid";
    private static final String SEQUENCE_NEXT = "sequenceNext";
    private static final String NOW = "now";
    private static final String DATE = "date";
}
