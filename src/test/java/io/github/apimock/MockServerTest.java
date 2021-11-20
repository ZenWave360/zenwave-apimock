package io.github.apimock;

import org.junit.Before;
import org.junit.Test;

public class MockServerTest {

    MockServer server;

    @Before
    public void setup() throws Exception {
        server = MockServer.builder()
                .openapi("classpath:petstore/petstore-openapi.yml")
                .features("classpath:petstore/mocks/PetMock/PetMock.feature")
                .pathPrefix("api/v3")
                .http(0).build();

        System.setProperty("karate.port", String.valueOf(server.getPort()));
    }

    public void tearDown() throws Exception {
        server.stop();
    }

}
