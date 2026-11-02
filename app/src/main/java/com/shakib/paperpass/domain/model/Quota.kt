package com.shakib.paperpass.domain.model

data class Quota(
    val id: Long = 0,
    val userId: Long,
    val dailyLimit: Int = 50,
    val monthlyLimit: Int = 500,
    val pagesUsedToday: Int = 0,
    val pagesUsedMonth: Int = 0,
    val lastResetDate: String = ""
) {
    val remainingDaily: Int
        get() = (dailyLimit - pagesUsedToday).coerceAtLeast(0)

    val remainingMonthly: Int
        get() = (monthlyLimit - pagesUsedMonth).coerceAtLeast(0)

    val isDailyQuotaExceeded: Boolean
        get() = pagesUsedToday >= dailyLimit

    val isMonthlyQuotaExceeded: Boolean
        get() = pagesUsedMonth >= monthlyLimit
}

