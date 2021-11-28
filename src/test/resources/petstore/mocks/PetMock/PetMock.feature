@mock
Feature: PetMock Mock

Background: 
* configure cors = true
* configure responseHeaders = { 'Content-Type': 'application/json' }

# populated from openapi.yml#/components/examples/pets
* def pets = []


 @addPet
 Scenario: methodIs('post') && pathMatches('/pet')
  * def pet = request
  * pet.id = sequenceNext()
  * pets.push(pet)
  * def response = pet
  * def responseStatus = 200

 @updatePet
 Scenario:  methodIs('put') && pathMatches('/pet')
  * def index = pets.findIndex(pet => pet.id == request.id)
  * pets[index] = index >= 0? request : null
  * def response = pets[index]
  * def responseStatus = index >= 0? 200 : 404

 @deletePet
 Scenario:  methodIs('delete') && pathMatches('/pet/{petId}')
  * def index = pets.findIndex(pet => pet.id == pathParams.petId)
  * pets.splice(index, 1);
  * def response = 'OK'
  * def responseStatus = 200

 @findPetsByStatus
 Scenario:  methodIs('get') && pathMatches('/pet/findByStatus')
  * def status = paramValue('status')
  * def response = pets.filter(pet => pet.status === status)
  * def responseStatus = 200

 @findPetsByTags
 Scenario:  methodIs('get') && pathMatches('/pet/findByTags')
  * def tags = paramValue('tags')
  * def response = pets.filter(pet => pet.tags.map(t => t.name).includes(tags))
  * def responseStatus = 200

 @getPetById
 Scenario: methodIs('get') && pathMatches('/pet/{petId}')
  * def response = pets.find(pet => pet.id == pathParams.petId)
  * def responseStatus = response? 200 : 404