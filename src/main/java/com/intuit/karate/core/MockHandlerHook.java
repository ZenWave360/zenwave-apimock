package com.intuit.karate.core;

import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;

public interface MockHandlerHook {

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
