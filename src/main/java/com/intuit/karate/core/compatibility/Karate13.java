package com.intuit.karate.core.compatibility;

import com.intuit.karate.Logger;
import com.intuit.karate.Suite;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureCall;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;
import com.intuit.karate.http.HttpClientFactory;

import java.util.HashMap;
import java.util.Map;

public class Karate13 implements Karate {

    @Override
    public FeatureRuntime featureRuntimeOf(Feature feature, Map<String, Object> args) {
        return FeatureRuntime.of(Suite.forTempUse(HttpClientFactory.DEFAULT), new FeatureCall(feature), args);
    }

    @Override
    public ScenarioEngine newScenarioEngine(ScenarioRuntime runtime, Map<String, Variable> globalVars) {
        return new ScenarioEngine(runtime.engine.getConfig(), runtime, globalVars, runtime.logger);
    }

}
