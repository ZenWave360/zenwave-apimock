package io.github.apimock.petstore;

import io.github.apimock.MockServer;
import io.github.apimock.petstore.client.api.PetApi;
import io.github.apimock.petstore.client.model.CategoryDto;
import io.github.apimock.petstore.client.model.PetDto;
import io.github.apimock.petstore.client.model.TagDto;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;

public class RestClientExampleInSeparateFilesTest extends RestClientExampleTest {

    @Before
    public void setup() throws Exception {
        server = MockServer.builder()
                .openapi("classpath:petstore/petstore-openapi-without-examples.yml", "classpath:petstore/petstore-openapi-examples.yml")
                .features("classpath:petstore/mocks/PetMock/PetMock.feature")
                .pathPrefix("api/v3")
                .http(0).build();
    }
}
