package com.intuit.karate.core.compatibility;

import com.intuit.karate.Logger;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureRuntime;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Variable;

import java.util.HashMap;
import java.util.Map;

public interface Karate {

    FeatureRuntime featureRuntimeOf(Feature feature, Map<String, Object> args);

    ScenarioEngine newScenarioEngine(ScenarioRuntime runtime, Map<String, Variable> globalVars);
}
