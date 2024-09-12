package com.miguelprojects.myapplication.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class NetworkSynchronizeUser(
    private val userViewModel: UserViewModel,
    private val sharedPreferences: SharedPreferences,
    private var userId: String
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        // Implementar a lógica de sincronização quando a rede estiver conectada
        if (context != null && isNetworkConnected(context) && userId.isEmpty()) {
            getUserId(context)
        }
    }

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getUserId(context: Context) {
        userViewModel.verifyExistsUserByOffId(userId) { res, resUserId ->
            if (res && !resUserId.isNullOrEmpty()) {
                userId = resUserId
                val broadcast = Intent("DATA_SYNCHRONIZED_USER")
                broadcast.putExtra("userId", resUserId)
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
            }
        }
    }
}
