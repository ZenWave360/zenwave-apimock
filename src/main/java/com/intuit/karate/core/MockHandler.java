/*
 * The MIT License
 *
 * Copyright 2020 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.core;

import com.intuit.karate.Json;
import com.intuit.karate.KarateException;
import com.intuit.karate.ScenarioActions;
import com.intuit.karate.StringUtils;
import com.intuit.karate.Suite;
import com.intuit.karate.core.compatibility.KarateCompatibility;
import com.intuit.karate.graal.JsValue;
import com.intuit.karate.http.HttpClientFactory;
import com.intuit.karate.http.HttpUtils;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.ResourceType;
import com.intuit.karate.http.Response;
import com.intuit.karate.http.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author pthomas3
 */
public class MockHandler implements ServerHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockHandler.class);

    private static final String REQUEST_BYTES = "requestBytes";
    private static final String REQUEST_PARAMS = "requestParams";
    private static final String REQUEST_PARTS = "requestParts";

    private static final String RESPONSE_DELAY = "responseDelay";

    private static final String PATH_MATCHES = "pathMatches";
    private static final String METHOD_IS = "methodIs";
    private static final String TYPE_CONTAINS = "typeContains";
    private static final String ACCEPT_CONTAINS = "acceptContains";
    private static final String HEADER_CONTAINS = "headerContains";
    private static final String PARAM_VALUE = "paramValue";
    private static final String PARAM_EXISTS = "paramExists";
    private static final String PATH_PARAMS = "pathParams";
    private static final String BODY_PATH = "bodyPath";

    private final List<Feature> featureList;
    private final Map<String, Object> args;
    private final LinkedHashMap<Feature, ScenarioRuntime> features = new LinkedHashMap<>(); // feature + holds global config and vars
    private final Map<String, Variable> globals = new HashMap<>();
    private boolean corsEnabled;

    protected static final ThreadLocal<Request> LOCAL_REQUEST = new ThreadLocal<>();
    private String prefix = null;

    private List<MockHandlerHook> handlerHooks = new ArrayList<>();

    public MockHandler(Feature feature) {
        this(feature, null);
    }

    public MockHandler(Feature feature, Map<String, Object> args) {
        this(Collections.singletonList(feature), args);
    }

    public MockHandler(List<Feature> features) {
        this(features, null);
    }

    public MockHandler(List<Feature> features, Map<String, Object> args) {
        this.featureList = features;
        this.args = args;
    }

    public MockHandler withPrefix(String prefix) {
        this.prefix = "/".equals(prefix) ? null : prefix;
        return this;
    }

    public MockHandler withHandlerHooks(List<MockHandlerHook> handlerHooks) {
        this.handlerHooks = handlerHooks;
        return this;
    }

    public MockHandler start() {
        reload();
        return this;
    }

    private Map<String, Variable> shallowCloneVariables(ScenarioEngine engine) {
        try {
            return engine.shallowCloneVariables();
        } catch (Throwable e) {
            try {
                Method method = ScenarioEngine.class.getDeclaredMethod("detachVariables", null);
                method.setAccessible(true);
                return (Map) method.invoke(engine);
            } catch (Exception ex) {
                logger.error("Unknown version of karate, couldn't find ScenarioEngine.shallowCloneVariables() method", ex);
                throw new RuntimeException("Unknown version of karate, couldn't find ScenarioEngine.shallowCloneVariables() method", ex);
            }
        }
    }

    public void reload() {
        for (MockHandlerHook hook : handlerHooks) {
            hook.reload();
        }
        this.featureList.replaceAll(feature -> Feature.read(feature.getResource()));
        for (Feature feature : featureList) {
            FeatureRuntime featureRuntime = KarateCompatibility.featureRuntimeOf(feature, args); // FeatureRuntime.of(forTempUse(HttpClientFactory.DEFAULT), feature, args);
            Scenario dummy = createDummyScenario(feature);
            ScenarioRuntime runtime = new ScenarioRuntime(featureRuntime, dummy);
            initRuntime(runtime);
            if (feature.isBackgroundPresent()) {
                // if we are within a scenario already e.g. karate.start(), preserve context
                ScenarioEngine prevEngine = ScenarioEngine.get();
                try {
                    ScenarioEngine.set(runtime.engine);
                    for (Step step : feature.getBackground().getSteps()) {
                        Result result = StepRuntime.execute(step, runtime.actions);
                        if (result.isFailed()) {
                            String message = "mock-server background failed - " + feature + ":" + step.getLine();
                            runtime.logger.error(message);
                            throw new KarateException(message, result.getError());
                        }
                    }
                } finally {
                    ScenarioEngine.set(prevEngine);
                }
            }
            corsEnabled = corsEnabled || runtime.engine.getConfig().isCorsEnabled();
            globals.putAll(shallowCloneVariables(runtime.engine));
            runtime.logger.info("mock server initialized: {}", feature);
            this.features.put(feature, runtime);
        }
        for (MockHandlerHook hook : handlerHooks) {
            hook.onSetup(features, globals);
        }
    }

    public static Scenario createDummyScenario(Feature feature) {
        FeatureSection section = new FeatureSection();
        section.setIndex(-1);
        Scenario dummy = new Scenario(feature, section, -1);
        section.setScenario(dummy);
        return dummy;
    }

    private void initRuntime(ScenarioRuntime runtime) {
        runtime.engine.setVariable(PATH_MATCHES, (Function<String, Boolean>) this::pathMatches);
        runtime.engine.setVariable(PARAM_EXISTS, (Function<String, Boolean>) this::paramExists);
        runtime.engine.setVariable(PARAM_VALUE, (Function<String, String>) this::paramValue);
        runtime.engine.setVariable(METHOD_IS, (Function<String, Boolean>) this::methodIs);
        runtime.engine.setVariable(TYPE_CONTAINS, (Function<String, Boolean>) this::typeContains);
        runtime.engine.setVariable(ACCEPT_CONTAINS, (Function<String, Boolean>) this::acceptContains);
        runtime.engine.setVariable(HEADER_CONTAINS, (BiFunction<String, String, Boolean>) this::headerContains);
        runtime.engine.setVariable(BODY_PATH, (Function<String, Object>) this::bodyPath);
        runtime.engine.init();
    }

    private static final Result PASSED = passed();
    private static final String ALLOWED_METHODS = "GET, HEAD, POST, PUT, DELETE, PATCH";

    @Override
    public synchronized Response handle(Request req) { // note the [synchronized]
        if (corsEnabled && "OPTIONS".equals(req.getMethod())) {
            Response response = new Response(200);
            response.setHeader("Allow", ALLOWED_METHODS);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
            List<String> requestHeaders = req.getHeaderValues("Access-Control-Request-Headers");
            if (requestHeaders != null) {
                response.setHeader("Access-Control-Allow-Headers", requestHeaders);
            }
            return response;
        }
        String path = ("/" + req.getPath()).replaceFirst("^//", "/");
        if (prefix != null && path.startsWith(prefix)) {
            req.setPath(path.substring(prefix.length()));
        }
        // rare case when http-client is active within same jvm
        // snapshot existing thread-local to restore
        ScenarioEngine prevEngine = ScenarioEngine.get();
        for (MockHandlerHook hook : this.handlerHooks) {
            Response response = hook.beforeRequest(req);
            if(response != null){
                logger.info("Returning response on 'beforeRequest' from hook: {}", hook);
                return response;
            }
        }
        for (Map.Entry<Feature, ScenarioRuntime> entry : this.features.entrySet()) {
            Feature feature = entry.getKey();
            ScenarioRuntime runtime = entry.getValue();
            // important for graal to work properly
            Thread.currentThread().setContextClassLoader(runtime.featureRuntime.suite.classLoader);
            LOCAL_REQUEST.set(req);
            req.processBody();
            ScenarioEngine engine = createScenarioEngine(req, runtime);
            Map<String, List<Map<String, Object>>> parts = req.getMultiParts();
            if (parts != null) {
                engine.setHiddenVariable(REQUEST_PARTS, parts);
            }
            for (FeatureSection fs : feature.getSections()) {
                if (fs.isOutline()) {
                    runtime.logger.warn("skipping scenario outline - {}:{}", feature, fs.getScenarioOutline().getLine());
                    break;
                }
                Scenario scenario = fs.getScenario();
                if (isMatchingScenario(scenario, engine)) {
                    for (MockHandlerHook hook : this.handlerHooks) {
                        Response response = hook.beforeScenario(req, engine);
                        if(response != null){
                            logger.info("Returning response on 'beforeScenario' from hook: {}", hook);
                            return response;
                        }
                    }
                    Map<String, Object> configureHeaders;
                    Variable response, responseStatus, responseHeaders, responseDelay;
                    ScenarioActions actions = new ScenarioActions(engine);
                    Result result = PASSED;
                    result = executeScenarioSteps(feature, runtime, scenario, actions, result);
                    engine.mockAfterScenario();
                    configureHeaders = engine.mockConfigureHeaders();
                    response = engine.vars.remove(ScenarioEngine.RESPONSE);
                    responseStatus = engine.vars.remove(ScenarioEngine.RESPONSE_STATUS);
                    responseHeaders = engine.vars.remove(ScenarioEngine.RESPONSE_HEADERS);
                    responseDelay = engine.vars.remove(RESPONSE_DELAY);
                    globals.putAll(shallowCloneVariables(engine));
                    Response res = new Response(200);
                    if (result.isFailed()) {
                        response = new Variable(result.getError().getMessage());
                        responseStatus = new Variable(500);
                    } else {
                        if (corsEnabled) {
                            res.setHeader("Access-Control-Allow-Origin", "*");
                        }
                        res.setHeaders(configureHeaders);
                        if (responseHeaders != null && responseHeaders.isMap()) {
                            res.setHeaders(responseHeaders.getValue());
                        }
                        if (responseDelay != null) {
                            res.setDelay(responseDelay.getAsInt());
                        }
                    }
                    if (response != null && !response.isNull()) {
                        res.setBody(response.getAsByteArray());
                        if (res.getContentType() == null) {
                            ResourceType rt = ResourceType.fromObject(response.getValue());
                            if (rt != null) {
                                res.setContentType(rt.contentType);
                            }
                        }
                    }
                    if (responseStatus != null) {
                        res.setStatus(responseStatus.getAsInt());
                    }
                    if (prevEngine != null) {
                        ScenarioEngine.set(prevEngine);
                    }
                    if(result.isFailed()) {
                        for (MockHandlerHook hook : this.handlerHooks) {
                            logger.trace("Running 'afterScenarioFailure' from hook: {}", hook);
                            res = hook.afterScenarioFailure(req, res, engine);
                        }
                    } else {
                        for (MockHandlerHook hook : this.handlerHooks) {
                            logger.trace("Running 'afterScenarioSuccess' from hook: {}", hook);
                            res = hook.afterScenarioSuccess(req, res, engine);
                        }
                    }
                    return res;
                }
            }
        }
        Response res = new Response(404);
        for (MockHandlerHook hook : this.handlerHooks) {
            logger.info("Returning response on 'noMatchingScenario' from hook: {}", hook);
            ScenarioRuntime runtime = features.values().stream().findFirst().get();
            res = hook.noMatchingScenario(req, res, createScenarioEngine(req, runtime));
        }
        logger.warn("no scenarios matched, returning 404: {}", req); // NOTE: not logging with engine.logger
        if (prevEngine != null) {
            ScenarioEngine.set(prevEngine);
        }
        return res;
    }

    private Result executeScenarioSteps(Feature feature, ScenarioRuntime runtime, Scenario scenario, ScenarioActions actions, Result result) {
        for (Step step : scenario.getSteps()) {
            result = StepRuntime.execute(step, actions);
            if (result.isAborted()) {
                runtime.logger.debug("abort at {}:{}", feature, step.getLine());
                break;
            }
            if (result.isFailed()) {
                String message = "server-side scenario failed, " + feature + ":" + step.getLine()
                        + "\n" + step.toString() + "\n" + result.getError().getMessage();
                runtime.logger.error(message);
                break;
            }
        }
        return result;
    }

    private ScenarioEngine createScenarioEngine(Request req, ScenarioRuntime runtime) {
        ScenarioEngine engine = KarateCompatibility.newScenarioEngine(runtime, new HashMap<>(globals));// new ScenarioEngine(runtime, new HashMap<>(globals));
        ScenarioEngine.set(engine);
        engine.init();
        engine.setVariable(ScenarioEngine.REQUEST_URL_BASE, req.getUrlBase());
        engine.setVariable(ScenarioEngine.REQUEST_URI, req.getPath());
        engine.setVariable(ScenarioEngine.REQUEST_METHOD, req.getMethod());
        engine.setVariable(ScenarioEngine.REQUEST_HEADERS, req.getHeaders());
        engine.setVariable(ScenarioEngine.REQUEST, req.getBodyConverted());
        engine.setVariable(REQUEST_PARAMS, req.getParams());
        engine.setVariable(REQUEST_BYTES, req.getBody());
        return engine;
    }

    private boolean isMatchingScenario(Scenario scenario, ScenarioEngine engine) {
        String expression = StringUtils.trimToNull(scenario.getName() + scenario.getDescription());
        if (expression == null) {
            engine.logger.debug("default scenario matched at line: {} - {}", scenario.getLine(), engine.getVariable(ScenarioEngine.REQUEST_URI));
            return true;
        }
        try {
            Variable v = engine.evalJs(expression);
            if (v.isTrue()) {
                engine.logger.debug("scenario matched at {} line {}: {}", scenario.getFeature().getResource().getFile(), scenario.getLine(), expression);
                return true;
            } else {
                engine.logger.trace("scenario skipped at {} line {}: {}", scenario.getFeature().getResource().getFile(), scenario.getLine(), expression);
                return false;
            }
        } catch (Exception e) {
            engine.logger.warn("scenario match evaluation failed at {} line {}: {} - {}", scenario.getFeature().getResource().getFile(), scenario.getLine(), expression, e + "");
            return false;
        }
    }

    public boolean pathMatches(String pattern) {
        String uri = LOCAL_REQUEST.get().getPath();
        if (uri.equals(pattern)) {
            return true;
        }
        Map<String, String> pathParams = HttpUtils.parseUriPattern(pattern, uri);
        if (pathParams == null) {
            return false;
        } else {
            ScenarioEngine.get().setVariable(PATH_PARAMS, pathParams);
            return true;
        }
    }

    public boolean paramExists(String name) {
        Map<String, List<String>> params = LOCAL_REQUEST.get().getParams();
        return params != null && params.containsKey(name);

    }

    public String paramValue(String name) {
        return LOCAL_REQUEST.get().getParam(name);
    }

    public boolean methodIs(String name) { // TODO no more supporting array arg
        return LOCAL_REQUEST.get().getMethod().equalsIgnoreCase(name);
    }

    public boolean typeContains(String text) {
        String contentType = LOCAL_REQUEST.get().getContentType();
        return contentType != null && contentType.contains(text);
    }

    public boolean acceptContains(String text) {
        String acceptHeader = LOCAL_REQUEST.get().getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains(text);
    }

    public boolean headerContains(String name, String value) {
        List<String> values = LOCAL_REQUEST.get().getHeaderValues(name);
        if (values != null) {
            for (String v : values) {
                if (v.contains(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Object bodyPath(String path) {
        Object body = LOCAL_REQUEST.get().getBodyConverted();
        if (body == null) {
            return null;
        }
        if (path.startsWith("/")) {
            Variable v = ScenarioEngine.evalXmlPath(new Variable(body), path);
            if (v.isNotPresent()) {
                return null;
            } else {
                return JsValue.fromJava(v.getValue());
            }
        } else {
            Json json = Json.of(body);
            Object result;
            try {
                result = json.get(path);
            } catch (Exception e) {
                return null;
            }
            return JsValue.fromJava(result);
        }
    }

    private static Result passed() {
        try {
            Method method = Result.class.getMethod("passed", long.class, long.class);
            return (Result) method.invoke(null, 0L, 0L);
        } catch (NoSuchMethodException ignored) {
            // ignored
        }
        catch(InvocationTargetException|IllegalAccessException e) {
            System.err.println("Error invoking method: Result.passed(0, 0). Please upgrade to karate +1.4.1"); // karate 1.4.1
        }
        try {
            Method method = Result.class.getMethod("passed", long.class);
            return (Result) method.invoke(null, 0L);
        } catch (NoSuchMethodException ignored) {
            // ignored
        }
        catch(InvocationTargetException|IllegalAccessException e) {
            System.err.println("Error invoking method: Result.passed(0)"); // karate 1.4.0
        }
        return Result.passed(0, 0);
    }
}
