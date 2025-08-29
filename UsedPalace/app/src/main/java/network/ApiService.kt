package network


import com.example.usedpalace.responses.ApiResponseForSearchSales
import com.example.usedpalace.responses.ApiResponseForSalesWithEverything
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
import com.example.usedpalace.fragments.ChatAndMessages.responses.ChatListResponse
import com.example.usedpalace.fragments.ChatAndMessages.Requests.InitiateChatRequest
import com.example.usedpalace.fragments.ChatAndMessages.responses.InitiateChatResponse
import com.example.usedpalace.fragments.ChatAndMessages.Requests.SearchChatRequest
import com.example.usedpalace.responses.UsernameResponse
import com.example.usedpalace.requests.ChangeEmailRequest
import com.example.usedpalace.requests.ChangePasswordRequest
import com.example.usedpalace.requests.ChangePhoneNumberRequest
import com.example.usedpalace.requests.ConfirmDeleteAccount
import com.example.usedpalace.requests.ConfirmEmailOrPasswordChangeOrDeleteRequest
import com.example.usedpalace.requests.ConfirmPhoneNumberChangeRequest
import com.example.usedpalace.requests.DeleteAccountRequest
import com.example.usedpalace.requests.DeleteImagesRequest
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SaveFcmTokenRequest
import com.example.usedpalace.requests.SearchRequestName
import com.example.usedpalace.requests.SearchRequestID
import com.example.usedpalace.responses.ApiResponseForDeletedSaleWithEverything
import com.example.usedpalace.responses.ApiResponseForLoadSales
import com.example.usedpalace.responses.DeleteImagesResponse
import com.example.usedpalace.responses.ResponseForLoginTokenExpiration
import com.example.usedpalace.responses.ResponseGetImages
import com.example.usedpalace.responses.UserDataResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    //Login and register
    @POST("register")
    fun registerUser(@Body user: RegUser): Call<ResponseMessage>

    @POST("login")
    fun loginUser(@Body user: NewLogin): Call<ResponseMessageWithUser>

    @DELETE("logout")
    fun logoutUser(@Header("Authorization") authHeader: String): Call<ResponseMessage>


    @GET("verify-token")
    fun verifyToken(@Header("Authorization") authHeader: String): Call<ResponseForLoginTokenExpiration>

    @POST("forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ResponseMessage>

    @POST("confirm-forgot-password")
    fun confirmPasswordReset(@Body request: ResetPasswordRequest): Call<ResponseMessage>

    @POST("send-verify-email")
    fun sendVerifyEmail(@Body request: EmailVerificationRequest): Call<ResponseMessage>

    @POST("verify-email")
    fun verifyEmail(@Body request: EmailVerificationWithCodeRequest): Call<ResponseMessage>

    @POST("/get-safe-user-data")
    suspend fun getSafeUserData(@Body request: Map<String, Int>): UserDataResponse


    //Sales
    @GET("getSales")
    suspend fun getSales(
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): ApiResponseForLoadSales


    @POST("search-sales-withSaleName")
    suspend fun searchSales(@Body request: SearchRequestName): ApiResponseForSearchSales

    @POST("search-sales-withCategory")
    suspend fun searchSalesCategory(@Body request: SearchRequestName): ApiResponseForSearchSales


    @POST("search-salesID")
    suspend fun searchSalesID(@Body request: SearchRequestID): ApiResponseForSearchSales   //USER ID-vel keresi a Saleket

    @POST("search-sales-withSID")
    suspend fun searchSalesSID(@Body request: SearchRequestID): ApiResponseForSalesWithEverything //Sales SID-el keresi a Saleket

    @POST("search-deletedSales-withSID")
    suspend fun searchDeletedSalesSID(@Body request: SearchRequestID): ApiResponseForDeletedSaleWithEverything

    @POST("create-sale")
    suspend fun createSale(@Body request: CreateSaleRequest): ResponseMessageWithFolder

    @Multipart
    @POST("/upload-sale-images")
    suspend fun uploadSaleImages(
        @Part("saleFolder") saleFolder: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): ApiResponseGeneric

    @POST("/get-sale-images")
    suspend fun getSaleImages(
        @Body request: GetSaleImagesRequest
    ): ResponseGetImages



    @HTTP(method = "DELETE", path = "delete-sale", hasBody = true)
    suspend fun deleteSale(@Body request: DeleteSaleRequest): ApiResponseGeneric

    @POST("get-images-with-saleId")
    suspend fun getImages(@Body request: SearchRequestID): ApiResponseForSalesWithEverything

    @PUT("modify-sale")
    suspend fun modifySale(@Body request: ModifySaleRequest): ResponseMessageWithFolder

    @POST("delete-images")
    suspend fun deleteImages(@Body request: DeleteImagesRequest): DeleteImagesResponse


    //CHAT
    @POST("initiate-chat")
    suspend fun initiateChat(@Body request: InitiateChatRequest): InitiateChatResponse

    @POST("load-user-chats")
    suspend fun getAllChats(@Body request: SearchChatRequest): ChatListResponse

    @POST("search-username")
    suspend fun searchUsername(@Body request: SearchRequestID): UsernameResponse

    @POST("save-fcm-token")
    suspend fun saveFcmToken(@Body request: SaveFcmTokenRequest): ApiResponseGeneric

    //Profil menu
    @POST("request-user-email-change")
    fun requestEmailChange(@Body request: ChangeEmailRequest): Call<ApiResponseGeneric>

    @PUT("confirm-user-email-change")
    fun confirmEmailChange(@Body request: ConfirmEmailOrPasswordChangeOrDeleteRequest): Call<ApiResponseGeneric>

    @POST("request-password-change")
    fun requestPasswordChange(@Body request: ChangePasswordRequest): Call<ApiResponseGeneric>

    @PUT("confirm-password-change")
    fun confirmPasswordChange(@Body request: ConfirmEmailOrPasswordChangeOrDeleteRequest): Call<ApiResponseGeneric>

    @POST("request-phoneNumber-change")
    fun requestPhoneNumberChange(@Body request: ChangePhoneNumberRequest): Call<ApiResponseGeneric>

    @PUT("confirm-phoneNumber-change")
    fun confirmPhoneNumberChange(@Body request: ConfirmPhoneNumberChangeRequest): Call<ApiResponseGeneric>

    @POST("request-delete-user")
    fun requestDeleteUser(@Body request: DeleteAccountRequest): Call<ApiResponseGeneric>

    @POST("confirm-delete-user")
    fun confirmDeleteUser(@Body request: ConfirmDeleteAccount): Call<ApiResponseGeneric>



}