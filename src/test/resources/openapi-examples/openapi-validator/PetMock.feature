@mock
Feature: PetMock Mock

Background: 
* configure cors = true
* configure responseHeaders = { 'Content-Type': 'application/json' }

# populated from openapi.yml#/components/examples/pets
* def pets = [{id: 10, noname: 'invalid property'}, {id: 20, noname: 'invalid property'}]

 @addPet
 Scenario: methodIs('post') && pathMatches('/pet')
  * def pet = request
  * pet.id = sequenceNext()
  * pets.push(pet)
  * def response = pet
  * def responseStatus = 200

 @getPetById
 Scenario: methodIs('get') && pathMatches('/pet/{petId}') && !paramExists('flag')
  * def response = pets.find(pet => pet.id == pathParams.petId)
  * def responseStatus = response? 200 : 404