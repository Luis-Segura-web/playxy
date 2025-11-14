package com.iptv.playxy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks

data class TrackInfo(
    val id: String,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@Composable
fun TrackSelectorDialog(
    player: Player?,
    onDismiss: () -> Unit
) {
    val audioTracks = remember { mutableStateListOf<TrackInfo>() }
    val subtitleTracks = remember { mutableStateListOf<TrackInfo>() }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(player) {
        player?.let { p ->
            val currentTracks = p.currentTracks

            // Extract audio tracks
            audioTracks.clear()
            currentTracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        audioTracks.add(
                            TrackInfo(
                                id = "$groupIndex-$i",
                                label = format.label ?: "Audio ${i + 1}",
                                language = format.language,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }

            // Extract subtitle tracks
            subtitleTracks.clear()
            subtitleTracks.add(
                TrackInfo(
                    id = "none",
                    label = "Desactivados",
                    language = null,
                    isSelected = currentTracks.groups.none {
                        it.type == C.TRACK_TYPE_TEXT && it.isSelected
                    }
                )
            )
            currentTracks.groups.forEachIndexed { groupIndex, group ->
                if (group.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        subtitleTracks.add(
                            TrackInfo(
                                id = "$groupIndex-$i",
                                label = format.label ?: "Subtítulo ${i + 1}",
                                language = format.language,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio y Subtítulos",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Audio (${audioTracks.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Subtítulos (${subtitleTracks.size})") }
                    )
                }

                // Content
                when (selectedTab) {
                    0 -> TrackList(
                        tracks = audioTracks,
                        onTrackSelected = { track ->
                            player?.let { p ->
                                val params = p.trackSelectionParameters
                                    .buildUpon()
                                    .setPreferredAudioLanguage(track.language ?: "und")
                                    .build()
                                p.trackSelectionParameters = params
                                onDismiss()
                            }
                        }
                    )
                    1 -> TrackList(
                        tracks = subtitleTracks,
                        onTrackSelected = { track ->
                            player?.let { p ->
                                if (track.id == "none") {
                                    val params = p.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredTextLanguage(null)
                                        .build()
                                    p.trackSelectionParameters = params
                                } else {
                                    val params = p.trackSelectionParameters
                                        .buildUpon()
                                        .setPreferredTextLanguage(track.language ?: "und")
                                        .build()
                                    p.trackSelectionParameters = params
                                }
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<TrackInfo>,
    onTrackSelected: (TrackInfo) -> Unit
) {
    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay pistas disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackSelected(track) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (track.language != null) {
                            Text(
                                text = track.language,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (track.isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun hasAudioOrSubtitleTracks(player: Player?): Boolean {
    var hasTracks by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        player?.let { p ->
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    val hasAudio = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO && it.length > 1 }
                    val hasSubtitles = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.length > 0 }
                    hasTracks = hasAudio || hasSubtitles
                }
            }
            p.addListener(listener)

            // Check initial state
            val tracks = p.currentTracks
            val hasAudio = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO && it.length > 1 }
            val hasSubtitles = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.length > 0 }
            hasTracks = hasAudio || hasSubtitles
        }
    }

    return hasTracks
}

