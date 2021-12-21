# OpenApi Codec 
![version](https://img.shields.io/badge/version-0.1.0-blue.svg) 

This microservice can validate open api dictionary, encode th2 messages or decode http body.

### Currently supported formats:
* application/json

## How it works

On start of box this microservice will validate linked dictionary. Format of dictionary must be **OpenApi yaml**.
If there are any problems that must be fixed, microservice will stop working with all info about problem in log.
Valid format of dictionary can be found [here](https://swagger.io/specification/).

### ENCODE: 
Each response or request from dictionary will generate message type according to the rules of camel case

As example
```
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