package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.JourneyViewModel
import com.example.data.Moment
import com.example.data.Project
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JourneyViewModel,
    onProjectClick: (String) -> Unit,
    onStatsClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val moments by viewModel.allMoments.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Trigger progress decay checks once on app startup
    LaunchedEffect(Unit) {
        viewModel.triggerDecayCheck()
    }

    // Listen to ViewModel snackbar emissions
    LaunchedEffect(viewModel.snackbarFlow) {
        viewModel.snackbarFlow.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = Color(0xFF6BCB77),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Moment",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White,
                                fontSize = 23.sp
                            )
                            Text(
                                text = "Loop",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF6BCB77),
                                fontSize = 23.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = Trans.t("stats", lang),
                            tint = Color.LightGray
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = Trans.t("settings", lang),
                            tint = Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF6BCB77),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = Trans.t("new_journey", lang),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = Color(0xFF141414)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (projects.isEmpty()) {
                EmptyStateLayout(lang = lang, onCreateClick = { showCreateDialog = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(projects) { project ->
                        val projectMoments = moments.filter { it.projectId == project.id }
                        val latestMoment = projectMoments.maxByOrNull { it.createdAt }

                        ProjectGridCard(
                            project = project,
                            latestMoment = latestMoment,
                            lang = lang,
                            onClick = { onProjectClick(project.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    // Dialog: Create Project
    if (showCreateDialog) {
        CreateProjectDialog(
            lang = lang,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, colorHex, bgPreset, startHour, endHour ->
                viewModel.createProject(title, colorHex, bgPreset, startHour, endHour)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Settings & Language Change
    if (showSettingsDialog) {
        SettingsDialog(
            lang = lang,
            remindersEnabled = remindersEnabled,
            onToggleReminder = { enabled ->
                viewModel.toggleReminder(enabled, context)
            },
            onToggleLanguage = { newLang ->
                viewModel.setLanguage(newLang)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun EmptyStateLayout(lang: String, onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFF1E1E1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFFD93D),
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = Trans.t("empty_state_title", lang),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = Trans.t("empty_state_desc", lang),
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6BCB77)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = Trans.t("empty_state_btn", lang),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProjectGridCard(
    project: Project,
    latestMoment: Moment?,
    lang: String,
    onClick: () -> Unit
) {
    val themeColor = runCatching { Color(android.graphics.Color.parseColor(project.thumbnailColor)) }
        .getOrDefault(Color(0xFF6BCB77))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Visual Color thumbnail header with fallback / preset
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222))
            ) {
                // Determine background display style:
                // 1. If we have a captured moment image, display it prominently!
                if (latestMoment != null) {
                    val isPlaceholder = latestMoment.imageUri.startsWith("placeholder_")
                    if (isPlaceholder) {
                        val fallbackGrad = when (latestMoment.imageUri) {
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
                            model = File(latestMoment.imageUri),
                            contentDescription = "Latest Moment Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    // Latest indicator overlay tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = Trans.t("latest_moment_preview", lang),
                            color = themeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Fall back to preset design style or dynamic gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThemePresets.getPresetCardBrush(project.backgroundImageUri, themeColor))
                    )
                }

                // Streaks Badge
                if (project.streak > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Whatshot,
                                contentDescription = "Streak",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${project.streak} d",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = project.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress indicator details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Trans.t("progress", lang),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "${project.progressPercent}%",
                    color = themeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { project.progressPercent.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = themeColor,
                trackColor = Color(0xFF2E2E2E)
            )
        }
    }
}

@Composable
fun CreateProjectDialog(
    lang: String,
    onDismiss: () -> Unit,
    onCreate: (title: String, colorHex: String, bgPreset: String?, startHour: Int, endHour: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(23) }
    
    // Expanding the colors to 10 beautiful professional gradient pastels
    val colors = listOf(
        "#FF6B6B", "#FFD93D", "#6BCB77", "#4A90E2", "#9C27B0",
        "#FF9F43", "#00B894", "#0984E3", "#E84393", "#6C5CE7"
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }

    // Selectable artistic preset background options
    val presets = listOf(
        "preset_none",
        "preset_fitness",
        "preset_productivity",
        "preset_cooking",
        "preset_spark",
        "preset_nature"
    )
    var selectedPreset by remember { mutableStateOf(presets.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = Trans.t("new_journey", lang),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Trans.t("journey_title", lang)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF6BCB77),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Trans.t("choose_theme", lang),
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable color palettes layout matching M3 design specifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val firstRowColors = colors.take(5)
                    firstRowColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(2.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val secondRowColors = colors.drop(5)
                    secondRowColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(2.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Trans.t("bg_preset", lang),
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display background presets as beautifully structured chips
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.chunked(2).forEach { rowPresets ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPresets.forEach { presetKey ->
                                val isSelected = selectedPreset == presetKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF6BCB77).copy(alpha = 0.15f)
                                            else Color(0xFF2A2A2A)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF6BCB77) else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPreset = presetKey }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Trans.t(presetKey, lang),
                                        color = if (isSelected) Color(0xFF6BCB77) else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Trans.t("widget_time_settings", lang),
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Trans.t("widget_time_desc", lang),
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${Trans.t("start_hour", lang)}: ${startHour}h",
                            color = Color(0xFF6BCB77),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = startHour.toFloat(),
                            onValueChange = { startHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6BCB77),
                                activeTrackColor = Color(0xFF6BCB77),
                                inactiveTrackColor = Color(0xFF444444)
                            )
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${Trans.t("end_hour", lang)}: ${endHour}h",
                            color = Color(0xFFFFD93D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = endHour.toFloat(),
                            onValueChange = { endHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD93D),
                                activeTrackColor = Color(0xFFFFD93D),
                                inactiveTrackColor = Color(0xFF444444)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(Trans.t("cancel", lang), color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreate(title, selectedColor, selectedPreset, startHour, endHour)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6BCB77))
                    ) {
                        Text(Trans.t("create", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    lang: String,
    remindersEnabled: Boolean,
    onToggleReminder: (Boolean) -> Unit,
    onToggleLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = Trans.t("settings", lang),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Daily reminders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Trans.t("daily_reminder", lang),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = Trans.t("reminder_desc", lang),
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { onToggleReminder(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF6BCB77)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = Color.White.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(16.dp))

                // Option 2: Language Switching Configs
                Text(
                    text = Trans.t("language", lang),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vietnamese switch
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (lang == "vi") Color(0xFF6BCB77).copy(alpha = 0.15f)
                                else Color(0xFF2A2A2A)
                            )
                            .border(
                                1.dp,
                                if (lang == "vi") Color(0xFF6BCB77) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleLanguage("vi") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇻🇳 ", fontSize = 14.sp)
                            Text(
                                text = "Tiếng Việt",
                                color = if (lang == "vi") Color(0xFF6BCB77) else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // English switch
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (lang == "en") Color(0xFF6BCB77).copy(alpha = 0.15f)
                                else Color(0xFF2A2A2A)
                            )
                            .border(
                                1.dp,
                                if (lang == "en") Color(0xFF6BCB77) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleLanguage("en") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇬🇧 ", fontSize = 14.sp)
                            Text(
                                text = "English",
                                color = if (lang == "en") Color(0xFF6BCB77) else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6BCB77))
                    ) {
                        Text(Trans.t("done", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
