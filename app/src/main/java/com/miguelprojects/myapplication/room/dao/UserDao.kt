package com.miguelprojects.myapplication.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.room.entity.Workspace

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User): Int

    @Query("SELECT * FROM User WHERE id = :id")
    suspend fun getUser(id: String): User?

    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query(
        """
       SELECT workspace.* 
        FROM workspace
        INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId
        WHERE WorkspaceAccess.userId = :userId
    """
    )
    suspend fun getUserWorkspaceAccess(userId: String): List<Workspace>

    @Query("SELECT * FROM User WHERE id = :offUSerId AND needsSync = 1")
    suspend fun getUsersNeedingSync(offUSerId: String): User?

    @Query("SELECT * FROM User WHERE needsSync = 0")
    suspend fun getAllUsersNotNeedingSync(): List<User>

    @Query("UPDATE User SET needsSync = :needsSync WHERE id = :offUserId")
    suspend fun updateSyncStatus(offUserId: String, needsSync: Boolean)

    @Query("UPDATE User SET id = :newUserId, firebaseId = :newUserId WHERE id = :oldUserId")
    suspend fun updateUserIdRoom(newUserId: String, oldUserId: String)

    @Query("DELETE FROM User WHERE id = :userId ")
    suspend fun delete(userId: String)

    @Delete
    suspend fun deleteList(listUser: List<User>)

    @Query("DELETE FROM WorkspaceAccess WHERE userId = :userId")
    suspend fun deleteListUserWorkspacesAccess(userId: String)

    @Query("DELETE FROM Workspace WHERE id IN (:listWorkspacesIds)")
    suspend fun deleteListUserWorkspaces(listWorkspacesIds: List<String>)
}
