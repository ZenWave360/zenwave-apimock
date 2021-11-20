@mock
Feature: PetMock Mock

Background: 
* configure cors = true
* configure responseHeaders = { 'Content-Type': 'application/json' }

# populated from openapi.yml#/components/exampes/pets
* def pets = []


# Find pet by ID
@getPetById
Scenario: methodIs('get') && pathMatches('/pet/{petId}')  
 * def petId = pathParams.petId
 * def response = pets[+petId]
 * def responseStatus = 200


