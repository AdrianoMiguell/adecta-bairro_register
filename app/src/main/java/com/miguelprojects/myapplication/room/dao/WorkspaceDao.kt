package com.miguelprojects.myapplication.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.miguelprojects.myapplication.room.entity.AccessWithUser
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.room.entity.WorkspaceAccess
import com.miguelprojects.myapplication.room.entity.WorkspaceWithAccess

@Dao
interface WorkspaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workspace: Workspace)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workspaces: List<Workspace>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceAccess(workspaceAccess: WorkspaceAccess)

    @Query("SELECT * FROM Workspace WHERE id = :id")
    suspend fun getWorkspace(id: String): Workspace?

    @Transaction
    @Query(
        """
    SELECT workspace.*
    FROM workspace
    INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId
    WHERE WorkspaceAccess.userId = :userId AND workspace.needsSync = 1
"""
    )
    suspend fun getWorkspacesNeedingSync(userId: String): List<Workspace>

    @Transaction
    @Query(
        """
    SELECT workspace.*
    FROM workspace
    INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId
    WHERE WorkspaceAccess.userId = :userId AND workspace.needsUpdate = 1
"""
    )
    suspend fun getWorkspacesNeedingUpdate(userId: String): List<Workspace>

    //    Não vai ser usada mais
    @Query("SELECT * FROM Workspace")
    suspend fun getAllWorkspaces(): List<Workspace>

    @Query("SELECT Workspace.* FROM Workspace INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId WHERE userId = :userId AND needsSync = 0")
    suspend fun getAllWorkspacesNotNeedingSync(userId: String): List<Workspace>

    @Transaction
    @Query(
        """SELECT Workspace.*, WorkspaceAccess.id AS workspaceAccessId FROM workspace
    INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId
    WHERE WorkspaceAccess.userId = :userId
    """
    )
    suspend fun getListWorkspacesWithAccess(userId: String): List<WorkspaceWithAccess>

    @Transaction
    @Query(
        """SELECT Workspace.*, WorkspaceAccess.id AS workspaceAccessId FROM workspace
    INNER JOIN WorkspaceAccess ON workspace.id = WorkspaceAccess.workspaceId
    WHERE WorkspaceAccess.userId = :userId AND needsSync = 1  
    """
    )
    suspend fun getWorkspacesWithAccessNeedsSync(userId: String): List<WorkspaceWithAccess>

    @Transaction
    @Query("SELECT * FROM WorkspaceAccess WHERE workspaceId = :workspaceId")
    fun getUsersForWorkspace(workspaceId: String): List<AccessWithUser>

    @Query("SELECT * FROM WorkspaceAccess WHERE workspaceId = :workspaceId AND userId = :userId")
    suspend fun getWorkspaceAccess(workspaceId: String, userId: String): WorkspaceAccess?

    @Query("SELECT * FROM WorkspaceAccess WHERE workspaceId = :workspaceId")
    suspend fun getListWorkspaceAccessWithWorkspaceId(workspaceId: String): List<WorkspaceAccess>

    @Update
    suspend fun update(workspace: Workspace)

    @Query("UPDATE Workspace SET id = :newId, firebaseId = :newId WHERE id = :agoId")
    fun updateId(newId: String, agoId: String)

    @Query("UPDATE Workspace SET needsSync = :needsSync WHERE id = :offWorkspaceId")
    suspend fun updateSyncStatus(offWorkspaceId: String, needsSync: Boolean)

    @Query("UPDATE Workspace SET needsUpdate = :needsUpdate WHERE id = :workspaceId")
    suspend fun updateNeedingUpdateStatus(workspaceId: String, needsUpdate: Boolean)

    @Query("UPDATE Workspace SET needsSync = :needsSync, creator = :creator, id = :firebaseId, firebaseId = :firebaseId WHERE id = :offWorkspaceId")
    suspend fun updateIdAndSyncStatus(
        firebaseId: String,
        offWorkspaceId: String,
        creator: String,
        needsSync: Boolean
    )

    @Query("UPDATE WorkspaceAccess SET workspaceId = :newWorkspaceId, userId = :newUserId WHERE id = :workspaceAccessId")
    suspend fun updateWorkspaceAccessIds(
        workspaceAccessId: Long,
        newWorkspaceId: String,
        newUserId: String
    )

    @Query("DELETE FROM WORKSPACE WHERE id = :workspaceId")
    suspend fun deleteWorkspaceById(workspaceId: String)

    @Query("DELETE FROM WorkspaceAccess WHERE id = :workspaceAccessId")
    suspend fun deleteWorkspaceAccess(workspaceAccessId: Long)

    @Delete
    suspend fun delete(workspace: Workspace)

    @Delete
    suspend fun deleteList(listWorkspace: List<Workspace>)
}