package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Project
import com.example.data.Moment
import java.io.File
import java.util.Calendar

class MomentGlanceWidget(private val sizeVariant: String) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        
        // Use standard GlanceAppWidgetManager to retrieve static AppWidgetId
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (e: Exception) {
            Log.e("MomentGlanceWidget", "Failed getting app widget id from glance ID", e)
            null
        }
        
        // Check for manual pinning preference
        val widgetPrefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val pinnedProjectId = if (appWidgetId != null && appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetPrefs.getString("widget_project_$appWidgetId", null as String?)
        } else {
            null
        }
        
        val allProjects = db.projectDao().getAllProjects()
        val allMoments = mutableMapOf<String, List<Moment>>()
        var hasAnyMoments = false
        
        for (proj in allProjects) {
            val list = db.momentDao().getMomentsForProject(proj.id)
            allMoments[proj.id] = list
            if (list.isNotEmpty()) {
                hasAnyMoments = true
            }
        }
        
        // If there are absolutely no moments in the app yet
        if (!hasAnyMoments) {
            provideContent {
                NoMomentsWidgetView()
            }
            return
        }
        
        var featuredProject: Project? = null
        var featuredMoments: List<Moment> = emptyList()
        var isNudgeMode = false
        
        // Pin explicitly by user if configured
        if (pinnedProjectId != null) {
            featuredProject = allProjects.firstOrNull { it.id == pinnedProjectId }
            if (featuredProject != null) {
                featuredMoments = allMoments[featuredProject.id] ?: emptyList()
            }
        }
        
        // Else, determine using Smart Time Widget Algorithm and Display Hour Schedules
        if (featuredProject == null) {
            val calendar = Calendar.getInstance()
            val curHourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val currentHour = curHourOfDay + (calendar.get(Calendar.MINUTE) / 60.0)
            
            // Helper function to check if active hours contain current hour range
            fun isHourInSchedule(hour: Int, start: Int, end: Int): Boolean {
                return if (start <= end) {
                    hour in start..end
                } else {
                    hour >= start || hour <= end
                }
            }

            // Filter projects where current hour fits their specific widget showtime
            val scheduledProjects = allProjects.filter { 
                isHourInSchedule(curHourOfDay, it.widgetShowStartHour, it.widgetShowEndHour)
            }

            // If some projects match active schedule, prioritize them. Else, fall back to all projects.
            val candidateProjects = if (scheduledProjects.isNotEmpty()) scheduledProjects else allProjects
            
            var closestProj: Project? = null
            var bestMoments: List<Moment> = emptyList()
            var minDiff = 24.0
            
            for (proj in candidateProjects) {
                val list = allMoments[proj.id] ?: emptyList()
                if (list.isEmpty()) continue
                
                // Calculate average hour when moments were added (0 to 23.99)
                val avgHour = list.map { m ->
                    val cal = Calendar.getInstance().apply { timeInMillis = m.createdAt }
                    cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0)
                }.average()
                
                // Calculate circular difference on a 24-hr clock
                val diff = Math.abs(currentHour - avgHour)
                val circularDiff = Math.min(diff, 24.0 - diff)
                
                if (circularDiff < minDiff) {
                    minDiff = circularDiff
                    closestProj = proj
                    bestMoments = list
                }
            }
            
            if (closestProj != null) {
                featuredProject = closestProj
                featuredMoments = bestMoments
            } else {
                // If no project has moments yet -> trigger gentle nudge mode
                // Show the project in the active candidate pool with the lowest recent activity (fewest moments)
                isNudgeMode = true
                val nudgeProj = candidateProjects.minByOrNull { allMoments[it.id]?.size ?: 0 }
                if (nudgeProj != null) {
                    featuredProject = nudgeProj
                    featuredMoments = allMoments[nudgeProj.id] ?: emptyList()
                }
            }
        }
        
        val project = featuredProject
        val moments = featuredMoments
        
        if (project == null || moments.isEmpty()) {
            provideContent {
                NoMomentsWidgetView()
            }
            return
        }
        
        val latestMoment = moments.first()
        val bgBitmap = getProjectImage(context, latestMoment.imageUri)
        val bgColor = getPlaceholderColor(latestMoment.imageUri, project.thumbnailColor)
        
        provideContent {
            WidgetLayout(
                sizeVariant = sizeVariant,
                project = project,
                latestMoment = latestMoment,
                allMoments = moments,
                bgBitmap = bgBitmap,
                bgColor = bgColor,
                isNudgeMode = isNudgeMode
            )
        }
    }
}

@Composable
fun NoMomentsWidgetView() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF13151A)))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxSize().padding(12.dp)
        ) {
            Text(
                text = "MomentLoop",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFD93D)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "Vẽ vòng lặp khoảnh khắc",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Thêm khoảnh khắc đầu tiên nhé!",
                style = TextStyle(
                    color = ColorProvider(Color.LightGray),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
fun WidgetLayout(
    sizeVariant: String,
    project: Project,
    latestMoment: Moment,
    allMoments: List<Moment>,
    bgBitmap: Bitmap?,
    bgColor: Color,
    isNudgeMode: Boolean
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF13151A)))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when (sizeVariant) {
            "SMALL" -> {
                if (bgBitmap != null) {
                    Image(
                        provider = ImageProvider(bgBitmap),
                        contentDescription = "Background Image",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor))) {}
                }
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.Black.copy(alpha = 0.55f)))
                ) {}
                SmallWidgetLayout(project, isNudgeMode)
            }
            "MEDIUM" -> {
                MediumWidgetLayout(project, latestMoment, bgBitmap, bgColor, isNudgeMode)
            }
            else -> {
                if (bgBitmap != null) {
                    Image(
                        provider = ImageProvider(bgBitmap),
                        contentDescription = "Background Image",
                        modifier = GlanceModifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor))) {}
                }
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color.Black.copy(alpha = 0.65f)))
                ) {}
                LargeWidgetLayout(project, allMoments, isNudgeMode)
            }
        }
    }
}

@Composable
fun SmallWidgetLayout(project: Project, isNudgeMode: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        if (isNudgeMode) {
            Text(
                text = "Gợi ý 🎯",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFD93D)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Time to loop in ${project.title}?",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.title,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "🔥 ${project.streak}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFA502)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tiến độ",
                    style = TextStyle(
                        color = ColorProvider(Color.LightGray),
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${project.progressPercent}%",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6BCB77)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        LinearProgressIndicator(
            progress = project.progressPercent / 100f,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp)
        )
    }
}

@Composable
fun MediumWidgetLayout(
    project: Project,
    latestMoment: Moment,
    bgBitmap: Bitmap?,
    bgColor: Color,
    isNudgeMode: Boolean
) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        // Left half: Image/Thumbnail
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (bgBitmap != null) {
                Image(
                    provider = ImageProvider(bgBitmap),
                    contentDescription = "Moment Preview",
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize().background(ColorProvider(bgColor))
                ) {}
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.Black.copy(alpha = 0.35f)))
            ) {}
        }
        
        // Right half: Detailed info list
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(ColorProvider(Color(0xFF1E1F25)))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            if (isNudgeMode) {
                Text(
                    text = "Gợi ý 🎯",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFD93D)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(1.dp))
                Text(
                    text = "Time to loop in ${project.title}?",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.title,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "🔥 ${project.streak}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFA502)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = latestMoment.noteText,
                    style = TextStyle(
                        color = ColorProvider(Color.LightGray),
                        fontSize = 11.sp
                    ),
                    maxLines = 2
                )
            }
            
            Spacer(modifier = GlanceModifier.height(6.dp))
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tiến độ",
                    style = TextStyle(
                        color = ColorProvider(Color.LightGray),
                        fontSize = 9.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${project.progressPercent}%",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6BCB77)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            LinearProgressIndicator(
                progress = project.progressPercent / 100f,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp)
            )
        }
    }
}

@Composable
fun LargeWidgetLayout(project: Project, allMoments: List<Moment>, isNudgeMode: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MomentLoop",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFD93D)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            val badgeText = if (isNudgeMode) "Gợi ý 🎯" else "Giờ Luyện Tập"
            Text(
                text = badgeText,
                style = TextStyle(
                    color = ColorProvider(if (isNudgeMode) Color(0xFFFF6B6B) else Color(0xFF6BCB77)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Text(
            text = project.title,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        
        Text(
            text = "Chuỗi: ${project.streak} ngày 🔥",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFFA502)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        
        if (isNudgeMode) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Time to loop in ${project.title}?",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFFFDF80)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.height(8.dp))
        
        Text(
            text = "KHOẢNH KHẮC GẦN ĐÂY:",
            style = TextStyle(
                color = ColorProvider(Color.LightGray),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = GlanceModifier.height(4.dp))
        
        // Take last 2 moments
        val displayed = allMoments.take(2)
        for ((idx, m) in displayed.withIndex()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color.White.copy(alpha = 0.08f)))
                    .padding(6.dp)
                    .cornerRadius(8.dp)
            ) {
                Text(
                    text = m.noteText,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 10.sp
                    ),
                    maxLines = if (idx == 0) 2 else 1
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                val cal = Calendar.getInstance().apply { timeInMillis = m.createdAt }
                val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1} lúc ${cal.get(Calendar.HOUR_OF_DAY)}h${cal.get(Calendar.MINUTE)}"
                Text(
                    text = dateStr,
                    style = TextStyle(
                        color = ColorProvider(Color.Gray),
                        fontSize = 8.sp
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        
        Spacer(modifier = GlanceModifier.defaultWeight())
        
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tiến độ vòng lặp",
                style = TextStyle(
                    color = ColorProvider(Color.LightGray),
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${project.progressPercent}%",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6BCB77)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        LinearProgressIndicator(
            progress = project.progressPercent / 100f,
            modifier = GlanceModifier.fillMaxWidth().height(5.dp)
        )
    }
}

// Recipient receivers for the actual provider binds
class MomentWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentGlanceWidget("SMALL")
}

class MomentWidgetMediumReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentGlanceWidget("MEDIUM")
}

class MomentWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentGlanceWidget("LARGE")
}

// Helpers for bitmaps and placeholders
private fun getProjectImage(context: Context, imageUriStr: String?): Bitmap? {
    if (imageUriStr == null || imageUriStr.startsWith("placeholder_")) return null
    return try {
        val uri = Uri.parse(imageUriStr)
        val file = if (uri.scheme == "file") {
            File(uri.path ?: "")
        } else if (uri.scheme == "content" || imageUriStr.startsWith("content://")) {
            null
        } else {
            File(imageUriStr)
        }
        
        if (file != null && file.exists()) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            var sampleSize = 1
            val maxDim = 300 // keeps binder transaction memory safe
            if (options.outWidth > maxDim || options.outHeight > maxDim) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / sampleSize) >= maxDim && (halfHeight / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }
            
            val finalOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val loadedBitmap = BitmapFactory.decodeFile(file.absolutePath, finalOptions)
            if (loadedBitmap != null) {
                try {
                    val exif = android.media.ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                    var rotationDegrees = 0
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
                    }
                    if (rotationDegrees != 0) {
                        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotated = Bitmap.createBitmap(loadedBitmap, 0, 0, loadedBitmap.width, loadedBitmap.height, matrix, true)
                        if (rotated != loadedBitmap) {
                            loadedBitmap.recycle()
                        }
                        rotated
                    } else {
                        loadedBitmap
                    }
                } catch (e: Exception) {
                    loadedBitmap
                }
            } else {
                null
            }
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val maxDim = 300
                if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                } else {
                    bitmap
                }
            } else null
        }
    } catch (e: Exception) {
        Log.e("MomentWidget", "Failed to load image $imageUriStr: ${e.message}")
        null
    }
}

private fun getPlaceholderColor(imageUriStr: String?, defaultColor: String): Color {
    val colorStr = imageUriStr ?: defaultColor
    return when {
        colorStr.contains("red", ignoreCase = true) -> Color(0xFFFF6B6B)
        colorStr.contains("green", ignoreCase = true) -> Color(0xFF6BCB77)
        colorStr.contains("blue", ignoreCase = true) -> Color(0xFF4D96FF)
        colorStr.contains("purple", ignoreCase = true) -> Color(0xFF9B51E0)
        colorStr.contains("yellow", ignoreCase = true) -> Color(0xFFFFD93D)
        colorStr.contains("pink", ignoreCase = true) -> Color(0xFFFF85B3)
        colorStr.contains("orange", ignoreCase = true) -> Color(0xFFFF9F43)
        colorStr.contains("teal", ignoreCase = true) -> Color(0xFF00D2D3)
        colorStr.contains("indigo", ignoreCase = true) -> Color(0xFF54A0FF)
        colorStr.startsWith("#") -> {
            try {
                Color(android.graphics.Color.parseColor(colorStr))
            } catch (e: Exception) {
                Color(0xFF6C5CE7)
            }
        }
        else -> Color(0xFF6C5CE7)
    }
}
