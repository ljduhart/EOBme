# EOBme Cloud Functions

## Required secrets

Veryfi credentials must be stored as Firebase Functions secrets — never in the Android app.

From the Veryfi portal **Settings → Keys**, copy these four values:

| Firebase secret | Veryfi portal field |
|-----------------|---------------------|
| `VERYFI_CLIENT_ID` | Client ID |
| `VERYFI_CLIENT_SECRET` | Client Secret |
| `VERYFI_USERNAME` | Username (not the Client ID) |
| `VERYFI_API_KEY` | API Key |

Set them before deploying functions:

```bash
firebase functions:secrets:set VERYFI_CLIENT_ID
firebase functions:secrets:set VERYFI_CLIENT_SECRET
firebase functions:secrets:set VERYFI_USERNAME
firebase functions:secrets:set VERYFI_API_KEY
```

Then deploy:

```bash
firebase deploy --only functions,firestore:rules,storage
```

## Veryfi AnyDocs endpoint

Hybrid extraction and the Storage trigger POST to:

`https://api.veryfi.com/api/v8/partner/any-documents/`

with JSON body:

```json
{
  "file_url": "https://...",
  "blueprint_name": "health_insurance_eob"
}
```

Requests are signed with `X-Veryfi-Request-Signature` and `X-Veryfi-Request-Timestamp` using `VERYFI_CLIENT_SECRET`.

The Android app uploads EOB files to `users/{uid}/eobs/*`. The `processUploadedEobWithVeryfi` function calls Veryfi and writes normalized EOB data to both `users/{uid}/eobs` and `users/{uid}/eob_records`.
