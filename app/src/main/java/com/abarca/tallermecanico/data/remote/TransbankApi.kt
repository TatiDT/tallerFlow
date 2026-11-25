package com.abarca.tallermecanico.data.remote

import com.abarca.tallermecanico.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class TransbankCreateRequest(
    @SerializedName("buy_order") val buyOrder: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("return_url") val returnUrl: String
)

data class TransbankCreateResponse(
    @SerializedName("token") val token: String,
    @SerializedName("url") val url: String
)

data class TransbankCommitResponse(
    @SerializedName("vci") val vci: String? = null,
    @SerializedName("amount") val amount: Long = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("buy_order") val buyOrder: String? = null,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("response_code") val responseCode: Int = -1,
    @SerializedName("authorization_code") val authorizationCode: String? = null,
    @SerializedName("transaction_date") val transactionDate: String? = null
) {
    val aprobado: Boolean get() = status == "AUTHORIZED" && responseCode == 0
}

interface TransbankApi {

    @POST("rswebpaytransaction/api/webpay/v1.2/transactions")
    suspend fun crearTransaccion(@Body request: TransbankCreateRequest): TransbankCreateResponse

    @PUT("rswebpaytransaction/api/webpay/v1.2/transactions/{token}")
    suspend fun confirmarTransaccion(@Path("token") token: String): TransbankCommitResponse
}

object TransbankClient {

    private const val INTEGRATION_COMMERCE_CODE = "597055555532"
    private const val INTEGRATION_API_KEY =
        "579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Tbk-Api-Key-Id", INTEGRATION_COMMERCE_CODE)
                .header("Tbk-Api-Key-Secret", INTEGRATION_API_KEY)
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        .build()

    val api: TransbankApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.TRANSBANK_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TransbankApi::class.java)
    }
}
