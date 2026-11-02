package com.shakib.paperpass.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shakib.paperpass.domain.model.Quota

@Entity(
    tableName = "quotas",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"], unique = true)]
)
data class QuotaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "daily_limit")
    val dailyLimit: Int = 50,

    @ColumnInfo(name = "monthly_limit")
    val monthlyLimit: Int = 500,

    @ColumnInfo(name = "pages_used_today")
    val pagesUsedToday: Int = 0,

    @ColumnInfo(name = "pages_used_month")
    val pagesUsedMonth: Int = 0,

    @ColumnInfo(name = "last_reset_date")
    val lastResetDate: String = ""
) {
    fun toDomain(): Quota = Quota(
        id = id,
        userId = userId,
        dailyLimit = dailyLimit,
        monthlyLimit = monthlyLimit,
        pagesUsedToday = pagesUsedToday,
        pagesUsedMonth = pagesUsedMonth,
        lastResetDate = lastResetDate
    )
}

