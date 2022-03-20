@mock
Feature: PetMock Mock

Background: 
* configure cors = true
* configure responseHeaders = { 'Content-Type': 'application/json' }

# populated from openapi.yml#/components/examples/pets
* def pets = [{id: 10, name: 'dog from mock.feature'}, {id: 20, name: 'cat from mock.feature'}]


 @getPetById
 Scenario: methodIs('get') && pathMatches('/pet/{petId}') && !paramExists('flag')
  * def response = pets.find(pet => pet.id == pathParams.petId)
  * def responseStatus = response? 200 : 404