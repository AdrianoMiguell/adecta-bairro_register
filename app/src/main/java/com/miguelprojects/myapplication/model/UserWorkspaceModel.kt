package com.miguelprojects.myapplication.model

data class UserWorkspaceModel(
    var id: String = "",
    val user_id: String = "",
    val workspace_id: String = "",
    val permission_level: Int = 0
)
