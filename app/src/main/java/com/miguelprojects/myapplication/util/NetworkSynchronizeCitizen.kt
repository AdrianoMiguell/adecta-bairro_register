package com.miguelprojects.myapplication.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel

class NetworkSynchronizeCitizen(
    private val citizenViewModel: CitizenViewModel,
    private var citizenId: String,
    private var workspaceId: String,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (NetworkChangeReceiver().isNetworkConnected(context) && citizenId.isEmpty()) {
                getCitizenId(context)
            }
        }
    }

    private fun getCitizenId(context: Context) {
        try {
            citizenViewModel.verifyExistsCitizenByOffId(
                workspaceId,
                citizenId
            ) { res, resCitizenId ->
                resCitizenId?.let {
                    if (res && it.isNotEmpty()) {
                        citizenId = it
                        val broadcast = Intent("DATA_SYNCHRONIZED_CITIZEN")
                        broadcast.putExtra("citizenId", it)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro inesperado no getCitizenId: ${e.message}")
        }
    }
}
