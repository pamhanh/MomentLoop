package com.example.ui

import android.content.Context
import android.net.Uri
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.JourneyViewModel
import com.example.data.Moment
import com.example.data.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.key
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: JourneyViewModel,
    projectId: String,
    onBack: () -> Unit,
    onNavigateToAddMoment: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val moments by viewModel.selectedMoments.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()

    var selectedDayForBottomSheet by remember { mutableStateOf<CalendarDayInfo?>(null) }
    var activeZoomedIndex by remember { mutableStateOf<Int?>(null) }
    var showCineLoop by remember { mutableStateOf(false) }
    var showExportBottomSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Trigger loading state for this project
    LaunchedEffect(projectId) {
        viewModel.selectProject(projectId)
    }

    // Set up default system camera TakePicture launcher
    val tempFile = remember { File(context.cacheDir, "temp_capture.jpg") }
    val tempUri = remember {
        FileProvider.getUriForFile(
            context,
            "com.aistudio.journeylens.qvtyzb.fileprovider",
            tempFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.setCapturedImageUri(tempUri.toString())
            onNavigateToAddMoment()
        }
    }

    val themeColor = runCatching { Color(android.graphics.Color.parseColor(project?.thumbnailColor ?: "#6BCB77")) }
        .getOrDefault(Color(0xFF6BCB77))

    val bgBrush = ThemePresets.getPresetBrush(project?.backgroundImageUri)

    BackHandler(enabled = activeZoomedIndex != null || selectedDayForBottomSheet != null || showCineLoop || showExportBottomSheet) {
        if (showCineLoop) {
            showCineLoop = false
        } else if (activeZoomedIndex != null) {
            activeZoomedIndex = null
        } else if (showExportBottomSheet) {
            showExportBottomSheet = false
        } else {
            selectedDayForBottomSheet = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = project?.title ?: "...",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = Trans.t("back", lang),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        project?.let { proj ->
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = Trans.t("edit_journey", lang),
                                    tint = themeColor
                                )
                            }
                            IconButton(onClick = { showExportBottomSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = Trans.t("export_title", lang),
                                    tint = themeColor
                                )
                            }
                        }
                        if (moments.isNotEmpty()) {
                            IconButton(onClick = { showCineLoop = true }) {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = Trans.t("cineloop_btn", lang),
                                    tint = themeColor
                                )
                            }
                        }
                        project?.let { proj ->
                            IconButton(onClick = {
                                viewModel.deleteProject(proj)
                                onBack()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = Trans.t("delete_journey", lang),
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        onNavigateToAddMoment()
                    },
                    containerColor = themeColor,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                project?.let { proj ->
                    // Progress stats bar card
                    ProgressHeaderCard(proj = proj, themeColor = themeColor, lang = lang)

                    // Streak Calendar placed above the timeline
                    StreakCalendar(moments = moments, lang = lang) { clickedDay ->
                        selectedDayForBottomSheet = clickedDay
                    }

                    if (moments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Trans.t("add_first_moment", lang),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    } else {
                        // Timeline centered line logic based on progress
                        val progress = proj.progressPercent
                        val timelineLineColor = when {
                            progress <= 33 -> Color(0xFFFF6B6B)
                            progress <= 66 -> Color(0xFFFFD93D)
                            else -> Color(0xFF6BCB77)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            // Drawing the vertical line precisely down the center
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .background(timelineLineColor.copy(alpha = 0.5f))
                                    .align(Alignment.Center)
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
                            ) {
                                itemsIndexed(moments) { index, moment ->
                                    val isEvenIdx = index % 2 == 0
                                    TimelineRowItem(
                                        moment = moment,
                                        isEven = isEvenIdx,
                                        lineColor = timelineLineColor,
                                        lang = lang,
                                        onClick = {
                                            activeZoomedIndex = index
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog && project != null) {
            EditProjectDialog(
                lang = lang,
                project = project!!,
                onDismiss = { showEditDialog = false },
                onSave = { title, colorHex, bgPreset, startHour, endHour ->
                    viewModel.updateProjectSettings(project!!, title, colorHex, bgPreset, startHour, endHour)
                    showEditDialog = false
                }
            )
        }

        // Immersive CineLoop Player Overlay
        if (showCineLoop && project != null) {
            CineLoopDialog(
                project = project!!,
                moments = moments,
                lang = lang,
                onDismiss = { showCineLoop = false }
            )
        }

        // Custom Slide-Up Bottom Sheet Display
        AnimatedVisibility(
            visible = selectedDayForBottomSheet != null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            // Scrim (dimmed background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedDayForBottomSheet = null
                    }
            )
        }

        AnimatedVisibility(
            visible = selectedDayForBottomSheet != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDayForBottomSheet?.let { day ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) {}, // Consume clicks on sheet itself
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp, top = 8.dp)
                    ) {
                        // Small aesthetic drag handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Khoảnh khắc ngày ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(day.date)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (day.moments.size == 1) {
                            val m = day.moments.first()
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val isPlaceholder = m.imageUri.startsWith("placeholder_")
                                if (isPlaceholder) {
                                    val fallbackGrad = when (m.imageUri) {
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
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Brush.linearGradient(fallbackGrad))
                                    )
                                } else {
                                    AsyncImage(
                                        model = File(m.imageUri),
                                        contentDescription = "Ảnh khoảnh khắc",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = m.noteText,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.createdAt))
                                Text(
                                    text = "Lúc $timeText",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(day.moments) { m ->
                                    Card(
                                        modifier = Modifier
                                            .width(280.dp)
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            val isPlaceholder = m.imageUri.startsWith("placeholder_")
                                            if (isPlaceholder) {
                                                val fallbackGrad = when (m.imageUri) {
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
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Brush.linearGradient(fallbackGrad))
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = File(m.imageUri),
                                                    contentDescription = "Ảnh khoảnh khắc",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = m.noteText,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.createdAt))
                                            Text(
                                                text = "Lúc $timeText",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Zoomed chronological fullscreen pager overlay
        AnimatedVisibility(
            visible = activeZoomedIndex != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (activeZoomedIndex != null) {
                key(activeZoomedIndex) {
                    ZoomedTimelinePhotoViewer(
                        initialIndex = activeZoomedIndex!!,
                        moments = moments,
                        onDismiss = { activeZoomedIndex = null },
                        onUpdateNote = { moment, newNote ->
                            viewModel.updateMomentNote(moment, newNote)
                        }
                    )
                }
            }
        }

        // Export Sheet display overlay
        AnimatedVisibility(
            visible = showExportBottomSheet,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showExportBottomSheet = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showExportBottomSheet,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            project?.let { proj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.4f), CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = Trans.t("export_title", lang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (lang == "vi") "Chọn hình thức xuất bản hành trình của bạn" else "Select how you want to export your journey",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Option A: Share as Slide Story (HTML)
                        val coroutineScope = rememberCoroutineScope()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportBottomSheet = false
                                    if (moments.isEmpty()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(Trans.t("export_not_enough_moments", lang))
                                        }
                                        return@clickable
                                    }
                                    
                                    // Run HTML creation in coroutine background
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val html = generateJourneyHtml(context, proj, moments, lang)
                                            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                            val cleanTitle = proj.title.filter { it.isLetterOrDigit() || it == '_' || it == ' ' }.replace(" ", "_")
                                            val file = File(downloadsDir, "Journey_${cleanTitle}_${System.currentTimeMillis()}.html")
                                            file.writeText(html)
                                            
                                            // Open share sheet on main thread
                                            withContext(Dispatchers.Main) {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/html"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    putExtra(Intent.EXTRA_SUBJECT, "${proj.title} - ${Trans.t("cineloop_title", lang)}")
                                                    putExtra(Intent.EXTRA_TEXT, "Check out my ${proj.title} progress story. Powered by MomentLoop!")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share Slide Story"))
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                snackbarHostState.showSnackbar(Trans.t("export_error", lang))
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF4A90E2).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Slideshow,
                                        contentDescription = null,
                                        tint = Color(0xFF4A90E2),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Trans.t("export_slide_story", lang),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Trans.t("export_slide_story_desc", lang),
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option B: Copy Summary Text
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportBottomSheet = false
                                    // Generate text
                                    val textSummary = buildString {
                                        append("${proj.title} Journey Summary\n")
                                        append("Progress: ${proj.progressPercent}%  |  ${moments.size} moments  |  ${proj.streak} day streak\n")
                                        append("——\n")
                                        
                                        // Sorted chronologically
                                        val sorted = moments.sortedBy { it.createdAt }
                                        val sdf = SimpleDateFormat(Trans.t("date_format", lang), Locale.getDefault())
                                        sorted.forEach { m ->
                                            val dateStr = sdf.format(Date(m.createdAt))
                                            append("[$dateStr] ${m.noteText}\n")
                                        }
                                        
                                        append("——\n")
                                        append("Exported from MomentLoop")
                                    }
                                    
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Journey Summary", textSummary)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(Trans.t("copied_toast", lang))
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF6BCB77).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF6BCB77),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Trans.t("export_summary_text", lang),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Trans.t("export_summary_text_desc", lang),
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressHeaderCard(proj: Project, themeColor: Color, lang: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Trans.t("goal_reached", lang),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = Trans.t("overall_progress", lang),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${proj.progressPercent}%",
                    color = themeColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { proj.progressPercent.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = themeColor,
                trackColor = Color(0xFF2E2E2E)
            )

            if (proj.streak > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Trans.t("streak_days", lang).replace("{count}", proj.streak.toString()),
                        color = Color(0xFFFFB74D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineRowItem(
    moment: Moment,
    isEven: Boolean,
    lineColor: Color,
    lang: String,
    onClick: () -> Unit
) {
    // Entrance animation using graphic calculations on launch
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 100),
        label = "Alpha"
    )

    val animatedSlideY by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SlideY"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = animatedAlpha,
                translationY = animatedSlideY
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEven) {
            // Moment card left
            Box(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxWidth()
            ) {
                MomentCard(moment = moment, onClick = onClick)
            }

            // Central Bullet points
            Box(
                modifier = Modifier.weight(0.12f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(lineColor)
                )
            }

            // Blank spacer content on the opposite side
            Spacer(modifier = Modifier.weight(0.44f))
        } else {
            // Blank spacer content on the opposite side
            Spacer(modifier = Modifier.weight(0.44f))

            // Central Bullet points
            Box(
                modifier = Modifier.weight(0.12f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(lineColor)
                )
            }

            // Moment card right
            Box(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxWidth()
            ) {
                MomentCard(moment = moment, onClick = onClick)
            }
        }
    }
}

@Composable
fun MomentCard(moment: Moment, onClick: () -> Unit) {
    val dateText = remember(moment.createdAt) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(moment.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Photo area with Overlaid User Note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val isPlaceholder = moment.imageUri.startsWith("placeholder_")
                
                if (isPlaceholder) {
                    val fallbackGrad = when (moment.imageUri) {
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
                        model = File(moment.imageUri),
                        contentDescription = "Ảnh khoảnh khắc",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark Scrim gradient overlay to guarantee text readability in white-accent
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 80f
                            )
                        )
                )

                // Overlaid white bold text note
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = moment.noteText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. AI Coach section & metadata below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Score badge and Coach title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coach Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD93D),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Coach",
                            color = Color(0xFFFFD93D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Score tag
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C2C2C), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Điểm: ${moment.aiScore}/10",
                            color = Color(0xFF6BCB77),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Coach Feedback Text
                Text(
                    text = moment.aiFeedback,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Date/Time Stamp
                Text(
                    text = dateText,
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

data class CalendarDayInfo(
    val date: Date,
    val dateString: String,
    val weekdayAbbr: String,
    val hasMoments: Boolean,
    val isToday: Boolean,
    val moments: List<Moment>
)

data class StreakData(
    val count: Int,
    val showClockIcon: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakCalendar(
    moments: List<Moment>,
    lang: String,
    onDayClick: (CalendarDayInfo) -> Unit
) {
    val dailyMomentsMap = remember(moments) {
        moments.groupBy { moment ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(moment.createdAt))
        }
    }

    val calendarDays = remember(moments) {
        val daysList = mutableListOf<CalendarDayInfo>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Generate dates from 89 days ago to today
        for (i in 89 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            
            val dateString = sdf.format(cal.time)
            val hasMoments = dailyMomentsMap.containsKey(dateString)
            val isToday = i == 0
            
            val weekdayAbbr = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                Calendar.SUNDAY -> "S"
                else -> ""
            }
            
            daysList.add(
                CalendarDayInfo(
                    date = cal.time,
                    dateString = dateString,
                    weekdayAbbr = weekdayAbbr,
                    hasMoments = hasMoments,
                    isToday = isToday,
                    moments = dailyMomentsMap[dateString] ?: emptyList()
                )
            )
        }
        daysList
    }

    val streakInfo = remember(dailyMomentsMap) {
        val todaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = todaySdf.format(Date())
        
        val todayHasMoment = dailyMomentsMap.containsKey(todayStr)
        
        var streakCount = 0
        var showClockIcon = false
        
        if (todayHasMoment) {
            val currentCal = Calendar.getInstance()
            while (true) {
                val dateStr = todaySdf.format(currentCal.time)
                if (dailyMomentsMap.containsKey(dateStr)) {
                    streakCount++
                    currentCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else {
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = todaySdf.format(yesterdayCal.time)
            val yesterdayHasMoment = dailyMomentsMap.containsKey(yesterdayStr)
            
            if (yesterdayHasMoment) {
                showClockIcon = true
                val currentCal = yesterdayCal
                while (true) {
                    val dateStr = todaySdf.format(currentCal.time)
                    if (dailyMomentsMap.containsKey(dateStr)) {
                        streakCount++
                        currentCal.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            } else {
                streakCount = 0
            }
        }
        
        StreakData(count = streakCount, showClockIcon = showClockIcon)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(calendarDays) {
        if (calendarDays.isNotEmpty()) {
            listState.scrollToItem(calendarDays.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Streak Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = Trans.t("streak_card", lang).replace("{count}", streakInfo.count.toString()),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (streakInfo.showClockIcon) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Chờ khoảnh khắc hôm nay",
                        tint = Color(0xFFFFD93D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Days grid scroll
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(calendarDays) { dayInfo ->
                    val isClickable = dayInfo.moments.isNotEmpty()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = isClickable) {
                                onDayClick(dayInfo)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = dayInfo.weekdayAbbr,
                            color = if (dayInfo.isToday) Color.White else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = if (dayInfo.isToday) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (dayInfo.isToday) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                    .padding(1.5.dp)
                                    .background(
                                        if (dayInfo.hasMoments) Color(0xFF4ECDC4) else Color(0xFF2A2A2A),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (dayInfo.hasMoments) Color(0xFF4ECDC4) else Color(0xFF2A2A2A),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomedTimelinePhotoViewer(
    initialIndex: Int,
    moments: List<Moment>,
    onDismiss: () -> Unit,
    onUpdateNote: (Moment, String) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { moments.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)) // Immersive deep dark background
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val moment = moments.getOrNull(page)
            if (moment != null) {
                ZoomedPageItem(
                    moment = moment,
                    onUpdateNote = onUpdateNote
                )
            }
        }

        // Circular background closed icon on top end
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Thoát xem ảnh",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ZoomedPageItem(
    moment: Moment,
    onUpdateNote: (Moment, String) -> Unit
) {
    var noteText by remember(moment.id) { mutableStateOf(moment.noteText) }
    var isEditing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Fits picture to full dimensions screen
        val isPlaceholder = moment.imageUri.startsWith("placeholder_")
        if (isPlaceholder) {
            val fallbackGrad = when (moment.imageUri) {
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
                model = File(moment.imageUri),
                contentDescription = "Ảnh khoảnh khắc lớn",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
        }

        // Dark gradient from bottom to 60% of the screen height to protect readability under white labels
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
        )

        // Metadata and edit fields at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Date & Score info header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(Date(moment.createdAt)),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${moment.aiScore}/10 điểm",
                                color = Color(0xFF6BCB77),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable notes area
                    if (isEditing) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Chỉnh sửa ghi chú của bạn") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFF6BCB77),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                isEditing = false
                                noteText = moment.noteText // Cancel edits restores original
                            }) {
                                Text("Huỷ", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onUpdateNote(moment, noteText)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6BCB77))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Lưu thay đổi",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lưu", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditing = true }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Sửa ghi chú",
                                        tint = Color(0xFF6BCB77),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ghi chú của bạn:",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Chạm để sửa 📝",
                                    color = Color(0xFF6BCB77),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (noteText.isNotBlank()) noteText else "Chưa có ghi chú. Chạm để nhập...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Coach advice review
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD93D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Huấn luyện viên AI Coach nhận xét",
                            color = Color(0xFFFFD93D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = moment.aiFeedback,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance indicator
            Text(
                text = "↑↓ Vuốt lên / xuống để chuyển khoảnh khắc dòng thời gian",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Function to generate customized self-contained scroll-snap HTML journey representation
fun generateJourneyHtml(context: Context, project: Project, moments: List<Moment>, lang: String): String {
    // Sort chronologically (oldest first)
    val chronologicalMoments = moments.sortedBy { it.createdAt }
    
    // Choose header preset gradient matching project's background uri
    val presetGradient = when (project.backgroundImageUri) {
        "preset_fitness" -> "linear-gradient(135deg, #1f1c2c, #928dab)"
        "preset_productivity" -> "linear-gradient(135deg, #2c3e50, #000000)"
        "preset_cooking" -> "linear-gradient(135deg, #e65c00, #F9D423)"
        "preset_spark" -> "linear-gradient(135deg, #0f2027, #203a43, #2c5364)"
        "preset_nature" -> "linear-gradient(135deg, #11998e, #38ef7d)"
        else -> "linear-gradient(135deg, #3a7bd5, #3a6073)"
    }
    
    val totalSlidesCount = chronologicalMoments.size + 2 // Header + Moments + Footer
    
    // Generate side dots
    val dotsHtml = StringBuilder()
    for (i in 0 until totalSlidesCount) {
        dotsHtml.append("        <div class='story-dot ${if (i == 0) "active" else ""}' onclick='scrollToSlide($i)'></div>\n")
    }
    
    val slidesHtml = StringBuilder()
    val sdf = SimpleDateFormat(Trans.t("date_format", lang), Locale.getDefault())
    
    chronologicalMoments.forEachIndexed { idx, m ->
        val slideIdx = idx + 1
        val dateText = sdf.format(Date(m.createdAt))
        val isPlaceholder = m.imageUri.startsWith("placeholder_")
        
        val mediaContent = if (isPlaceholder) {
            val colors = when (m.imageUri) {
                "placeholder_red" -> "#E57373, #C62828"
                "placeholder_green" -> "#81C784, #2E7D32"
                "placeholder_blue" -> "#64B5F6, #1565C0"
                "placeholder_yellow" -> "#FFF176, #F9A825"
                "placeholder_purple" -> "#BA68C8, #6A1B9A"
                "placeholder_pink" -> "#F06292, #AD1457"
                "placeholder_orange" -> "#FFB74D, #E65100"
                "placeholder_teal" -> "#4DB6AC, #00695C"
                else -> "#7986CB, #283593"
            }
            "            <div class=\"slide-media\" style=\"background: linear-gradient(135deg, $colors)\"></div>"
        } else {
            val b64Str = getResizedBase64Image(m.imageUri)
            val base64Data = if (b64Str.isNotEmpty()) "data:image/jpeg;base64,$b64Str" else ""
            if (base64Data.isNotEmpty()) {
                "            <div class=\"slide-media\"><img src=\"$base64Data\" alt=\"Moment Photo\"></div>"
            } else {
                "            <div class=\"slide-media\" style=\"background: linear-gradient(135deg, #2c3e50, #4ca1af)\"></div>"
            }
        }
        
        slidesHtml.append("""
        <!-- Slide $slideIdx -->
        <section class="slide" id="slide-$slideIdx">
$mediaContent
            <div class="slide-scrim"></div>
            <div class="slide-content">
                <div class="slide-note">"${m.noteText}"</div>
                <div class="slide-time">$dateText</div>
            </div>
            <div class="divider"></div>
        </section>
        """.trimIndent()).append("\n")
    }
    
    return """
<!DOCTYPE html>
<html lang="${if (lang == "vi") "vi" else "en"}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Journey Story: ${project.title}</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }
        body, html {
            height: 100%;
            overflow: hidden;
            background-color: #0D0D0D;
            color: #FFFFFF;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        /* Scroll snap container */
        .story-container {
            height: 100%;
            overflow-y: scroll;
            scroll-snap-type: y mandatory;
            scroll-behavior: smooth;
        }
        /* Full-screen slides */
        .slide {
            width: 100%;
            height: 100vh;
            scroll-snap-align: start;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }
        /* Normal slides and header slide */
        .header-slide {
            background-color: #121212;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 2rem;
            text-align: center;
        }
        .header-bg {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            opacity: 0.15;
            filter: blur(20px);
            z-index: 1;
        }
        .header-content {
            position: relative;
            z-index: 2;
            max-width: 600px;
        }
        .header-title {
            font-size: 3rem;
            font-weight: 900;
            margin-bottom: 1rem;
            letter-spacing: -0.05em;
            background: linear-gradient(135deg, #6BCB77, #4A90E2);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        .badge-row {
            display: flex;
            gap: 10px;
            justify-content: center;
            margin-bottom: 2rem;
        }
        .badge {
            background: rgba(255, 255, 255, 0.1);
            border: 1.5px solid rgba(255, 255, 255, 0.15);
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 0.9rem;
            font-weight: bold;
            display: flex;
            align-items: center;
            gap: 4px;
        }
        .badge.accent {
            background: rgba(107, 203, 119, 0.15);
            border-color: #6BCB77;
            color: #6BCB77;
        }
        .badge.streak {
            background: rgba(255, 152, 0, 0.15);
            border-color: #FF9800;
            color: #FF9800;
        }
        .scroll-down {
            margin-top: 3rem;
            font-size: 0.85rem;
            color: #888888;
            letter-spacing: 0.1em;
            text-transform: uppercase;
            animation: bounce 2s infinite;
        }
        @keyframes bounce {
            0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
            40% { transform: translateY(-8px); }
            60% { transform: translateY(-4px); }
        }
        /* Media / Image container */
        .slide-media {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            z-index: 1;
        }
        .slide-media img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
        /* Gradient Scrim Overlay */
        .slide-scrim {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            background: linear-gradient(to top, rgba(13, 13, 13, 1) 0%, rgba(13, 13, 13, 0.5) 25%, rgba(13, 13, 13, 0.2) 60%, rgba(13, 13, 13, 0.6) 100%);
            z-index: 2;
        }
        /* Slide Content over Scrim */
        .slide-content {
            position: absolute;
            bottom: 8%;
            left: 5%;
            right: 5%;
            z-index: 3;
            max-width: 600px;
            margin: 0 auto;
            text-align: center;
        }
        .slide-note {
            font-size: 1.6rem;
            font-weight: 700;
            line-height: 1.4;
            color: #ffffff;
            margin-bottom: 0.8rem;
            text-shadow: 0 4px 12px rgba(0,0,0,0.5);
        }
        .slide-time {
            font-size: 0.9rem;
            color: #999999;
            font-weight: 500;
            letter-spacing: 0.05em;
        }
        /* Subtle divider between slides */
        .divider {
            position: absolute;
            bottom: 0;
            left: 10%;
            right: 10%;
            height: 1px;
            background: rgba(255, 255, 255, 0.08);
            z-index: 4;
        }
        /* Floating Indicator on Right Side */
        .story-dots {
            position: fixed;
            right: 24px;
            top: 50%;
            transform: translateY(-50%);
            display: flex;
            flex-direction: column;
            gap: 12px;
            z-index: 10;
        }
        .story-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.3);
            cursor: pointer;
            transition: all 0.3s ease;
        }
        .story-dot.active {
            background: #6BCB77;
            transform: scale(1.4);
            box-shadow: 0 0 10px #6BCB77;
        }
        /* Footer slide styling */
        .footer-slide {
            background-color: #080808;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 2rem;
            color: #888888;
        }
        .footer-branding {
            font-size: 1.2rem;
            font-weight: 800;
            color: #FFFFFF;
            margin-bottom: 0.5rem;
            letter-spacing: -0.03em;
        }
        .footer-branding span {
            color: #6BCB77;
        }
        /* Hide scrollbars but keep functionality */
        ::-webkit-scrollbar {
            display: none;
        }
    </style>
</head>
<body>

    <div class="story-dots" id="storyDots">
$dotsHtml    </div>

    <div class="story-container" id="storyContainer" onscroll="handleScroll()">
        <!-- Slide 0: Header -->
        <section class="slide header-slide" id="slide-0">
            <div class="header-bg" style="background: $presetGradient"></div>
            <div class="header-content">
                <div class="header-title">${project.title}</div>
                <div class="badge-row">
                    <span class="badge accent">🎯 Progress: ${project.progressPercent}%</span>
                    <span class="badge streak">🔥 ${project.streak} Day's Streak</span>
                </div>
                <div class="scroll-down">Scroll Down To Begin</div>
            </div>
            <div class="divider"></div>
        </section>

$slidesHtml
        <!-- Final slide: Footer Branding -->
        <section class="slide footer-slide" id="slide-last">
            <p class="footer-branding">Moment<span>Loop</span></p>
            <p style="font-size: 0.9rem; margin-top: 5px;">Your Journey, Beautifully Recorded.</p>
        </section>
    </div>

    <script>
        const container = document.getElementById('storyContainer');
        const dots = document.querySelectorAll('.story-dot');
        const slideCount = $totalSlidesCount;

        function handleScroll() {
            const scrollTop = container.scrollTop;
            const height = window.innerHeight;
            const activeIndex = Math.round(scrollTop / height);
            
            const dotElements = document.getElementsByClassName('story-dot');
            for (let i = 0; i < dotElements.length; i++) {
                if (i === activeIndex) {
                    dotElements[i].classList.add('active');
                } else {
                    dotElements[i].classList.remove('active');
                }
            }
        }

        function scrollToSlide(index) {
            const height = window.innerHeight;
            container.scrollTo({
                top: index * height,
                behavior: 'smooth'
            });
        }
    </script>
</body>
</html>
    """.trimIndent()
}

fun getResizedBase64Image(filePath: String, maxDimension: Int = 800): String {
    val file = File(filePath)
    if (!file.exists()) return ""
    
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)
        
        var inSampleSize = 1
        val width = options.outWidth
        val height = options.outHeight
        if (width > maxDimension || height > maxDimension) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return ""
        
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

@Composable
fun EditProjectDialog(
    lang: String,
    project: Project,
    onDismiss: () -> Unit,
    onSave: (title: String, colorHex: String, bgPreset: String?, startHour: Int, endHour: Int) -> Unit
) {
    var title by remember { mutableStateOf(project.title) }

    val colors = listOf(
        "#FF6B6B", "#FFD93D", "#6BCB77", "#4A90E2", "#9C27B0",
        "#FF9F43", "#00B894", "#0984E3", "#E84393", "#6C5CE7"
    )
    var selectedColor by remember { mutableStateOf(project.thumbnailColor) }

    val presets = listOf(
        "preset_none",
        "preset_fitness",
        "preset_productivity",
        "preset_cooking",
        "preset_spark",
        "preset_nature"
    )
    var selectedPreset by remember { mutableStateOf(project.backgroundImageUri ?: "preset_none") }

    var startHour by remember { mutableStateOf(project.widgetShowStartHour) }
    var endHour by remember { mutableStateOf(project.widgetShowEndHour) }

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
                    text = Trans.t("edit_journey", lang),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Trans.t("journey_title_edit", lang)) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val firstRowColors = colors.take(5)
                    firstRowColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
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
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.chunked(3).forEach { rowPresets ->
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
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Trans.t(presetKey, lang),
                                        color = if (isSelected) Color(0xFF6BCB77) else Color.LightGray,
                                        fontSize = 9.sp,
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
                                onSave(
                                    title,
                                    selectedColor,
                                    if (selectedPreset == "preset_none") null else selectedPreset,
                                    startHour,
                                    endHour
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6BCB77))
                    ) {
                        Text(Trans.t("done", lang), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

