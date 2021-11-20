Feature: Find pet by ID
	Returns a single pet

Background:
* url baseUrl

@inline
Scenario: explore getPetById inline
	You may use this test for quick API exploratorial purposes.
* def statusCode = 200
* def params = {"petId":1}
* def matchResponse = true
* call read('getPetById.feature@operation') 


@ignore
@operation @operationId=getPetById @openapi-file=src/test/resources/petstore/petstore-openapi.yml
Scenario: operation PetApi/getPetById
* def args = 
"""
{
    auth: #(karate.get('auth')), 
    headers: #(karate.get('headers')), 
    params: #(karate.get('params')), 
    body: #(karate.get('body')), 
    statusCode: #(karate.get('statusCode')), 
    matchResponse: #(karate.get('matchResponse'))
}
"""
* def authHeader = call read('classpath:karate-auth.js') args.auth
* def headers = karate.merge(args.headers || {}, authHeader || {})
Given path '/pet/', args.params.petId
And headers headers
When method GET

* def expectedStatusCode = args.statusCode || responseStatus
* match responseStatus == expectedStatusCode

* if (args.matchResponse === true) karate.call('getPetById.feature@validate')

@ignore @validate
Scenario: validates getPetById response

* def responseMatch =
"""
{
  "id": "##number",
  "name": "#string",
  "category": {
    "id": "##number",
    "name": "##string"
  },
  "photoUrls": "#array",
  "tags": "##array",
  "status": "##string"
}
"""
* match  response contains responseMatch

# validate nested array: photoUrls
* def photoUrls_MatchesEach = "##string"
* def photoUrls_Response = response.photoUrls || []
* match each photoUrls_Response contains photoUrls_MatchesEach
# validate nested array: tags
* def tags_MatchesEach = {"id":"##number","name":"##string"}
* def tags_Response = response.tags || []
* match each tags_Response contains tags_MatchesEach
