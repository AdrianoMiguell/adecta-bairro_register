package com.miguelprojects.myapplication.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.miguelprojects.myapplication.room.entity.Citizen
import com.miguelprojects.myapplication.room.entity.Workspace

@Dao
interface CitizenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(citizen: Citizen)

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertWorkspace(workspace: Workspace)

    @Query("SELECT * FROM Workspace WHERE id = :id AND firebaseId = :id AND needsSync = 0")
    suspend fun verifyExistsWorkspaceFirebaseId(id: String): Workspace?

    @Query("SELECT * FROM Citizen WHERE numberregister = :numberRegister AND active = 1")
    fun verifyNumberRegister(numberRegister: String): Int

    @Query("SELECT * FROM Citizen WHERE id = :id")
    suspend fun getCitizen(id: String): Citizen?

//    @Query("SELECT * FROM WorkspaceAccess WHERE userId = :userId")
//    suspend fun getWorkspaceReference(userId: String)

    @Query("SELECT * FROM Workspace WHERE id = :workspaceId")
    suspend fun getWorkspaceReference(workspaceId: String): Workspace?

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId")
    suspend fun getAllCitizens(workspaceId: String): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId AND active = 1 LIMIT :limit")
    suspend fun getCitizensForWorkspace(workspaceId: String, limit: Int): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId AND needsSync = 1")
    suspend fun getCitizensNeedingSync(workspaceId: String): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId AND needsSync = 0 AND needsUpdate = 1")
    suspend fun getCitizensNeedingUpdate(workspaceId: String): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId AND active = 0 AND isDelete = 0")
    suspend fun getCitizensInactive(workspaceId: String): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE workspaceId = :workspaceId AND isDelete = 1")
    suspend fun getCitizensNeedingDelete(workspaceId: String): List<Citizen>

    @Query("SELECT * FROM Citizen WHERE needsSync = 0 AND id = :offWorkspaceId")
    suspend fun getCitizensNotNeedingSync(offWorkspaceId: String): List<Citizen>

    @RawQuery
    suspend fun getCitizensByField(query: SupportSQLiteQuery): List<Citizen>

    @Update
    suspend fun update(citizen: Citizen): Int

    @Query("UPDATE Workspace SET id = :newId, firebaseId = :newId, needsSync = 0 WHERE id = :agoId")
    suspend fun updateWorkspaceId(newId: String, agoId: String)

    @Query("UPDATE Citizen SET id = :newId, firebaseId = :newId, needsSync = 1 WHERE id = :oldId")
    suspend fun updateCitizenId(newId: String, oldId: String)

    @Query("UPDATE Citizen SET needsSync = :needsSync, needsUpdate = :needsSync WHERE id = :offCitizenId")
    suspend fun updateSyncStatus(offCitizenId: String, needsSync: Boolean): Int

    @Query("UPDATE Workspace SET needsSync = :needsSync, needsUpdate = :needsSync WHERE id = :workspaceId")
    suspend fun updateSyncWorkspace(workspaceId: String, needsSync: Boolean): Int

    @Query("UPDATE Citizen SET needsUpdate = :needsUpdate WHERE id = :citizenId")
    suspend fun updateNeedsUpdateStatus(citizenId: String, needsUpdate: Boolean): Int

    @Query("UPDATE Citizen SET active = :isActive, needsUpdate = :needsUpdate WHERE id = :offCitizenId")
    suspend fun updateIsActive(offCitizenId: String, isActive: Boolean, needsUpdate: Boolean)

    @Query("UPDATE Citizen SET isDelete = :valueISDelete WHERE id = :offCitizenId")
    suspend fun updateIsDelete(offCitizenId: String, valueISDelete: Boolean): Int

    @Delete
    suspend fun delete(citizen: Citizen): Int

    @Delete
    suspend fun deleteListCitizens(citizens: List<Citizen>)
}