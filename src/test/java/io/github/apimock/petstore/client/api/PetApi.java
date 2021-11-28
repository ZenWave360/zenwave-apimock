package io.github.apimock.petstore.client.api;

import io.github.apimock.petstore.client.ApiClient;
import io.github.apimock.petstore.client.model.PetDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-11-26T11:46:18.035881+01:00[Europe/Paris]")
@Component("io.github.apimock.petstore.client.api.PetApi")
public class PetApi {
    private ApiClient apiClient;

    public PetApi() {
        this(new ApiClient());
    }

    @Autowired
    public PetApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Add a new pet to the store
     * Add a new pet to the store
     * <p><b>200</b> - Successful operation
     * <p><b>400</b> - Invalid input
     * @param petDto Create a new pet in the store (required)
     * @return PetDto
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PetDto addPet(PetDto petDto) throws RestClientException {
        return addPetWithHttpInfo(petDto).getBody();
    }

    /**
     * Add a new pet to the store
     * Add a new pet to the store
     * <p><b>200</b> - Successful operation
     * <p><b>400</b> - Invalid input
     * @param petDto Create a new pet in the store (required)
     * @return ResponseEntity&lt;PetDto&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PetDto> addPetWithHttpInfo(PetDto petDto) throws RestClientException {
        Object postBody = petDto;
        
        // verify the required parameter 'petDto' is set
        if (petDto == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petDto' when calling addPet");
        }
        
        String path = apiClient.expandPath("/pet", Collections.<String, Object>emptyMap());

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        final String[] accepts = { 
            "application/json"
        };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { 
            "application/json"
        };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<PetDto> returnType = new ParameterizedTypeReference<PetDto>() {};
        return apiClient.invokeAPI(path, HttpMethod.POST, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Deletes a pet
     * 
     * <p><b>200</b> - OK
     * <p><b>400</b> - Invalid pet value
     * @param petId Pet id to delete (required)
     * @param apiKey  (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deletePet(Long petId, String apiKey) throws RestClientException {
        deletePetWithHttpInfo(petId, apiKey);
    }

    /**
     * Deletes a pet
     * 
     * <p><b>200</b> - OK
     * <p><b>400</b> - Invalid pet value
     * @param petId Pet id to delete (required)
     * @param apiKey  (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deletePetWithHttpInfo(Long petId, String apiKey) throws RestClientException {
        Object postBody = null;
        
        // verify the required parameter 'petId' is set
        if (petId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petId' when calling deletePet");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("petId", petId);
        String path = apiClient.expandPath("/pet/{petId}", uriVariables);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        if (apiKey != null)
        headerParams.add("api_key", apiClient.parameterToString(apiKey));

        final String[] accepts = { };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<Void> returnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI(path, HttpMethod.DELETE, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Finds Pets by status
     * Multiple status values can be provided with comma separated strings
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid status value
     * @param status Status values that need to be considered for filter (optional, default to available)
     * @return List&lt;PetDto&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<PetDto> findPetsByStatus(String status) throws RestClientException {
        return findPetsByStatusWithHttpInfo(status).getBody();
    }

    /**
     * Finds Pets by status
     * Multiple status values can be provided with comma separated strings
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid status value
     * @param status Status values that need to be considered for filter (optional, default to available)
     * @return ResponseEntity&lt;List&lt;PetDto&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<PetDto>> findPetsByStatusWithHttpInfo(String status) throws RestClientException {
        Object postBody = null;
        
        String path = apiClient.expandPath("/pet/findByStatus", Collections.<String, Object>emptyMap());

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "status", status));

        final String[] accepts = { 
            "application/json"
        };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<List<PetDto>> returnType = new ParameterizedTypeReference<List<PetDto>>() {};
        return apiClient.invokeAPI(path, HttpMethod.GET, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Finds Pets by tags
     * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid tag value
     * @param tags Tags to filter by (optional, default to new ArrayList&lt;&gt;())
     * @return List&lt;PetDto&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<PetDto> findPetsByTags(List<String> tags) throws RestClientException {
        return findPetsByTagsWithHttpInfo(tags).getBody();
    }

    /**
     * Finds Pets by tags
     * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid tag value
     * @param tags Tags to filter by (optional, default to new ArrayList&lt;&gt;())
     * @return ResponseEntity&lt;List&lt;PetDto&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<PetDto>> findPetsByTagsWithHttpInfo(List<String> tags) throws RestClientException {
        Object postBody = null;
        
        String path = apiClient.expandPath("/pet/findByTags", Collections.<String, Object>emptyMap());

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        queryParams.putAll(apiClient.parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "tags", tags));

        final String[] accepts = { 
            "application/json"
        };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<List<PetDto>> returnType = new ParameterizedTypeReference<List<PetDto>>() {};
        return apiClient.invokeAPI(path, HttpMethod.GET, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Find pet by ID
     * Returns a single pet
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid ID supplied
     * <p><b>404</b> - Pet not found
     * @param petId ID of pet to return (required)
     * @return PetDto
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PetDto getPetById(Long petId) throws RestClientException {
        return getPetByIdWithHttpInfo(petId).getBody();
    }

    /**
     * Find pet by ID
     * Returns a single pet
     * <p><b>200</b> - successful operation
     * <p><b>400</b> - Invalid ID supplied
     * <p><b>404</b> - Pet not found
     * @param petId ID of pet to return (required)
     * @return ResponseEntity&lt;PetDto&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PetDto> getPetByIdWithHttpInfo(Long petId) throws RestClientException {
        Object postBody = null;
        
        // verify the required parameter 'petId' is set
        if (petId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petId' when calling getPetById");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("petId", petId);
        String path = apiClient.expandPath("/pet/{petId}", uriVariables);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        final String[] accepts = { 
            "application/json"
        };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "api_key", "petstore_auth" };

        ParameterizedTypeReference<PetDto> returnType = new ParameterizedTypeReference<PetDto>() {};
        return apiClient.invokeAPI(path, HttpMethod.GET, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Update an existing pet
     * Update an existing pet by Id
     * <p><b>200</b> - Successful operation
     * <p><b>400</b> - Invalid ID supplied
     * <p><b>404</b> - Pet not found
     * <p><b>405</b> - Validation exception
     * @param petDto Update an existent pet in the store (required)
     * @return PetDto
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public PetDto updatePet(PetDto petDto) throws RestClientException {
        return updatePetWithHttpInfo(petDto).getBody();
    }

    /**
     * Update an existing pet
     * Update an existing pet by Id
     * <p><b>200</b> - Successful operation
     * <p><b>400</b> - Invalid ID supplied
     * <p><b>404</b> - Pet not found
     * <p><b>405</b> - Validation exception
     * @param petDto Update an existent pet in the store (required)
     * @return ResponseEntity&lt;PetDto&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<PetDto> updatePetWithHttpInfo(PetDto petDto) throws RestClientException {
        Object postBody = petDto;
        
        // verify the required parameter 'petDto' is set
        if (petDto == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petDto' when calling updatePet");
        }
        
        String path = apiClient.expandPath("/pet", Collections.<String, Object>emptyMap());

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        final String[] accepts = { 
            "application/json"
        };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { 
            "application/json"
        };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<PetDto> returnType = new ParameterizedTypeReference<PetDto>() {};
        return apiClient.invokeAPI(path, HttpMethod.PUT, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
    /**
     * Updates a pet in the store with form data
     * 
     * <p><b>405</b> - Invalid input
     * @param petId ID of pet that needs to be updated (required)
     * @param name Name of pet that needs to be updated (optional)
     * @param status Status of pet that needs to be updated (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void updatePetWithForm(Long petId, String name, String status) throws RestClientException {
        updatePetWithFormWithHttpInfo(petId, name, status);
    }

    /**
     * Updates a pet in the store with form data
     * 
     * <p><b>405</b> - Invalid input
     * @param petId ID of pet that needs to be updated (required)
     * @param name Name of pet that needs to be updated (optional)
     * @param status Status of pet that needs to be updated (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> updatePetWithFormWithHttpInfo(Long petId, String name, String status) throws RestClientException {
        Object postBody = null;
        
        // verify the required parameter 'petId' is set
        if (petId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'petId' when calling updatePetWithForm");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("petId", petId);
        String path = apiClient.expandPath("/pet/{petId}", uriVariables);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap formParams = new LinkedMultiValueMap();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "name", name));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "status", status));

        final String[] accepts = { };
        final List<MediaType> accept = apiClient.selectHeaderAccept(accepts);
        final String[] contentTypes = { };
        final MediaType contentType = apiClient.selectHeaderContentType(contentTypes);

        String[] authNames = new String[] { "petstore_auth" };

        ParameterizedTypeReference<Void> returnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI(path, HttpMethod.POST, queryParams, postBody, headerParams, cookieParams, formParams, accept, contentType, authNames, returnType);
    }
}
