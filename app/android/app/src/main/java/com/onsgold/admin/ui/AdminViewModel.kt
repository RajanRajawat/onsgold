package com.onsgold.admin.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onsgold.admin.data.ActivityLogResponse
import com.onsgold.admin.data.AdminListItem
import com.onsgold.admin.data.AdminOrderItem
import com.onsgold.admin.data.AdminRepository
import com.onsgold.admin.data.ProductListResponse
import com.onsgold.admin.data.ProductPayload
import com.onsgold.admin.data.ProductResponse
import com.onsgold.admin.data.UserResponse
import com.onsgold.admin.data.toUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: AdminRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            val session = repository.storedSession()
            val user = session.user
            val token = session.token
            if (token.isNullOrBlank() || user == null) {
                _uiState.value = AdminUiState(isBootstrapping = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                currentUser = user,
                isAuthenticated = true,
            )

            runCatching {
                val refreshedUser = repository.refreshCurrentUser()
                _uiState.value = _uiState.value.copy(currentUser = refreshedUser)
                refreshAll()
            }.onFailure {
                repository.logout()
                _uiState.value = AdminUiState(isBootstrapping = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authLoading = true)
            runCatching {
                val user = repository.login(email, password)
                _uiState.value = _uiState.value.copy(
                    authLoading = false,
                    isAuthenticated = true,
                    currentUser = user,
                )
                refreshAll()
                emitEvent("Login successful.")
            }.onFailure {
                _uiState.value = _uiState.value.copy(authLoading = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AdminUiState(isBootstrapping = false)
        }
    }

    fun requestForgotOtp(email: String) {
        viewModelScope.launch {
            runCatching {
                repository.requestForgotPassword(email)
            }.onSuccess {
                emitEvent(it)
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun resetForgotPassword(email: String, otp: String, newPassword: String) {
        viewModelScope.launch {
            runCatching {
                repository.resetForgotPassword(email, otp, newPassword)
            }.onSuccess {
                emitEvent(it)
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun refreshAll() {
        loadOrders()
        loadProducts(_uiState.value.productPage, _uiState.value.productSearch, silent = false)
        if (isSuperAdmin()) {
            loadAdmins()
            loadActivityLogs()
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ordersLoading = true)
            runCatching {
                repository.loadOrders()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    ordersLoading = false,
                    orders = it,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(ordersLoading = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun loadProducts(page: Int = 1, search: String = _uiState.value.productSearch, silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                productsLoading = !silent,
                productPage = page,
                productSearch = search,
            )
            runCatching {
                repository.listProducts(page = page, pageSize = PAGE_SIZE, search = search)
            }.onSuccess { result ->
                applyProducts(result)
            }.onFailure {
                _uiState.value = _uiState.value.copy(productsLoading = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun createProduct(payload: ProductPayload, images: List<Uri>) {
        viewModelScope.launch {
            runCatching {
                repository.createProduct(payload, images)
            }.onSuccess {
                emitEvent("Product created successfully.")
                loadOrders()
                loadProducts(page = 1, search = _uiState.value.productSearch)
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun updateProduct(productId: String, payload: ProductPayload, images: List<Uri>) {
        viewModelScope.launch {
            runCatching {
                repository.updateProduct(productId, payload, images)
            }.onSuccess {
                emitEvent("Product updated.")
                loadOrders()
                loadProducts(page = _uiState.value.productPage, search = _uiState.value.productSearch)
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteProduct(productId)
            }.onSuccess {
                emitEvent(it)
                loadOrders()
                loadProducts(page = _uiState.value.productPage, search = _uiState.value.productSearch)
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun loadAdmins() {
        if (!isSuperAdmin()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(adminsLoading = true)
            runCatching {
                repository.listAdmins()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(adminsLoading = false, admins = it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(adminsLoading = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun loadActivityLogs() {
        if (!isSuperAdmin()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activityLoading = true)
            runCatching {
                repository.listActivityLogs()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(activityLoading = false, activityLogs = it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(activityLoading = false)
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun requestRegisterOtp(name: String, email: String) = simpleAdminAction(
        action = { repository.requestRegisterOtp(name, email) },
    )

    fun confirmRegisterAdmin(name: String, email: String, otp: String) = simpleAdminAction(
        action = { repository.registerAdmin(name, email, otp) },
        refreshAdmins = true,
    )

    fun requestDeleteOtp(email: String) = simpleAdminAction(
        action = { repository.requestDeleteOtp(email) },
    )

    fun confirmDeleteAdmin(email: String, otp: String) = simpleAdminAction(
        action = { repository.deleteAdmin(email, otp) },
        refreshAdmins = true,
    )

    fun requestEditOtp(targetEmail: String) = simpleAdminAction(
        action = { repository.requestEditOtp(targetEmail) },
    )

    fun updateAdminCredentials(targetEmail: String, otp: String, newEmail: String?, newPassword: String?) =
        simpleAdminAction(
            action = { repository.updateAdminCredentials(targetEmail, otp, newEmail, newPassword) },
            refreshAdmins = true,
        )

    fun updateOrderStatus(orderRef: String, orderKind: String, status: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateOrderStatus(orderRef, orderKind, status)
            }.onSuccess {
                emitEvent(it)
                loadOrders()
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun addOrderComment(orderRef: String, orderKind: String, comment: String) {
        viewModelScope.launch {
            runCatching {
                repository.addOrderComment(orderRef, orderKind, comment)
            }.onSuccess {
                emitEvent(it)
                loadOrders()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun deleteOrder(orderRef: String, orderKind: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteOrder(orderRef, orderKind)
            }.onSuccess {
                emitEvent(it)
                loadOrders()
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateProfile(name, email)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(currentUser = it)
                emitEvent("Profile updated.")
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            runCatching {
                repository.updatePassword(currentPassword, newPassword)
            }.onSuccess {
                emitEvent(it)
                logout()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun updateEmail(email: String, currentPassword: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateEmail(email, currentPassword)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(currentUser = it)
                emitEvent("Email updated. Please sign in again.")
                logout()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    fun reportBug(title: String, severity: String, description: String, imageUri: Uri?) {
        viewModelScope.launch {
            runCatching {
                repository.reportBug(title, severity, description, imageUri)
            }.onSuccess {
                emitEvent(it)
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    private fun applyProducts(result: ProductListResponse) {
        _uiState.value = _uiState.value.copy(
            productsLoading = false,
            products = result.items,
            productPage = result.page,
            productTotal = result.total,
        )
    }

    private fun isSuperAdmin(): Boolean {
        return _uiState.value.currentUser?.role == "super_admin"
    }

    private fun simpleAdminAction(
        action: suspend () -> String,
        refreshAdmins: Boolean = false,
    ) {
        viewModelScope.launch {
            runCatching {
                action()
            }.onSuccess {
                emitEvent(it)
                if (refreshAdmins) loadAdmins()
                if (isSuperAdmin()) loadActivityLogs()
            }.onFailure {
                emitEvent(it.toUserMessage())
            }
        }
    }

    private suspend fun emitEvent(message: String) {
        _events.emit(message)
    }

    companion object {
        const val PAGE_SIZE = 25
    }
}

data class AdminUiState(
    val isBootstrapping: Boolean = true,
    val isAuthenticated: Boolean = false,
    val authLoading: Boolean = false,
    val currentUser: UserResponse? = null,
    val ordersLoading: Boolean = false,
    val productsLoading: Boolean = false,
    val adminsLoading: Boolean = false,
    val activityLoading: Boolean = false,
    val orders: List<AdminOrderItem> = emptyList(),
    val products: List<ProductResponse> = emptyList(),
    val productSearch: String = "",
    val productPage: Int = 1,
    val productTotal: Int = 0,
    val admins: List<AdminListItem> = emptyList(),
    val activityLogs: List<ActivityLogResponse> = emptyList(),
)

class AdminViewModelFactory(
    private val repository: AdminRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
