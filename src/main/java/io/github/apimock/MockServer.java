/*
 * The MIT License
 *
 * Copyright 2020 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.apimock;

import com.intuit.karate.core.MockHandler;
import com.intuit.karate.http.HttpServer;
import com.intuit.karate.http.HttpServerHandler;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import com.intuit.karate.http.ServerHandler;
import com.intuit.karate.http.SslContextFactory;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;

import java.io.File;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author pthomas3
 */
public class MockServer extends HttpServer {

    MockServer(ServerBuilder sb) {
        super(sb);
    }

    public static class Builder extends MockServerBuilder {
        public <T> T build() {
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
            if (openapi != null) {
                files.add(openapi);
            }
            ServerHandler handler = watch ? new MockServer.ReloadingMockHandler(mockHandler, files) : mockHandler;
            HttpService service = new HttpServerHandler(handler);
            sb.service("prefix:/" + prefix, service);
            return (T) new MockServer(sb);
        }
    }

    public static class ReloadingMockHandler implements ServerHandler {
                
        private MockHandler handler;
        private final LinkedHashMap<File, Long> files = new LinkedHashMap<>();

        public ReloadingMockHandler(MockHandler handler, List<File> files) {
            this.handler = handler;
            for (File f : files) {
                this.files.put(f, f.lastModified());
            }
            logger.debug("watch mode init - {}", files);
        }

        @Override
        public Response handle(Request request) {
            boolean reload = files.entrySet().stream().filter(e -> e.getKey().lastModified() > e.getValue()).collect(Collectors.toList()).size() > 0;
            if(reload) {
                logger.debug("Reloading MockHandler...");
                files.entrySet().forEach(e -> e.setValue(e.getKey().lastModified()));
                handler.reload();
            }
            return handler.handle(request);
        }
        
    }

    public static Builder builder() {
        return new Builder();
    }


}
