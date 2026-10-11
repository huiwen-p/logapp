package com.example.logapp.feature.management.model

import androidx.compose.ui.graphics.Color

data class ActivityIconPreset(
    val icon: String,
    val label: String
)

object ActivityPresets {

    val colors: List<Color> = listOf(
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF059669), // Emerald Green
        Color(0xFFD97706), // Amber Orange
        Color(0xFFDC2626), // Ruby Red
        Color(0xFF7C3AED), // Deep Violet
        Color(0xFF0891B2), // Ocean Cyan
        Color(0xFFDB2777), // Vibrant Pink
        Color(0xFF4F46E5), // Indigo
        Color(0xFF65A30D), // Lime Green
        Color(0xFFEA580C), // Warm Tangerine
        Color(0xFF0D9488), // Teal
        Color(0xFF9333EA), // Purple
        Color(0xFFE11D48), // Rose
        Color(0xFF475569), // Slate Grey
        Color(0xFFB45309), // Warm Bronze
        Color(0xFF0284C7)  // Sky Blue
    )

    val iconPresets: List<ActivityIconPreset> = listOf(
        ActivityIconPreset("💻", "Lập trình"),
        ActivityIconPreset("💼", "Công việc"),
        ActivityIconPreset("📚", "Đọc sách / Học"),
        ActivityIconPreset("✍️", "Ghi chép"),
        ActivityIconPreset("🏃", "Chạy bộ"),
        ActivityIconPreset("🏋️", "Thể hình"),
        ActivityIconPreset("🧘", "Thiền / Yoga"),
        ActivityIconPreset("🎮", "Game"),
        ActivityIconPreset("🎧", "Âm nhạc"),
        ActivityIconPreset("🎬", "Xem phim"),
        ActivityIconPreset("☕", "Cà phê"),
        ActivityIconPreset("🍳", "Ăn uống"),
        ActivityIconPreset("🛌", "Nghỉ ngơi"),
        ActivityIconPreset("🚗", "Di chuyển"),
        ActivityIconPreset("🛒", "Mua sắm"),
        ActivityIconPreset("🧹", "Việc nhà"),
        ActivityIconPreset("💡", "Ý tưởng"),
        ActivityIconPreset("📱", "Mạng xã hội")
    )
}
