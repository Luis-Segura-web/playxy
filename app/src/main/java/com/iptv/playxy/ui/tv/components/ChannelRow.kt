package com.iptv.playxy.ui.tv.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.playxy.domain.LiveStream

@Composable
fun ChannelRow(
    channel: LiveStream,
    isFavorite: Boolean,
    isPlaying: Boolean = false,
    onChannelClick: (LiveStream) -> Unit,
    onFavoriteClick: (LiveStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Animaciones
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isPlaying) 2.dp else 0.dp,
        animationSpec = tween(300),
        label = "border"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "color"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isPlaying) 4.dp else 0.dp,
        shadowElevation = if (isPlaying) 8.dp else 0.dp,
        color = containerColor,
        border = if (isPlaying) {
            androidx.compose.foundation.BorderStroke(
                borderWidth,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onChannelClick(channel) }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel logo con efecto de reproducción
            Box(modifier = Modifier.size(56.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(
                            android.R.drawable.ic_menu_gallery
                        ),
                        placeholder = androidx.compose.ui.res.painterResource(
                            android.R.drawable.ic_menu_gallery
                        )
                    )
                }

                // Indicador de reproducción
                androidx.compose.animation.AnimatedVisibility(
                    visible = isPlaying,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduciendo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                }
                
                // Borde de reproducción en el logo
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            // Channel info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val titleStyle = if (isPlaying) {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                }
                val titleColor = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                AutoFitChannelTitle(
                    text = channel.name,
                    style = titleStyle,
                    color = titleColor
                )
                
                // Estado
                AnimatedVisibility(
                    visible = isPlaying,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "En reproducción",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Favorito moderno
            Surface(
                onClick = { onFavoriteClick(channel) },
                shape = CircleShape,
                color = if (isFavorite) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoFitChannelTitle(
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int = 3,
    minFontSize: TextUnit = 12.sp
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val baseFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified } ?: 16.sp
        val fittedSize = remember(text, maxWidthPx, baseFontSize) {
            var size = baseFontSize
            while (size > minFontSize) {
                val result = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(fontSize = size),
                    constraints = Constraints(maxWidth = maxWidthPx),
                    maxLines = maxLines
                )
                if (!result.hasVisualOverflow) break
                size = (size.value - 1f).sp
            }
            size
        }
        Text(
            text = text,
            style = style.copy(fontSize = fittedSize),
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Clip
        )
    }
}
