# EOBme Cloud Functions

## Required secrets

The Veryfi credentials must be stored as Firebase Functions secrets, not in the Android app.

Set them before deploying functions:

```bash
firebase functions:secrets:set VERYFI_CLIENT_ID
firebase functions:secrets:set VERYFI_USERNAME
firebase functions:secrets:set VERYFI_API_KEY
```

Then deploy:

```bash
firebase deploy --only functions,firestore:rules,storage
```

The Android app uploads EOB files to `users/{uid}/eob_uploads/*`. The `processUploadedEobWithVeryfi` function calls Veryfi and writes normalized EOB data to both `users/{uid}/eobs` and `users/{uid}/eob_records`.
