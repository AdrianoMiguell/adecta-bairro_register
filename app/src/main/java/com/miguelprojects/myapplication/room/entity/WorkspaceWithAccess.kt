package com.miguelprojects.myapplication.room.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkspaceWithAccess(
    @Embedded val workspace: Workspace,
    @Relation(
        parentColumn = "id",
        entityColumn = "workspaceId"
    )
    val accessList: List<WorkspaceAccess>
)
