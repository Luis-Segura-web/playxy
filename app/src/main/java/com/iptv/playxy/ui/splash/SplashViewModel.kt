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
            val profile = repository.getProfile()
            
            // Wait a bit to show the splash screen
            kotlinx.coroutines.delay(2000)
            
            if (profile != null && profile.isValid) {
                _navigationEvent.value = SplashNavigation.ToLoading
            } else {
                _navigationEvent.value = SplashNavigation.ToLogin
            }
        }
    }
    
    fun onNavigationHandled() {
        _navigationEvent.value = null
    }
}
