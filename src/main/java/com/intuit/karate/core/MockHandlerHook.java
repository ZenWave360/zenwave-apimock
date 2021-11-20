package com.intuit.karate.core;

import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;

import java.util.Map;

public interface MockHandlerHook {

    default void reload() {
    }

    default void onSetup(Map<Feature, ScenarioRuntime> features, Map<String, Variable> globals) {
    }

    default Response beforeRequest(Request req) {
        return null;
    }

    default Response beforeScenario(Request req, ScenarioEngine engine) {
        return null;
    }

    default Response afterScenarioSuccess(Request req, Response response, ScenarioEngine engine) {
        return response;
    }

    default Response afterScenarioFailure(Request req, Response response, ScenarioEngine engine) {
        return response;
    }

    default Response noMatchingScenario(Request req, Response response, ScenarioEngine engine) {
        return response;
    }
}
