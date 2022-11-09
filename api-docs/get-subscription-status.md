# Get Subscription Status

Get the ECL subscription status for a given business partner ID.

**URL** : `/economic-crime-levy-registration/subscription-status/:businessPartnerId`

**Uri Parameters** :

| Parameter         | Description                                                                     |
|-------------------|---------------------------------------------------------------------------------|
| businessPartnerId | The business partner ID that you want to check the ECL subscription status for. |

**Method** : `GET`

**Request headers** :

| Header Name   | Header Value |
|---------------|--------------|
| Authorization | Bearer xxx   |

## Success Response

**Code** : `200 OK`

**Content examples**

For a business partner ID that holds an ECL subscription

```json
{
  "subscriptionStatus": {
    "status": "Subscribed",
    "eclRegistrationReference": "XMECL0000000001"
  }
}
```

For a business partner ID that does not hold an ECL subscription.

```json
{
    "subscriptionStatus": "NotSubscribed"
}
```
