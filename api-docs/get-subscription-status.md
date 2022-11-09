# Get Subscription Status

Get the ECL subscription status for a given business partner ID.

**URL**: `/economic-crime-levy-registration/subscription-status/:businessPartnerId`

**Method**: `GET`

**Required URI Parameters**:

| Parameter         | Description                                                       |
|-------------------|-------------------------------------------------------------------|
| businessPartnerId | The business partner ID to check the ECL subscription status for. |

**Required Request Headers**:

| Header Name | Header Value | Description |
|---------------|--------------|--------------|
| Authorization | Bearer {TOKEN} | A valid bearer token from the auth service |

## Success Response

**Code**: `200 OK`

**Response Body Examples**

For a business partner ID that holds an ECL subscription:

```json
{
  "subscriptionStatus": {
    "status": "Subscribed",
    "eclRegistrationReference": "XMECL0000000001"
  }
}
```

For a business partner ID that does not hold an ECL subscription:

```json
{
    "subscriptionStatus": "NotSubscribed"
}
```