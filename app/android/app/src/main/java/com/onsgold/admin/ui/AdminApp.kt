package com.onsgold.admin.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.onsgold.admin.data.ActivityLogResponse
import com.onsgold.admin.data.AdminListItem
import com.onsgold.admin.data.AdminOrderItem
import com.onsgold.admin.data.OrderComment
import com.onsgold.admin.data.OrderProductSnapshot
import com.onsgold.admin.data.ProductPayload
import com.onsgold.admin.data.ProductResponse
import com.onsgold.admin.data.UserResponse
import com.onsgold.admin.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private val ProductCategories = listOf(
    "Ring", "Necklace", "Chain", "Pendant", "Locket", "Mangalsutra", "Choker", "Haram",
    "Bridal Set", "Jewelry Set", "Earrings", "Jhumka", "Bali", "Stud", "Nose Pin", "Nath",
    "Maang Tikka", "Bracelet", "Bangle", "Kada", "Anklet", "Toe Ring", "Armlet",
    "Kamarbandh", "Brooch", "Temple Jewelry", "Gold Coin",
)
private val StockOptions = listOf("in_stock", "low_stock", "out_of_stock", "made_to_order")
private val OrderStatusOptions = listOf("new", "contacted", "quoted", "closed")
private val SeverityOptions = listOf("Urgent", "High", "Medium", "Low")

private enum class RootDestination(val title: String) {
    Products("Products"),
    Orders("Orders"),
    Admins("Manage Admins"),
    Activity("Activity Logs"),
    Account("My Account"),
}

// Notification banner colors
private val BannerGreen = Color(0xFF22C55E)
private val BannerRed = Color(0xFFEF4444)
private val NavBarBlue = Color(0xFF1A56DB)

// Status pill colors
private val StatusNew = Color(0xFF9CA3AF)
private val StatusContacted = Color(0xFF3B82F6)
private val StatusQuoted = Color(0xFFF59E0B)
private val StatusClosed = Color(0xFF22C55E)

// Stock dot colors
private val StockInColor = Color(0xFF22C55E)
private val StockLowColor = Color(0xFFF59E0B)
private val StockOutColor = Color(0xFFEF4444)
private val StockMadeColor = Color(0xFF3B82F6)

// Role badge colors
private val RoleSuperAdmin = Color(0xFF1E3A8A)
private val RoleAdmin = Color(0xFF93C5FD)

// Log action colors
private val LogOrderCreated = Color(0xFF3B82F6)
private val LogStatusUpdated = Color(0xFFF59E0B)
private val LogLogin = Color(0xFF9CA3AF)

@Composable
fun AdminApp(viewModel: AdminViewModel) {
    val state by viewModel.uiState.collectAsState()
    var currentNotification by remember { mutableStateOf<AppNotification?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { notification ->
            currentNotification = notification
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.isBootstrapping -> BootScreen()
            !state.isAuthenticated -> LoginScreen(
                loading = state.authLoading,
                onLogin = viewModel::login,
                onRequestForgotOtp = viewModel::requestForgotOtp,
                onResetForgotPassword = viewModel::resetForgotPassword,
            )
            else -> HomeScreen(
                state = state,
                viewModel = viewModel,
            )
        }

        // Floating notification banner overlay
        NotificationBanner(
            notification = currentNotification,
            onDismiss = { currentNotification = null },
        )
    }
}

@Composable
private fun NotificationBanner(
    notification: AppNotification?,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(notification) {
        if (notification != null) {
            delay(3000)
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            notification?.let { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isError) BannerRed else BannerGreen,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (notif.isError) "❌" else "✅",
                            fontSize = 18.sp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = notif.message,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BootScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BrandHeader(onPrimary = true)
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Text(
                "Loading ONS Gold Admin",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BrandHeader(
    compact: Boolean = false,
    onPrimary: Boolean = false,
) {
    val titleColor = if (onPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (onPrimary) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "ONS Gold logo",
            modifier = Modifier
                .size(if (compact) 42.dp else 62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .padding(if (compact) 6.dp else 8.dp),
            contentScale = ContentScale.Fit,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "ONS Gold",
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            Text(
                text = "Admin Portal",
                style = MaterialTheme.typography.labelLarge,
                color = subtitleColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onRequestForgotOtp: (String) -> Unit,
    onResetForgotPassword: (String, String, String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var forgotMode by rememberSaveable { mutableStateOf(false) }
    var forgotOtp by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var forgotPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var forgotConfirmVisible by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BrandHeader()
                Text(
                    text = if (forgotMode) "Reset access" else "Admin mobile portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (forgotMode) {
                        "Request an OTP, set a new password, and get back into the portal from your phone."
                    } else {
                        "Manage products, orders, admins, and account operations with the same gold-red identity as the website."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!forgotMode) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    PasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        visible = passwordVisible,
                        onVisibilityChange = { passwordVisible = !passwordVisible },
                    )
                    Button(
                        onClick = { onLogin(email.trim(), password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text("Sign In")
                    }
                    TextButton(onClick = { forgotMode = true }) {
                        Text("Forgot password?")
                    }
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Registered admin email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    Button(
                        onClick = { onRequestForgotOtp(email.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank(),
                    ) {
                        Text("Send OTP")
                    }
                    OutlinedTextField(
                        value = forgotOtp,
                        onValueChange = { forgotOtp = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    PasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New password",
                        visible = forgotPasswordVisible,
                        onVisibilityChange = { forgotPasswordVisible = !forgotPasswordVisible },
                    )
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm password",
                        visible = forgotConfirmVisible,
                        onVisibilityChange = { forgotConfirmVisible = !forgotConfirmVisible },
                    )
                    Button(
                        onClick = { onResetForgotPassword(email.trim(), forgotOtp.trim(), newPassword) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank() &&
                            forgotOtp.isNotBlank() &&
                            newPassword.isNotBlank() &&
                            newPassword == confirmPassword,
                    ) {
                        Text("Reset Password")
                    }
                    TextButton(onClick = { forgotMode = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to sign in")
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onVisibilityChange) {
                Text(if (visible) "Hide" else "Show")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: AdminUiState,
    viewModel: AdminViewModel,
) {
    var destination by rememberSaveable { mutableStateOf(RootDestination.Products) }
    var productDialog by remember { mutableStateOf<ProductEditorState?>(null) }
    var selectedOrderRef by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOrderKind by rememberSaveable { mutableStateOf<String?>(null) }
    var profileDialog by remember { mutableStateOf<ProfileDialogMode?>(null) }
    var bugDialogOpen by remember { mutableStateOf(false) }
    var previewProduct by remember { mutableStateOf<OrderProductSnapshot?>(null) }

    val selectedOrder = state.orders.firstOrNull {
        it.orderRef == selectedOrderRef && it.orderKind == selectedOrderKind
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AdminBottomBar(
                destination = destination,
                onSelectDestination = { dest ->
                    destination = dest
                    if (dest == RootDestination.Admins) viewModel.loadAdmins()
                    if (dest == RootDestination.Activity) viewModel.loadActivityLogs()
                },
            )
        },
    ) { scaffoldPadding ->
        AdminContent(
            scaffoldPadding = scaffoldPadding,
            destination = destination,
            state = state,
            onSearchProducts = { viewModel.loadProducts(page = 1, search = it) },
            onChangeProductPage = { viewModel.loadProducts(page = it, search = state.productSearch) },
            onRefreshProducts = { viewModel.loadProducts(page = state.productPage, search = state.productSearch) },
            onCreateProduct = { productDialog = ProductEditorState() },
            onEditProduct = { productDialog = ProductEditorState.fromProduct(it) },
            onOpenOrder = {
                selectedOrderRef = it.orderRef
                selectedOrderKind = it.orderKind
            },
            onRequestAdminsRefresh = viewModel::loadAdmins,
            onRequestActivityRefresh = viewModel::loadActivityLogs,
            onOpenProfileDialog = { profileDialog = it },
            onOpenBugDialog = { bugDialogOpen = true },
            onNavigateTo = { destination = it },
            onLogout = viewModel::logout,
            viewModel = viewModel,
        )
    }

    productDialog?.let { editor ->
        ProductEditorSheet(
            initial = editor,
            onDismiss = { productDialog = null },
            onSave = { payload, imageUris ->
                if (editor.productId == null) {
                    viewModel.createProduct(payload, imageUris)
                } else {
                    viewModel.updateProduct(editor.productId, payload, imageUris)
                }
                productDialog = null
            },
            onDelete = {
                editor.productId?.let(viewModel::deleteProduct)
                productDialog = null
            },
        )
    }

    selectedOrder?.let { order ->
        OrderDetailSheet(
            order = order,
            currentUser = state.currentUser,
            onDismiss = {
                selectedOrderRef = null
                selectedOrderKind = null
            },
            onUpdateStatus = { status -> viewModel.updateOrderStatus(order.orderRef, order.orderKind, status) },
            onAddComment = { comment -> viewModel.addOrderComment(order.orderRef, order.orderKind, comment) },
            onOpenProduct = { previewProduct = it },
            onDelete = {
                viewModel.deleteOrder(order.orderRef, order.orderKind)
                selectedOrderRef = null
                selectedOrderKind = null
            },
        )
    }

    profileDialog?.let { mode ->
        ProfileDialog(
            mode = mode,
            currentUser = state.currentUser,
            onDismiss = { profileDialog = null },
            onSubmit = { fieldA, fieldB ->
                when (mode) {
                    ProfileDialogMode.Name -> state.currentUser?.let {
                        viewModel.updateProfile(fieldA, it.email)
                    }
                    ProfileDialogMode.Password -> viewModel.updatePassword(fieldA, fieldB)
                    ProfileDialogMode.Email -> viewModel.updateEmail(fieldA, fieldB)
                }
                profileDialog = null
            },
        )
    }

    if (bugDialogOpen) {
        BugReportSheet(
            onDismiss = { bugDialogOpen = false },
            onSubmit = { title, severity, description, imageUri ->
                viewModel.reportBug(title, severity, description, imageUri)
                bugDialogOpen = false
            },
        )
    }

    previewProduct?.let { snapshot ->
        ProductPreviewDialog(
            snapshot = snapshot,
            onDismiss = { previewProduct = null },
        )
    }
}

@Composable
private fun AdminBottomBar(
    destination: RootDestination,
    onSelectDestination: (RootDestination) -> Unit,
) {
    val isMore = destination !in listOf(RootDestination.Products, RootDestination.Orders)
    NavigationBar(
        containerColor = NavBarBlue,
        contentColor = Color.White,
    ) {
        NavigationBarItem(
            selected = destination == RootDestination.Products,
            onClick = { onSelectDestination(RootDestination.Products) },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = RootDestination.Products.title) },
            label = {
                Text(
                    "Products",
                    fontWeight = if (destination == RootDestination.Products) FontWeight.Bold else FontWeight.Normal,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                indicatorColor = Color.White.copy(alpha = 0.18f),
            ),
        )
        NavigationBarItem(
            selected = destination == RootDestination.Orders,
            onClick = { onSelectDestination(RootDestination.Orders) },
            icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = RootDestination.Orders.title) },
            label = {
                Text(
                    "Orders",
                    fontWeight = if (destination == RootDestination.Orders) FontWeight.Bold else FontWeight.Normal,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                indicatorColor = Color.White.copy(alpha = 0.18f),
            ),
        )
        NavigationBarItem(
            selected = isMore,
            onClick = { onSelectDestination(RootDestination.Account) },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
            label = {
                Text(
                    "More",
                    fontWeight = if (isMore) FontWeight.Bold else FontWeight.Normal,
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                unselectedIconColor = Color.White.copy(alpha = 0.7f),
                unselectedTextColor = Color.White.copy(alpha = 0.7f),
                indicatorColor = Color.White.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun AdminContent(
    scaffoldPadding: PaddingValues,
    destination: RootDestination,
    state: AdminUiState,
    onSearchProducts: (String) -> Unit,
    onChangeProductPage: (Int) -> Unit,
    onRefreshProducts: () -> Unit,
    onCreateProduct: () -> Unit,
    onEditProduct: (ProductResponse) -> Unit,
    onOpenOrder: (AdminOrderItem) -> Unit,
    onRequestAdminsRefresh: () -> Unit,
    onRequestActivityRefresh: () -> Unit,
    onOpenProfileDialog: (ProfileDialogMode) -> Unit,
    onOpenBugDialog: () -> Unit,
    onNavigateTo: (RootDestination) -> Unit,
    onLogout: () -> Unit,
    viewModel: AdminViewModel,
) {
    when (destination) {
        RootDestination.Products -> ProductsScreen(
            padding = screenPadding(scaffoldPadding),
            products = state.products,
            loading = state.productsLoading,
            page = state.productPage,
            total = state.productTotal,
            pageSize = AdminViewModel.PAGE_SIZE,
            currentSearch = state.productSearch,
            onSearch = onSearchProducts,
            onPrev = { if (state.productPage > 1) onChangeProductPage(state.productPage - 1) },
            onNext = {
                val totalPages = maxOf(1, (state.productTotal + AdminViewModel.PAGE_SIZE - 1) / AdminViewModel.PAGE_SIZE)
                if (state.productPage < totalPages) onChangeProductPage(state.productPage + 1)
            },
            onRefresh = onRefreshProducts,
            onCreateProduct = onCreateProduct,
            onEditProduct = onEditProduct,
        )
        RootDestination.Orders -> OrdersScreen(
            padding = screenPadding(scaffoldPadding),
            orders = state.orders,
            loading = state.ordersLoading,
            onOpenOrder = onOpenOrder,
        )
        RootDestination.Admins -> AdminsScreen(
            padding = screenPadding(scaffoldPadding),
            currentUser = state.currentUser,
            admins = state.admins,
            loading = state.adminsLoading,
            onRefresh = onRequestAdminsRefresh,
            onRequestRegisterOtp = viewModel::requestRegisterOtp,
            onConfirmRegister = viewModel::confirmRegisterAdmin,
            onRequestDeleteOtp = viewModel::requestDeleteOtp,
            onConfirmDeleteAdmin = viewModel::confirmDeleteAdmin,
            onRequestEditOtp = viewModel::requestEditOtp,
            onUpdateAdminCredentials = viewModel::updateAdminCredentials,
        )
        RootDestination.Activity -> ActivityLogsScreen(
            padding = screenPadding(scaffoldPadding),
            activityLogs = state.activityLogs,
            loading = state.activityLoading,
            onRefresh = onRequestActivityRefresh,
        )
        RootDestination.Account -> MoreScreen(
            padding = screenPadding(scaffoldPadding),
            currentUser = state.currentUser,
            isSuperAdmin = state.currentUser?.role == "super_admin",
            onOpenProfileDialog = onOpenProfileDialog,
            onOpenBugDialog = onOpenBugDialog,
            onNavigateTo = onNavigateTo,
            onLogout = onLogout,
        )
    }
}

private fun screenPadding(scaffoldPadding: PaddingValues): PaddingValues {
    return PaddingValues(
        start = 16.dp,
        top = 16.dp,
        end = 16.dp,
        bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionHeroCard(
    title: String,
    body: String,
    highlights: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
            )
            if (highlights.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    highlights.filter { it.isNotBlank() }.forEach { value ->
                        AssistChip(
                            onClick = {},
                            label = { Text(value) },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                labelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductsScreen(
    padding: PaddingValues,
    products: List<ProductResponse>,
    loading: Boolean,
    page: Int,
    total: Int,
    pageSize: Int,
    currentSearch: String,
    onSearch: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    onCreateProduct: () -> Unit,
    onEditProduct: (ProductResponse) -> Unit,
) {
    var query by rememberSaveable(currentSearch) { mutableStateOf(currentSearch) }
    val totalPages = maxOf(1, (total + pageSize - 1) / pageSize)
    var showFilter by remember { mutableStateOf(false) }
    var filterCategory by rememberSaveable { mutableStateOf("All") }
    var filterMetal by rememberSaveable { mutableStateOf("All") }
    var filterStock by rememberSaveable { mutableStateOf("All") }

    val filtered = remember(products, filterCategory, filterMetal, filterStock) {
        products.filter { p ->
            (filterCategory == "All" || p.category.equals(filterCategory, ignoreCase = true)) &&
            (filterMetal == "All" || p.metal.equals(filterMetal, ignoreCase = true)) &&
            (filterStock == "All" || p.stockStatus == filterStock)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("Search by name, ID, or category") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(onClick = { showFilter = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous page",
                enabled = page > 1,
                onClick = onPrev,
            )
            SheetActionButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next page",
                enabled = page < totalPages,
                onClick = onNext,
            )
            SheetActionButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Refresh products",
                onClick = onRefresh,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onCreateProduct,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Product")
            }
        }
        if (loading && products.isEmpty()) {
            LoadingCard("Loading products")
        } else if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Inventory2,
                title = "No products found",
                subtitle = "Try adjusting your search or filters.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered) { product ->
                    ProductCard(product = product, onEdit = { onEditProduct(product) })
                }
            }
        }
        Text(
            text = "Page $page of $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showFilter) {
        ProductFilterDialog(
            category = filterCategory,
            metal = filterMetal,
            stock = filterStock,
            onApply = { cat, met, stk ->
                filterCategory = cat
                filterMetal = met
                filterStock = stk
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFilterDialog(
    category: String,
    metal: String,
    stock: String,
    onApply: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategory by rememberSaveable { mutableStateOf(category) }
    var selectedMetal by rememberSaveable { mutableStateOf(metal) }
    var selectedStock by rememberSaveable { mutableStateOf(stock) }
    var catExpanded by remember { mutableStateOf(false) }
    var metalExpanded by remember { mutableStateOf(false) }
    var stockExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onApply(selectedCategory, selectedMetal, selectedStock) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = {
                selectedCategory = "All"; selectedMetal = "All"; selectedStock = "All"
                onApply("All", "All", "All")
            }) { Text("Clear All") }
        },
        title = { Text("Filter Products") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(
                    label = "Category",
                    value = selectedCategory,
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it },
                    options = listOf("All") + ProductCategories,
                    onSelected = { selectedCategory = it; catExpanded = false },
                )
                DropdownField(
                    label = "Metal",
                    value = selectedMetal,
                    expanded = metalExpanded,
                    onExpandedChange = { metalExpanded = it },
                    options = listOf("All", "gold", "silver"),
                    onSelected = { selectedMetal = it; metalExpanded = false },
                )
                DropdownField(
                    label = "Stock Status",
                    value = selectedStock,
                    expanded = stockExpanded,
                    onExpandedChange = { stockExpanded = it },
                    options = listOf("All") + StockOptions,
                    onSelected = { selectedStock = it; stockExpanded = false },
                )
            }
        },
    )
}

@Composable
private fun ProductCard(
    product: ProductResponse,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(product.productId, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            labelize(product.category),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        StockDot(product.stockStatus)
                        Text(
                            labelize(product.stockStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = stockDotColor(product.stockStatus),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDot(stockStatus: String) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(stockDotColor(stockStatus)),
    )
}

private fun stockDotColor(status: String): Color {
    return when (status) {
        "in_stock" -> StockInColor
        "low_stock" -> StockLowColor
        "out_of_stock" -> StockOutColor
        "made_to_order" -> StockMadeColor
        else -> StockInColor
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrdersScreen(
    padding: PaddingValues,
    orders: List<AdminOrderItem>,
    loading: Boolean,
    onOpenOrder: (AdminOrderItem) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    val filtered = remember(query, orders, statusFilter) {
        orders.filter { order ->
            (statusFilter == "all" || order.status == statusFilter) &&
            (query.isBlank() || listOf(
                order.orderRef,
                order.customerName,
                order.phone,
                order.city,
                order.jewelryType,
                order.budget,
                order.description,
                order.products.joinToString(" ") { "${it.productId} ${it.title}" },
            ).joinToString(" ").contains(query, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search orders") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all", "new", "contacted", "quoted", "closed").forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text(labelize(status)) },
                )
            }
        }
        if (loading && orders.isEmpty()) {
            LoadingCard("Loading orders")
        } else if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "No orders found",
                subtitle = if (statusFilter != "all") "No ${labelize(statusFilter)} orders." else "Orders will appear here.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { order ->
                    OrderRowCard(order = order, onClick = { onOpenOrder(order) })
                }
            }
        }
    }
}

@Composable
private fun OrderRowCard(
    order: AdminOrderItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.orderRef, fontWeight = FontWeight.Bold)
                    Text(order.customerName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(labelize(order.orderKind.removeSuffix("_order")), style = MaterialTheme.typography.bodySmall) },
                    )
                    StatusPill(status = order.status)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val phone = normalizePhone(order.phone)
                        if (phone.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusContacted),
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(order.phone, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(formatDate(order.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "new" -> StatusNew
        "contacted" -> StatusContacted
        "quoted" -> StatusQuoted
        "closed" -> StatusClosed
        else -> StatusNew
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = labelize(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AdminsScreen(
    padding: PaddingValues,
    currentUser: UserResponse?,
    admins: List<AdminListItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onRequestRegisterOtp: (String, String) -> Unit,
    onConfirmRegister: (String, String, String) -> Unit,
    onRequestDeleteOtp: (String) -> Unit,
    onConfirmDeleteAdmin: (String, String) -> Unit,
    onRequestEditOtp: (String) -> Unit,
    onUpdateAdminCredentials: (String, String, String?, String?) -> Unit,
) {
    var newAdminName by rememberSaveable { mutableStateOf("") }
    var newAdminEmail by rememberSaveable { mutableStateOf("") }
    var newAdminOtp by rememberSaveable { mutableStateOf("") }
    var deleteEmail by rememberSaveable { mutableStateOf("") }
    var deleteOtp by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }

    val filtered = remember(query, admins) {
        if (query.isBlank()) admins else admins.filter {
            "${it.name} ${it.email} ${it.role}".contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Manage admins", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Super admin actions only.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh admins")
                }
            }
        }
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Create admin", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = newAdminName,
                        onValueChange = { newAdminName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full name") },
                    )
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        value = newAdminOtp,
                        onValueChange = { newAdminOtp = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRequestRegisterOtp(newAdminName, newAdminEmail) },
                            enabled = newAdminName.isNotBlank() && newAdminEmail.isNotBlank(),
                        ) {
                            Text("Request OTP")
                        }
                        Button(
                            onClick = {
                                onConfirmRegister(newAdminName, newAdminEmail, newAdminOtp)
                                newAdminOtp = ""
                            },
                            enabled = newAdminName.isNotBlank() && newAdminEmail.isNotBlank() && newAdminOtp.isNotBlank(),
                        ) {
                            Text("Create Admin")
                        }
                    }
                }
            }
        }
        item {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Delete admin", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = deleteEmail,
                        onValueChange = { deleteEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Admin email") },
                    )
                    OutlinedTextField(
                        value = deleteOtp,
                        onValueChange = { deleteOtp = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("OTP") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRequestDeleteOtp(deleteEmail) },
                            enabled = deleteEmail.isNotBlank(),
                        ) { Text("Request OTP") }
                        Button(
                            onClick = { onConfirmDeleteAdmin(deleteEmail, deleteOtp) },
                            enabled = deleteEmail.isNotBlank() && deleteOtp.isNotBlank(),
                        ) { Text("Delete Admin") }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search admins") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
        }
        if (loading && admins.isEmpty()) {
            item { LoadingCard("Loading admins") }
        } else if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "No admin accounts found",
                    subtitle = "Create a new admin to get started.",
                )
            }
        } else {
            items(filtered) { admin ->
                ExpandableAdminCard(
                    admin = admin,
                    currentUser = currentUser,
                    onRequestEditOtp = onRequestEditOtp,
                    onUpdateAdminCredentials = onUpdateAdminCredentials,
                    onRequestDeleteOtp = onRequestDeleteOtp,
                    onDeleteAdmin = onConfirmDeleteAdmin,
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(role: String) {
    val isSuperAdmin = role == "super_admin"
    val bgColor = if (isSuperAdmin) RoleSuperAdmin else RoleAdmin
    val textColor = if (isSuperAdmin) Color.White else Color(0xFF1E3A8A)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = if (isSuperAdmin) 1f else 0.3f),
    ) {
        Text(
            text = labelize(role),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ExpandableAdminCard(
    admin: AdminListItem,
    currentUser: UserResponse?,
    onRequestEditOtp: (String) -> Unit,
    onUpdateAdminCredentials: (String, String, String?, String?) -> Unit,
    onRequestDeleteOtp: (String) -> Unit,
    onDeleteAdmin: (String, String) -> Unit,
) {
    var expanded by rememberSaveable(admin.email) { mutableStateOf(false) }
    var newEmail by rememberSaveable(admin.email) { mutableStateOf("") }
    var newPassword by rememberSaveable(admin.email) { mutableStateOf("") }
    var editOtp by rememberSaveable(admin.email) { mutableStateOf("") }
    var deleteOtp by rememberSaveable(admin.email) { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(admin.name, fontWeight = FontWeight.SemiBold)
                    Text(admin.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (admin.email != currentUser?.email) {
                    FilledTonalButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (expanded) "Hide" else "Manage")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (admin.roles.ifEmpty { listOf(admin.role) }).forEach { role ->
                    RoleBadge(role = role)
                }
            }

            if (expanded) {
                HorizontalDivider()
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New email (optional)") },
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New password (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = editOtp,
                    onValueChange = { editOtp = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Edit OTP") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onRequestEditOtp(admin.email) }) {
                        Text("Request Edit OTP")
                    }
                    Button(
                        onClick = { onUpdateAdminCredentials(admin.email, editOtp, newEmail, newPassword) },
                        enabled = editOtp.isNotBlank() && (newEmail.isNotBlank() || newPassword.isNotBlank()),
                    ) {
                        Text("Save")
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = deleteOtp,
                    onValueChange = { deleteOtp = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Delete OTP") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onRequestDeleteOtp(admin.email) }) {
                        Text("Request Delete OTP")
                    }
                    FilledTonalButton(
                        onClick = { onDeleteAdmin(admin.email, deleteOtp) },
                        enabled = deleteOtp.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityLogsScreen(
    padding: PaddingValues,
    activityLogs: List<ActivityLogResponse>,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var dateFilter by rememberSaveable { mutableStateOf("all") }
    val filtered = remember(query, activityLogs, dateFilter) {
        val now = LocalDate.now()
        activityLogs.filter { log ->
            val matchesDate = when (dateFilter) {
                "today" -> runCatching {
                    OffsetDateTime.parse(log.createdAt)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDate() == now
                }.getOrElse { true }
                "week" -> runCatching {
                    val logDate = OffsetDateTime.parse(log.createdAt)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDate()
                    val weekFields = WeekFields.of(Locale.getDefault())
                    logDate.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear()) &&
                        logDate.year == now.year
                }.getOrElse { true }
                "month" -> runCatching {
                    val logDate = OffsetDateTime.parse(log.createdAt)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDate()
                    logDate.month == now.month && logDate.year == now.year
                }.getOrElse { true }
                else -> true
            }
            matchesDate && (query.isBlank() || listOf(
                log.action,
                log.performedByName,
                log.performedByEmail,
                log.target,
                log.detail,
                log.oldValue?.toString(),
                log.newValue?.toString(),
                log.extra?.toString(),
            ).joinToString(" ").contains(query, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Activity logs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh logs")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search logs") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("all" to "All", "today" to "Today", "week" to "This Week", "month" to "This Month").forEach { (key, label) ->
                FilterChip(
                    selected = dateFilter == key,
                    onClick = { dateFilter = key },
                    label = { Text(label) },
                    leadingIcon = if (key != "all") {{ Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }} else null,
                )
            }
        }
        if (loading && activityLogs.isEmpty()) {
            LoadingCard("Loading activity logs")
        } else if (filtered.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Visibility,
                title = "No activity logs found",
                subtitle = "Logs will appear as actions are performed.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { log ->
                    ActivityLogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun ActivityLogCard(log: ActivityLogResponse) {
    var expanded by rememberSaveable(log.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionLabel(action = log.action)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "View")
                }
            }
            Text(log.target ?: "-", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (log.performedByName != null || log.performedByEmail != null) {
                Text(
                    "by ${log.performedByName ?: log.performedByEmail ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatDate(log.createdAt), style = MaterialTheme.typography.bodySmall)
            Text(log.detail ?: "-", maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            if (expanded) {
                HumanReadableBlock("Old Value", log.oldValue?.toString())
                HumanReadableBlock("New Value", log.newValue?.toString())
                HumanReadableBlock("Extra", log.extra?.toString())
            }
        }
    }
}

@Composable
private fun ActionLabel(action: String) {
    val normalized = action.lowercase().replace(" ", "_")
    val color = when {
        normalized.contains("order_created") || normalized.contains("create") -> LogOrderCreated
        normalized.contains("status_updated") || normalized.contains("update") -> LogStatusUpdated
        normalized.contains("login") -> LogLogin
        normalized.contains("delete") -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = labelize(action),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun HumanReadableBlock(title: String, text: String?) {
    if (text.isNullOrBlank() || text == "null") return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        val formatted = formatJsonHumanReadable(text)
        Text(
            formatted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp),
        )
    }
}

/** Convert raw JSON-like strings to human-readable key: value format. */
private fun formatJsonHumanReadable(raw: String): String {
    return try {
        // Simple parsing: remove braces, split by commas, format key-value pairs
        val cleaned = raw.trim().removePrefix("{").removeSuffix("}")
        if (cleaned.isBlank()) return raw
        cleaned.split(",").joinToString("\n") { pair ->
            val parts = pair.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().removeSurrounding("\"")
                "${labelize(key)}: $value"
            } else {
                pair.trim()
            }
        }
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun MoreScreen(
    padding: PaddingValues,
    currentUser: UserResponse?,
    isSuperAdmin: Boolean,
    onOpenProfileDialog: (ProfileDialogMode) -> Unit,
    onOpenBugDialog: () -> Unit,
    onNavigateTo: (RootDestination) -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Profile header with avatar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Avatar circle with initials
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NavBarBlue, Color(0xFF3B82F6)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = currentUser?.name
                                ?.split(" ")
                                ?.take(2)
                                ?.mapNotNull { it.firstOrNull()?.uppercase() }
                                ?.joinToString("")
                                ?: "?",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        currentUser?.name ?: "Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (currentUser != null) {
                        RoleBadge(role = currentUser.role)
                    }
                }
            }
        }
        // Navigation cards for super admin
        if (isSuperAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTo(RootDestination.Admins) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manage Admins", fontWeight = FontWeight.SemiBold)
                            Text("Create, edit, or delete admin accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTo(RootDestination.Activity) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activity Logs", fontWeight = FontWeight.SemiBold)
                            Text("View all portal activity and changes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Profile actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile actions", fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Name) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change display name")
                    }
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Email) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change email")
                    }
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Password) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change password")
                    }
                }
            }
        }
        // Support
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Support", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Report issues from the mobile app directly to the admin portal support flow.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onOpenBugDialog, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report bug")
                    }
                }
            }
        }
        // Logout
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorSheet(
    initial: ProductEditorState,
    onDismiss: () -> Unit,
    onSave: (ProductPayload, List<Uri>) -> Unit,
    onDelete: () -> Unit,
) {
    var title by rememberSaveable(initial.productId) { mutableStateOf(initial.title) }
    var category by rememberSaveable(initial.productId) { mutableStateOf(initial.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var metal by rememberSaveable(initial.productId) { mutableStateOf(initial.metal) }
    var metalExpanded by remember { mutableStateOf(false) }
    var purity by rememberSaveable(initial.productId) { mutableStateOf(initial.purity) }
    var weight by rememberSaveable(initial.productId) { mutableStateOf(initial.weight) }
    var stock by rememberSaveable(initial.productId) { mutableStateOf(initial.stockStatus) }
    var stockExpanded by remember { mutableStateOf(false) }
    var tags by rememberSaveable(initial.productId) { mutableStateOf(initial.tags) }
    var description by rememberSaveable(initial.productId) { mutableStateOf(initial.description) }
    val selectedImages = remember(initial.productId) { mutableStateListOf<Uri>().apply { addAll(initial.newImages) } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris)
        }
    }
    val canSave = title.isNotBlank() &&
        category.isNotBlank() &&
        purity.isNotBlank() &&
        description.length >= 10 &&
        weight.toDoubleOrNull()?.let { it > 0 } == true &&
        (selectedImages.isNotEmpty() || initial.existingImages.isNotEmpty())
    var confirmDelete by rememberSaveable(initial.productId) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (initial.productId == null) "Create product" else "Edit product",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") })
            DropdownField(
                label = "Category",
                value = category,
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                options = ProductCategories,
                onSelected = { category = it; categoryExpanded = false },
            )
            DropdownField(
                label = "Metal",
                value = metal,
                expanded = metalExpanded,
                onExpandedChange = { metalExpanded = it },
                options = listOf("gold", "silver"),
                onSelected = { metal = it; metalExpanded = false },
            )
            OutlinedTextField(value = purity, onValueChange = { purity = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Purity") })
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weight (grams)") },
                supportingText = { Text("Use at least 3 decimal places, for example 3.441") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            DropdownField(
                label = "Stock status",
                value = stock,
                expanded = stockExpanded,
                onExpandedChange = { stockExpanded = it },
                options = StockOptions,
                onSelected = { stock = it; stockExpanded = false },
            )
            OutlinedTextField(value = tags, onValueChange = { tags = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tags (comma separated)") })
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Description") },
            )
            Text("Images", fontWeight = FontWeight.SemiBold)
            if (initial.existingImages.isNotEmpty()) {
                HorizontalImageRow(urls = initial.existingImages)
            }
            if (selectedImages.isNotEmpty()) {
                HorizontalImageRow(urls = selectedImages.map(Uri::toString))
            }
            OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (selectedImages.isEmpty()) "Choose images" else "Replace images")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (initial.productId != null) {
                    FilledTonalButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsedWeight = weight.toDoubleOrNull() ?: 0.0
                            onSave(
                                ProductPayload(
                                    title = title.trim(),
                                    category = category.trim(),
                                    metal = metal,
                                    description = description.trim(),
                                    purity = purity.trim(),
                                    weight = parsedWeight,
                                    images = initial.existingImages,
                                    stockStatus = stock,
                                    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                ),
                                selectedImages.toList(),
                            )
                        },
                        enabled = canSave,
                    ) {
                        Text(if (initial.productId == null) "Create" else "Save")
                    }
                }
            }
        }
    }

    if (confirmDelete && initial.productId != null) {
        ConfirmDeleteDialog(
            label = "product ${initial.productId}",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelize(option)) },
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun HorizontalImageRow(urls: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        urls.take(4).forEach { image ->
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailSheet(
    order: AdminOrderItem,
    currentUser: UserResponse?,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onOpenProduct: (OrderProductSnapshot) -> Unit,
    onDelete: () -> Unit,
) {
    var comment by rememberSaveable(order.orderRef) { mutableStateOf("") }
    var draftStatus by rememberSaveable(order.orderRef) { mutableStateOf(order.status) }
    var confirmDelete by rememberSaveable(order.orderRef) { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(order.status) {
        draftStatus = order.status
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(order.orderRef, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            ChipRow(values = listOf(labelize(order.orderKind.replace("_", " ")), labelize(draftStatus)))
            Text("${order.customerName} • ${order.phone}")
            Text(formatDate(order.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    val phone = normalizePhone(order.phone)
                    if (phone.isNotBlank()) {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        context.startActivity(dialIntent)
                    }
                }) {
                    Icon(Icons.Default.Phone, contentDescription = "Call ${order.phone}")
                }
            }
            OrderStatusSelector(current = draftStatus, onSelect = { draftStatus = it })

            if (order.orderKind == "custom_order") {
                DetailLine("Jewelry type", order.jewelryType.orEmpty())
                DetailLine("City", order.city.orEmpty())
                DetailLine("Budget", order.budget.orEmpty())
                DetailLine("Purity", order.purity.orEmpty())
                DetailLine("Description", order.description.orEmpty())
                if (order.imageUrls.isNotEmpty()) {
                    HorizontalImageRow(order.imageUrls)
                }
            } else {
                Text("Products", fontWeight = FontWeight.SemiBold)
                order.products.forEach {
                    ProductSnapshotRow(snapshot = it, onClick = { onOpenProduct(it) })
                }
                if (!order.notes.isNullOrBlank()) {
                    DetailLine("Notes", order.notes)
                }
            }

            HorizontalDivider()
            Text("Comments", fontWeight = FontWeight.SemiBold)
            if (order.comments.isEmpty()) {
                Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                order.comments.forEach { commentItem ->
                    CommentCard(commentItem)
                }
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add comment") },
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onAddComment(comment)
                    comment = ""
                }, enabled = comment.isNotBlank()) {
                    Text("Add Comment")
                }
                FilledTonalButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
            Button(
                onClick = { onUpdateStatus(draftStatus) },
                modifier = Modifier.fillMaxWidth(),
                enabled = draftStatus != order.status,
            ) {
                Text("Save Status")
            }
            currentUser?.let {
                Text(
                    "Signed in as ${it.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            label = "order ${order.orderRef}",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderStatusSelector(
    current: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Update status", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderStatusOptions.forEach { status ->
                FilterChip(
                    selected = current == status,
                    onClick = { onSelect(status) },
                    label = { Text(labelize(status)) },
                )
            }
        }
    }
}

@Composable
private fun ProductSnapshotRow(
    snapshot: OrderProductSnapshot,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = snapshot.image,
            contentDescription = snapshot.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Column {
            Text(snapshot.title, fontWeight = FontWeight.SemiBold)
            Text("${snapshot.productId} • Qty ${snapshot.quantity}")
        }
    }
}

@Composable
private fun CommentCard(comment: OrderComment) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(comment.addedByName ?: comment.addedBy, fontWeight = FontWeight.SemiBold)
            Text(comment.comment)
            Text(formatDate(comment.createdAt), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "-" })
    }
}

@Composable
private fun ProfileDialog(
    mode: ProfileDialogMode,
    currentUser: UserResponse?,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var fieldA by rememberSaveable(mode.name) {
        mutableStateOf(
            when (mode) {
                ProfileDialogMode.Name -> currentUser?.name.orEmpty()
                ProfileDialogMode.Email -> currentUser?.email.orEmpty()
                ProfileDialogMode.Password -> ""
            },
        )
    }
    var fieldB by rememberSaveable(mode.name) { mutableStateOf("") }
    var fieldC by rememberSaveable(mode.name) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        ProfileDialogMode.Name -> onSubmit(fieldA, "")
                        ProfileDialogMode.Email -> onSubmit(fieldA, fieldB)
                        ProfileDialogMode.Password -> onSubmit(fieldA, fieldB)
                    }
                },
                enabled = when (mode) {
                    ProfileDialogMode.Name -> fieldA.trim().length >= 2
                    ProfileDialogMode.Email -> fieldA.isNotBlank() && fieldB.isNotBlank()
                    ProfileDialogMode.Password -> fieldA.isNotBlank() && fieldB.isNotBlank() && fieldB == fieldC
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text(
                when (mode) {
                    ProfileDialogMode.Name -> "Change display name"
                    ProfileDialogMode.Email -> "Change email"
                    ProfileDialogMode.Password -> "Change password"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (mode) {
                    ProfileDialogMode.Name -> {
                        OutlinedTextField(
                            value = fieldA,
                            onValueChange = { fieldA = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New name") },
                        )
                    }
                    ProfileDialogMode.Email -> {
                        OutlinedTextField(
                            value = fieldA,
                            onValueChange = { fieldA = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        OutlinedTextField(
                            value = fieldB,
                            onValueChange = { fieldB = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Current password") },
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                    ProfileDialogMode.Password -> {
                        OutlinedTextField(
                            value = fieldA,
                            onValueChange = { fieldA = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Current password") },
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedTextField(
                            value = fieldB,
                            onValueChange = { fieldB = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New password") },
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        OutlinedTextField(
                            value = fieldC,
                            onValueChange = { fieldC = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm new password") },
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BugReportSheet(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Uri?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf(SeverityOptions[1]) }
    var severityExpanded by remember { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Report bug", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bug title") },
            )
            DropdownField(
                label = "Severity",
                value = severity,
                expanded = severityExpanded,
                onExpandedChange = { severityExpanded = it },
                options = SeverityOptions,
                onSelected = { severity = it; severityExpanded = false },
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Description") },
            )
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (imageUri == null) "Attach screenshot" else "Replace screenshot")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSubmit(title, severity, description, imageUri) },
                    enabled = title.length >= 3 && description.length >= 10,
                ) { Text("Submit") }
            }
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text)
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(values: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.filter { it.isNotBlank() }.forEach { value ->
            AssistChip(
                onClick = {},
                label = { Text(labelize(value)) },
            )
        }
    }
}

@Composable
private fun AccountLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SheetActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    label: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Confirm delete") },
        text = { Text("Delete $label? This cannot be undone.") },
    )
}

@Composable
private fun ProductPreviewDialog(
    snapshot: OrderProductSnapshot,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(snapshot.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = snapshot.image,
                    contentDescription = snapshot.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                )
                DetailLine("Product ID", snapshot.productId)
                DetailLine("Quantity", snapshot.quantity.toString())
                snapshot.price?.let { DetailLine("Price", it.toString()) }
            }
        },
    )
}

private fun destinationIcon(destination: RootDestination) = when (destination) {
    RootDestination.Products -> Icons.Default.Inventory2
    RootDestination.Orders -> Icons.AutoMirrored.Filled.ListAlt
    RootDestination.Admins -> Icons.Default.AdminPanelSettings
    RootDestination.Activity -> Icons.Default.Visibility
    RootDestination.Account -> Icons.Default.Person
}

private fun labelize(value: String): String {
    return value
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

private fun normalizePhone(value: String): String {
    return value.filter { it.isDigit() || it == '+' }
}

private fun formatDate(value: String): String {
    return runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
    }.getOrElse { value }
}

private enum class ProfileDialogMode {
    Name,
    Password,
    Email,
}

private data class ProductEditorState(
    val productId: String? = null,
    val title: String = "",
    val category: String = ProductCategories.first(),
    val metal: String = "gold",
    val purity: String = "",
    val weight: String = "",
    val stockStatus: String = StockOptions.first(),
    val tags: String = "",
    val description: String = "",
    val existingImages: List<String> = emptyList(),
    val newImages: List<Uri> = emptyList(),
) {
    companion object {
        fun fromProduct(product: ProductResponse): ProductEditorState {
            return ProductEditorState(
                productId = product.productId,
                title = product.title,
                category = product.category,
                metal = product.metal,
                purity = product.purity,
                weight = String.format(Locale.US, "%.3f", product.weight),
                stockStatus = product.stockStatus,
                tags = product.tags.joinToString(", "),
                description = product.description,
                existingImages = product.images,
            )
        }
    }
}
