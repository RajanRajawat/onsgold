package com.onsgold.admin.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ProductResponse(
    val id: String,
    @SerialName("product_id") val productId: String,
    val title: String,
    val category: String,
    val metal: String,
    val description: String,
    val purity: String,
    val weight: Double,
    val price: Double? = null,
    @SerialName("price_on_request") val priceOnRequest: Boolean = false,
    val images: List<String>,
    @SerialName("stock_status") val stockStatus: String,
    val tags: List<String> = emptyList(),
    val slug: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ProductListResponse(
    val items: List<ProductResponse>,
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
)

@Serializable
data class OrderProductSnapshot(
    @SerialName("product_id") val productId: String,
    val title: String,
    val quantity: Int,
    val price: Double? = null,
    val image: String? = null,
)

@Serializable
data class OrderComment(
    val comment: String,
    @SerialName("added_by") val addedBy: String,
    @SerialName("added_by_name") val addedByName: String? = null,
    @SerialName("added_by_email") val addedByEmail: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class OrderResponse(
    val id: String,
    @SerialName("inquiry_id") val inquiryId: String,
    @SerialName("customer_name") val customerName: String,
    val phone: String,
    val notes: String? = null,
    val status: String,
    @SerialName("inquiry_source") val inquirySource: String,
    val products: List<OrderProductSnapshot>,
    val comments: List<OrderComment> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("whatsapp_url") val whatsappUrl: String? = null,
)

@Serializable
data class CustomRequestResponse(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("customer_name") val customerName: String,
    val phone: String,
    val city: String,
    @SerialName("jewelry_type") val jewelryType: String,
    val budget: String,
    val description: String,
    val purity: String,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val status: String,
    val comments: List<OrderComment> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("inquiry_source") val inquirySource: String,
    val email: String? = null,
    @SerialName("whatsapp_url") val whatsappUrl: String? = null,
)

@Serializable
data class ActivityLogResponse(
    val id: String,
    val action: String,
    @SerialName("performed_by_email") val performedByEmail: String? = null,
    @SerialName("performed_by_name") val performedByName: String? = null,
    val target: String? = null,
    val detail: String? = null,
    @SerialName("old_value") val oldValue: JsonElement? = null,
    @SerialName("new_value") val newValue: JsonElement? = null,
    val extra: JsonElement? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AdminListEnvelope(
    val message: String,
    val data: List<AdminListItem>,
)

@Serializable
data class AdminListItem(
    val id: String,
    @SerialName("_id") val legacyId: String? = null,
    val name: String,
    val email: String,
    val role: String,
    val roles: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class ProductPayload(
    val title: String,
    val category: String,
    val metal: String,
    val description: String,
    val purity: String,
    val weight: Double,
    val price: Double? = null,
    @SerialName("price_on_request") val priceOnRequest: Boolean = false,
    val images: List<String>,
    @SerialName("stock_status") val stockStatus: String,
    val tags: List<String>,
)

@Serializable
data class StatusUpdateRequest(
    val status: String,
)

@Serializable
data class CommentRequest(
    val comment: String,
)

@Serializable
data class AdminRegisterRequest(
    val name: String,
    val email: String,
    val otp: String? = null,
)

@Serializable
data class TargetEmailRequest(
    @SerialName("target_email") val targetEmail: String,
)

@Serializable
data class DeleteAdminRequest(
    val email: String,
    val otp: String,
)

@Serializable
data class UpdateAdminCredentialsRequest(
    @SerialName("target_email") val targetEmail: String,
    val otp: String,
    @SerialName("new_email") val newEmail: String? = null,
    @SerialName("new_password") val newPassword: String? = null,
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String,
)

@Serializable
data class UpdatePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class UpdateEmailRequest(
    val email: String,
    @SerialName("current_password") val currentPassword: String,
)

@Serializable
data class BugReportPayload(
    val title: String,
    val severity: String,
    val description: String,
    @SerialName("image_base64") val imageBase64: String? = null,
    @SerialName("image_mime") val imageMime: String? = null,
)

@Serializable
data class UploadResponse(
    val urls: List<String>,
)

data class AdminOrderItem(
    val id: String,
    val orderRef: String,
    val orderKind: String,
    val customerName: String,
    val phone: String,
    val status: String,
    val createdAt: String,
    val notes: String? = null,
    val inquirySource: String,
    val products: List<OrderProductSnapshot> = emptyList(),
    val comments: List<OrderComment> = emptyList(),
    val city: String? = null,
    val jewelryType: String? = null,
    val budget: String? = null,
    val description: String? = null,
    val purity: String? = null,
    val imageUrls: List<String> = emptyList(),
    val whatsappUrl: String? = null,
    val email: String? = null,
)
