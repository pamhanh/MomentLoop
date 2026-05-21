package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getMomentsForProjectFlow(projectId: String): Flow<List<Moment>>

    @Query("SELECT * FROM moments WHERE projectId = :projectId ORDER BY createdAt DESC")
    suspend fun getMomentsForProject(projectId: String): List<Moment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: Moment)

    @Update
    suspend fun updateMoment(moment: Moment)

    @Delete
    suspend fun deleteMoment(moment: Moment)

    @Query("DELETE FROM moments WHERE id = :id")
    suspend fun deleteMomentById(id: String)
}
