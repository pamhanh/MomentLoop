package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.glance.appwidget.updateAll
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.JourneyRepository
import com.example.data.Moment
import com.example.data.Project
import com.example.notification.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class JourneyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JourneyRepository
    val allProjects: StateFlow<List<Project>>
    val allMoments: StateFlow<List<Moment>>

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getProjectByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedMoments: StateFlow<List<Moment>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMomentsForProjectFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera / Add Moment flow state
    private val _capturedImageUri = MutableStateFlow<String?>(null)
    val capturedImageUri: StateFlow<String?> = _capturedImageUri.asStateFlow()

    // Notification setting
    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    // Language setting flow
    private val _language = MutableStateFlow("vi")
    val language: StateFlow<String> = _language.asStateFlow()

    // Snackbar event channel
    private val _snackbarChannel = MutableSharedFlow<String>()
    val snackbarFlow = _snackbarChannel.asSharedFlow()

    // Flag to ensure decay check is only done once per app launch
    private var decayChecked = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = JourneyRepository(database.projectDao(), database.momentDao())
        
        allProjects = repository.allProjectsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allMoments = repository.allMomentsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Load reminder & language preferences
        val sharedPrefs = application.getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
        _remindersEnabled.value = sharedPrefs.getBoolean("reminders_enabled", true)
        _language.value = sharedPrefs.getString("language", "vi") ?: "vi"

        // Schedule Widget Periodic Refresh (every 15 minutes)
        try {
            val workManager = WorkManager.getInstance(application)
            val widgetRefreshRequest = PeriodicWorkRequestBuilder<com.example.widget.WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "widget_periodic_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                widgetRefreshRequest
            )
        } catch (e: Exception) {
            Log.e("JourneyViewModel", "Failed to schedule widget periodic refresh", e)
        }
    }

    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun setCapturedImageUri(uriString: String?) {
        _capturedImageUri.value = uriString
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        val sharedPrefs = getApplication<Application>().getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", lang).apply()
        
        viewModelScope.launch {
            val welcomeMsg = if (lang == "vi") "Đã cấu hình ngôn ngữ Tiếng Việt thành công! 🇻🇳" else "English language optimized! 🇬🇧"
            _snackbarChannel.emit(welcomeMsg)
        }
    }

    fun createProject(
        title: String,
        colorHex: String,
        backgroundImageUri: String? = null,
        widgetShowStartHour: Int = 0,
        widgetShowEndHour: Int = 23
    ) {
        viewModelScope.launch {
            val newProject = Project(
                title = title,
                thumbnailColor = colorHex,
                progressPercent = 0,
                lastUpdated = System.currentTimeMillis(),
                backgroundImageUri = backgroundImageUri,
                widgetShowStartHour = widgetShowStartHour,
                widgetShowEndHour = widgetShowEndHour
            )
            repository.insertProject(newProject)
            val successMsg = if (_language.value == "vi") "Đã tạo hành trình mới: $title 🚀" else "New journey created: $title 🚀"
            _snackbarChannel.emit(successMsg)
        }
    }

    fun updateProjectSettings(
        project: Project,
        title: String,
        colorHex: String,
        backgroundImageUri: String?,
        startHour: Int,
        endHour: Int
    ) {
        viewModelScope.launch {
            val updated = project.copy(
                title = title,
                thumbnailColor = colorHex,
                backgroundImageUri = backgroundImageUri,
                widgetShowStartHour = startHour,
                widgetShowEndHour = endHour,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertProject(updated)
            val msg = if (_language.value == "vi") "Đã cập nhật cài đặt hành trình! ✨" else "Journey settings updated! ✨"
            _snackbarChannel.emit(msg)
            
            // Also refresh widgets instantly!
            try {
                com.example.widget.MomentGlanceWidget("SMALL").updateAll(getApplication())
                com.example.widget.MomentGlanceWidget("MEDIUM").updateAll(getApplication())
                com.example.widget.MomentGlanceWidget("LARGE").updateAll(getApplication())
            } catch (e: Exception) {
                android.util.Log.e("JourneyViewModel", "Failed updating widgets upon project setting update", e)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProjectById(project.id)
            _snackbarChannel.emit("Đã xoá hành trình ${project.title}")
        }
    }

    fun triggerDecayCheck() {
        if (decayChecked) return
        decayChecked = true
        
        viewModelScope.launch {
            val projects = repository.getAllProjects()
            val now = System.currentTimeMillis()
            
            projects.forEach { project ->
                val diffMills = now - project.lastUpdated
                val diffDays = diffMills / (1000 * 60 * 60 * 24)
                
                var decayPercent = 0
                if (diffDays in 2..4) {
                    decayPercent = 2
                } else if (diffDays in 5..7) {
                    decayPercent = 5
                } else if (diffDays > 7) {
                    decayPercent = 10
                }
                
                if (decayPercent > 0) {
                    val newProgress = (project.progressPercent - decayPercent).coerceAtLeast(5)
                    // Update project with decreased progress and update lastUpdated to current so we don't double decay
                    val updatedProject = project.copy(
                        progressPercent = newProgress,
                        lastUpdated = now
                    )
                    repository.updateProject(updatedProject)
                    _snackbarChannel.emit("Hành trình ${project.title} đang nhớ bạn đấy 👀 — hãy tiếp tục nào!")
                }
            }
        }
    }

    fun addMoment(noteText: String, context: Context, onSuccess: () -> Unit) {
        val projId = _selectedProjectId.value ?: return
        val imageUriStr = _capturedImageUri.value ?: return

        viewModelScope.launch {
            try {
                // 1. Copy captured image from cache to internal permanent moments directory if not a placeholder
                val permanentPath = if (imageUriStr.startsWith("placeholder_")) {
                    imageUriStr
                } else {
                    withContext(Dispatchers.IO) {
                        val cacheUri = Uri.parse(imageUriStr)
                        val inputStream = if (cacheUri.scheme == "file") {
                            val file = File(cacheUri.path ?: "")
                            if (file.exists()) file.inputStream() else null
                        } else {
                            context.contentResolver.openInputStream(cacheUri)
                        }
                        
                        val momentsDir = File(context.filesDir, "moments")
                        if (!momentsDir.exists()) {
                            momentsDir.mkdirs()
                        }
                        
                        val destFile = File(momentsDir, "moment_${UUID.randomUUID()}.jpg")
                        inputStream?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        correctOrientationOfFile(destFile)
                        destFile.absolutePath
                    }
                }

                _snackbarChannel.emit("Đang phân tích khoảnh khắc bằng trí tuệ nhân tạo (Gemini)... 🤖")

                // 2. Fetch project context to call Gemini
                val project = repository.getProjectById(projId) ?: return@launch

                // 3. Call Gemini AI to get score and feedback (Multimodal)
                val geminiResult = GeminiClient.evaluateProgress(project.title, noteText, permanentPath)
                val aiScore = geminiResult.score
                val aiFeedback = geminiResult.feedback

                // 4. Save Moment with evaluated feedback to database
                val newMoment = Moment(
                    projectId = projId,
                    imageUri = permanentPath,
                    noteText = noteText,
                    createdAt = System.currentTimeMillis(),
                    aiScore = aiScore,
                    aiFeedback = aiFeedback
                )
                repository.insertMoment(newMoment)

                val progressIncrement = aiScore / 2 // max +5%
                val currentProgress = project.progressPercent
                val newProgress = (currentProgress + progressIncrement).coerceIn(0, 100)

                // Compute streak logic
                // Check if last moment was logged today or yesterday to maintain/increment streak,
                // of if streak reset is required.
                val moments = repository.getMomentsForProject(projId)
                val newStreak = calculateStreak(moments)

                val updatedProject = project.copy(
                    progressPercent = newProgress,
                    lastUpdated = System.currentTimeMillis(),
                    streak = newStreak
                )
                repository.updateProject(updatedProject)

                _capturedImageUri.value = null // reset
                _snackbarChannel.emit("AI Coach: $aiFeedback +$progressIncrement% tiến trình! 💪")
                
                // Refresh home widgets immediately
                try {
                    com.example.widget.MomentGlanceWidget("SMALL").updateAll(context)
                    com.example.widget.MomentGlanceWidget("MEDIUM").updateAll(context)
                    com.example.widget.MomentGlanceWidget("LARGE").updateAll(context)
                } catch (we: Exception) {
                    Log.e("JourneyViewModel", "Failed updating Glance widgets on moment added", we)
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("JourneyViewModel", "Failed to add moment", e)
                _snackbarChannel.emit("Lỗi: Không thể lưu khoảnh khắc của bạn.")
            }
        }
    }

    private fun calculateStreak(moments: List<Moment>): Int {
        if (moments.isEmpty()) return 1
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        
        val uniqueDays = moments.map { sdf.format(Date(it.createdAt)) }.distinct().sortedDescending()
        if (uniqueDays.isEmpty()) return 1
        
        val mostRecentDay = uniqueDays.first()
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val yesterdayStr = sdf.format(yesterdayCalendar.time)

        // If no log today or yesterday, streak is broken, returned as 1 for this new moment
        if (mostRecentDay != todayStr && mostRecentDay != yesterdayStr) {
            return 1
        }

        var streakCount = 0
        var checkCalendar = Calendar.getInstance()
        
        for (i in 0..100) { // check up to last 100 days
            val checkStr = sdf.format(checkCalendar.time)
            if (uniqueDays.contains(checkStr)) {
                streakCount++
            } else {
                // If it is today, and we haven't checked yesterday yet, wait, let's keep checking.
                // But generally if a link in the chain is missing, the streak is broken.
                if (checkStr != todayStr) {
                    break
                }
            }
            checkCalendar.add(Calendar.DATE, -1)
        }
        
        return streakCount.coerceAtLeast(1)
    }

    fun toggleReminder(enabled: Boolean, context: Context) {
        _remindersEnabled.value = enabled
        val sharedPrefs = context.getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("reminders_enabled", enabled).apply()

        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val periodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateDelayTo8PM(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "journey_lens_daily_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } else {
            workManager.cancelUniqueWork("journey_lens_daily_reminder")
        }
    }

    private fun calculateDelayTo8PM(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DATE, 1)
        }
        return calendar.timeInMillis - now
    }

    fun updateMomentNote(moment: Moment, newNoteText: String) {
        viewModelScope.launch {
            try {
                val updatedMoment = moment.copy(noteText = newNoteText)
                repository.updateMoment(updatedMoment)
                _snackbarChannel.emit("Đã cập nhật ghi chú! 📝")
            } catch (e: Exception) {
                Log.e("JourneyViewModel", "Failed to update moment note", e)
                _snackbarChannel.emit("Lỗi: Không thể cập nhật ghi chú.")
            }
        }
    }

    private fun correctOrientationOfFile(file: File) {
        try {
            val path = file.absolutePath
            val exif = android.media.ExifInterface(path)
            val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
            var rotationDegrees = 0
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
            }
            if (rotationDegrees != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path) ?: return
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                java.io.FileOutputStream(file).use { out ->
                    rotated.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                if (rotated != bitmap) {
                    rotated.recycle()
                }
                val newExif = android.media.ExifInterface(path)
                newExif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL.toString())
                newExif.saveAttributes()
            }
        } catch (e: Exception) {
            android.util.Log.e("JourneyViewModel", "Error correcting copy orientation", e)
        }
    }
}
