package com.intuit.karate.core.compatibility;

import com.intuit.karate.Logger;
import com.intuit.karate.Suite;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;
import com.intuit.karate.http.HttpClientFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Karate12 implements Karate {

    @Override
    public FeatureRuntime featureRuntimeOf(Feature feature, Map<String, Object> args) {
        try {
            return (FeatureRuntime) FeatureRuntime.class.getMethod("of", Suite.class, Feature.class, Map.class)
                    .invoke(null, Suite.forTempUse(HttpClientFactory.DEFAULT), feature, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ScenarioEngine newScenarioEngine(ScenarioRuntime runtime, Map<String, Variable> globalVars) {
        try {
            return ScenarioEngine.class.getConstructor(ScenarioRuntime.class, Map.class).newInstance(runtime, globalVars);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Suite forTempUse(HttpClientFactory hcf) {
        try {
            return Suite.forTempUse(hcf);
        } catch (Throwable e) {
            try {
                return (Suite) Suite.class.getMethod("forTempUse").invoke(null);
            } catch (Exception ex) {
                throw new RuntimeException("Unknown version of karate, couldn't find Suite.forTempUse() method", ex);
            }
        }
    }
}
