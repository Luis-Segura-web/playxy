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
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.ui.player.PlayerManager
import com.iptv.playxy.ui.player.PlayerType
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
    private val recentChannelDao: RecentChannelDao,
    private val playerManager: PlayerManager
) : ViewModel() {


    private val _currentChannel = MutableStateFlow<LiveStream?>(null)
    val currentChannel: StateFlow<LiveStream?> = _currentChannel.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _filteredChannels = MutableStateFlow<List<LiveStream>>(emptyList())
    val filteredChannels: StateFlow<List<LiveStream>> = _filteredChannels.asStateFlow()
    private val _orderedChannels = MutableStateFlow<List<LiveStream>>(emptyList())

    // Cache para nombres normalizados y claves de orden natural
    private val nameCache = mutableMapOf<String, NameCache>()

    private val _favoriteChannelIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChannelIds: StateFlow<Set<String>> = _favoriteChannelIds.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
        loadFavoriteIds()
    }

    fun initialize() {
        loadCategories()
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

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val providerCategoriesRaw = repository.getCategories("live")
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todos")
                val blocked = repository.getBlockedCategories("live")
                val filteredProviders = providerCategories.filterNot { blocked.contains(it.categoryId) }

                val allCategories = buildList {
                    add(Category("all", "Todos", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(filteredProviders)
                }

                _categories.value = allCategories

                // Select "Todos" by default and play first channel if none is playing
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
        viewModelScope.launch {
            _selectedCategory.value = category
            filterChannels(category)

            // Solo filtra los canales, NO reproduce automáticamente
            // El usuario debe hacer clic en un canal para reproducirlo
        }
    }

    private suspend fun filterChannels(category: Category) {
        try {
            val blockAdult = repository.isParentalControlEnabled()
            val blockedCategories = repository.getBlockedCategories("live")
            val channels = when (category.categoryId) {
                "all" -> repository.getLiveStreams().distinctBy { it.streamId }
                "favorites" -> loadFavoriteChannels()
                "recents" -> loadRecentChannels()
                else -> repository.getLiveStreamsByCategory(category.categoryId)
            }.filterNot {
                (blockAdult && it.isAdult) || blockedCategories.contains(it.categoryId)
            }
            channels.forEach { cacheNameData(it) }
            _filteredChannels.value = channels
            _orderedChannels.value = channels
        } catch (e: Exception) {
            _filteredChannels.value = emptyList()
            _orderedChannels.value = emptyList()
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
            startChannelPlayback(channel)
            addChannelToRecents(channel)
        }
    }

    private fun startChannelPlayback(channel: LiveStream) {
        val profile = _userProfile.value ?: return
        val streamUrl = StreamUrlBuilder.buildLiveStreamUrl(profile, channel)
        playerManager.playMedia(streamUrl, PlayerType.TV, forcePrepare = true)
    }

    private suspend fun addChannelToRecents(channel: LiveStream) {
        try {
            val limit = repository.getRecentsLimit()
            recentChannelDao.deleteRecent(channel.streamId) // evitar duplicados; refresca timestamp
            recentChannelDao.insertRecent(
                RecentChannelEntity(
                    channelId = channel.streamId,
                    timestamp = System.currentTimeMillis()
                )
            )
            recentChannelDao.trim(limit)
        } catch (e: Exception) {
            // Handle error
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
        stopPlayback()
    }

    fun stopPlayback() {
        playerManager.stopPlayback()
        _currentChannel.value = null
    }

    fun playNextChannel() {
        val nextChannel = findAdjacentChannel(1) ?: return
        viewModelScope.launch {
            _currentChannel.value = nextChannel
            startChannelPlayback(nextChannel)
            addChannelToRecents(nextChannel)
        }
    }

    fun playPreviousChannel() {
        val previousChannel = findAdjacentChannel(-1) ?: return
        viewModelScope.launch {
            _currentChannel.value = previousChannel
            startChannelPlayback(previousChannel)
            addChannelToRecents(previousChannel)
        }
    }

    fun hasNextChannel(): Boolean = findAdjacentChannel(1) != null

    fun hasPreviousChannel(): Boolean = findAdjacentChannel(-1) != null

    fun updateOrderedChannels(list: List<LiveStream>) {
        _orderedChannels.value = list
    }

    fun getNormalizedName(channel: LiveStream): String {
        return nameCache[channel.streamId]?.normalizedName ?: cacheNameData(channel).normalizedName
    }

    fun getNaturalSortKey(channel: LiveStream): String {
        return nameCache[channel.streamId]?.sortKey ?: cacheNameData(channel).sortKey
    }

    private fun findAdjacentChannel(offset: Int): LiveStream? {
        val channels = _orderedChannels.value.ifEmpty { _filteredChannels.value }
        val current = _currentChannel.value ?: return null
        if (channels.isEmpty()) return null
        val currentIndex = channels.indexOfFirst { it.streamId == current.streamId }
        if (currentIndex == -1) return null
        val targetIndex = currentIndex + offset
        return if (targetIndex in channels.indices) channels[targetIndex] else null
    }

    private fun cacheNameData(channel: LiveStream): NameCache {
        val normalized = normalizeString(channel.name)
        val sortKey = naturalSortKey(channel.name)
        return NameCache(normalizedName = normalized, sortKey = sortKey).also {
            nameCache[channel.streamId] = it
        }
    }

    private fun normalizeString(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        return normalized.replace(accentRegex, "")
    }

    private fun naturalSortKey(input: String): String {
        val builder = StringBuilder()
        var lastIndex = 0
        numberRegex.findAll(input).forEach { match ->
            builder.append(input.substring(lastIndex, match.range.first).lowercase())
            builder.append(match.value.padStart(10, '0'))
            lastIndex = match.range.last + 1
        }
        if (lastIndex < input.length) {
            builder.append(input.substring(lastIndex).lowercase())
        }
        return builder.toString()
    }
}

private val accentRegex = Regex("\\p{M}")
private val numberRegex = Regex("\\d+")

private data class NameCache(
    val normalizedName: String,
    val sortKey: String
)
