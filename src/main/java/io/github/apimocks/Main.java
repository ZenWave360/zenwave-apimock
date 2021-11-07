package io.github.apimocks;


import com.intuit.karate.core.MockServer;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ApiMocks server, wrapping karate mocks and openapi validation.
 *
 * Installation:
 * <pre>
 *     curl -Ls https://sh.jbang.dev | bash -s - alias add --name=apimocks "io.github.apimocks:apimocks:<VERSION>"
 * </pre>
 *
 * Basic usage:
 * <pre>
 *     jbang apimocks --watch -o openapi-rest.yml -m mocks/UserMock/UserMock.feature -p 3000
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

    MockServer server;

    public static void main(String[] args) throws Exception {
        System.setProperty("logback.configurationFile", "logback-apimocks.xml");
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
        OpenApiValidator4Karate openApiValidator = OpenApiValidator4Karate.fromURL(openapi.toURI().toURL());
        server = MockServer.featureFiles(mock)
                .pathPrefix(contextPath)
                .watch(watch)
                .withHook(new OpenApiValidatorHook(openApiValidator))
                .withHook(new OpenApiExamplesHook(openApiValidator))
                .http(port).build();
        // server.waitSync();
        if(watch &&  !watching) {
            watching = true;
            watch(openapi);
        }
        return null;
    }

    public void reload() throws Exception {
        this.server.stop();
        this.call();
    }

    public void watch(File... files) {
        List<Path> paths = Arrays.asList(files).stream().map(f -> f.toPath()).collect(Collectors.toList());
        List<String> filenames = Arrays.asList(files).stream().map(f -> f.getName()).collect(Collectors.toList());
        new Thread(() -> {
                try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                    paths.stream().forEach(p -> {
                        try {
                            p.getParent().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                        } catch (IOException e) {
                           throw new RuntimeException(e);
                        }
                    });
                    while (true) {
                        WatchKey key;
                        try {
                            key = watcher.poll(25, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (key == null) {
                            Thread.yield();
                            continue;
                        }

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path filename = ev.context();

                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY && filenames.contains(filename.toString())) {
                                reload();
                            }
                            boolean valid = key.reset();
                            if (!valid) {
                                break;
                            }
                        }
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }).start();
    }
}
