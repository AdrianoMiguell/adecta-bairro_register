package com.miguelprojects.myapplication.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.miguelprojects.myapplication.room.entity.WorkspaceWithAccess

@Dao
interface WorkspaceAccessDao {
    @Transaction
    @Query("SELECT * FROM workspace")
    fun getWorkspacesWithAccess(): List<WorkspaceWithAccess>
}