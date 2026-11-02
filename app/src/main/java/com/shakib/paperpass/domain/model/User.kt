package com.shakib.paperpass.domain.model

enum class UserRole {
    ADMIN,
    USER,
    GUEST
}

data class User(
    val id: Long = 0,
    val employeeId: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val department: String,
    val securityPin: String,
    val isActive: Boolean = true
)

