package io.github.apimock;

import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;

public class MockServerBuildTest {

    @Test
    public void test_build_mockserver_with_files() throws MalformedURLException {
        MockServer server = MockServer.builder()
                .openapi(getClasspathFile("petstore/petstore-openapi.yml"))
                .features(Arrays.asList(getClasspathFile("petstore/mocks/PetMock/PetMock.feature")))
                .pathPrefix("contextPath")
                .watch(true)
                .http(0).build();
        server.stop();
    }

    @Test
    public void test_reloading_mockserver_with_files() throws IOException {
        File openapiFile = getClasspathFile("petstore/petstore-openapi.yml");
        File featureFile = getClasspathFile("petstore/mocks/PetMock/PetMock.feature");
        File targetOpenapiFile = new File("target/" + System.currentTimeMillis() + ".yml");
        File targetFeatureFile = new File("target/" + System.currentTimeMillis() + ".feature");
        Files.copy(openapiFile.toPath(), targetOpenapiFile.toPath());
        Files.copy(featureFile.toPath(), targetFeatureFile.toPath());

        MockServer server = MockServer.builder()
                .openapi(targetOpenapiFile)
                .features(Arrays.asList(targetFeatureFile))
                .pathPrefix("contextPath")
                .watch(true)
                .http(0).build();

        try {
            Files.write(targetOpenapiFile.toPath(), "\n#appending to file".getBytes(), StandardOpenOption.APPEND);
            String baseUrl = "http://localhost:" + server.getPort() + "contextPath" + "/pet/";
            RestTemplate restTemplate = new RestTemplate();
            Map result = restTemplate.getForObject(baseUrl + 10, Map.class);
        } finally {
            server.stop();
        }
    }

    private File getClasspathFile(String classpathResource) {
        return new File(getClass().getClassLoader().getResource(classpathResource).getFile());
    }
}
