package com.iptv.playxy.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptv.playxy.data.db.FavoriteChannelDao
import com.iptv.playxy.data.db.FavoriteChannelEntity
import com.iptv.playxy.data.db.RecentChannelDao
import com.iptv.playxy.data.db.RecentChannelEntity
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.ui.main.SortOrder
import com.iptv.playxy.ui.player.PlayerManager
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.util.StreamUrlBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TVUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val favoriteChannelIds: Set<String> = emptySet(),
    val parentalEnabled: Boolean = false,
    val blockedCategories: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val recents: List<String> = emptyList()
)

@HiltViewModel
class TVViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val favoriteChannelDao: FavoriteChannelDao,
    private val recentChannelDao: RecentChannelDao,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TVUiState())
    val uiState: StateFlow<TVUiState> = _uiState.asStateFlow()

    private val _pagingFlow = MutableStateFlow<kotlinx.coroutines.flow.Flow<PagingData<LiveStream>>>(flowOf(PagingData.empty()))
    val pagingFlow: StateFlow<kotlinx.coroutines.flow.Flow<PagingData<LiveStream>>> = _pagingFlow.asStateFlow()

    private val _currentChannel = MutableStateFlow<LiveStream?>(null)
    val currentChannel: StateFlow<LiveStream?> = _currentChannel.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Lista ordenada de canales para navegación
    private val _orderedChannels = MutableStateFlow<List<LiveStream>>(emptyList())
    val orderedChannels: StateFlow<List<LiveStream>> = _orderedChannels.asStateFlow()

    private val _currentChannelIndex = MutableStateFlow(-1)
    val currentChannelIndex: StateFlow<Int> = _currentChannelIndex.asStateFlow()

    init {
        loadUserProfile()
        loadInitial()
        observeRecentsCleared()
        observePrefEvents()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            loadFavoriteIds()
            loadRecentIds()
            loadCategories()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _userProfile.value = repository.getProfile()
        }
    }

    private suspend fun refreshParentalState(): Pair<Boolean, Set<String>> {
        val enabled = repository.isParentalControlEnabled()
        val blocked = if (enabled) repository.getBlockedCategories("live") else emptySet()
        _uiState.value = _uiState.value.copy(parentalEnabled = enabled, blockedCategories = blocked)
        return enabled to blocked
    }

    private fun normalizeCategories(list: List<Category>, defaultAllName: String): List<Category> {
        val normalized = list.map {
            if (it.categoryName.equals("Todos", ignoreCase = true) || it.categoryName.equals("Todas", ignoreCase = true))
                it.copy(categoryName = defaultAllName)
            else it
        }
        return normalized.distinctBy { it.categoryId to it.categoryName.lowercase() }
    }

    private suspend fun loadFavoriteIds() {
        val favorites = favoriteChannelDao.getAllFavorites()
        _uiState.value = _uiState.value.copy(favoriteChannelIds = favorites.map { it.channelId }.toSet())
    }

    private suspend fun loadRecentIds() {
        val recents = recentChannelDao.getRecentChannels()
        _uiState.value = _uiState.value.copy(recents = recents.map { it.channelId })
    }

    private fun observeRecentsCleared() {
        viewModelScope.launch {
            repository.recentsCleared().collect { type ->
                if (type == "live" || type == "all") {
                    _uiState.value = _uiState.value.copy(recents = emptyList())
                    if (_uiState.value.selectedCategory?.categoryId == "recents") {
                        refreshPaging()
                    }
                }
            }
        }
    }

    private fun observePrefEvents() {
        viewModelScope.launch {
            repository.prefEvents().collect { event ->
                if (event == "parental" || event == "blocked_live") {
                    loadCategories()
                    refreshPaging()
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val providerCategoriesRaw = repository.getCategories("live")
                val (parentalEnabled, blocked) = refreshParentalState()
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todos")
                    .filterNot { parentalEnabled && blocked.contains(it.categoryId) }

                val allCategories = buildList {
                    add(Category("all", "Todos", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(providerCategories)
                }
                _uiState.value = _uiState.value.copy(categories = allCategories, selectedCategory = allCategories.first())
                refreshPaging()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun selectCategory(category: Category) {
        if (_uiState.value.selectedCategory?.categoryId == category.categoryId) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        refreshPaging()
    }

    fun updateFilters(search: String, sortOrder: SortOrder) {
        if (_uiState.value.searchQuery == search && _uiState.value.sortOrder == sortOrder) return
        _uiState.value = _uiState.value.copy(searchQuery = search, sortOrder = sortOrder)
        refreshPaging()
    }

    fun refreshCurrentCategory() {
        refreshPaging()
    }

    private fun refreshPaging() {
        viewModelScope.launch {
            val (parentalEnabled, blockedCategories) = refreshParentalState()
            val categoryId = when (val selected = _uiState.value.selectedCategory?.categoryId) {
                null, "all" -> null
                "favorites", "recents" -> selected
                else -> selected
            }
            val search = _uiState.value.searchQuery.ifBlank { null }
            val sortCode = mapSortOrder(_uiState.value.sortOrder)
            val flow = when (categoryId) {
                "favorites" -> {
                    val favorites = withContext(Dispatchers.Default) {
                        repository.getLiveStreams()
                            .filterNot { parentalEnabled && blockedCategories.contains(it.categoryId) }
                            .filter { _uiState.value.favoriteChannelIds.contains(it.streamId) }
                            .distinctBy { it.streamId }
                    }
                    flowOf(PagingData.from(favorites))
                }
                "recents" -> {
                    val recentsIds = _uiState.value.recents
                    val all = withContext(Dispatchers.Default) {
                        repository.getLiveStreams()
                            .filterNot { parentalEnabled && blockedCategories.contains(it.categoryId) }
                            .associateBy { it.streamId }
                    }
                    val recents = recentsIds.mapNotNull { all[it] }.distinctBy { it.streamId }
                    flowOf(PagingData.from(recents))
                }
                else -> {
                    val allowBlockedList = blockedCategories.toList()
                    repository.getPagedLiveStreams(
                        categoryId = categoryId,
                        searchQuery = search,
                        blockAdult = false,
                        blockedCategories = allowBlockedList,
                        sortOrder = sortCode,
                        pageSize = 60
                    ).cachedIn(viewModelScope)
                }
            }
            _pagingFlow.value = flow
        }
    }

    private fun mapSortOrder(order: SortOrder): Int = when (order) {
        SortOrder.DEFAULT -> 0
        SortOrder.A_TO_Z -> 1
        SortOrder.Z_TO_A -> 2
        else -> 0
    }

    fun playChannel(channel: LiveStream) {
        viewModelScope.launch {
            _currentChannel.value = channel
            // Actualizar el índice del canal actual
            val channels = _orderedChannels.value
            val index = channels.indexOfFirst { it.streamId == channel.streamId }
            _currentChannelIndex.value = index
            addChannelToRecents(channel)
            startChannelPlayback(channel)
        }
    }

    fun updateChannelList(channels: List<LiveStream>) {
        _orderedChannels.value = channels
        // Actualizar índice si el canal actual está en la lista
        _currentChannel.value?.let { current ->
            val index = channels.indexOfFirst { it.streamId == current.streamId }
            _currentChannelIndex.value = index
        }
    }

    fun playPreviousChannel(): Boolean {
        val channels = _orderedChannels.value
        val currentIndex = _currentChannelIndex.value
        if (channels.isEmpty() || currentIndex <= 0) return false
        
        val previousChannel = channels[currentIndex - 1]
        playChannel(previousChannel)
        return true
    }

    fun playNextChannel(): Boolean {
        val channels = _orderedChannels.value
        val currentIndex = _currentChannelIndex.value
        if (channels.isEmpty() || currentIndex < 0 || currentIndex >= channels.size - 1) return false
        
        val nextChannel = channels[currentIndex + 1]
        playChannel(nextChannel)
        return true
    }

    fun hasPreviousChannel(): Boolean {
        return _currentChannelIndex.value > 0
    }

    fun hasNextChannel(): Boolean {
        val channels = _orderedChannels.value
        val currentIndex = _currentChannelIndex.value
        return currentIndex >= 0 && currentIndex < channels.size - 1
    }

    private suspend fun addChannelToRecents(channel: LiveStream) {
        try {
            val limit = repository.getRecentsLimit()
            recentChannelDao.deleteRecent(channel.streamId)
            recentChannelDao.insertRecent(
                RecentChannelEntity(channelId = channel.streamId, timestamp = System.currentTimeMillis())
            )
            recentChannelDao.trim(limit)
            loadRecentIds()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun startChannelPlayback(channel: LiveStream) {
        val profile = _userProfile.value ?: return
        val streamUrl = StreamUrlBuilder.buildLiveStreamUrl(profile, channel)
        playerManager.playMedia(streamUrl, PlayerType.TV, forcePrepare = true)
    }

    fun toggleFavorite(channel: LiveStream) {
        viewModelScope.launch {
            try {
                val isFavorite = _uiState.value.favoriteChannelIds.contains(channel.streamId)
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
                loadFavoriteIds()
                if (_uiState.value.selectedCategory?.categoryId == "favorites") refreshPaging()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun requiresPinForCategory(categoryId: String): Boolean {
        return repository.isCategoryRestricted("live", categoryId)
    }

    suspend fun validateParentalPin(pin: String): Boolean {
        return repository.verifyParentalPin(pin)
    }
}
