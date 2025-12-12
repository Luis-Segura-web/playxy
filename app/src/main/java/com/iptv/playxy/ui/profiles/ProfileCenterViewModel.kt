package com.iptv.playxy.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSummary(
    val id: Int,
    val name: String,
    val username: String,
    val url: String,
    val status: String?,
    val isActive: Boolean
)

data class ProfileFormState(
    val id: Int? = null,
    val profileName: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val activateOnSave: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class ProfileCenterState(
    val profiles: List<ProfileSummary> = emptyList(),
    val isLoading: Boolean = true,
    val activationInProgress: Int? = null,
    val formState: ProfileFormState? = null,
    val deleteConfirmationId: Int? = null
)

sealed class ProfileCenterEvent {
    object NavigateToLoading : ProfileCenterEvent()
    data class ShowMessage(val message: String) : ProfileCenterEvent()
}

@HiltViewModel
class ProfileCenterViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileCenterState())
    val state: StateFlow<ProfileCenterState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProfileCenterEvent>()
    val events: SharedFlow<ProfileCenterEvent> = _events.asSharedFlow()

    init {
        observeProfiles()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            repository.getProfilesFlow().collect { profiles ->
                _state.update {
                    it.copy(
                        profiles = profiles.map { profile -> profile.toSummary() },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun openNewProfile() {
        _state.update {
            it.copy(
                formState = ProfileFormState(
                    activateOnSave = true
                )
            )
        }
    }

    fun openEditProfile(profileId: Int) {
        viewModelScope.launch {
            val profile = repository.getProfileById(profileId) ?: return@launch
            _state.update {
                it.copy(
                    formState = ProfileFormState(
                        id = profile.id,
                        profileName = profile.profileName,
                        username = profile.username,
                        password = profile.password,
                        url = profile.url,
                        activateOnSave = profile.isActive
                    )
                )
            }
        }
    }

    fun onFormNameChange(value: String) = updateForm { 
        copy(profileName = value.replace("\n", "").replace("\r", ""), errorMessage = null) 
    }
    fun onFormUsernameChange(value: String) = updateForm { 
        copy(username = value.trim().replace("\n", "").replace("\r", ""), errorMessage = null) 
    }
    fun onFormPasswordChange(value: String) = updateForm { 
        copy(password = value.trim().replace("\n", "").replace("\r", ""), errorMessage = null) 
    }
    fun onFormUrlChange(value: String) = updateForm { 
        copy(url = value.trim().replace("\n", "").replace("\r", ""), errorMessage = null) 
    }
    fun onActivateToggle(value: Boolean) = updateForm { copy(activateOnSave = value, errorMessage = null) }

    fun closeForm() {
        _state.update { it.copy(formState = null) }
    }

    fun requestDelete(profileId: Int) {
        _state.update { it.copy(deleteConfirmationId = profileId) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteConfirmationId = null) }
    }

    fun deleteProfile() {
        val profileId = _state.value.deleteConfirmationId ?: return
        viewModelScope.launch {
            val activeId = repository.getProfile()?.id
            repository.deleteProfile(profileId)
            if (activeId == profileId) {
                repository.clearUserData()
                repository.deactivateProfiles()
            }
            _events.emit(ProfileCenterEvent.ShowMessage("Perfil eliminado"))
            _state.update { it.copy(deleteConfirmationId = null) }
        }
    }

    fun activateProfile(profileId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(activationInProgress = profileId) }
            try {
                repository.setActiveProfile(profileId)
                _events.emit(ProfileCenterEvent.NavigateToLoading)
            } catch (e: Exception) {
                _events.emit(ProfileCenterEvent.ShowMessage("No se pudo activar el perfil: ${e.message ?: "Error desconocido"}"))
            } finally {
                _state.update { it.copy(activationInProgress = null) }
            }
        }
    }

    fun submitProfile() {
        val form = _state.value.formState ?: return
        val validationError = validateForm(form)
        if (validationError != null) {
            _state.update { it.copy(formState = form.copy(errorMessage = validationError)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(formState = form.copy(isSaving = true, errorMessage = null)) }
            try {
                val loginResult = repository.fetchAccountInfoWithError(form.username, form.password, form.url)
                
                if (loginResult.isFailure) {
                    _state.update {
                        it.copy(formState = form.copy(
                            isSaving = false, 
                            errorMessage = loginResult.exceptionOrNull()?.message ?: "Error de conexión"
                        ))
                    }
                    return@launch
                }
                
                val loginInfo = loginResult.getOrNull()
                val isValid = loginInfo?.userInfo?.status?.equals("active", true) == true
                if (!isValid) {
                    val status = loginInfo?.userInfo?.status
                    val errorMsg = when {
                        status.isNullOrBlank() -> "El servidor no devolvió información de cuenta"
                        status.equals("expired", true) -> "La cuenta ha expirado"
                        status.equals("disabled", true) -> "La cuenta está deshabilitada"
                        status.equals("banned", true) -> "La cuenta está suspendida"
                        else -> "Cuenta inactiva (estado: $status)"
                    }
                    _state.update {
                        it.copy(formState = form.copy(isSaving = false, errorMessage = errorMsg))
                    }
                    return@launch
                }

                val profile = UserProfile(
                    id = form.id ?: 0,
                    profileName = form.profileName,
                    username = form.username,
                    password = form.password,
                    url = form.url,
                    isValid = true,
                    expiry = loginInfo.userInfo?.expDate?.toLongOrNull()?.takeIf { exp -> exp > 0 },
                    maxConnections = loginInfo.userInfo?.maxConnections?.toIntOrNull(),
                    status = loginInfo.userInfo?.status,
                    isActive = form.activateOnSave
                )

                if (form.id == null) {
                    repository.saveProfile(profile, setActive = form.activateOnSave)
                } else {
                    repository.updateProfile(profile, setActive = form.activateOnSave)
                }

                if (form.activateOnSave) {
                    repository.clearUserData()
                    _events.emit(ProfileCenterEvent.NavigateToLoading)
                } else {
                    _events.emit(ProfileCenterEvent.ShowMessage("Perfil guardado"))
                }
                _state.update { it.copy(formState = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        formState = form.copy(
                            isSaving = false,
                            errorMessage = e.message ?: "No se pudo guardar el perfil"
                        )
                    )
                }
            }
        }
    }

    private fun updateForm(block: ProfileFormState.() -> ProfileFormState) {
        _state.update { current ->
            val form = current.formState ?: return@update current
            current.copy(formState = block(form))
        }
    }

    private fun validateForm(form: ProfileFormState): String? {
        if (form.profileName.isBlank()) return "Ingresa un nombre de perfil"
        if (form.username.isBlank()) return "El usuario es obligatorio"
        if (form.password.isBlank()) return "La contraseña es obligatoria"
        if (form.url.isBlank()) return "La URL es obligatoria"
        val normalizedUrl = form.url.lowercase()
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            return "La URL debe iniciar con http:// o https://"
        }
        return null
    }

    private fun UserProfile.toSummary(): ProfileSummary {
        return ProfileSummary(
            id = id,
            name = profileName,
            username = username,
            url = url,
            status = status?.ifBlank { null },
            isActive = isActive
        )
    }
}
