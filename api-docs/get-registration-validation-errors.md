# Get Registration Validation Errors

Validates a registration that is held in mongo and returns any errors that mean the subscription cannot be submitted to
the HoD.

**URL**: `/economic-crime-levy-registration/registrations/:id/validation-errors`

**Method**: `GET`

**Required URI Parameters**:

| Parameter | Description                               |
|-----------|-------------------------------------------|
| id        | The ID of the registration held in mongo. |

**Required Request Headers**:

| Header Name   | Header Value   | Description                                |
|---------------|----------------|--------------------------------------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Responses

### The registration is valid and no errors are returned

**Code**: `204 NO_CONTENT`

### The registration is not valid and validations errors are returned

**Code**: `200 OK`

**Response Body Example**

```json
{
  "errors": [
    {
      "message": "Data item 1 is missing"
    },
    {
      "message": "Data item 2 is missing"
    }
  ]
}
```

### A registration could not be found in mongo for the given ID

**Code**: `404 NOT_FOUND`

