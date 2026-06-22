package app.eob.me.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Typed Retrofit/OkHttp client for Veryfi AnyDocs. Production hybrid scans still proxy through
 * Firebase Cloud Functions; this client documents the multipart contract and timeout profile.
 */
object VeryfiAnyDocNetworkClient {
    const val CONNECT_TIMEOUT_SECONDS = 45L
    const val READ_TIMEOUT_SECONDS = 45L
    const val WRITE_TIMEOUT_SECONDS = 45L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    val anyDocApi: VeryfiAnyDocApiService by lazy {
        Retrofit.Builder()
            .baseUrl(VeryfiAnyDocConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VeryfiAnyDocApiService::class.java)
    }

    fun blueprintRequestBody(
        blueprintName: String = VeryfiAnyDocConstants.BLUEPRINT_HEALTH_INSURANCE_EOB
    ): RequestBody = blueprintName.toRequestBody("text/plain".toMediaType())

    fun documentTypeRequestBody(
        documentType: String = VeryfiAnyDocConstants.DOCUMENT_TYPE_EOB
    ): RequestBody = documentType.toRequestBody("text/plain".toMediaType())

    fun categoriesRequestBody(
        categories: List<String> = VeryfiAnyDocConstants.CATEGORIES_INSURANCE
    ): RequestBody = categories.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        .toRequestBody("application/json".toMediaType())

    fun filePart(file: File, contentType: String): MultipartBody.Part {
        val mediaType = contentType.toMediaType()
        return MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody(mediaType)
        )
    }
}
