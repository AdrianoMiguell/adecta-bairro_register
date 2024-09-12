package com.miguelprojects.myapplication

import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.google.firebase.database.FirebaseDatabase
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class MyApplication() : Application() {
    lateinit var citizenViewModel: CitizenViewModel
    lateinit var workspaceViewModel: WorkspaceViewModel

    val database: MyAppDatabase by lazy { MyAppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Inicializar NetworkChangeReceiver aqui
        val networkChangeReceiver = NetworkChangeReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(networkChangeReceiver, intentFilter)
    }
}