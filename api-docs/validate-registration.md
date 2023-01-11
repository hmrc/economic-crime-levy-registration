# Validate Registration

Validate a registration that is held in mongo to ensure that is has no errors and can be submitted to the HoD.

**URL**: `/economic-crime-levy-registration/registrations/validate/:id`

**Method**: `POST`

**Required URI Parameters**:

| Parameter | Description                               |
|-----------|-------------------------------------------|
| id        | The ID of the registration held in mongo. |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### The registration is valid and can be submitted to the HoD

**Code**: `204 NO_CONTENT`

### The registration is not valid and therefore should not be submitted to the HoD

**Code**: `200 OK`

**Response Body Example**

Data validation error descriptions are returned in the body:

```json
{
  "errors": [
    "Data item 1 is missing",
    "Data item 2 is missing"
  ]
}
```

### A registration could not be found in mongo for the given ID

**Code**: `404 NOT_FOUND`

