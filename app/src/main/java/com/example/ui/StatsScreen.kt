package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.JourneyViewModel
import com.example.api.AppInsight
import com.example.api.GeminiClient
import com.example.data.Moment
import com.example.data.Project
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: JourneyViewModel,
    onBack: () -> Unit
) {
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val moments by viewModel.allMoments.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Biểu đồ hoạt động", "So sánh hành trình", "Nhận xét AI coach")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thống kê tổng hợp",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant primary tab row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF121212),
                contentColor = Color(0xFF6BCB77),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF6BCB77)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body rendering according to the selected tab
            Crossfade(
                targetState = selectedTabIndex,
                label = "StatsTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ActivityChartTab(moments = moments, projects = projects)
                    1 -> ProjectComparisonTab(projects = projects, moments = moments)
                    2 -> InsightsTab(projects = projects, moments = moments)
                }
            }
        }
    }
}

// Helper to determine project colors according to user specification
fun getProjectColor(projectTitle: String, colorHex: String): Color {
    val titleLower = projectTitle.lowercase(Locale.getDefault())
    return when {
        titleLower.contains("fitness") || titleLower.contains("tập thể dục") || titleLower.contains("gym") -> Color(0xFFFF6B6B)
        titleLower.contains("upwork") || titleLower.contains("lập trình") || titleLower.contains("code") -> Color(0xFF4ECDC4)
        titleLower.contains("cooking") || titleLower.contains("nấu ăn") || titleLower.contains("ăn uống") -> Color(0xFFFFD93D)
        else -> {
            runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                .getOrDefault(Color(0xFF6BCB77))
        }
    }
}

// ----------------------------------------------------
// TAB 1: Activity Chart (Last 30 Days Stacked Bar Chart)
// ----------------------------------------------------
@Composable
fun ActivityChartTab(
    moments: List<Moment>,
    projects: List<Project>
) {
    // Generate dates for the last 30 days
    val last30DaysStrings = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until 30) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Group moments by formatted creation date
    val momentDayGroups = remember(moments) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        moments.groupBy { sdf.format(Date(it.createdAt)) }
    }

    var selectedDayIndex by remember { mutableStateOf(29) } // default to today (the last index)

    val selectedDayString = last30DaysStrings.getOrNull(selectedDayIndex) ?: ""
    val selectedDayMoments = remember(selectedDayString, moments) {
        momentDayGroups[selectedDayString] ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Số khoảnh khắc 30 ngày qua",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (moments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có dữ liệu khoảnh khắc. Hãy ghi lại kết quả để hiển thị biểu đồ!",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Custom Canvas components for Stacked Bar Chart
                        var showTapCoordinates by remember { mutableStateOf<Offset?>(null) }
                        
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            val canvasWidth = constraints.maxWidth.toFloat()
                            val canvasHeight = constraints.maxHeight.toFloat()

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(last30DaysStrings) {
                                        detectTapGestures { offset ->
                                            showTapCoordinates = offset
                                            val indexWidth = canvasWidth / 30f
                                            val clicked = (offset.x / indexWidth)
                                                .toInt()
                                                .coerceIn(0, 29)
                                            selectedDayIndex = clicked
                                        }
                                    }
                            ) {
                                val barCount = 30
                                val spacingRatio = 0.25f // fraction of available space allocated to gaps
                                val widthPerItem = canvasWidth / barCount
                                val barWidth = widthPerItem * (1f - spacingRatio)
                                val barGap = widthPerItem * spacingRatio

                                // Find max count on any single day to adjust scale dynamically
                                val maxOnSingleDay = last30DaysStrings.maxOfOrNull { dayStr ->
                                    momentDayGroups[dayStr]?.size ?: 0
                                }?.coerceAtLeast(4) ?: 4

                                for (i in 0 until barCount) {
                                    val dayStr = last30DaysStrings[i]
                                    val curDayMoments = momentDayGroups[dayStr] ?: emptyList()

                                    val xOffset = i * widthPerItem + (barGap / 2f)

                                    if (i == selectedDayIndex) {
                                        // Highlight background of chosen bar
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.1f),
                                            topLeft = Offset(i * widthPerItem, 0f),
                                            size = Size(widthPerItem, canvasHeight)
                                        )
                                    }

                                    // Draw stacked blocks
                                    var cumulativeHeight = 0f
                                    
                                    // Group current day moments by project
                                    val projectGroups = curDayMoments.groupBy { it.projectId }
                                    
                                    if (curDayMoments.isEmpty()) {
                                        // Draw a tiny placeholder dot at the bottom for inactive days
                                        drawCircle(
                                            color = Color.Gray.copy(alpha = 0.3f),
                                            radius = 2.dp.toPx(),
                                            center = Offset(xOffset + (barWidth / 2f), canvasHeight - 4.dp.toPx())
                                        )
                                    } else {
                                        projectGroups.forEach { (projId, list) ->
                                            val proj = projects.firstOrNull { it.id == projId }
                                            val color = getProjectColor(
                                                projectTitle = proj?.title ?: "Unknown",
                                                colorHex = proj?.thumbnailColor ?: "#6BCB77"
                                            )
                                            val itemHeight = (list.size.toFloat() / maxOnSingleDay) * canvasHeight
                                            
                                            drawRect(
                                                color = color,
                                                topLeft = Offset(xOffset, canvasHeight - cumulativeHeight - itemHeight),
                                                size = Size(barWidth, itemHeight)
                                            )
                                            cumulativeHeight += itemHeight
                                        }
                                    }
                                }

                                // Draw baseline axis line
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(0f, canvasHeight),
                                    end = Offset(canvasWidth, canvasHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chart legends
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        projects.forEach { proj ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(getProjectColor(proj.title, proj.thumbnailColor))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = proj.title,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Details list header for the selected day in Vietnam timezone locale
        item {
            val dateLabel = remember(selectedDayString) {
                if (selectedDayString.isNotBlank()) {
                    try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDayString)
                        if (parsed != null) {
                            SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault()).format(parsed)
                        } else {
                            selectedDayString
                        }
                    } catch (e: Exception) {
                        selectedDayString
                    }
                } else {
                    "Hôm nay"
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dòng thời gian ngày ($dateLabel)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedDayMoments.size} khoảnh khắc",
                        color = Color(0xFF6BCB77),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List selected day's moments
        if (selectedDayMoments.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Ngày này chưa có mốc tiến trình nào được thêm.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(selectedDayMoments) { moment ->
                val associatedProject = projects.firstOrNull { it.id == moment.projectId }
                val projectColor = getProjectColor(
                    projectTitle = associatedProject?.title ?: "Khác",
                    colorHex = associatedProject?.thumbnailColor ?: "#6BCB77"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini image preview or color icon
                        val isPlaceholder = moment.imageUri.startsWith("placeholder_")
                        if (!isPlaceholder && File(moment.imageUri).exists()) {
                            AsyncImage(
                                model = File(moment.imageUri),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(projectColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = projectColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = associatedProject?.title ?: "Dự án cũ",
                                color = projectColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = moment.noteText.ifBlank { "(Không có ghi chú)" },
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Score Badge
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(projectColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${moment.aiScore}/10",
                                    color = projectColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(moment.createdAt))
                            Text(
                                text = timeStr,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: Project Comparison
// ----------------------------------------------------
@Composable
fun ProjectComparisonTab(
    projects: List<Project>,
    moments: List<Moment>
) {
    var selectedMetricIndex by remember { mutableStateOf(0) }
    val metricsList = listOf("Tiến trình %", "Khoảnh khắc đã ghi", "Chuỗi ngày hiện tại")

    val projectMomentsCounts = remember(projects, moments) {
        projects.associate { proj ->
            proj.id to moments.count { it.projectId == proj.id }
        }
    }

    // Sort projects by selected metric value for a clean staircase view
    val sortedProjects = remember(projects, selectedMetricIndex, projectMomentsCounts) {
        when (selectedMetricIndex) {
            0 -> projects.sortedByDescending { it.progressPercent }
            1 -> projects.sortedByDescending { projectMomentsCounts[it.id] ?: 0 }
            else -> projects.sortedByDescending { it.streak }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Metric controller tab headers rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metricsList.forEachIndexed { idx, label ->
                    val isSelected = selectedMetricIndex == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF6BCB77) else Color(0xFF1E1E1E))
                            .clickable { selectedMetricIndex = idx }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            // Horizontal bar container card comparison
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "So sánh bằng biểu đồ thanh ngang",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (projects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có hành trình mục tiêu nào cần hiển thị.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Gather maximum value for dynamic scaling of bars
                        val maxVal = when (selectedMetricIndex) {
                            0 -> 100f
                            1 -> (projectMomentsCounts.values.maxOrNull() ?: 1).toFloat().coerceAtLeast(1f)
                            else -> (projects.maxOfOrNull { it.streak } ?: 1).toFloat().coerceAtLeast(1f)
                        }

                        sortedProjects.forEach { proj ->
                            val value = when (selectedMetricIndex) {
                                0 -> proj.progressPercent.toFloat()
                                1 -> (projectMomentsCounts[proj.id] ?: 0).toFloat()
                                else -> proj.streak.toFloat()
                            }

                            val projectColor = getProjectColor(proj.title, proj.thumbnailColor)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = proj.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = when (selectedMetricIndex) {
                                            0 -> "${value.toInt()}%"
                                            1 -> "${value.toInt()} lần"
                                            else -> "${value.toInt()} ngày"
                                        },
                                        color = projectColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Render custom horizontal progress rail
                                val progressFraction = (value / maxVal).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2C2C2C))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(projectColor, projectColor.copy(alpha = 0.6f))
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary table title card
        item {
            Text(
                text = "Bảng so sánh chi tiết",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            // Scrollable detailed horizontal/vertical table Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C2C2C))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Hành trình", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                        Text(text = "Tiến độ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                        Text(text = "Nhật ký", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                        Text(text = "Chuỗi", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(text = "Gần nhất", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }

                    // Body rows
                    if (projects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Chưa có dự án", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        projects.forEach { proj ->
                            val projMoments = moments.filter { it.projectId == proj.id }
                            val momentCount = projMoments.size
                            val lastActiveText = if (projMoments.isNotEmpty()) {
                                val maxTime = projMoments.maxOf { it.createdAt }
                                val diff = System.currentTimeMillis() - maxTime
                                when {
                                    diff < 60 * 60 * 1000L -> "Hôm nay"
                                    diff < 24 * 60 * 60 * 1000L -> "Gần đây"
                                    diff < 2 * 24 * 60 * 60 * 1000L -> "Hôm qua"
                                    else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(maxTime))
                                }
                            } else {
                                "Chưa có"
                            }

                            val projectColor = getProjectColor(proj.title, proj.thumbnailColor)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.8f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(projectColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = proj.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${proj.progressPercent}%",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1.1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$momentCount",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1.1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${proj.streak}đ",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = lastActiveText,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: Insights (Gemini AI analysis)
// ----------------------------------------------------
@Composable
fun InsightsTab(
    projects: List<Project>,
    moments: List<Moment>
) {
    var insights by remember { mutableStateOf<List<AppInsight>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val serializedData = remember(projects, moments) {
        projects.joinToString(separator = "\n") { project ->
            val projectMoments = moments.filter { it.projectId == project.id }
            val count = projectMoments.size
            val lastActiveStr = if (projectMoments.isNotEmpty()) {
                val maxTime = projectMoments.maxOf { it.createdAt }
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(maxTime))
            } else {
                "chưa có"
            }
            "Hành trình: ${project.title}, Tiến độ: ${project.progressPercent}%, Số khoảnh khắc: $count, Chuỗi: ${project.streak} ngày, Hoạt động gần nhất: $lastActiveStr"
        }
    }

    LaunchedEffect(serializedData) {
        if (projects.isEmpty() && moments.isEmpty()) {
            insights = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        insights = GeminiClient.generateInsights(serializedData)
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD93D).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD93D),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Phân tích Thói quen thông minh",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Huấn luyện viên AI phân tích dữ liệu hoạt động và gợi ý bài học.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        if (isLoading) {
            // UI shimmer state elements while compiling Gemini response
            items(3) { index ->
                ShimmerInsightCard()
            }
        } else if (projects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Chưa có dự án nào được khởi tạo. Thêm hành trình trước khi chạy phân tích AI nhé!",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(insights) { insight ->
                // Choose emoji icons relative to index to keep UI distinctive & colorful
                val emojiIcon = when {
                    insight.title.contains("gợi ý", ignoreCase = true) || insight.title.contains("hành động", ignoreCase = true) || insight.title.contains("tiếp theo", ignoreCase = true) -> "🚀"
                    insight.title.contains("bền bỉ", ignoreCase = true) || insight.title.contains("chuỗi", ignoreCase = true) || insight.title.contains("kiên trì", ignoreCase = true) -> "🔥"
                    else -> "💡"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Vertical decorative colored left-border line
                        val accentColor = when (emojiIcon) {
                            "🚀" -> Color(0xFF4ECDC4)
                            "🔥" -> Color(0xFFFF6B6B)
                            else -> Color(0xFFFFD93D)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(5.dp)
                                .background(accentColor)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = emojiIcon,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = insight.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = insight.body,
                                    color = Color.LightGray,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ShimmerInsightCard() {
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = alphaAnim))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(18.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
