package com.miguelprojects.myapplication.data

import WorkspaceRepository
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

class WorkspaceSyncWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var database: MyAppDatabase

    override fun doWork(): Result {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val deferrendResult = coroutineScope.async {
            executeWork()
        }

        return runBlocking {
            deferrendResult.await()
        }
    }

    private suspend fun executeWork(): Result {
        database = (applicationContext as MyApplication).database
        val workspaceDao = database.workspaceDao()

        workspaceRepository = WorkspaceRepository(workspaceDao)

        val userId = inputData.getString("userId") ?: ""

        println("No executeWork - try")
        return try {
            synchronizeWorkspaceWithResult(userId)
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun synchronizeWorkspaceWithResult(userId: String): Result {
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                workspaceRepository.synchronizeWorkspace(
                    userId,
                    object : WorkspaceRepository.SynchronizationWorkspaceCallback {
                        override fun onSuccess() {
                            println("Sucesso no override sync workspace")
                            if (!continuation.isActive) return
                            else continuation.resume(Result.success()) {}
                        }
                        override fun onFailure(error: String) {
                            println("falha no override sync workspace")
                            if (!continuation.isActive) return
                            else continuation.resume(Result.retry()) {}
                        }
                    })
            }
        }
    }
}