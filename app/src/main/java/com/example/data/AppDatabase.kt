package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Project::class, Moment::class], version = 1, exportSchema = false)
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
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        // Insert standard mock projects via synchronous raw SQL in onCreate
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak) VALUES ('fitness', 'Fitness', '#FF6B6B', 72, $now, 4)")
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak) VALUES ('upwork', 'Upwork', '#FFD93D', 45, $now, 2)")
                        db.execSQL("INSERT INTO projects (id, title, thumbnailColor, progressPercent, lastUpdated, streak) VALUES ('cooking', 'Cooking', '#6BCB77', 20, $now, 0)")
                        
                        // Also pre-populate 3 moments per project as requested by Phase 1.1: "Use mock data: 3 moments per project"
                        // Fitness moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('f1', 'fitness', 'placeholder_red', 'Chạy bộ 5km buổi sáng sảng khoái', ${now - 86400000 * 2})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('f2', 'fitness', 'placeholder_green', 'Đạt kỷ lục đẩy tạ mới 80kg', ${now - 86400000})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('f3', 'fitness', 'placeholder_blue', 'Bữa ăn lành mạnh chuẩn Keto', $now)")

                        // Upwork moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('u1', 'upwork', 'placeholder_purple', 'Nhận dự án thiết kế UI đầu tiên', ${now - 86400000 * 3})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('u2', 'upwork', 'placeholder_yellow', 'Trao đổi tích cực với khách hàng bận rộn', ${now - 86400000})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('u3', 'upwork', 'placeholder_pink', 'Gửi milestone 1 đúng hạn', $now)")

                        // Cooking moments
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('c1', 'cooking', 'placeholder_orange', 'Học cách ướp thịt nướng ngon', ${now - 86400000 * 4})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('c2', 'cooking', 'placeholder_teal', 'Lần đầu thử làm bánh bông lan', ${now - 86400000 * 2})")
                        db.execSQL("INSERT INTO moments (id, projectId, imageUri, noteText, createdAt) VALUES ('c3', 'cooking', 'placeholder_indigo', 'Trang trí đĩa salad rực rỡ', $now)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
