package com.shakib.paperpass.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shakib.paperpass.data.local.entity.QuotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotaDao {
    @Query("SELECT * FROM quotas WHERE user_id = :userId LIMIT 1")
    suspend fun getQuotaByUserId(userId: Long): QuotaEntity?

    @Query("SELECT * FROM quotas WHERE user_id = :userId LIMIT 1")
    fun getQuotaByUserIdFlow(userId: Long): Flow<QuotaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateQuota(quota: QuotaEntity): Long

    @Update
    suspend fun updateQuota(quota: QuotaEntity)

    @Query("UPDATE quotas SET pages_used_today = pages_used_today + :pages, pages_used_month = pages_used_month + :pages WHERE user_id = :userId")
    suspend fun deductQuota(userId: Long, pages: Int)

    @Query("UPDATE quotas SET pages_used_today = 0, last_reset_date = :todayDate WHERE user_id = :userId")
    suspend fun resetDailyQuota(userId: Long, todayDate: String)
}

