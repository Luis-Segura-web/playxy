package com.iptv.playxy.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val profileName: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isValidated: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    fun onProfileNameChange(value: String) {
        _state.value = _state.value.copy(profileName = value, errorMessage = null)
    }
    
    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, errorMessage = null)
    }
    
    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, errorMessage = null)
    }
    
    fun onUrlChange(value: String) {
        _state.value = _state.value.copy(url = value, errorMessage = null)
    }
    
    fun onSubmit() {
        val currentState = _state.value
        
        // Validation
        if (currentState.profileName.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Por favor ingrese un nombre de perfil")
            return
        }
        if (currentState.username.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Por favor ingrese un usuario")
            return
        }
        if (currentState.password.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Por favor ingrese una contraseña")
            return
        }
        if (currentState.url.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Por favor ingrese una URL")
            return
        }
        
        // URL format validation
        if (!isValidUrl(currentState.url)) {
            _state.value = currentState.copy(errorMessage = "URL inválida. Debe comenzar con http:// o https://")
            return
        }
        
        // Validate credentials with API
        validateAndSave()
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) || 
               url.startsWith("https://", ignoreCase = true)
    }
    
    private fun validateAndSave() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val currentState = _state.value
                
                val loginInfo = repository.fetchAccountInfo(
                    currentState.username,
                    currentState.password,
                    currentState.url
                )

                val isValid = loginInfo?.userInfo?.status?.equals("active", ignoreCase = true) == true
                if (isValid) {
                    val profile = UserProfile(
                        profileName = currentState.profileName,
                        username = currentState.username,
                        password = currentState.password,
                        url = currentState.url,
                        isValid = true,
                        expiry = loginInfo?.userInfo?.expDate?.toLongOrNull()?.takeIf { it > 0 },
                        maxConnections = loginInfo?.userInfo?.maxConnections?.toIntOrNull(),
                        status = loginInfo?.userInfo?.status
                    )
                    repository.saveProfile(profile)
                    
                    _state.value = currentState.copy(
                        isLoading = false,
                        isValidated = true
                    )
                } else {
                    _state.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "Credenciales inválidas. Por favor verifique sus datos."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Error al validar credenciales: ${e.message}"
                )
            }
        }
    }
    
    fun onNavigationHandled() {
        _state.value = _state.value.copy(isValidated = false)
    }
}
