package com.shakib.paperpass.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shakib.paperpass.domain.model.User
import com.shakib.paperpass.domain.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["employee_id"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "employee_id")
    val employeeId: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "security_pin")
    val securityPin: String,

    @ColumnInfo(name = "role")
    val role: String, // "ADMIN", "USER", "GUEST"

    @ColumnInfo(name = "department")
    val department: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): User = User(
        id = id,
        employeeId = employeeId,
        fullName = fullName,
        email = email,
        role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.USER),
        department = department,
        securityPin = securityPin,
        isActive = isActive
    )
}

