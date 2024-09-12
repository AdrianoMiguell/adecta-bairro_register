package com.miguelprojects.myapplication.adapter.listener

import com.miguelprojects.myapplication.model.WorkspaceModel

class WorkspaceOnClickListener (val clickListener: (workspaceModel: WorkspaceModel) -> Unit) {
    fun onClick(workspaceModel: WorkspaceModel) = clickListener
}