package com.onsgold.admin.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.onsgold.admin.data.ActivityLogResponse
import com.onsgold.admin.data.AdminListItem
import com.onsgold.admin.data.AdminOrderItem
import com.onsgold.admin.data.AnalyticsResponse
import com.onsgold.admin.data.OrderComment
import com.onsgold.admin.data.OrderProductSnapshot
import com.onsgold.admin.data.ProductPayload
import com.onsgold.admin.data.ProductResponse
import com.onsgold.admin.data.UserResponse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    Overview("Overview"),
    Products("Products"),
    Orders("Orders"),
    Admins("Admins"),
    Activity("Activity"),
    Account("Account"),
}

@Composable
fun AdminApp(viewModel: AdminViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { snackbarHostState.showSnackbar(it) }
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
                snackbarHostState = snackbarHostState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun BootScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading ONS Gold Admin")
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
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "ONS Gold Admin",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (forgotMode) {
                        "Reset your admin password from mobile."
                    } else {
                        "Manage products, orders, admins, and account operations from the app."
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
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
    snackbarHostState: SnackbarHostState,
    viewModel: AdminViewModel,
) {
    var destination by rememberSaveable { mutableStateOf(RootDestination.Overview) }
    var productDialog by remember { mutableStateOf<ProductEditorState?>(null) }
    var selectedOrder by remember { mutableStateOf<AdminOrderItem?>(null) }
    var profileDialog by remember { mutableStateOf<ProfileDialogMode?>(null) }
    var bugDialogOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val navItems = buildList {
        add(RootDestination.Overview)
        add(RootDestination.Products)
        add(RootDestination.Orders)
        if (state.currentUser?.role == "super_admin") {
            add(RootDestination.Admins)
            add(RootDestination.Activity)
        }
        add(RootDestination.Account)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp
        val navigationContent: @Composable () -> Unit = {
            if (useRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    navItems.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Icon(destinationIcon(item), contentDescription = item.title) },
                            label = { Text(item.title) },
                        )
                    }
                }
            }
        }

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                navigationContent()
                MainScaffold(
                    modifier = Modifier.weight(1f),
                    destination = destination,
                    state = state,
                    snackbarHostState = snackbarHostState,
                    onRefresh = viewModel::refreshAll,
                    onLogout = viewModel::logout,
                    onCreateProduct = { productDialog = ProductEditorState() },
                    content = { scaffoldPadding ->
                        AdminContent(
                            scaffoldPadding = scaffoldPadding,
                            destination = destination,
                            state = state,
                            onSearchProducts = { viewModel.loadProducts(page = 1, search = it) },
                            onChangeProductPage = { viewModel.loadProducts(page = it, search = state.productSearch) },
                            onEditProduct = { productDialog = ProductEditorState.fromProduct(it) },
                            onOpenOrder = { selectedOrder = it },
                            onRequestAdminsRefresh = viewModel::loadAdmins,
                            onRequestActivityRefresh = viewModel::loadActivityLogs,
                            onOpenProfileDialog = { profileDialog = it },
                            onOpenBugDialog = { bugDialogOpen = true },
                            onExportOrders = {
                                viewModel.exportOrdersCsv { csv ->
                                    if (csv == null) return@exportOrdersCsv
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "ONS Gold Orders Export")
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share CSV"))
                                }
                            },
                            viewModel = viewModel,
                        )
                    },
                )
            }
        } else {
            MainScaffold(
                modifier = Modifier.fillMaxSize(),
                destination = destination,
                state = state,
                snackbarHostState = snackbarHostState,
                onRefresh = viewModel::refreshAll,
                onLogout = viewModel::logout,
                onCreateProduct = { productDialog = ProductEditorState() },
                bottomBar = {
                    NavigationBar {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(destinationIcon(item), contentDescription = item.title) },
                                label = { Text(item.title) },
                            )
                        }
                    }
                },
                content = { scaffoldPadding ->
                    AdminContent(
                        scaffoldPadding = scaffoldPadding,
                        destination = destination,
                        state = state,
                        onSearchProducts = { viewModel.loadProducts(page = 1, search = it) },
                        onChangeProductPage = { viewModel.loadProducts(page = it, search = state.productSearch) },
                        onEditProduct = { productDialog = ProductEditorState.fromProduct(it) },
                        onOpenOrder = { selectedOrder = it },
                        onRequestAdminsRefresh = viewModel::loadAdmins,
                        onRequestActivityRefresh = viewModel::loadActivityLogs,
                        onOpenProfileDialog = { profileDialog = it },
                        onOpenBugDialog = { bugDialogOpen = true },
                        onExportOrders = {
                            viewModel.exportOrdersCsv { csv ->
                                if (csv == null) return@exportOrdersCsv
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "ONS Gold Orders Export")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share CSV"))
                            }
                        },
                        viewModel = viewModel,
                    )
                },
            )
        }
    }

    productDialog?.let { editor ->
        ProductEditorDialog(
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
            onDismiss = { selectedOrder = null },
            onUpdateStatus = { status -> viewModel.updateOrderStatus(order.orderRef, order.orderKind, status) },
            onAddComment = { comment -> viewModel.addOrderComment(order.orderRef, order.orderKind, comment) },
            onDelete = {
                viewModel.deleteOrder(order.orderRef, order.orderKind)
                selectedOrder = null
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
        BugReportDialog(
            onDismiss = { bugDialogOpen = false },
            onSubmit = { title, severity, description, imageUri ->
                viewModel.reportBug(title, severity, description, imageUri)
                bugDialogOpen = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    modifier: Modifier,
    destination: RootDestination,
    state: AdminUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onCreateProduct: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(destination.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = state.currentUser?.email.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (destination == RootDestination.Products) {
                        IconButton(onClick = onCreateProduct) {
                            Icon(Icons.Default.Add, contentDescription = "Add product")
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (destination == RootDestination.Products) {
                FloatingActionButton(onClick = onCreateProduct) {
                    Icon(Icons.Default.Add, contentDescription = "Create product")
                }
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        content(padding)
    }
}

@Composable
private fun AdminContent(
    scaffoldPadding: PaddingValues,
    destination: RootDestination,
    state: AdminUiState,
    onSearchProducts: (String) -> Unit,
    onChangeProductPage: (Int) -> Unit,
    onEditProduct: (ProductResponse) -> Unit,
    onOpenOrder: (AdminOrderItem) -> Unit,
    onRequestAdminsRefresh: () -> Unit,
    onRequestActivityRefresh: () -> Unit,
    onOpenProfileDialog: (ProfileDialogMode) -> Unit,
    onOpenBugDialog: () -> Unit,
    onExportOrders: () -> Unit,
    viewModel: AdminViewModel,
) {
    when (destination) {
        RootDestination.Overview -> OverviewScreen(
            padding = screenPadding(scaffoldPadding),
            summary = state.summary,
            orders = state.orders.take(5),
            loading = state.dashboardLoading,
            onOpenOrder = onOpenOrder,
        )
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
            onEditProduct = onEditProduct,
        )
        RootDestination.Orders -> OrdersScreen(
            padding = screenPadding(scaffoldPadding),
            orders = state.orders,
            loading = state.dashboardLoading,
            onOpenOrder = onOpenOrder,
            onExportOrders = onExportOrders,
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
        RootDestination.Account -> AccountScreen(
            padding = screenPadding(scaffoldPadding),
            currentUser = state.currentUser,
            onOpenProfileDialog = onOpenProfileDialog,
            onOpenBugDialog = onOpenBugDialog,
        )
    }
}

private fun screenPadding(scaffoldPadding: PaddingValues): PaddingValues {
    return PaddingValues(
        start = 16.dp,
        top = scaffoldPadding.calculateTopPadding() + 16.dp,
        end = 16.dp,
        bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
    )
}

@Composable
private fun OverviewScreen(
    padding: PaddingValues,
    summary: AnalyticsResponse?,
    orders: List<AdminOrderItem>,
    loading: Boolean,
    onOpenOrder: (AdminOrderItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Dashboard overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Track products, inquiries, and current order activity.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            if (loading && summary == null) {
                LoadingCard("Loading dashboard")
            } else if (summary != null) {
                StatsGrid(summary)
            }
        }
        item {
            Text("Recent orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (orders.isEmpty()) {
            item { EmptyCard("No orders yet.") }
        } else {
            items(orders) { order ->
                OrderRowCard(order = order, onClick = { onOpenOrder(order) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsGrid(summary: AnalyticsResponse) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard("Total Products", summary.totalProducts.toString(), Color(0xFF1D4ED8))
        SummaryCard("Catalog Orders", summary.totalOrders.toString(), Color(0xFFD97706))
        SummaryCard("Custom Orders", summary.totalCustomRequests.toString(), Color(0xFF15803D))
        SummaryCard("New Orders", summary.newOrders.toString(), Color(0xFFB91C1C))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, accent: Color) {
    Card(
        modifier = Modifier.widthIn(min = 150.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.08f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = accent, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
    onEditProduct: (ProductResponse) -> Unit,
) {
    var query by rememberSaveable(currentSearch) { mutableStateOf(currentSearch) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Products", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search by name, ID, or category") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        if (loading && products.isEmpty()) {
            LoadingCard("Loading products")
        } else if (products.isEmpty()) {
            EmptyCard("No products found.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(products) { product ->
                    ProductCard(product = product, onEdit = { onEditProduct(product) })
                }
            }
        }
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Page $page of ${maxOf(1, (total + pageSize - 1) / pageSize)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPrev, enabled = page > 1) { Text("Previous") }
                    OutlinedButton(
                        onClick = onNext,
                        enabled = page < maxOf(1, (total + pageSize - 1) / pageSize),
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
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
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit product")
                }
            }
            ChipRow(
                values = listOf(
                    product.category,
                    labelize(product.metal),
                    product.purity,
                    "${product.weight} g",
                    labelize(product.stockStatus),
                ),
            )
            if (product.tags.isNotEmpty()) {
                ChipRow(values = product.tags.take(4))
            }
            Text(
                product.description,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OrdersScreen(
    padding: PaddingValues,
    orders: List<AdminOrderItem>,
    loading: Boolean,
    onOpenOrder: (AdminOrderItem) -> Unit,
    onExportOrders: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, orders) {
        if (query.isBlank()) orders else orders.filter { order ->
            listOf(
                order.orderRef,
                order.customerName,
                order.phone,
                order.city,
                order.jewelryType,
                order.budget,
                order.description,
                order.products.joinToString(" ") { "${it.productId} ${it.title}" },
            ).joinToString(" ").contains(query, ignoreCase = true)
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
            Text("Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onExportOrders) { Text("Export CSV") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search orders") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        if (loading && orders.isEmpty()) {
            LoadingCard("Loading orders")
        } else if (filtered.isEmpty()) {
            EmptyCard("No orders found.")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                ChipRow(values = listOf(labelize(order.orderKind.removeSuffix("_order")), labelize(order.status)))
            }
            Text(order.phone, style = MaterialTheme.typography.bodyMedium)
            Text(formatDate(order.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create admin", fontWeight = FontWeight.SemiBold)
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
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Delete admin", fontWeight = FontWeight.SemiBold)
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
            item { EmptyCard("No admin accounts found.") }
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

    Card(modifier = Modifier.fillMaxWidth()) {
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
                TextButton(onClick = { expanded = !expanded }, enabled = admin.email != currentUser?.email) {
                    Text(if (expanded) "Hide" else "Manage")
                }
            }
            ChipRow(values = (admin.roles.ifEmpty { listOf(admin.role) }).map(::labelize))

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

@Composable
private fun ActivityLogsScreen(
    padding: PaddingValues,
    activityLogs: List<ActivityLogResponse>,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, activityLogs) {
        if (query.isBlank()) activityLogs else activityLogs.filter {
            listOf(
                it.action,
                it.performedByName,
                it.performedByEmail,
                it.target,
                it.detail,
                it.oldValue?.toString(),
                it.newValue?.toString(),
                it.extra?.toString(),
            ).joinToString(" ").contains(query, ignoreCase = true)
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
        if (loading && activityLogs.isEmpty()) {
            LoadingCard("Loading activity logs")
        } else if (filtered.isEmpty()) {
            EmptyCard("No activity logs found.")
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(labelize(log.action), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "View")
                }
            }
            Text(log.target ?: "-", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDate(log.createdAt), style = MaterialTheme.typography.bodySmall)
            Text(log.detail ?: "-", maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            if (expanded) {
                JsonBlock("Old Value", log.oldValue?.toString())
                JsonBlock("New Value", log.newValue?.toString())
                JsonBlock("Extra", log.extra?.toString())
            }
        }
    }
}

@Composable
private fun JsonBlock(title: String, text: String?) {
    if (text.isNullOrBlank() || text == "null") return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp),
        )
    }
}

@Composable
private fun AccountScreen(
    padding: PaddingValues,
    currentUser: UserResponse?,
    onOpenProfileDialog: (ProfileDialogMode) -> Unit,
    onOpenBugDialog: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("My account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    AccountLine(Icons.Default.Person, "Name", currentUser?.name ?: "-")
                    AccountLine(Icons.Default.Email, "Email", currentUser?.email ?: "-")
                    AccountLine(Icons.Default.AdminPanelSettings, "Role", labelize(currentUser?.role ?: "-"))
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile actions", fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Name) }) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change display name")
                    }
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Email) }) {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change email")
                    }
                    FilledTonalButton(onClick = { onOpenProfileDialog(ProfileDialogMode.Password) }) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change password")
                    }
                }
            }
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Support", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Report issues from the mobile app directly to the admin portal support flow.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenBugDialog) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Report bug")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
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
    var featured by rememberSaveable(initial.productId) { mutableStateOf(initial.featured) }
    val selectedImages = remember(initial.productId) { mutableStateListOf<Uri>().apply { addAll(initial.newImages) } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
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
                            featured = featured,
                        ),
                        selectedImages.toList(),
                    )
                },
                enabled = title.isNotBlank() &&
                    category.isNotBlank() &&
                    purity.isNotBlank() &&
                    description.length >= 10 &&
                    weight.toDoubleOrNull()?.let { it > 0 } == true &&
                    (selectedImages.isNotEmpty() || initial.existingImages.isNotEmpty()),
            ) {
                Text(if (initial.productId == null) "Create" else "Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initial.productId != null) {
                    FilledTonalButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        title = { Text(if (initial.productId == null) "Create product" else "Edit product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured", modifier = Modifier.weight(1f))
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
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
            }
        },
    )
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
    onDelete: () -> Unit,
) {
    var comment by rememberSaveable(order.orderRef) { mutableStateOf("") }
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
            ChipRow(values = listOf(labelize(order.orderKind.replace("_", " ")), labelize(order.status)))
            Text("${order.customerName} • ${order.phone}")
            Text(formatDate(order.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OrderStatusSelector(current = order.status, onSelect = onUpdateStatus)

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
                    ProductSnapshotRow(it)
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
                FilledTonalButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
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
private fun ProductSnapshotRow(snapshot: OrderProductSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun BugReportDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSubmit(title, severity, description, imageUri) },
                enabled = title.length >= 3 && description.length >= 10,
            ) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Report bug") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            }
        },
    )
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
private fun EmptyCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun destinationIcon(destination: RootDestination) = when (destination) {
    RootDestination.Overview -> Icons.Default.Analytics
    RootDestination.Products -> Icons.Default.Inventory2
    RootDestination.Orders -> Icons.Default.ListAlt
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
    val featured: Boolean = false,
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
                weight = product.weight.toString(),
                stockStatus = product.stockStatus,
                tags = product.tags.joinToString(", "),
                description = product.description,
                featured = product.featured,
                existingImages = product.images,
            )
        }
    }
}
