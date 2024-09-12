package com.miguelprojects.myapplication.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.miguelprojects.myapplication.data.CitizenSyncWorker
import com.miguelprojects.myapplication.data.WorkspaceSyncWorker

object WorkManagerUtil {
    fun scheduleWorkspaceSync(context: Context, userId: String) {
        println("chegou na scheduleWorkspaceSync")
        val data = workDataOf(
            "userId" to userId,
        )

        val workspaceSyncRequest = OneTimeWorkRequestBuilder<WorkspaceSyncWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "WorkspacesSyncWork${userId}",
            ExistingWorkPolicy.KEEP,
            workspaceSyncRequest
        )
    }


    fun scheduleCitizenSync(context: Context, userId: String, workspaceId: String) {
        val data = workDataOf(
            "userId" to userId,
            "workspaceId" to workspaceId,
        )

        val citizenSyncRequest = OneTimeWorkRequestBuilder<CitizenSyncWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "CitizenSyncWork${workspaceId}",
            ExistingWorkPolicy.KEEP, // ou REPLACE para substituir o trabalho existente
            citizenSyncRequest
        )
    }

}