package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "moments",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class Moment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val imageUri: String,
    val noteText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val aiScore: Int = 4,
    val aiFeedback: String = "Tiếp tục duy trì và nỗ lực hằng ngày nhé!"
)
