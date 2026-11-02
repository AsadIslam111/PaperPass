package com.shakib.paperpass.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shakib.paperpass.data.local.entity.PrintJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintJobDao {
    @Query("SELECT * FROM print_jobs ORDER BY submitted_at DESC")
    fun getAllJobsFlow(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE user_id = :userId ORDER BY submitted_at DESC")
    fun getJobsForUserFlow(userId: Long): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE status = 'PENDING' ORDER BY submitted_at ASC")
    fun getPendingRequestsFlow(): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Long): PrintJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: PrintJobEntity): Long

    @Update
    suspend fun updateJob(job: PrintJobEntity)

    @Query("UPDATE print_jobs SET status = :status, completed_at = :completedAt, error_message = :errorMessage WHERE id = :jobId")
    suspend fun updateJobStatus(
        jobId: Long,
        status: String,
        completedAt: Long? = System.currentTimeMillis(),
        errorMessage: String? = null
    )

    @Query("SELECT COUNT(*) FROM print_jobs WHERE status = 'COMPLETED'")
    fun getCompletedJobCountFlow(): Flow<Int>
}

