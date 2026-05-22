package com.example.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = "Chào mừng tới MomentLoop",
            subtitle = "Ghi lại cuộc hành trình của bạn, từng khoảnh khắc ý nghĩa.",
            icon = Icons.Default.Map,
            gradient = listOf(Color(0xFF3F51B5), Color(0xFF2196F3))
        ),
        OnboardingPageData(
            title = "Ghi lại từng khoảnh khắc",
            subtitle = "Sử dụng camera chụp ảnh thật và viết ghi chú nhanh để lưu giữ nỗ lực mỗi ngày.",
            icon = Icons.Default.CameraAlt,
            gradient = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
        ),
        OnboardingPageData(
            title = "Theo dõi sự tiến bộ bằng AI",
            subtitle = "HLV Trí tuệ Nhân tạo Gemini đọc ghi chú của bạn và cộng điểm tiến trình tự động.",
            icon = Icons.Default.AutoAwesome,
            gradient = listOf(Color(0xFF9C27B0), Color(0xFF673AB7))
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (pagerState.currentPage < 2) {
                    TextButton(onClick = {
                        saveOnboardingCompleted(context)
                        onFinished()
                    }) {
                        Text(
                            text = "Bỏ qua",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                val page = pages[index]
                OnboardingPageLayout(page)
            }

            // Bottom section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Page indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF6BCB77) else Color.DarkGray)
                        )
                    }
                }

                // Action Button
                val isLastPage = pagerState.currentPage == 2
                Button(
                    onClick = {
                        if (isLastPage) {
                            saveOnboardingCompleted(context)
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) Color(0xFF6BCB77) else Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        text = if (isLastPage) "Bắt đầu ngay" else "Tiếp tục",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

@Composable
fun OnboardingPageLayout(page: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Rounded Illustration card
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.linearGradient(page.gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            color = Color.LightGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun saveOnboardingCompleted(context: Context) {
    val sharedPrefs = context.getSharedPreferences("journey_lens_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
}
