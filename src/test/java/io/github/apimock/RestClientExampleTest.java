package io.github.apimock;

import io.github.apimock.petstore.client.api.PetApi;
import io.github.apimock.petstore.client.model.CategoryDto;
import io.github.apimock.petstore.client.model.PetDto;
import io.github.apimock.petstore.client.model.TagDto;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;

public class RestClientExampleTest {

    io.github.apimock.MockServer server;

    @Before
    public void setup() throws Exception {
        server = MockServer.builder()
                .openapi("classpath:petstore/petstore-openapi.yml")
                .features("classpath:petstore/mocks/PetMock/PetMock.feature")
                .pathPrefix("api/v3")
                .http(0).build();
    }

    @Test
    public void testRestWithMockServer() {
        PetApi petApiClient = new PetApi();
        petApiClient.getApiClient().setBasePath("http://localhost:" + server.getPort() + "/api/v3");
        PetDto pet = petApiClient.getPetById(1L);
        Assert.assertNotNull(pet);

        List<PetDto> pets = petApiClient.findPetsByStatus("available");
        Assert.assertNotNull(pets);
        Assert.assertFalse(pets.isEmpty());
    }

    @Test
    public void testCrudPet() {
        PetApi petApiClient = new PetApi();
        petApiClient.getApiClient().setBasePath("http://localhost:" + server.getPort() + "/api/v3");

        PetDto pet = new PetDto();
        pet.setName("Name");
        pet.setStatus(PetDto.StatusEnum.AVAILABLE);
        pet.setTags(Arrays.asList(new TagDto().id(0L).name("dog")));
        pet.setCategory(new CategoryDto().id(0L).name("dogs"));

        PetDto created = petApiClient.addPet(pet);
        Assert.assertNotNull(created);
        Assert.assertNotNull(created.getId());

        PetDto found = petApiClient.getPetById(created.getId());
        Assert.assertNotNull(found);

        created.setName("Updated Name");
        PetDto updated = petApiClient.updatePet(created);
        Assert.assertEquals("Updated Name", updated.getName());

        petApiClient.deletePet(created.getId(), null);

        try {
            PetDto notfound = petApiClient.getPetById(created.getId());
            Assert.fail("Pet was not deleted");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof HttpClientErrorException.NotFound);
        }
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

}
