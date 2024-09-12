package com.miguelprojects.myapplication.room.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AccessWithUser(
    @Embedded val access: WorkspaceAccess,

    @Relation(
        parentColumn = "userId",
        entityColumn = "id"
    )
    val user: User
)
