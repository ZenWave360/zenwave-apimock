package com.intuit.karate.core.compatibility;

import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;

import java.util.Map;

public class KarateCompatibility {

    private static Karate compatibility;
    static {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("com.intuit.karate.core.FeatureCall");
            compatibility = new Karate13();
        } catch (ClassNotFoundException e) {
            compatibility = new Karate12();
        }
    }

    public static FeatureRuntime featureRuntimeOf(Feature feature, Map<String, Object> args) {
        return compatibility.featureRuntimeOf(feature, args);
    }

    public static ScenarioEngine newScenarioEngine(ScenarioRuntime runtime, Map<String, Variable> globalVars) {
        return compatibility.newScenarioEngine(runtime, globalVars);
    }
}
