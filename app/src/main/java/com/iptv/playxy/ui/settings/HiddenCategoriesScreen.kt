package com.iptv.playxy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iptv.playxy.domain.Category

@Composable
fun HiddenCategoriesScreen(
    liveCategories: List<Category>,
    vodCategories: List<Category>,
    seriesCategories: List<Category>,
    initialLive: Set<String>,
    initialVod: Set<String>,
    initialSeries: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>, Set<String>, Set<String>) -> Unit
) {
    fun defaultSelected(categories: List<Category>, initial: Set<String>): Set<String> {
        if (initial.isNotEmpty() || categories.isEmpty()) return initial
        val keywordRegex = Regex("(adult|adultos|xxx|porn|porno|sex|sexual|\\+18)", RegexOption.IGNORE_CASE)
        val auto = categories.filter { keywordRegex.containsMatchIn(it.categoryName.orEmpty()) }
            .map { it.categoryId }
            .toSet()
        return initial + auto
    }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedLive by remember(initialLive, liveCategories) { mutableStateOf(defaultSelected(liveCategories, initialLive)) }
    var selectedVod by remember(initialVod, vodCategories) { mutableStateOf(defaultSelected(vodCategories, initialVod)) }
    var selectedSeries by remember(initialSeries, seriesCategories) { mutableStateOf(defaultSelected(seriesCategories, initialSeries)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Categorías ocultas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Marca lo que deseas ocultar. Las categorías con contenido adulto se marcan automáticamente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("TV") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Películas") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Series") })
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> CategoryList(
                            categories = liveCategories,
                            selected = selectedLive,
                            onToggle = { id ->
                                selectedLive = if (selectedLive.contains(id)) selectedLive - id else selectedLive + id
                            }
                        )
                        1 -> CategoryList(
                            categories = vodCategories,
                            selected = selectedVod,
                            onToggle = { id ->
                                selectedVod = if (selectedVod.contains(id)) selectedVod - id else selectedVod + id
                            }
                        )
                        2 -> CategoryList(
                            categories = seriesCategories,
                            selected = selectedSeries,
                            onToggle = { id ->
                                selectedSeries = if (selectedSeries.contains(id)) selectedSeries - id else selectedSeries + id
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedLive, selectedVod, selectedSeries) }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay categorías disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    categories.forEach { category ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = selected.contains(category.categoryId),
                    onCheckedChange = { onToggle(category.categoryId) }
                )
                Column {
                    Text(category.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (category.parentId.isNotBlank() && category.parentId != "0") {
                        Text(
                            text = "ID: ${category.categoryId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
