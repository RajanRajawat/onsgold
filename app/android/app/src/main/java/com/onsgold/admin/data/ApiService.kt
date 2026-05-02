package com.onsgold.admin.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("api/v1/auth/me")
    suspend fun me(@Header("Authorization") authorization: String): UserResponse

    @POST("api/v1/auth/forgot-password/request")
    suspend fun requestForgotPassword(@Body body: ForgotPasswordRequest): MessageResponse

    @POST("api/v1/auth/forgot-password/reset")
    suspend fun resetForgotPassword(@Body body: ResetPasswordRequest): MessageResponse

    @GET("api/v1/admin/orders")
    suspend fun adminOrders(@Header("Authorization") authorization: String): List<OrderResponse>

    @GET("api/v1/admin/custom-requests")
    suspend fun adminCustomRequests(@Header("Authorization") authorization: String): List<CustomRequestResponse>

    @GET("api/v1/products")
    suspend fun products(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
        @Query("search") search: String? = null,
    ): ProductListResponse

    @Multipart
    @POST("api/v1/uploads/product-images")
    suspend fun uploadProductImages(
        @Header("Authorization") authorization: String,
        @Part files: List<MultipartBody.Part>,
    ): UploadResponse

    @POST("api/v1/products")
    suspend fun createProduct(
        @Header("Authorization") authorization: String,
        @Body body: ProductPayload,
    ): ProductResponse

    @PUT("api/v1/products/{identifier}")
    suspend fun updateProduct(
        @Header("Authorization") authorization: String,
        @Path("identifier") identifier: String,
        @Body body: ProductPayload,
    ): ProductResponse

    @DELETE("api/v1/products/{identifier}")
    suspend fun deleteProduct(
        @Header("Authorization") authorization: String,
        @Path("identifier") identifier: String,
    ): MessageResponse

    @GET("api/v1/admin/all-admins")
    suspend fun allAdmins(@Header("Authorization") authorization: String): AdminListEnvelope

    @POST("api/v1/admin/request-register-otp")
    suspend fun requestRegisterOtp(
        @Header("Authorization") authorization: String,
        @Body body: AdminRegisterRequest,
    ): MessageResponse

    @POST("api/v1/admin/register")
    suspend fun registerAdmin(
        @Header("Authorization") authorization: String,
        @Body body: AdminRegisterRequest,
    ): MessageResponse

    @POST("api/v1/admin/request-delete-otp")
    suspend fun requestDeleteOtp(
        @Header("Authorization") authorization: String,
        @Body body: TargetEmailRequest,
    ): MessageResponse

    @POST("api/v1/admin/delete-admin")
    suspend fun deleteAdmin(
        @Header("Authorization") authorization: String,
        @Body body: DeleteAdminRequest,
    ): MessageResponse

    @POST("api/v1/admin/request-edit-otp")
    suspend fun requestEditOtp(
        @Header("Authorization") authorization: String,
        @Body body: TargetEmailRequest,
    ): MessageResponse

    @POST("api/v1/admin/update-admin-credentials")
    suspend fun updateAdminCredentials(
        @Header("Authorization") authorization: String,
        @Body body: UpdateAdminCredentialsRequest,
    ): MessageResponse

    @GET("api/v1/admin/activity-logs")
    suspend fun activityLogs(@Header("Authorization") authorization: String): List<ActivityLogResponse>

    @PATCH("api/v1/admin/orders/{orderId}/status")
    suspend fun updateCatalogOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String,
        @Body body: StatusUpdateRequest,
    ): MessageResponse

    @PATCH("api/v1/admin/custom-requests/{requestId}/status")
    suspend fun updateCustomOrderStatus(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
        @Body body: StatusUpdateRequest,
    ): MessageResponse

    @POST("api/v1/admin/orders/{orderId}/comments")
    suspend fun addCatalogOrderComment(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String,
        @Body body: CommentRequest,
    ): MessageResponse

    @POST("api/v1/admin/custom-requests/{requestId}/comments")
    suspend fun addCustomOrderComment(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
        @Body body: CommentRequest,
    ): MessageResponse

    @DELETE("api/v1/admin/orders/{orderId}")
    suspend fun deleteCatalogOrder(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String,
    ): MessageResponse

    @DELETE("api/v1/admin/custom-requests/{requestId}")
    suspend fun deleteCustomOrder(
        @Header("Authorization") authorization: String,
        @Path("requestId") requestId: String,
    ): MessageResponse

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body body: UpdateProfileRequest,
    ): UserResponse

    @PATCH("api/v1/auth/me/password")
    suspend fun updatePassword(
        @Header("Authorization") authorization: String,
        @Body body: UpdatePasswordRequest,
    ): MessageResponse

    @PATCH("api/v1/auth/me/email")
    suspend fun updateEmail(
        @Header("Authorization") authorization: String,
        @Body body: UpdateEmailRequest,
    ): UserResponse

    @POST("api/v1/admin/report-bug")
    suspend fun reportBug(
        @Header("Authorization") authorization: String,
        @Body body: BugReportPayload,
    ): MessageResponse
}
