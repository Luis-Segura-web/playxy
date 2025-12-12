package com.iptv.playxy.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashNavigation {
    object ToLogin : SplashNavigation()
    object ToLoading : SplashNavigation()
    object ToProfiles : SplashNavigation()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _navigationEvent = MutableStateFlow<SplashNavigation?>(null)
    val navigationEvent: StateFlow<SplashNavigation?> = _navigationEvent.asStateFlow()
    
    init {
        checkForValidProfile()
    }
    
    private fun checkForValidProfile() {
        viewModelScope.launch {
            val activeProfile = repository.getProfile()
            
            // Wait a bit to show the splash screen
            kotlinx.coroutines.delay(2000)
            
            _navigationEvent.value = when {
                activeProfile != null && activeProfile.isValid -> SplashNavigation.ToLoading
                repository.hasProfiles() -> SplashNavigation.ToProfiles
                else -> SplashNavigation.ToLogin
            }
        }
    }
    
    fun onNavigationHandled() {
        _navigationEvent.value = null
    }
}
