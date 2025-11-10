package com.iptv.playxy.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.db.FavoriteChannelDao
import com.iptv.playxy.data.db.FavoriteChannelEntity
import com.iptv.playxy.data.db.RecentChannelEntity
import com.iptv.playxy.data.db.RecentChannelDao
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.PlayerState
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.util.StreamUrlBuilder
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

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

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
        loadCategories()
        loadFavoriteIds()
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

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val providerCategories = repository.getCategories("live")

                val allCategories = buildList {
                    add(Category("all", "Todos", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(providerCategories)
                }

                _categories.value = allCategories
                
                // Select "Todos" by default
                if (allCategories.isNotEmpty()) {
                    selectCategory(allCategories[0])
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

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        filterChannels(category)
    }

    private fun filterChannels(category: Category) {
        viewModelScope.launch {
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

    fun playChannel(channel: LiveStream) {
        viewModelScope.launch {
            _currentChannel.value = channel
            _playerState.value = PlayerState.Playing
            
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
        _playerState.value = PlayerState.Idle
    }

    fun togglePlayPause() {
        _playerState.value = when (_playerState.value) {
            is PlayerState.Playing -> PlayerState.Paused
            is PlayerState.Paused -> PlayerState.Playing
            else -> _playerState.value
        }
    }

    fun playNextChannel() {
        val currentChannelValue = _currentChannel.value ?: return
        val channels = _filteredChannels.value
        val currentIndex = channels.indexOfFirst { it.streamId == currentChannelValue.streamId }
        if (currentIndex != -1 && currentIndex < channels.size - 1) {
            playChannel(channels[currentIndex + 1])
        }
    }

    fun playPreviousChannel() {
        val currentChannelValue = _currentChannel.value ?: return
        val channels = _filteredChannels.value
        val currentIndex = channels.indexOfFirst { it.streamId == currentChannelValue.streamId }
        if (currentIndex > 0) {
            playChannel(channels[currentIndex - 1])
        }
    }

    fun onBuffering() {
        _playerState.value = PlayerState.Buffering
    }

    fun onPlaying() {
        _playerState.value = PlayerState.Playing
    }

    fun onError(message: String) {
        _playerState.value = PlayerState.Error(message)
    }
}
