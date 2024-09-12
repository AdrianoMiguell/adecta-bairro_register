package com.miguelprojects.myapplication.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "WorkspaceAccess")
data class WorkspaceAccess (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "workspaceId") val workspaceId: String,
    @ColumnInfo(name = "userId") val userId: String
){
    // Construtor vazio
    constructor() : this(0L, "", "")
}