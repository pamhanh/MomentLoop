package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project?.title ?: "Đang tải...",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    project?.let { proj ->
                        IconButton(onClick = {
                            viewModel.deleteProject(proj)
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xoá hành trình",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Start camera capture
                    // Prepare temp file and execute contract
                    try {
                        if (tempFile.exists()) tempFile.delete()
                        tempFile.createNewFile()
                        cameraLauncher.launch(tempUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    contentDescription = "Chụp khoảnh khắc",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            project?.let { proj ->
                // Progress stats bar card
                ProgressHeaderCard(proj = proj, themeColor = themeColor)

                if (moments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chạm vào máy ảnh để thêm khoảnh khắc đầu tiên hằng ngày",
                            color = Color.Gray,
                            fontSize = 15.sp,
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
                                .background(timelineLineColor)
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
                                    lineColor = timelineLineColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressHeaderCard(proj: Project, themeColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
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
                        text = "Mục tiêu: vươn tới 100%",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Tiến độ tổng thể",
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
                        text = "Duy trì liên tục: ${proj.streak} ngày hằng ngày! 🔥",
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
    lineColor: Color
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
                MomentCard(moment = moment)
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
                MomentCard(moment = moment)
            }
        }
    }
}

@Composable
fun MomentCard(moment: Moment) {
    val dateText = remember(moment.createdAt) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        sdf.format(Date(moment.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
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
                                startY = 100f
                            )
                        )
                )

                // Overlaid white bold text note
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = moment.noteText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Timestamp details below
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = dateText,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}
