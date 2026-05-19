package app.eob.me.util

import android.content.Context
import android.net.Uri

object OcrProcessor {
    private val client = okhttp3.OkHttpClient()
    private val gson = com.google.gson.Gson()

    // Authenticated Veryfi API Developer Credentials
    private const val CLIENT_ID = "vrfFGAvphpB29l4yoZDf8ggk0HGN2NlbWlhm1Us"
    private const val USERNAME = "heathenistic.jd"
    private const val API_KEY = "cd5aa63ad8487603b344aee0e5326b5f"

    /**
     * Sends the selected EOB file straight to Veryfi's extraction engine
     */
    suspend fun recognizeFromUri(context: Context, uri: Uri): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Read the file's raw bytes directly from the user's phone storage
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw java.io.IOException("Failed to open file stream")
        val fileBytes = inputStream.readBytes()
        inputStream.close()

        // Configure the custom medical field parameters we want Veryfi to map out
        val parameters = mapOf(
            "categories" to listOf("Medical", "Health Insurance", "EOB"),
            "tags" to listOf("provider_name", "billed_amount", "insurance_paid", "patient_responsibility", "cpt_codes"),
            "auto_delete" to false
        )
        val parametersJson = gson.toJson(parameters)

        // Pack the image data and parameters together
        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "eob_upload.jpg",
                okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), fileBytes)
            )
            .addFormDataPart("parameters", parametersJson)
            .build()

        // Build the authenticated secure web request targeting Veryfi's platform
        val request = okhttp3.Request.Builder()
            .url("https://api.veryfi.com/api/v8/partner/documents")
            .addHeader("Client-Id", CLIENT_ID)
            .addHeader("Authorization", "apikey $USERNAME:$API_KEY")
            .post(requestBody)
            .build()

        // Execute the server communication call safely
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Veryfi extraction call failed with status code: ${response.code}")
            }

            // Return the raw structured JSON payload back to your app's processing queue
            return@withContext response.body()?.string() ?: throw java.io.IOException("Empty response received")
        }
    }
}