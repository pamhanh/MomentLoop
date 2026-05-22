package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val thumbnailColor: String, // Hex string, e.g. "#FF6B6B"
    val progressPercent: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val streak: Int = 0,
    val backgroundImageUri: String? = null, // Background preset name or local file URI
    val widgetShowStartHour: Int = 0,
    val widgetShowEndHour: Int = 23
)

