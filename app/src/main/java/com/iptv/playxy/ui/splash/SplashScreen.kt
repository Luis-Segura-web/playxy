package com.iptv.playxy.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToLoading: () -> Unit
) {
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is SplashNavigation.ToLogin -> {
                viewModel.onNavigationHandled()
                onNavigateToLogin()
            }
            is SplashNavigation.ToLoading -> {
                viewModel.onNavigationHandled()
                onNavigateToLoading()
            }
            null -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Name
            Text(
                text = "PLAYXY",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "IPTV Player",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
