package io.github.apimock;


import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * MockServer wrapping karate mocks and openapi validation.
 *
 * Installation:
 * <pre>
 *     curl -Ls https://sh.jbang.dev | bash -s - alias add --name=apimock "io.github.apimock:apimock:<VERSION>"
 * </pre>
 *
 * Basic usage:
 * <pre>
 *     jbang apimock --watch -o openapi-rest.yml -m mocks/UserMock/UserMock.feature -p 3000
 * </pre>
 */
public class Main implements Callable<Void> {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @CommandLine.Option(names = {"-o", "--openapi"}, required = true, description = "openapi definition file for request/response validation")
    File openapi;

    @CommandLine.Option(names = {"-m", "--mock"}, split = ";", required = true, description = "one or more karate mock features")
    List<File> mock;

    @CommandLine.Option(names = {"-p", "--port"}, description = "server port (default 8080)")
    int port = 8080;

    @CommandLine.Option(names = {"-P", "--prefix"}, defaultValue = "/", description = "server context path (default '/')")
    String contextPath = "/";

    @CommandLine.Option(names = {"-W", "--watch"}, description = "watch (and hot-reload) mock server file for changes")
    boolean watch;
    boolean watching = false;

    public static void main(String[] args) throws Exception {
        System.setProperty("logback.configurationFile", "logback-apimock.xml");
        CommandLine cmd = new CommandLine(new Main());
        int returnCode = cmd.execute(args);
        Thread.currentThread().join();
        System.exit(returnCode);
    }

    public Void call() throws Exception {
        if (openapi == null || !openapi.exists() || mock == null) {
            CommandLine.usage(this, System.err);
            return null;
        }
        if(isKarateShadowedJar()) {
            MockServer server = MockServer.builder()
                    .openapi(openapi)
                    .features(mock)
                    .pathPrefix(contextPath)
                    .watch(watch)
                    .http(port).build();
        } else {
            MockServerWithArmeria server = MockServerWithArmeria.builder()
                    .openapi(openapi)
                    .features(mock)
                    .pathPrefix(contextPath)
                    .watch(watch)
                    .http(port).build();
        }
        // server.waitSync();
        return null;
    }

    private boolean isKarateShadowedJar() {
        try {
            Class.forName("karate.com.linecorp.armeria.server.HttpService");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
