package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AddMomentScreen
import com.example.ui.HomeScreen
import com.example.ui.OnboardingScreen
import com.example.ui.ProjectDetailScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: JourneyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check onboarding flag
        val sharedPrefs = getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        val startDestination = if (onboardingCompleted) "home" else "onboarding"

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinished = {
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onProjectClick = { projectId ->
                                    navController.navigate("project_detail/$projectId")
                                },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        
                        composable(
                            route = "project_detail/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                            ProjectDetailScreen(
                                viewModel = viewModel,
                                projectId = projectId,
                                onBack = { navController.popBackStack() },
                                onNavigateToAddMoment = { navController.navigate("add_moment") },
                                snackbarHostState = snackbarHostState
                            )
                        }
                        
                        composable("add_moment") {
                            AddMomentScreen(
                                viewModel = viewModel,
                                onSaveSuccess = { navController.popBackStack() },
                                onRetake = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
