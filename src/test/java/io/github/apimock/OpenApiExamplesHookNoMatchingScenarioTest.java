package io.github.apimock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class OpenApiExamplesHookNoMatchingScenarioTest {

    io.github.apimock.MockServer server;
    String pathPrefix = "/api/v3";

    @Before
    public void setup() throws Exception {
        server = MockServer.builder()
                .openapi("classpath:openapi-examples/x-apimock-when/openapi.yml")
                .features("classpath:openapi-examples/x-apimock-when/PetMock.feature")
                .pathPrefix(pathPrefix)
                .http(0).build();
    }

    @Test
    public void test_responses_from_apimock_when() {
        String baseUrl = "http://localhost:" + server.getPort() + pathPrefix + "/pet/";
        RestTemplate restTemplate = new RestTemplate();

        Map result = restTemplate.getForObject(baseUrl + 10, Map.class);
        assertThat(JsonPath.read(result, "name"), is("dog from mock.feature"));

        result = restTemplate.getForObject(baseUrl + 10 + "?flag=1", Map.class);
        assertThat(JsonPath.read(result, "name"), is("dog 1 from openapi-x-apimock-when"));

        result = restTemplate.getForObject(baseUrl + 10 + "?flag=2", Map.class);
        assertThat(JsonPath.read(result, "name"), is("dog 2 from openapi-x-apimock-when"));
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }
}
