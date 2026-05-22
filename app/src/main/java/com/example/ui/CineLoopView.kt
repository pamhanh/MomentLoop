package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Moment
import com.example.data.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CineLoopDialog(
    project: Project,
    moments: List<Moment>,
    lang: String,
    onDismiss: () -> Unit
) {
    if (moments.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            CineLoopContent(
                project = project,
                moments = moments.sortedBy { it.createdAt },
                lang = lang,
                onDismiss = onDismiss
            )
        }
    }
}

enum class ColorGradingFilter {
    NORMAL, VINTAGE, CYBERPUNK, MONOCHROME, DRAMATIC
}

enum class MusicSoundtrack {
    LOFI, TECHNO, AMBIENT, TRIUMPHANT
}

@Composable
fun CineLoopContent(
    project: Project,
    moments: List<Moment>,
    lang: String,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf(ColorGradingFilter.NORMAL) }
    var activeSoundtrack by remember { mutableStateOf(MusicSoundtrack.LOFI) }
    var playbackSpeedMillis by remember { mutableStateOf(3500) } // speed in MS
    
    val currentMoment = moments[currentIndex]

    // Slide timer loop
    LaunchedEffect(isPlaying, currentIndex, playbackSpeedMillis) {
        if (isPlaying) {
            kotlinx.coroutines.delay(playbackSpeedMillis.toLong())
            currentIndex = (currentIndex + 1) % moments.size
        }
    }

    // Ken Burns Zoom/Pan Animation
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurnsZoom")
    val zoomScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Zoom"
    )
    val panOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pan"
    )

    // Apply color matrix filter according to filter chosen
    val colorMatrix = remember(activeFilter) {
        when (activeFilter) {
            ColorGradingFilter.VINTAGE -> ColorMatrix().apply {
                setToScale(1.1f, 0.9f, 0.7f, 1f) // Sepia-warm tone
            }
            ColorGradingFilter.CYBERPUNK -> ColorMatrix().apply {
                // Enhance blues & pink magentas
                setToScale(1.2f, 0.7f, 1.4f, 1f)
            }
            ColorGradingFilter.MONOCHROME -> ColorMatrix().apply {
                setToSaturation(0f)
            }
            ColorGradingFilter.DRAMATIC -> ColorMatrix().apply {
                setToSaturation(1.3f)
            }
            ColorGradingFilter.NORMAL -> ColorMatrix()
        }
    }

    // Dynamic procedural audio visualizer waves
    val activeColorTheme = runCatching { Color(android.graphics.Color.parseColor(project.thumbnailColor)) }
        .getOrDefault(Color(0xFF6BCB77))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Ken Burns Styled Slide Image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale,
                    translationX = panOffset,
                    translationY = panOffset / 2
                )
        ) {
            val isPlaceholder = currentMoment.imageUri.startsWith("placeholder_")
            if (isPlaceholder) {
                val fallbackGrad = when (currentMoment.imageUri) {
                    "placeholder_red" -> listOf(Color(0xFFE57373), Color(0xFFC62828))
                    "placeholder_green" -> listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                    "placeholder_blue" -> listOf(Color(0xFF64B5F6), Color(0xFF1565C0))
                    "placeholder_yellow" -> listOf(Color(0xFFFFF176), Color(0xFFF9A825))
                    "placeholder_purple" -> listOf(Color(0xFFBA68C8), Color(0xFF6A1B9A))
                    "placeholder_pink" -> listOf(Color(0xFFF06292), Color(0xFFAD1457))
                    "placeholder_orange" -> listOf(Color(0xFFFFB74D), Color(0xFFE65100))
                    "placeholder_teal" -> listOf(Color(0xFF4DB6AC), Color(0xFF00695C))
                    else -> listOf(Color(0xFF7986CB), Color(0xFF283593))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(fallbackGrad))
                )
            } else {
                AsyncImage(
                    model = File(currentMoment.imageUri),
                    contentDescription = "CineSlide Photo",
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Translucent Screen Scrim Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 3. Top Floating Status Bar Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(activeColorTheme)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.title.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${Trans.t("progress", lang)} ${project.progressPercent}% • ${Trans.t("streak", lang)} ${project.streak}d",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // 4. Center Controls Playback overlay HUD (fades out contextually to keep view cinematic)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            IconButton(
                onClick = {
                    currentIndex = if (currentIndex - 1 < 0) moments.size - 1 else currentIndex - 1
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(70.dp)
                    .background(activeColorTheme.copy(alpha = 0.85f), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = {
                    currentIndex = (currentIndex + 1) % moments.size
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 5. Bottom Cinematic Subtitles Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live EQ Soundtrack and simple guide instructions at the top of the controller panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = activeColorTheme,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${Trans.t("music_mood", lang)}${activeSoundtrack.name}",
                        color = activeColorTheme,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Beautiful pulsing waveform effect
                    MusicEqualizerVisualizer(isPlaying = isPlaying, activeColor = activeColorTheme)
                }

                Text(
                    text = Trans.t("cineloop_instructions", lang),
                    color = Color.Gray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.End
                )
            }

            // Cinematic Customization Board: Colors grading & music selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Background preset custom controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorGradingFilter.values().forEach { filter ->
                        val isSelected = activeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) activeColorTheme else Color.Black.copy(
                                        alpha = 0.5f
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color.Gray.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { activeFilter = filter }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.name.take(4),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Music beat preset controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MusicSoundtrack.values().forEach { music ->
                        val isSelected = activeSoundtrack == music
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color.Gray.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    activeSoundtrack = music
                                    // Change speed based on music rhythm! Faster speed for techno, slower for ambient lofi
                                    playbackSpeedMillis = when (music) {
                                        MusicSoundtrack.TECHNO -> 2000
                                        MusicSoundtrack.LOFI -> 3500
                                        MusicSoundtrack.AMBIENT -> 5000
                                        MusicSoundtrack.TRIUMPHANT -> 4000
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = music.name.take(4),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Moment Card details placed at the absolute bottom hugging the image edge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(currentMoment.createdAt))
                    Text(
                        text = formattedDate.uppercase(),
                        fontSize = 11.sp,
                        color = activeColorTheme,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${currentMoment.noteText}\"",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // AI Coach subtitles overlay
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD93D),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentMoment.aiFeedback,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicEqualizerVisualizer(isPlaying: Boolean, activeColor: Color) {
    val durationMultiplier = if (isPlaying) 1f else 0f
    
    Row(
        modifier = Modifier.height(15.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barsCount = 7
        for (i in 0 until barsCount) {
            val baseSeconds = 400 + (i * 150)
            val infiniteAnim = rememberInfiniteTransition(label = "VisualizerBar_$i")
            val barHeight by infiniteAnim.animateFloat(
                initialValue = 2.dp.value,
                targetValue = 15.dp.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = (baseSeconds / (durationMultiplier.takeIf { it > 0 } ?: 0.0001f)).toInt(), easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarHeight"
            )

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(if (isPlaying) barHeight.dp else 2.dp)
                    .background(activeColor, RoundedCornerShape(1.dp))
            )
        }
    }
}
