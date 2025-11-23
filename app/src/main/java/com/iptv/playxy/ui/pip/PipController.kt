package com.iptv.playxy.ui.pip

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import com.iptv.playxy.ui.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PipController(
    private val activity: ComponentActivity,
    private val playerManager: PlayerManager,
    private val fullscreenState: MutableState<Boolean>
) {

    private val _isInPip = MutableStateFlow(false)
    val isInPip: StateFlow<Boolean> = _isInPip.asStateFlow()

    private val _hidePlayerUi = MutableStateFlow(false)
    val hidePlayerUi: StateFlow<Boolean> = _hidePlayerUi.asStateFlow()

    private var restoreAction: (() -> Unit)? = null
    private var closeAction: (() -> Unit)? = null
    private var fullscreenForced = false

    fun requestPip(
        hideUi: Boolean = true,
        onRestore: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null
    ) {
        if (!supportsPip() || activity.isInPictureInPictureMode || _isInPip.value) return
        restoreAction = onRestore ?: {}
        closeAction = onClose ?: { playerManager.stopPlayback() }
        // No forzar fullscreen al entrar a PiP; evita parpadeo landscape al volver.
        fullscreenForced = false
        _hidePlayerUi.value = hideUi
        val entered = enterPipInternal()
        if (!entered) {
            _hidePlayerUi.value = false
            if (fullscreenForced) {
                fullscreenState.value = false
                fullscreenForced = false
            }
            restoreAction = null
            closeAction = null
        }
    }

    fun handlePictureInPictureModeChanged(isInPipMode: Boolean, finishing: Boolean) {
        _isInPip.value = isInPipMode
        if (!isInPipMode) {
            if (finishing) {
                completePipExit(restored = false)
            } else {
                evaluateRestoreState()
            }
        }
    }

    private fun evaluateRestoreState(attemptsRemaining: Int = 3, delayMs: Long = 120L) {
        val decor = activity.window?.decorView
        if (decor == null) {
            completePipExit(restored = true)
            return
        }
        decor.postDelayed({
            val restored =
                activity.hasWindowFocus() && !activity.isFinishing && !activity.isDestroyed
            when {
                restored -> completePipExit(restored = true)
                attemptsRemaining > 0 ->
                    evaluateRestoreState(attemptsRemaining - 1, delayMs)
                else -> completePipExit(restored = false)
            }
        }, delayMs)
    }

    private fun completePipExit(restored: Boolean) {
        _hidePlayerUi.value = false
        // Restaurar fullscreen state ANTES de invocar restoreAction
        if (fullscreenForced) {
            fullscreenState.value = false
            fullscreenForced = false
        }
        if (restored) {
            restoreAction?.invoke()
        } else {
            closeAction?.invoke()
        }
        restoreAction = null
        closeAction = null
    }

    private fun supportsPip(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun enterPipInternal(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } else {
            false
        }
    }
}
