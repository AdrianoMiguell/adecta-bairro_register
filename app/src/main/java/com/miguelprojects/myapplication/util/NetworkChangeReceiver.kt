package com.miguelprojects.myapplication.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
//        // Verificar o estado da conectividade e tomar ações apropriadas
        if (isNetworkConnected(context)) {
            val broadcast = Intent("DATA_SYNCHRONIZED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
            println("O id do usuario no firebase está vazio!")
        } else {
            // A rede está desconectada, desabilitar funcionalidades dependentes de rede ou exibir mensagem
            val broadcast = Intent("DATA_OFF_SYNCHRONIZED")
            Toast.makeText(context, "Sem conexão de internet!", Toast.LENGTH_SHORT).show()
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast)
        }
    }

    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
