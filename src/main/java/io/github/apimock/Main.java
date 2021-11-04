package io.github.apimock;


import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * MockServer wrapping karate mocks and openapi validation.
 *
 * Basic usage:
 * <pre>
 *     java -cp "apimock.jar;karate-1.2.0.jar" io.github.apimock.Main -o openapi.yml -m Mock.feature -p 3000 -P context/path -W
 * </pre>
 * See: https://github.com/ivangsa/apimock
 *
 * @author ivangsa
 */
public class Main implements Callable<Void> {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @CommandLine.Option(names = {"-o", "--openapi"}, description = "openapi definition file for request/response validation")
    File openapi;

    @CommandLine.Option(names = {"-m", "--mock"}, split = ",", description = "one or more karate mock features")
    List<File> mock;

    @CommandLine.Option(names = {"-p", "--port"}, description = "server port (default 8080)")
    int port = 8080;

    @CommandLine.Option(names = {"-P", "--prefix"}, defaultValue = "/", description = "server context path (default '/')")
    String contextPath = "/";

    @CommandLine.Option(names = {"-W", "--watch"}, description = "watch (and hot-reload) mock server file for changes")
    boolean watch;

    public static void main(String[] args) throws Exception {
        System.setProperty("logback.configurationFile", "logback-apimock.xml");
        CommandLine cmd = new CommandLine(new Main());
        int returnCode = cmd.execute(args);
        Thread.currentThread().join();
        System.exit(returnCode);
    }

    public Void call() throws Exception {
        if (openapi == null && mock == null) {
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
