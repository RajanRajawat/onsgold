package com.onsgold.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onsgold.admin.data.AdminRepository
import com.onsgold.admin.data.SessionStore
import com.onsgold.admin.ui.AdminApp
import com.onsgold.admin.ui.AdminViewModel
import com.onsgold.admin.ui.AdminViewModelFactory
import com.onsgold.admin.ui.theme.OnsGoldAdminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(applicationContext)
        val repository = AdminRepository(applicationContext, sessionStore)

        setContent {
            OnsGoldAdminTheme {
                val viewModel: AdminViewModel = viewModel(
                    factory = AdminViewModelFactory(repository),
                )
                AdminApp(viewModel = viewModel)
            }
        }
    }
}
