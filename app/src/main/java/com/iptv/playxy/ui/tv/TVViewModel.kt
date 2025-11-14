package com.iptv.playxy.ui.tv

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.db.FavoriteChannelDao
import com.iptv.playxy.data.db.FavoriteChannelEntity
import com.iptv.playxy.data.db.RecentChannelEntity
import com.iptv.playxy.data.db.RecentChannelDao
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.util.StreamUrlBuilder
// import com.iptv.playxy.ui.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TVViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val favoriteChannelDao: FavoriteChannelDao,
    private val recentChannelDao: RecentChannelDao
) : ViewModel() {


    private val _currentChannel = MutableStateFlow<LiveStream?>(null)
    val currentChannel: StateFlow<LiveStream?> = _currentChannel.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<LiveStream>>(emptyList())
    val filteredChannels: StateFlow<List<LiveStream>> = _filteredChannels.asStateFlow()

    private val _favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChannelIds: StateFlow<Set<String>> = _favoriteChannelIds.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
        loadFavoriteIds()
    }

    fun initialize(context: Context) {
        loadCategories(context)
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.getProfile()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun normalizeCategories(list: List<Category>, defaultAllName: String): List<Category> {
        // Unificar nombres 'Todas'/'Todos' y quitar duplicados por id
        val normalized = list.map {
            if (it.categoryName.equals("Todos", ignoreCase = true) || it.categoryName.equals("Todas", ignoreCase = true))
                it.copy(categoryName = defaultAllName)
            else it
        }
        return normalized.distinctBy { it.categoryId to it.categoryName.lowercase() }
    }

    private fun loadCategories(context: Context) {
        viewModelScope.launch {
            try {
                val providerCategoriesRaw = repository.getCategories("live")
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todos")

                val allCategories = buildList {
                    add(Category("all", "Todos", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(providerCategories)
                }

                _categories.value = allCategories

                // Select "Todos" by default and play first channel if none is playing
                if (allCategories.isNotEmpty()) {
                    selectCategory(allCategories[0], context)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadFavoriteIds() {
        viewModelScope.launch {
            try {
                val favorites = favoriteChannelDao.getAllFavorites()
                _favoriteChannelIds.value = favorites.map { it.channelId }.toSet()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectCategory(category: Category, context: Context) {
        viewModelScope.launch {
            _selectedCategory.value = category
            filterChannels(category)

            // Solo filtra los canales, NO reproduce automáticamente
            // El usuario debe hacer clic en un canal para reproducirlo
        }
    }

    private suspend fun filterChannels(category: Category) {
        try {
            val channels = when (category.categoryId) {
                "all" -> repository.getLiveStreams().distinctBy { it.streamId }
                "favorites" -> loadFavoriteChannels()
                "recents" -> loadRecentChannels()
                else -> repository.getLiveStreamsByCategory(category.categoryId)
            }
            _filteredChannels.value = channels
        } catch (e: Exception) {
            _filteredChannels.value = emptyList()
        }
    }

    private suspend fun loadFavoriteChannels(): List<LiveStream> {
        val favoriteIds = favoriteChannelDao.getAllFavorites().map { it.channelId }
        val allStreams = repository.getLiveStreams()
        return allStreams.filter { stream -> favoriteIds.contains(stream.streamId) }
            .distinctBy { it.streamId }
    }

    private suspend fun loadRecentChannels(): List<LiveStream> {
        val recentIds = recentChannelDao.getRecentChannels().map { it.channelId }
        val allStreams = repository.getLiveStreams()
        return recentIds.mapNotNull { channelId ->
            allStreams.find { it.streamId == channelId }
        }.distinctBy { it.streamId }
    }

    fun playChannel(context: Context, channel: LiveStream) {
        viewModelScope.launch {
            _currentChannel.value = channel

            // Get user profile to build stream URL
            val profile = _userProfile.value
            if (profile != null) {
                val streamUrl = StreamUrlBuilder.buildLiveStreamUrl(profile, channel)

                // TODO: Use PlayerManager to play the channel
                // PlayerManager.changeMedia(
                //     context = context,
                //     url = streamUrl,
                //     title = channel.name,
                //     type = PlayerManager.MediaType.LiveTV
                // )
            }

            // Add to recents
            try {
                recentChannelDao.insertRecent(
                    RecentChannelEntity(
                        channelId = channel.streamId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleFavorite(channel: LiveStream) {
        viewModelScope.launch {
            try {
                val isFavorite = _favoriteChannelIds.value.contains(channel.streamId)
                if (isFavorite) {
                    favoriteChannelDao.deleteFavorite(channel.streamId)
                } else {
                    favoriteChannelDao.insertFavorite(
                        FavoriteChannelEntity(
                            channelId = channel.streamId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                
                // Reload favorites
                loadFavoriteIds()
                
                // Refresh filtered channels if in favorites category
                _selectedCategory.value?.let { category ->
                    if (category.categoryId == "favorites") {
                        filterChannels(category)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun closePlayer() {
        _currentChannel.value = null
        // TODO: PlayerManager.stop()
    }

    fun stopPlayback() {
        _currentChannel.value = null
    }

    fun playNextChannel() {
        val channels = _filteredChannels.value
        val current = _currentChannel.value
        if (current != null && channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.streamId == current.streamId }
            if (currentIndex != -1 && currentIndex < channels.size - 1) {
                _currentChannel.value = channels[currentIndex + 1]
            }
        }
    }

    fun playPreviousChannel() {
        val channels = _filteredChannels.value
        val current = _currentChannel.value
        if (current != null && channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.streamId == current.streamId }
            if (currentIndex > 0) {
                _currentChannel.value = channels[currentIndex - 1]
            }
        }
    }

    fun hasNextChannel(): Boolean {
        val channels = _filteredChannels.value
        val current = _currentChannel.value
        if (current != null && channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.streamId == current.streamId }
            return currentIndex != -1 && currentIndex < channels.size - 1
        }
        return false
    }

    fun hasPreviousChannel(): Boolean {
        val channels = _filteredChannels.value
        val current = _currentChannel.value
        if (current != null && channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.streamId == current.streamId }
            return currentIndex > 0
        }
        return false
    }
}
