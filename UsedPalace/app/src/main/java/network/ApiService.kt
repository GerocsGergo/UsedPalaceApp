package network


import com.example.usedpalace.responses.ApiResponse
import com.example.usedpalace.responses.ApiResponseForModify
import com.example.usedpalace.responses.ApiResponseForSaleWithSid
import com.example.usedpalace.responses.ApiResponseGeneric
import com.example.usedpalace.requests.CreateSaleRequest
import com.example.usedpalace.requests.DeleteSaleRequest
import com.example.usedpalace.requests.EmailVerificationRequest
import com.example.usedpalace.requests.EmailVerificationWithCodeRequest
import com.example.usedpalace.requests.ForgotPasswordRequest
import com.example.usedpalace.requests.ModifySaleRequest
import com.example.usedpalace.requests.NewLogin
import com.example.usedpalace.requests.RegUser
import com.example.usedpalace.requests.ResetPasswordRequest
import com.example.usedpalace.responses.ResponseMessage
import com.example.usedpalace.responses.ResponseMessageWithFolder
import com.example.usedpalace.responses.ResponseMessageWithUser
import com.example.usedpalace.Sale
import com.example.usedpalace.requests.DeleteSingleImageRequest
import com.example.usedpalace.requests.SearchRequest
import com.example.usedpalace.requests.SearchRequestID
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiService {

    @POST("register")
    fun registerUser(@Body user: RegUser): Call<ResponseMessage>

    @POST("login")
    fun loginUser(@Body user: NewLogin): Call<ResponseMessageWithUser>

    // Request a password reset (send 6-digit code)
    @POST("forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ResponseMessage>

    // Reset the user's password
    @POST("reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResponseMessage>

    @POST("send-verify-email")
    fun sendVerifyEmail(@Body request: EmailVerificationRequest): Call<ResponseMessage>

    @POST("verify-email")
    fun VerifyEmail(@Body request: EmailVerificationWithCodeRequest): Call<ResponseMessage>

    @GET("/api/sales")
    suspend fun getSales(): List<Sale>

    @POST("search-sales")
    suspend fun searchSales(@Body request: SearchRequest): ApiResponse

    @POST("search-salesID")
    suspend fun searchSalesID(@Body request: SearchRequestID): ApiResponseForSaleWithSid

    @POST("create-sale")
    fun createSale(@Body request: CreateSaleRequest): Call<ResponseMessageWithFolder>

    @Multipart
    @POST("image-uploader")
    fun uploadImage( @Part saleFolder: MultipartBody.Part,
                     @Part imageIndex: MultipartBody.Part,
                     @Part image: MultipartBody.Part): Call<ResponseMessage>



    @HTTP(method = "DELETE", path = "delete-sale", hasBody = true)
    suspend fun deleteSale(@Body request: DeleteSaleRequest): ApiResponseGeneric

    @POST("get-images-with-saleId")
    suspend fun searchImages(@Body request: SearchRequestID): ApiResponseForModify

    @PUT("modify-sale")
    suspend fun modifySale(@Body request: ModifySaleRequest): ResponseMessageWithFolder

    @POST("delete-single-image")
    suspend fun deleteSingleImage(@Body request: DeleteSingleImageRequest): ApiResponseGeneric
}