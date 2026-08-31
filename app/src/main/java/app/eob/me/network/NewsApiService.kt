package app.eob.me.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

interface NewsApiService {
    @GET
    suspend fun getFeed(@Url feedUrl: String): ResponseBody
}

object RetrofitClient {
    private const val PLACEHOLDER_BASE_URL = "https://placeholder.invalid/"
    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val api: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(httpClient)
            .build()
            .create(NewsApiService::class.java)
    }
}
