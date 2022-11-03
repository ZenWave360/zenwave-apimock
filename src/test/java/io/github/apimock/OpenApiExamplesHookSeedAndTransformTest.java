package io.github.apimock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.intuit.karate.JsonUtils;
import com.intuit.karate.Suite;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.MockHandler;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;
import com.intuit.karate.core.compatibility.KarateCompatibility;
import com.intuit.karate.http.HttpClientFactory;
import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenApiExamplesHookSeedAndTransformTest {

    Map<Feature, ScenarioRuntime> createTestFeatureScenarioRuntimeMap() {
        Feature feature = Feature.read("classpath:io/github/apimock/default.feature");
        FeatureRuntime featureRuntime = KarateCompatibility.featureRuntimeOf(feature, new HashMap<>()); // FeatureRuntime.of(Suite.forTempUse(HttpClientFactory.DEFAULT), feature, new HashMap<>());
        ScenarioRuntime runtime = new ScenarioRuntime(featureRuntime, MockHandler.createDummyScenario(feature));
        Map<Feature, ScenarioRuntime> featureScenarioRuntimeMap = new HashMap();
        featureScenarioRuntimeMap.put(feature, runtime);
        return featureScenarioRuntimeMap;
    }

    @Test
    public void test_transforms_value() throws Exception {
        Map<String, Variable> globalVars = new HashMap<>();
        globalVars.put("variable", new Variable(new ArrayList<>()));
        Map<Feature, ScenarioRuntime> featureScenarioRuntimeMap = createTestFeatureScenarioRuntimeMap();

        OpenApiExamplesHook examplesHook = new OpenApiExamplesHook(OpenApiValidator4Karate.fromURL(MockServer.getURL("classpath:openapi-examples/transforms-value.yml")));
        examplesHook.onSetup(featureScenarioRuntimeMap, globalVars);
        Object value = globalVars.get("variable").getValue();

        assertThat(JsonPath.read(value, "$[0].id"), is(1));
        assertThat(JsonPath.read(value, "$[0].status"), not("before-transform"));
    }

    @Test
    public void test_transforms_array_items() throws Exception {
        Map<String, Variable> globalVars = new HashMap<>();
        globalVars.put("variable", new Variable(new ArrayList<>()));
        Map<Feature, ScenarioRuntime> featureScenarioRuntimeMap = createTestFeatureScenarioRuntimeMap();

        OpenApiExamplesHook examplesHook = new OpenApiExamplesHook(OpenApiValidator4Karate.fromURL(MockServer.getURL("classpath:openapi-examples/transforms-array-items.yml")));
        examplesHook.onSetup(featureScenarioRuntimeMap, globalVars);

        Object value = globalVars.get("variable").getValue();
        assertThat(JsonPath.read(value, "$[*].id"), hasItems(1, 2));
    }


    @Test
    public void test_when_seed_is_an_integer() throws Exception {
        Map<String, Variable> globalVars = new HashMap<>();
        globalVars.put("variable", new Variable(new ArrayList<>()));
        Map<Feature, ScenarioRuntime> featureScenarioRuntimeMap = createTestFeatureScenarioRuntimeMap();

        OpenApiExamplesHook examplesHook = new OpenApiExamplesHook(OpenApiValidator4Karate.fromURL(MockServer.getURL("classpath:openapi-examples/seed-as-integer.yml")));
        examplesHook.onSetup(featureScenarioRuntimeMap, globalVars);

        Object value = globalVars.get("variable").getValue();
        assertThat(JsonPath.read(value, "$[*]"), hasSize(10));
    }

    @Test
    public void test_when_seed_is_a_map() throws Exception {
        Map<String, Variable> globalVars = new HashMap<>();
        globalVars.put("variable", new Variable(new ArrayList<>()));
        Map<Feature, ScenarioRuntime> featureScenarioRuntimeMap = createTestFeatureScenarioRuntimeMap();

        OpenApiExamplesHook examplesHook = new OpenApiExamplesHook(OpenApiValidator4Karate.fromURL(MockServer.getURL("classpath:openapi-examples/seed-as-map.yml")));
        examplesHook.onSetup(featureScenarioRuntimeMap, globalVars);

        Object value = globalVars.get("variable").getValue();
        assertThat(JsonPath.read(value, "$[0].data"), hasSize(10));
    }
}
