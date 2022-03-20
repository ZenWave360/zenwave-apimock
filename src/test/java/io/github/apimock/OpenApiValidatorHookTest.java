package io.github.apimock;

import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OpenApiValidatorHookTest {

    io.github.apimock.MockServer server;
    String pathPrefix = "/api/v3";

    @Before
    public void setup() throws Exception {
        server = MockServer.builder()
                .openapi("classpath:openapi-examples/openapi-validator/openapi.yml")
                .features("classpath:openapi-examples/openapi-validator/PetMock.feature")
                .pathPrefix(pathPrefix)
                .http(0).build();
    }

    @Test
    public void test_validate_request_for_invalid_path_params() {
        String baseUrl = "http://localhost:" + server.getPort() + pathPrefix + "/pet/";
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.getForObject(baseUrl + "not-a-number", Map.class);
            fail();
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertTrue(e.getMessage().contains("Value 'not-a-number' does not match format 'int64'."));
            assertTrue(e.getMessage().contains("Type expected 'integer', found 'string'."));
        }
    }

    @Test
    public void test_validate_request_invalid_body() {
        String baseUrl = "http://localhost:" + server.getPort() + pathPrefix + "/pet";
        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> petBody = new HashMap<>();
            petBody.put("unknown", "unknown");
            restTemplate.postForObject(baseUrl, petBody, Map.class);
            fail();
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertTrue(e.getMessage().contains("Additional property 'unknown' is not allowed."));
            assertTrue(e.getMessage().contains("Field 'name' is required."));
        }
    }

    @Test
    public void test_validate_response_invalid_body() {
        String baseUrl = "http://localhost:" + server.getPort() + pathPrefix + "/pet/";
        RestTemplate restTemplate = new RestTemplate();

        try {
            restTemplate.getForObject(baseUrl + "10", Map.class);
            fail();
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertTrue(e.getMessage().contains("Additional property 'noname' is not allowed."));
            assertTrue(e.getMessage().contains("Field 'name' is required."));
        }
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }
}
