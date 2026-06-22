package app.eob.me.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit contract for Veryfi documents API. blueprint_name in the multipart body routes
 * extraction to health_insurance_eob. Production calls are proxied through Firebase Cloud
 * Functions ([VeryfiDocumentClient]) so API credentials never ship in the Android binary.
 */
interface VeryfiAnyDocApiService {
    @Multipart
    @POST(VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH)
    suspend fun processAnyDocument(
        @Header("Client-Id") clientId: String,
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("blueprint_name") blueprintName: RequestBody,
        @Part("document_type") documentType: RequestBody,
        @Part("categories") categories: RequestBody
    ): VeryfiAnyDocResponseDto
}

object VeryfiApiClient {
    val anyDocApi: VeryfiAnyDocApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(VeryfiAnyDocConstants.BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(VeryfiAnyDocApiService::class.java)
    }
}
