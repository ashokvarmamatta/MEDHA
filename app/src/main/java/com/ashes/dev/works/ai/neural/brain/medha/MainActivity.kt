package com.ashes.dev.works.ai.neural.brain.medha

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.chat.ChatScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.menu.MenuScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.model.ModelListScreen
import com.ashes.dev.works.ai.neural.brain.medha.presentation.screens.online.OnlineModelSelectionScreen
import com.ashes.dev.works.ai.neural.brain.medha.ui.theme.MEDHATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkStoragePermission()
        
        setContent {
            MEDHATheme {
                AppNavigation()
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Please grant All Files Access to load the model from Downloads", Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                navController.navigate("chat/${Uri.encode(uri.toString())}")
            }
        }
    }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MenuScreen(
                onOfflineModeClick = { navController.navigate("model_list") },
                onOnlineModeClick = { navController.navigate("online_model_selection") }
            )
        }
        composable("model_list") {
            ModelListScreen(
                onModelClick = { navController.navigate("chat/$it") },
                onImportClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    importLauncher.launch(intent)
                }
            )
        }
        composable("online_model_selection") {
            OnlineModelSelectionScreen(
                onNavigateToChat = { apiKey, modelName ->
                    navController.navigate("chat/$modelName?apiKey=$apiKey&isOnline=true")
                }
            )
        }
        composable(
            route = "chat/{modelNameOrUri}?apiKey={apiKey}&isOnline={isOnline}",
            arguments = listOf(
                navArgument("modelNameOrUri") { type = NavType.StringType },
                navArgument("apiKey") { 
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("isOnline") { 
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            val modelNameOrUri = it.arguments?.getString("modelNameOrUri")
            val isOnline = it.arguments?.getBoolean("isOnline")
            val apiKey = it.arguments?.getString("apiKey")
            ChatScreen(modelNameOrUri = modelNameOrUri, isOnline = isOnline, apiKey = apiKey)
        }
    }
}