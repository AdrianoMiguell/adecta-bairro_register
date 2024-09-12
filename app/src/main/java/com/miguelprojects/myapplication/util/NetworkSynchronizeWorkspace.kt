package com.miguelprojects.myapplication.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class NetworkSynchronizeWorkspace(
    private val workspaceViewModel: WorkspaceViewModel,
    private var workspaceId: String,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        // Implementar a lógica de sincronização quando a rede estiver conectada
        if (context != null && NetworkChangeReceiver().isNetworkConnected(context) && workspaceId.isEmpty()) {
            getWorkspaceId(context)
        }
    }

    private fun getWorkspaceId(context: Context) {
        workspaceViewModel.verifyExistsWorkspaceByOffId(workspaceId) { res, resWorkspaceId ->
            if (res && !resWorkspaceId.isNullOrEmpty()) {
                workspaceId = resWorkspaceId
                val broadcast = Intent("DATA_SYNCHRONIZED_WORKSPACE")
                broadcast.putExtra("workspaceId", resWorkspaceId)
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
            }
        }
    }
}
