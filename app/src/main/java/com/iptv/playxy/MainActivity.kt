package com.iptv.playxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptv.playxy.ui.Routes
import com.iptv.playxy.ui.loading.LoadingScreen
import com.iptv.playxy.ui.login.LoginScreen
import com.iptv.playxy.ui.main.MainScreen
import com.iptv.playxy.ui.splash.SplashScreen
import com.iptv.playxy.ui.theme.PlayxyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayxyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayxyApp()
                }
            }
        }
    }
}

@Composable
fun PlayxyApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLoading = {
                    navController.navigate(Routes.LOADING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToLoading = {
                    navController.navigate(Routes.LOADING) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.LOADING) {
            LoadingScreen(
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOADING) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onNavigateToLoading = {
                    navController.navigate(Routes.LOADING) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                    }
                }
            )
        }
    }
}
