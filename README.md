# OpenApi Codec 
![version](https://img.shields.io/badge/version-0.1.0-blue.svg) 

This microservice can validate open api dictionary, encode th2 messages or decode http body.

### Currently supported formats:
* application/json

## How it works

On start of box this microservice will validate linked dictionary. Format of dictionary must be **OpenApi** yml or json.
If there are any problems that must be fixed, microservice will stop working with all info about problem in log.
Valid format of dictionary can be found [here](https://swagger.io/specification/) or [here](https://github.com/OAI/OpenAPI-Specification).

### ENCODE: 
Each response or request from dictionary will generate message type according to the rules of camel case

As example
```yaml
paths:
  /test:
    get:
      tags:
        - test
      summary: Store an object
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ArrayTest"
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ArrayTest"
```

will provide two message types for encode: 
* TestGetApplicationJson (Request)  
* TestGet200ApplicationJson (Response).

Path that contains parameters inside will be converted as:
* /test/{client} -> TestClient
* /test/{client}/{audio} -> TestClientAudio

All messages processing by encode must contain those types.

Decode accepts only raw messages, all parsed will be passed.

In result of encode will be sent two messages:
1. **Request/Response** parsed message with all info about http part (required)
2. **RawMessage** that contain encoded body (optional)

If needed to specify in:query or in:path parameters: please create message-field 'uriParameters' inside of encoding message. 
In 'uriParameters' needs to be all required params that was mentioned in dictionary. \
As example:

```yaml
  /test/{path}:
    get:
      tags:
        - test
      summary: Store an object
      parameters:
        - name: path
          in: path
          required: true
          schema:
            type: string
      responses:
        "200":
          description: OK
```

uriParameters:

```json
{
  "message": {
    "uriParameters": {
      "path": "somePath"
    }
  }
}
```

If needed to specify in:header parameters: please create message-field 'headerParameters' inside of encoding message.
In 'headerParameters' needs to be all required params that was mentioned in dictionary. \
As example:

```json
{
  "message": {
    "headerParameters": {
      "media": "picture"
    }
  }
}
```

Please mention that right now only supports string params, all values will be converted into it.

### DECODE: 

Decode accepts group of messages: 
1. **Request/Response** parsed message with all info about http part (required)
2. **RawMessage** that contain encoded body (optional)

Result of decode: 
1. Same **Request/Response** parsed message (required)
2. New parsed message representing data of RawMessage (optional)

### Message descriptions
#### Request

|Field|Type|Description|
|:---:|:---:|:---:|
|method|String|HTTP method name (e.g. GET, POST, etc.)|
|uri|String|Request URI (e.g. /some/request/path?param1=value1&param2=value2...)|
|headers|List\<Header>|HTTP headers (e.g. Host, Content-Length, etc.)|

---
#### Response

|  Field  |Type|Description|
|:-------:|:---:|:---:|
|  code   |String|HTTP method name (e.g. GET, POST, etc.)|
| headers |List\<Header>|HTTP headers (e.g. Host, Content-Length, etc.)|

---
#### Header

|Field|Type|Description|
|:---:|:---:|:---:|
|name|String|HTTP header name|
|value|String|HTTP header value|


### Codec configs:

Config contain two main types of options.

**validationSettings (open api dictionary)**
* enableRecommendations - Enable or Disable recommendations, true by default.
* enableApacheNginxUnderscoreRecommendation - Enable or Disable the recommendation check for Apache/Nginx potentially ignoring header with underscore by default.
* enableOneOfWithPropertiesRecommendation - Enable or Disable the recommendation check for schemas containing properties and oneOf definitions.
* enableUnusedSchemasRecommendation - Enable or Disable the recommendation check for unused schemas.
* enableSchemaTypeRecommendation - Enable or Disable the recommendation check for schemas containing type definitions, specifically for changes between OpenAPI 3.0.x and 3.1.
* enableNullableAttributeRecommendation - Enable or Disable the recommendation check for the 'nullable' attribute.
* enableInvalidTypeRecommendation - Enable or Disable the recommendation check for the 'type' attribute.

**dictionaryParseOption (open api dictionary)**
* resolve - true by default;
* resolveCombinators - true by default;
* resolveFully;
* flatten;
* flattenComposedSchemas;
* camelCaseFlattenNaming;
* skipMatches;

### Configuration example

```yaml
validationSettings:
  enableRecommendations: true
dictionaryParseOption:
  resolve: true
```

May be empty due to missing required fields
