package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Project::class, Moment::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun momentDao(): MomentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "journey_lens_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        // Insert standard mock projects via synchronous raw SQL in onCreate
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak, backgroundImageUri, widgetShowStartHour, widgetShowEndHour) VALUES ('fitness', 'Fitness', '#FF6B6B', 72, $now, 4, 'preset_fitness', 0, 23)")
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak, backgroundImageUri, widgetShowStartHour, widgetShowEndHour) VALUES ('upwork', 'Upwork', '#FFD93D', 45, $now, 2, 'preset_productivity', 0, 23)")
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak, backgroundImageUri, widgetShowStartHour, widgetShowEndHour) VALUES ('cooking', 'Cooking', '#6BCB77', 20, $now, 0, 'preset_cooking', 0, 23)")
                        
                        // Also pre-populate 3 moments per project as requested by Phase 1.1: "Use mock data: 3 moments per project"
                        // Fitness moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('f1', 'fitness', 'placeholder_red', 'Chạy bộ 5km buổi sáng sảng khoái', ${now - 86400000 * 2}, 6, 'Tuyệt vời! Chạy bộ sáng sớm giúp thúc đẩy tuần hoàn máu và tỉnh táo tinh thần.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('f2', 'fitness', 'placeholder_green', 'Đạt kỷ lục đẩy tạ mới 80kg', ${now - 86400000}, 9, 'Đáng kinh ngạc! Đạt kỷ lục mới chứng tỏ cơ bắp và sức bền của bạn đang tăng trưởng vượt bậc.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('f3', 'fitness', 'placeholder_blue', 'Bữa ăn lành mạnh chuẩn Keto', $now, 8, 'Chuẩn Keto hoàn hảo! Dinh dưỡng sạch là chìa khoá cho hiệu năng tập luyện dài lâu.')")

                        // Upwork moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('u1', 'upwork', 'placeholder_purple', 'Nhận dự án thiết kế UI đầu tiên', ${now - 86400000 * 3}, 8, 'Khởi đầu rực rỡ! Dự án đầu tiên luôn là bước đệm quan trọng nhất.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('u2', 'upwork', 'placeholder_yellow', 'Trao đổi tích cực với khách hàng bận rộn', ${now - 86400000}, 7, 'Giao tiếp tốt! Thấu hiểu khách hàng bận rộn giúp tăng uy tín chuyên nghiệp.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('u3', 'upwork', 'placeholder_pink', 'Gửi milestone 1 đúng hạn', $now, 9, 'Đúng hạn hoàn hảo! Duy trì phong độ này sẽ giúp bạn nhận được đánh giá 5 sao dễ dàng.')")

                        // Cooking moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('c1', 'cooking', 'placeholder_orange', 'Học cách ướp thịt nướng ngon', ${now - 86400000 * 4}, 5, 'Kỹ năng nền tảng vững vàng! Thịt nướng ướp đúng vị sẽ luôn khiến bữa ăn tròn trịa.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('c2', 'cooking', 'placeholder_teal', 'Lần đầu thử làm bánh bông lan', ${now - 86400000 * 2}, 6, 'Lần đầu thú vị! Làm bánh đòi hỏi sự chính xác, hãy tiếp tục tinh chỉnh công thức.')")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt, aiScore, aiFeedback) VALUES ('c3', 'cooking', 'placeholder_indigo', 'Trang trí đĩa salad rực rỡ', $now, 8, 'Nghệ thuật ẩm thực! Đĩa salad bắt mắt kích thích vị giác và tốt cho sức khoẻ.')")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
