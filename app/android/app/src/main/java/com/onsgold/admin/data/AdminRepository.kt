package com.onsgold.admin.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.onsgold.admin.BuildConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
class AdminRepository(
    private val context: Context,
    private val sessionStore: SessionStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val sessionMutex = Mutex()

    private val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    suspend fun storedSession(): StoredSession = sessionStore.sessionFlow.first()

    suspend fun login(email: String, password: String): UserResponse {
        val result = api.login(LoginRequest(email = email.trim(), password = password))
        sessionStore.saveSession(result.accessToken, result.user)
        return result.user
    }

    suspend fun refreshCurrentUser(): UserResponse {
        val token = requireToken()
        val user = api.me(authHeader(token))
        sessionStore.updateUser(user)
        return user
    }

    suspend fun requestForgotPassword(email: String): String {
        return api.requestForgotPassword(ForgotPasswordRequest(email.trim())).message
    }

    suspend fun resetForgotPassword(email: String, otp: String, newPassword: String): String {
        return api.resetForgotPassword(
            ResetPasswordRequest(email = email.trim(), otp = otp.trim(), newPassword = newPassword),
        ).message
    }

    suspend fun loadOrders(): List<AdminOrderItem> {
        val token = requireToken()
        val auth = authHeader(token)
        val orders = api.adminOrders(auth).map {
            AdminOrderItem(
                id = it.id,
                orderRef = it.inquiryId,
                orderKind = "catalog_order",
                customerName = it.customerName,
                phone = it.phone,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt ?: it.createdAt,
                notes = it.notes,
                inquirySource = it.inquirySource,
                products = it.products,
                comments = it.comments,
                whatsappUrl = it.whatsappUrl,
            )
        }
        val custom = api.adminCustomRequests(auth).map {
            AdminOrderItem(
                id = it.id,
                orderRef = it.requestId,
                orderKind = "custom_order",
                customerName = it.customerName,
                phone = it.phone,
                status = it.status,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt ?: it.createdAt,
                inquirySource = it.inquirySource,
                comments = it.comments,
                city = it.city,
                jewelryType = it.jewelryType,
                budget = it.budget,
                description = it.description,
                purity = it.purity,
                imageUrls = it.imageUrls,
                whatsappUrl = it.whatsappUrl,
                email = it.email,
            )
        }
        return (orders + custom).sortedByDescending { it.createdAt }
    }

    suspend fun listProducts(page: Int, pageSize: Int, search: String?): ProductListResponse {
        val token = requireToken()
        return api.products(authHeader(token), page, pageSize, search?.takeIf { it.isNotBlank() })
    }

    suspend fun createProduct(payload: ProductPayload, imageUris: List<Uri>): ProductResponse {
        val token = requireToken()
        val images = uploadProductImages(token, imageUris)
        return api.createProduct(authHeader(token), payload.copy(images = images))
    }

    suspend fun updateProduct(productId: String, payload: ProductPayload, newImageUris: List<Uri>): ProductResponse {
        val token = requireToken()
        val uploadedImages = if (newImageUris.isEmpty()) emptyList() else uploadProductImages(token, newImageUris)
        val mergedImages = payload.images + uploadedImages
        return api.updateProduct(authHeader(token), productId, payload.copy(images = mergedImages))
    }

    suspend fun deleteProduct(productId: String): String {
        val token = requireToken()
        return api.deleteProduct(authHeader(token), productId).message
    }

    suspend fun listAdmins(): List<AdminListItem> {
        val token = requireToken()
        return api.allAdmins(authHeader(token)).data
    }

    suspend fun requestRegisterOtp(name: String, email: String): String {
        val token = requireToken()
        return api.requestRegisterOtp(
            authHeader(token),
            AdminRegisterRequest(name = name.trim(), email = email.trim()),
        ).message
    }

    suspend fun registerAdmin(name: String, email: String, otp: String): String {
        val token = requireToken()
        return api.registerAdmin(
            authHeader(token),
            AdminRegisterRequest(name = name.trim(), email = email.trim(), otp = otp.trim()),
        ).message
    }

    suspend fun requestDeleteOtp(email: String): String {
        val token = requireToken()
        return api.requestDeleteOtp(authHeader(token), TargetEmailRequest(email.trim())).message
    }

    suspend fun deleteAdmin(email: String, otp: String): String {
        val token = requireToken()
        return api.deleteAdmin(authHeader(token), DeleteAdminRequest(email.trim(), otp.trim())).message
    }

    suspend fun requestEditOtp(targetEmail: String): String {
        val token = requireToken()
        return api.requestEditOtp(authHeader(token), TargetEmailRequest(targetEmail.trim())).message
    }

    suspend fun updateAdminCredentials(targetEmail: String, otp: String, newEmail: String?, newPassword: String?): String {
        val token = requireToken()
        return api.updateAdminCredentials(
            authHeader(token),
            UpdateAdminCredentialsRequest(
                targetEmail = targetEmail.trim(),
                otp = otp.trim(),
                newEmail = newEmail?.trim()?.takeIf { it.isNotBlank() },
                newPassword = newPassword?.takeIf { it.isNotBlank() },
            ),
        ).message
    }

    suspend fun listActivityLogs(): List<ActivityLogResponse> {
        val token = requireToken()
        return api.activityLogs(authHeader(token))
    }

    suspend fun updateOrderStatus(orderRef: String, orderKind: String, status: String): String {
        val token = requireToken()
        val auth = authHeader(token)
        return if (orderKind == "custom_order") {
            api.updateCustomOrderStatus(auth, orderRef, StatusUpdateRequest(status)).message
        } else {
            api.updateCatalogOrderStatus(auth, orderRef, StatusUpdateRequest(status)).message
        }
    }

    suspend fun addOrderComment(orderRef: String, orderKind: String, comment: String): String {
        val token = requireToken()
        val auth = authHeader(token)
        return if (orderKind == "custom_order") {
            api.addCustomOrderComment(auth, orderRef, CommentRequest(comment.trim())).message
        } else {
            api.addCatalogOrderComment(auth, orderRef, CommentRequest(comment.trim())).message
        }
    }

    suspend fun deleteOrder(orderRef: String, orderKind: String): String {
        val token = requireToken()
        val auth = authHeader(token)
        return if (orderKind == "custom_order") {
            api.deleteCustomOrder(auth, orderRef).message
        } else {
            api.deleteCatalogOrder(auth, orderRef).message
        }
    }

    suspend fun updateProfile(name: String, email: String): UserResponse {
        val token = requireToken()
        val updated = api.updateProfile(authHeader(token), UpdateProfileRequest(name.trim(), email.trim()))
        sessionStore.updateUser(updated)
        return updated
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): String {
        val token = requireToken()
        return api.updatePassword(
            authHeader(token),
            UpdatePasswordRequest(currentPassword = currentPassword, newPassword = newPassword),
        ).message
    }

    suspend fun updateEmail(email: String, currentPassword: String): UserResponse {
        val token = requireToken()
        val updated = api.updateEmail(
            authHeader(token),
            UpdateEmailRequest(email.trim(), currentPassword),
        )
        sessionStore.updateUser(updated)
        return updated
    }

    suspend fun reportBug(title: String, severity: String, description: String, imageUri: Uri?): String {
        val token = requireToken()
        val payload = if (imageUri == null) {
            BugReportPayload(title = title.trim(), severity = severity, description = description.trim())
        } else {
            val bytes = readBytes(imageUri)
            val mime = context.contentResolver.getType(imageUri)?.lowercase(Locale.ROOT) ?: "image/jpeg"
            BugReportPayload(
                title = title.trim(),
                severity = severity,
                description = description.trim(),
                imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                imageMime = mime,
            )
        }
        return api.reportBug(authHeader(token), payload).message
    }

    suspend fun logout() {
        sessionMutex.withLock {
            sessionStore.clear()
        }
    }

    private suspend fun uploadProductImages(token: String, uris: List<Uri>): List<String> {
        if (uris.isEmpty()) return emptyList()
        val files = uris.mapIndexed { index, uri ->
            val bytes = readBytes(uri)
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = mime.substringAfter('/', "jpg")
            val body = bytes.toRequestBody(mime.toMediaType())
            MultipartBody.Part.createFormData(
                name = "files",
                filename = "product_${System.currentTimeMillis()}_$index.$extension",
                body = body,
            )
        }
        return api.uploadProductImages(authHeader(token), files).urls
    }

    private fun readBytes(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: throw IOException("Unable to read selected file.")
    }

    private suspend fun requireToken(): String {
        return storedSession().token ?: throw IllegalStateException("You are not signed in.")
    }

    private fun authHeader(token: String): String = "Bearer $token"
}

fun Throwable.toUserMessage(): String {
    return when (this) {
        is HttpException -> {
            val payload = response()?.errorBody()?.string().orEmpty()
            val detail = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.getOrNull(1)
            detail ?: message()
        }
        is IOException -> message ?: "Unable to connect to the server."
        else -> message ?: "Something went wrong."
    }
}
