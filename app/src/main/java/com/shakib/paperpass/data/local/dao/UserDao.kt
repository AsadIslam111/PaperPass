package com.shakib.paperpass.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shakib.paperpass.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE employee_id = :employeeId AND is_active = 1 LIMIT 1")
    suspend fun getUserByEmployeeId(employeeId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET is_active = :isActive WHERE id = :userId")
    suspend fun setUserActiveStatus(userId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

