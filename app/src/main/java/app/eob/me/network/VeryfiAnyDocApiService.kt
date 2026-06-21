package app.eob.me.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit contract for Veryfi AnyDocs. Production calls are proxied through Firebase Cloud
 * Functions ([VeryfiDocumentClient]) so API credentials never ship in the Android binary.
 */
interface VeryfiAnyDocApiService {
    @Multipart
    @POST(VeryfiAnyDocConstants.ANY_DOCUMENTS_PATH)
    suspend fun processAnyDocument(
        @Header("Client-Id") clientId: String,
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("blueprint_name") blueprintName: RequestBody
    ): VeryfiAnyDocResponseDto
}
