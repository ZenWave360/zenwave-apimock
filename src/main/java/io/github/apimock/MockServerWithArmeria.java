package io.github.apimock;

import com.intuit.karate.core.MockHandler;
import com.intuit.karate.http.HttpServer;
import com.intuit.karate.http.HttpServerHandler;
import com.intuit.karate.http.ServerHandler;
import com.intuit.karate.http.SslContextFactory;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 *  @author ivangsa
 */
public class MockServerWithArmeria {

    protected static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final Server server;
    private final CompletableFuture<Void> future;
    private final int port;

    public MockServerWithArmeria(ServerBuilder sb) {
        HttpService httpStopService = (ctx, req) -> {
            logger.debug("received command to stop server: {}", req.path());
            this.stop();
            return HttpResponse.of(HttpStatus.ACCEPTED);
        };
        sb.service("/__admin/stop", httpStopService);
        server = sb.build();
        future = server.start();
        future.join();
        this.port = server.activePort().localAddress().getPort();
        logger.debug("server started: {}:{}", server.defaultHostname(), this.port);
    }

    public CompletableFuture stop() {
        return server.stop();
    }

    public static class Builder extends MockServerBuilder {

        public <T> T  build() {
            ServerBuilder sb = Server.builder();
            sb.requestTimeoutMillis(0);
            if (ssl) {
                sb.https(port);
                SslContextFactory factory = new SslContextFactory();
                factory.setCertFile(certFile);
                factory.setKeyFile(keyFile);
                factory.build();
                sb.tls(factory.getCertFile(), factory.getKeyFile());
            } else {
                sb.http(port);
            }
            MockHandler mockHandler = new MockHandler(features, args).withPrefix(prefix).withHandlerHooks(hooks).start();
            List<File> files = features.stream().map(f -> f.getResource().getFile()).collect(Collectors.toList());
            if(openapi != null) {
                files.add(openapi);
            }
            ServerHandler handler = watch ? new MockServer.ReloadingMockHandler(mockHandler, files) : mockHandler;
            HttpService service = (HttpService) new HttpServerHandler(handler);
            sb.service("prefix:/" + prefix, service);
            return (T) new MockServerWithArmeria(sb);
        }

    }
    
    public static Builder builder() {
        return new Builder();
    }

}
