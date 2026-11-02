package com.shakib.paperpass.data.repository

import com.shakib.paperpass.core.security.SecurityUtils
import com.shakib.paperpass.core.util.Resource
import com.shakib.paperpass.data.local.dao.QuotaDao
import com.shakib.paperpass.data.local.dao.UserDao
import com.shakib.paperpass.domain.model.Quota
import com.shakib.paperpass.domain.model.User
import com.shakib.paperpass.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val quotaDao: QuotaDao
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUserFlow: Flow<User?> = _currentUser.asStateFlow()

    override suspend fun login(employeeId: String, passwordRaw: String): Resource<User> {
        val userEntity = userDao.getUserByEmployeeId(employeeId.trim())
            ?: return Resource.Error("Employee ID '$employeeId' not found.")

        if (!userEntity.isActive) {
            return Resource.Error("Your account has been deactivated. Please contact IT.")
        }

        val isValid = SecurityUtils.verifyPassword(passwordRaw, userEntity.passwordHash)
        return if (isValid) {
            val user = userEntity.toDomain()
            _currentUser.value = user
            Resource.Success(user)
        } else {
            Resource.Error("Invalid password. Please check your credentials.")
        }
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun verifyPin(userId: Long, enteredPin: String): Boolean {
        val userEntity = userDao.getUserById(userId) ?: return false
        return SecurityUtils.verifyPin(enteredPin, userEntity.securityPin)
    }

    override fun getQuotaFlow(userId: Long): Flow<Quota?> {
        return quotaDao.getQuotaByUserIdFlow(userId).map { it?.toDomain() }
    }
}

