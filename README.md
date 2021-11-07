# Test Doubles for REST APIs

Joining OpenAPI and KarateDSL Server Side Features for REST API mocking.

With this library you can leverage OpenAPI schemas and examples for request/response validation and declarative stateless mocks and powerful yet simple stateful KarateDSL mocks.

## Features

This is a wrapper around KarateDSL mocks with some OpenAPI features.

With OpenAPI you can:

- Validates requests using OpenAPI schemas and sends an 400 status code error in case request fails validation.
- Select response examples from OpenAPI definitions using custom extension 'x-apimock-when' to choose which example to serve.
- Create dynamic examples payload using provided generators (`uuid()`, `date(time, 'dd/MM/yyy')`, `sequence:name(start,step)`) or custom generators configured in KarateDSL features.
- Validates response payload using OpenAPI schemas sending 400 status code in case it fails validation.

With KarateDSL you can:

- *Missuse* Gerkin language to create simple yet powerful stateful mocks. See official KarateDSL for details.

## How it works

- It's KarateDSL mocks server in charge of listening for requests and serving responses.
- Before a request arrives Karate features, a custom hook validates request payload against provided OpenAPI definition file, responding with a 400 status code error in case it fails validation.
- If request passes validation, then it is routed to KarateDSL server side features. Matched Karate scenario serves a response.
- If no Karate scenario matches for your request, then a custom hook will search for examples in OpenAPI definition file and will filter them using `x-apimock-when`. This when clause uses the same functions and functionality as KarateDSL scenario names selection. If no example in examples section matches when clause the it will use default response example or send a 404 if no one is found.
- OpenAPI examples are processed for interpolated generator tags using `{{ }}`. For instance `{{ uuid() }}` will be replaced with the output of `uuid()` generator. You can configure any custom generator defining them in the Background of first Karate mock feature.
- Last, response payload is validated against OpenAPI schema and will send a 400 in case it fails validation.

### Configuration

#### Build from source

#### Maven dependency

#### Starting a mock server

#### Setup and tear down in Unit/Integration tests