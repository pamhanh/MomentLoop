package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.JourneyViewModel
import com.example.data.Project
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun AddMomentScreen(
    viewModel: JourneyViewModel,
    onSaveSuccess: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val imageUriStr by viewModel.capturedImageUri.collectAsStateWithLifecycle()
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val themeColor = runCatching { Color(android.graphics.Color.parseColor(project?.thumbnailColor ?: "#6BCB77")) }
        .getOrDefault(Color(0xFF6BCB77))

    var noteText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Camera states
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraLens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Draggable label offset
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCapturedImageUri(uri.toString())
        }
    }

    // Base deep blue Locket background gradient
    val backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF0D1B4B), Color(0xFF1A3A6B))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D1B4B)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Locket Top Bar (above viewfinder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Avatar circle / icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Project name
                Text(
                    text = project?.title ?: "Hành Trình",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                // Right: Flash toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled }
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash Toggle",
                        tint = if (flashEnabled) Color.Yellow else Color.White
                    )
                }
            }

            // Crossfade viewfinder - live preview or captured image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            ) {
                Crossfade(
                    targetState = imageUriStr,
                    animationSpec = tween(durationMillis = 500),
                    modifier = Modifier.fillMaxSize()
                ) { currentUri ->
                    if (currentUri == null) {
                        // CAMERA LIVE PREVIEW
                        if (hasCameraPermission) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                                    .border(2.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
                            ) {
                                CameraPreviewContainer(
                                    cameraLens = cameraLens,
                                    flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF,
                                    imageCapture = imageCapture,
                                    modifier = Modifier.fillMaxSize(),
                                    onReady = { isCameraReady = it }
                                )

                                if (!isCameraReady) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = themeColor,
                                                modifier = Modifier.size(36.dp),
                                                strokeWidth = 3.dp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Đang kết nối máy ảnh...",
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    val colors = listOf("red", "green", "blue", "yellow", "purple", "pink", "orange", "teal")
                                                    val randomColor = colors.random()
                                                    viewModel.setCapturedImageUri("placeholder_$randomColor")
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.White.copy(alpha = 0.15f),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text("Dùng ảnh giả lập 🚀", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Permission nudge
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "MomentLoop cần quyền sử dụng máy ảnh để chụp ghi lại nỗ lực.",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Color.Black)
                                    ) {
                                        Text("Cấp quyền")
                                    }
                                }
                            }
                        }
                    } else {
                        // COPIED / CAPTURED PICTURE
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                                .border(2.dp, themeColor, RoundedCornerShape(32.dp))
                        ) {
                            val isPlaceholder = currentUri.startsWith("placeholder_")
                            if (isPlaceholder) {
                                val fallbackGrad = when (currentUri) {
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
                                    model = Uri.parse(currentUri),
                                    contentDescription = "Captured Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Draggable Label directly over the photo
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                offsetX += dragAmount.x
                                                offsetY += dragAmount.y
                                            }
                                        }
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .widthIn(min = 120.dp, max = 280.dp)
                                ) {
                                    BasicTextField(
                                        value = noteText,
                                        onValueChange = { noteText = it },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            if (noteText.isEmpty()) {
                                                Text(
                                                    text = "Chạm để ghi chú... ✏️",
                                                    color = Color.LightGray.copy(alpha = 0.6f),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom controls or final submit bar
            if (imageUriStr == null) {
                // CAMERA MODE CONTROLS BELOW VIEWFINDER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Pick from gallery
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Chọn ảnh",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Center: Shutter trigger
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(4.dp, if (isCameraReady) Color(0xFF4ECDC4) else Color.Gray.copy(alpha = 0.5f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isCameraReady) Color.White else Color.White.copy(alpha = 0.5f))
                                    .clickable(enabled = isCameraReady) {
                                        if (hasCameraPermission) {
                                            try {
                                                val photoFile = File(context.cacheDir, "locket_moment_${UUID.randomUUID()}.jpg")
                                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                                imageCapture.takePicture(
                                                    outputOptions,
                                                    ContextCompat.getMainExecutor(context),
                                                    object : ImageCapture.OnImageSavedCallback {
                                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                            val isFront = cameraLens == CameraSelector.LENS_FACING_FRONT
                                                            Thread {
                                                                processCapturedImage(photoFile, isFront)
                                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                    val savedUri = Uri.fromFile(photoFile)
                                                                    viewModel.setCapturedImageUri(savedUri.toString())
                                                                }
                                                            }.start()
                                                        }
                                                        override fun onError(exception: ImageCaptureException) {
                                                            Log.e("AddMomentScreen", "Capture failed: ${exception.message}", exception)
                                                        }
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                Log.e("AddMomentScreen", "Failed to take picture", e)
                                            }
                                        }
                                    }
                            )
                        }

                        // Right: Flip camera
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isCameraReady) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .clickable(enabled = isCameraReady) {
                                    cameraLens = if (cameraLens == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Đổi Camera",
                                tint = if (isCameraReady) Color.White else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "History ↓" indicator (decorative)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "Lịch sử",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // SAVING / EDIT ACTION BAR BELOW VIEWFINDER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 30.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Retake Button
                    Button(
                        onClick = {
                            viewModel.setCapturedImageUri(null)
                            noteText = ""
                            offsetX = 0f
                            offsetY = 0f
                            onRetake()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.4f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Chụp lại",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Chụp lại", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Right: Send to Loop Button
                    Button(
                        onClick = {
                            isSaving = true
                            viewModel.addMoment(noteText.ifBlank { "Đã lưu khoảnh khắc" }, context) {
                                isSaving = false
                                onSaveSuccess()
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4ECDC4),
                            contentColor = Color.Black,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Gửi",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gửi vào Loop",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewContainer(
    cameraLens: Int,
    flashMode: Int,
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Dynamically apply flashMode updates without unbinding/binding the entire camera
    LaunchedEffect(flashMode) {
        try {
            imageCapture.flashMode = flashMode
        } catch (e: Exception) {
            Log.e("CameraPreviewContainer", "Failed to set flashMode dynamically", e)
        }
    }

    DisposableEffect(lifecycleOwner, cameraLens) {
        onReady(false)
        var isDisposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            if (isDisposed) return@addListener
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // Only bind to CameraX if the lifecycle is active (not destroyed)
                val currentState = lifecycleOwner.lifecycle.currentState
                if (currentState != androidx.lifecycle.Lifecycle.State.DESTROYED) {
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(cameraLens)
                        .build()

                    if (provider.hasCamera(cameraSelector)) {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        onReady(true)
                    } else {
                        Log.w("CameraPreviewContainer", "Required camera lens not available")
                        onReady(false)
                    }
                }
            } catch (t: Throwable) {
                Log.e("CameraPreviewContainer", "Binding failed", t)
                onReady(false)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            isDisposed = true
            try {
                cameraProvider?.unbindAll()
            } catch (t: Throwable) {
                Log.e("CameraPreviewContainer", "Error on dispose unbinding", t)
            }
            onReady(false)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        decorationBox = decorationBox
    )
}

fun processCapturedImage(photoFile: File, isFrontCamera: Boolean) {
    try {
        val path = photoFile.absolutePath
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        var rotationDegrees = 0
        var flipHorizontal = false
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipHorizontal = true
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                rotationDegrees = 180
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotationDegrees = 90
                flipHorizontal = true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotationDegrees = 270
                flipHorizontal = true
            }
        }
        
        // If front camera, mirror horizontally so it matches standard preview expectations
        if (isFrontCamera) {
            flipHorizontal = !flipHorizontal
        }
        
        // Load the bitmap
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return
        
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }
        
        // Create the modified bitmap
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        
        // Save the modified bitmap back to the file
        FileOutputStream(photoFile).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        // Recycle bitmaps to avoid OutOfMemory
        bitmap.recycle()
        if (rotatedBitmap != bitmap) {
            rotatedBitmap.recycle()
        }
        
        // Clean output EXIF tags so they don't apply double orientation fixes
        val newExif = ExifInterface(path)
        newExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        newExif.saveAttributes()
        
    } catch (e: Exception) {
        Log.e("ImageProcessor", "Failed to process captured image", e)
    }
}
