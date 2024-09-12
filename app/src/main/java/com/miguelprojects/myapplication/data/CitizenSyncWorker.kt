package com.miguelprojects.myapplication.data

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

class CitizenSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private lateinit var citizenRepository: CitizenRepository
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
        val citizenDao = database.citizenDao()

        citizenRepository = CitizenRepository(citizenDao)
        val userId = inputData.getString("userId") ?: return Result.failure()
        val workspaceId = inputData.getString("workspaceId") ?: return Result.failure()

        return try {
            synchronizeData(userId, workspaceId)
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun synchronizeData(userId: String, workspaceId: String): Result {
        return suspendCancellableCoroutine { continuation ->
            if (workspaceId.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    citizenRepository.synchronizeCitizen(
                        userId,
                        workspaceId,
                        object : CitizenRepository.SynchronizationCitizenCallback {
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
}
