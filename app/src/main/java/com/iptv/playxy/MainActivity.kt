package com.iptv.playxy

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.Routes
import com.iptv.playxy.ui.loading.LoadingScreen
import com.iptv.playxy.ui.login.LoginScreen
import com.iptv.playxy.ui.main.MainScreen
import com.iptv.playxy.ui.movies.ActorDetailScreen
import com.iptv.playxy.ui.movies.MovieDetailScreen
import com.iptv.playxy.ui.series.SeriesDetailScreen
import com.iptv.playxy.ui.splash.SplashScreen
import com.iptv.playxy.ui.theme.PlayxyTheme
import com.iptv.playxy.ui.player.LocalPlayerContainerHost
import com.iptv.playxy.ui.player.PlayerManager
import com.iptv.playxy.ui.player.rememberPlayerContainerHost
import com.iptv.playxy.ui.pip.PipController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val fullscreenState = mutableStateOf(false)

    @Inject
    lateinit var repository: IptvRepository

    @Inject
    lateinit var playerManager: PlayerManager

    private lateinit var pipController: PipController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pipController = PipController(this, playerManager, fullscreenState)
        setContent {
            PlayxyTheme {
                val playerContainerHost = rememberPlayerContainerHost(playerManager)
                CompositionLocalProvider(
                    LocalFullscreenState provides fullscreenState,
                    LocalPlayerManager provides playerManager,
                    LocalPipController provides pipController,
                    LocalPlayerContainerHost provides playerContainerHost
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PlayxyNavigation(repository)
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipController.handlePictureInPictureModeChanged(isInPictureInPictureMode, isFinishing)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        pipController.handlePictureInPictureModeChanged(isInPictureInPictureMode, isFinishing)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        maybeEnterBackgroundPip()
    }

    private fun maybeEnterBackgroundPip() {
        if (!isInPictureInPictureMode && playerManager.hasActivePlayback()) {
            pipController.requestPip(onClose = { playerManager.stopPlayback() })
        }
    }
}

@Composable
fun PlayxyNavigation(repository: IptvRepository) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

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
                },
                onNavigateToMovieDetail = { streamId, categoryId ->
                    navController.navigate(Routes.movieDetail(streamId, categoryId))
                },
                onNavigateToSeriesDetail = { seriesId, categoryId ->
                    navController.navigate(Routes.seriesDetail(seriesId, categoryId))
                }
            )
        }

        composable(
            route = Routes.MOVIE_DETAIL,
            arguments = listOf(
                navArgument("streamId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("fromLink") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId") ?: ""
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val fromLink = backStackEntry.arguments?.getBoolean("fromLink") ?: false

            val movie = remember { mutableStateOf<VodStream?>(null) }

            LaunchedEffect(streamId, categoryId) {
                scope.launch {
                    val movies = repository.getVodStreamsByCategory(categoryId)
                    movie.value = movies.find { it.streamId == streamId }
                }
            }

            movie.value?.let { vodStream ->
                MovieDetailScreen(
                    movie = vodStream,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToMovie = { toStreamId, toCategoryId ->
                        navController.navigate(Routes.movieDetail(toStreamId, toCategoryId, true))
                    },
                    onNavigateToActor = { cast ->
                        navController.navigate(Routes.actorDetail(cast))
                    },
                    showHomeButton = fromLink,
                    onNavigateHome = {
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    }
                )
            }
        }

        composable(
            route = Routes.ACTOR_DETAIL,
            arguments = listOf(
                navArgument("actorId") { type = NavType.StringType },
                navArgument("actorName") { type = NavType.StringType; defaultValue = "" },
                navArgument("actorProfile") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val actorId = backStackEntry.arguments?.getString("actorId")?.toIntOrNull() ?: -1
            val actorName = backStackEntry.arguments?.getString("actorName")?.let { android.net.Uri.decode(it) } ?: ""
            val actorProfile = backStackEntry.arguments?.getString("actorProfile")?.let { android.net.Uri.decode(it) } ?: ""
            ActorDetailScreen(
                actorId = actorId,
                fallbackName = actorName,
                fallbackProfile = actorProfile,
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToMovie = { toStreamId, toCategoryId ->
                    navController.navigate(Routes.movieDetail(toStreamId, toCategoryId, true))
                },
                onNavigateToSeries = { toSeriesId, toCategoryId ->
                    navController.navigate(Routes.seriesDetail(toSeriesId, toCategoryId))
                }
            )
        }

        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""

            SeriesDetailScreen(
                seriesId = seriesId,
                categoryId = categoryId,
                onBackClick = { navController.popBackStack() },
                onNavigateToSeries = { toSeriesId, toCategoryId ->
                    navController.navigate(Routes.seriesDetail(toSeriesId, toCategoryId))
                },
                onNavigateToActor = { cast ->
                    navController.navigate(Routes.actorDetail(cast))
                }
            )
        }
    }
}
