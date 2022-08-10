package io.github.apimock;

import com.intuit.karate.core.Feature;
import com.intuit.karate.core.MockHandlerHook;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 *  @author ivangsa
 */
public abstract class MockServerBuilder {
    File openapi;
    List<Feature> features = Arrays.asList(Feature.read("classpath:io/github/apimock/default.feature"));
    int port;
    boolean ssl;
    boolean watch;
    File certFile;
    File keyFile;
    Map<String, Object> args;
    String prefix = null;
    List<MockHandlerHook> hooks = new ArrayList<>();

    public MockServerBuilder openapi(File openapi) throws MalformedURLException {
        if(openapi == null) { // used in Main
            return this;
        }
        this.openapi = openapi;
        OpenApiValidator4Karate openApiValidator = OpenApiValidator4Karate.fromURL(this.openapi.toURI().toURL());
        return this.openapi(openApiValidator);
    }

    public MockServerBuilder openapi(String ...apis) throws Exception {
        URL[] urls = Arrays.stream(apis).map(openapi -> MockServer.getURL(openapi)).toArray(i -> new URL[i]);
        return openapi(urls);
    }

    public MockServerBuilder openapi(URL ...urls) throws MalformedURLException {
        OpenApiValidator4Karate openApiValidator = OpenApiValidator4Karate.fromURL(urls);
        return this.openapi(openApiValidator);
    }

    public MockServerBuilder openapi(OpenApiValidator4Karate openApiValidator) {
        withHook(new OpenApiExamplesHook(openApiValidator));
        withHook(new OpenApiValidatorHook(openApiValidator));
        return this;
    }

    public MockServerBuilder features(String... features) {
        this.features = Arrays.asList(features).stream().map(f -> Feature.read(f)).collect(Collectors.toList());
        return this;
    }

    public MockServerBuilder features(List<File> features) {
        if(features == null) { // used in Main
            return this;
        }

        this.features = features.stream().map(f -> Feature.read(f)).collect(Collectors.toList());
        return this;
    }

    public MockServerBuilder watch(boolean value) {
        watch = value;
        return this;
    }

    public MockServerBuilder http(int value) {
        port = value;
        return this;
    }

    public MockServerBuilder https(int value) {
        ssl = true;
        port = value;
        return this;
    }

    public MockServerBuilder certFile(File value) {
        certFile = value;
        return this;
    }

    public MockServerBuilder keyFile(File value) {
        keyFile = value;
        return this;
    }

    public MockServerBuilder pathPrefix(String prefix) {
        this.prefix = prefix != null && !prefix.startsWith("/")? "/" + prefix : prefix;
        return this;
    }

    public MockServerBuilder withHook(MockHandlerHook hook) {
        this.hooks.add(hook);
        return this;
    }

    public MockServerBuilder args(Map<String, Object> value) {
        args = value;
        return this;
    }

    public MockServerBuilder arg(String name, Object value) {
        if (args == null) {
            args = new HashMap();
        }
        args.put(name, value);
        return this;
    }

    public abstract <T> T build();

}
