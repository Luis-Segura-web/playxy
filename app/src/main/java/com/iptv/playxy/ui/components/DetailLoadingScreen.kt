package com.iptv.playxy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pantalla de carga elegante para detalles de películas/series
 * Muestra un skeleton loading animado mientras se cargan los datos
 */
@Composable
fun DetailLoadingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        start = androidx.compose.ui.geometry.Offset(shimmerProgress * 1000f - 500f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerProgress * 1000f, 0f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header con backdrop skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(shimmerBrush)
        ) {
            // Botón de volver
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(12.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Gradiente inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // Contenido skeleton
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título skeleton
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp),
                brush = shimmerBrush
            )

            // Rating y metadata
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.size(80.dp, 24.dp),
                    brush = shimmerBrush
                )
                ShimmerBox(
                    modifier = Modifier.size(60.dp, 24.dp),
                    brush = shimmerBrush
                )
                ShimmerBox(
                    modifier = Modifier.size(100.dp, 24.dp),
                    brush = shimmerBrush
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de reproducir skeleton
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                brush = shimmerBrush,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sinopsis skeleton
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(16.dp),
                    brush = shimmerBrush
                )
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth().height(16.dp),
                    brush = shimmerBrush
                )
                ShimmerBox(
                    modifier = Modifier.fillMaxWidth(0.8f).height(16.dp),
                    brush = shimmerBrush
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección skeleton (ej: elenco)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(20.dp),
                brush = shimmerBrush
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier.size(70.dp),
                            brush = shimmerBrush,
                            shape = CircleShape
                        )
                        ShimmerBox(
                            modifier = Modifier.size(60.dp, 12.dp),
                            brush = shimmerBrush
                        )
                    }
                }
            }
        }

        // Indicador de carga central
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    brush: Brush,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}
