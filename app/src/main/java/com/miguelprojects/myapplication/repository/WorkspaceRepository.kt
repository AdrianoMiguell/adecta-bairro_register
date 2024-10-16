import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miguelprojects.myapplication.model.SyncNotificationsModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.room.dao.WorkspaceDao
import com.miguelprojects.myapplication.room.entity.AccessWithUser
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.room.entity.WorkspaceAccess
import com.miguelprojects.myapplication.room.entity.WorkspaceWithAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkspaceRepository(private val workspaceDao: WorkspaceDao) {
    private val syncMutex = Mutex()
    private val userLocks = ConcurrentHashMap<String, Mutex>()
    private var lastSyncTime: Long = 0 // Armazena o tempo do último salvamento
    private val syncInterval = 25000L // Intervalo de espera em milissegundos (ex: 25 segundos)

    var isSynchronized = false

    fun generateInviteCode(): String {
        return UUID.randomUUID().toString()
            .substring(0, 8) // Gerar um código de convite de 8 caracteres
    }

    fun saveWorkspaceFirebase(
        userId: String,
        workspace: WorkspaceModel,
        callback: (Boolean, String) -> Unit
    ) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces").push()

        workspace.id = reference.key ?: ""

        // Gerar código de convite se estiver vazio
        if (workspace.inviteCode.isEmpty()) {
            workspace.inviteCode = generateInviteCode()
        }

        if (workspace.userIds.isEmpty()) {
            workspace.userIds[userId] = true
        }

        // Verificar se o modelo está vazio
        if (workspace.modelIsEmpty()) {
            println("Modelo de workspace está vazio ou com dados faltando")
            callback(false, "") // Chamar callback aqui para indicar falha
            return
        }

        reference.setValue(workspace)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Dados de workspace salvos no firebase")
                } else {
                    println("Falha ao salvar dados do workspace")
                }

                println("Workspace id do firebase ${workspace.id}")
                callback(task.isSuccessful, workspace.id)
            }
            .addOnFailureListener { e ->
                Log.e("Workspace Save", "${e.message}")
            }
    }

    suspend fun saveWorkspaceRoom(
        workspace: Workspace,
        userId: String,
        needsSync: Boolean?
    ): String {
        try {
            println(workspace.id)
            if (workspace.id.isEmpty()) {
                val id = UUID.randomUUID().toString()
                workspace.id = id
            } else {
                val workspaceRoom = workspaceDao.getWorkspace(workspace.id)

                if (workspaceRoom != null && workspaceRoom.id.isNotEmpty()) return ("")
                else workspace.firebaseId = workspace.id
            }

            workspace.needsSync = needsSync ?: true
            if (workspace.inviteCode.isEmpty()) {
                workspace.inviteCode = generateInviteCode()
            }

            workspaceDao.insert(workspace)

            // Se um userId foi fornecido, salva o acesso do usuário ao workspace
            saveWorkspaceAccessRoom(workspace.id, userId)

            return workspace.id // Retorna o ID do workspace
        } catch (e: Exception) {
            return ""
        }
    }

    suspend fun saveWorkspaceListRoom(workspaceList: List<Workspace>) {
        workspaceDao.insertAll(workspaceList)
    }

    fun updateWorkspace(workspaceModel: WorkspaceModel, callback: (Boolean, String) -> Unit) {
        if (workspaceModel.id.isEmpty()) {
            println("Dados de workspace_id faltando.")
            callback(false, "Dados de workspace_id faltando.")
        } else {
            val reference =
                FirebaseDatabase.getInstance().getReference("workspaces")
                    .child(workspaceModel.id)
            reference.updateChildren(workspaceModel.toMap()).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        println("Dados de workspace atualizados")
                        updateSyncStatus(
                            workspaceModel.id,
                            false
                        )
                    }
                    callback(true, "Atualização bem sucedida.")
                    println("Atualização bem sucedida")
                } else {
                    callback(false, "Problemas na atualização do Workspace")
                    println("Atualização mal sucedida")
                }
            }.addOnFailureListener { exception ->
                callback(false, "Erro ao executar esta ação.")
                println("Atualização mal sucedida: ${exception.message}")
            }
        }
    }

    fun loadWorkspace(workspaceId: String, callback: (WorkspaceModel) -> Unit) {
        val reference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val workspace = dataSnapshot.getValue(WorkspaceModel::class.java)
                if (workspace != null) {
                    callback(workspace)
                } else {
                    callback(WorkspaceModel())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(WorkspaceModel())
            }
        })
    }

    suspend fun loadWorkspaceRoom(workspaceId: String): Workspace? {
        return workspaceDao.getWorkspace(workspaceId)
    }

    fun loadListWorkspace(userId: String, callback: (List<WorkspaceModel>) -> Unit) {
        val workspaceListRef = FirebaseDatabase.getInstance().getReference("workspaces")
        val workspacesList = mutableListOf<WorkspaceModel>()

        workspaceListRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                workspacesList.clear()
                // Limpar a lista antes de preencher novamente

                for (workspaceSnapshot in dataSnapshot.children) {
                    val workspace = workspaceSnapshot.getValue(WorkspaceModel::class.java)
                    val userIds = workspace?.userIds

                    // Check if the userId is in the user_ids map
                    if (userIds?.get(userId) == true) {
                        Log.d("loadListWorkspace", "user_Id = $userId ; userIds = $userIds")
                        workspace?.let {
                            workspacesList.add(it)
                        }
                    }
                }

                callback(workspacesList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Workspace Repository", "Error loading workspaces: ${error.message}")
                callback(emptyList())
            }
        })
    }

    suspend fun getAllWorkspacesNotNeedingSync(userId: String): List<Workspace> {
        return workspaceDao.getAllWorkspacesNotNeedingSync(userId)
    }

    fun accessSecurityCheckUser(
        userId: String,
        workspaceId: String,
        callback: (Boolean) -> Unit
    ) {
        val reference =
            FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val workspace = snapshot.getValue(WorkspaceModel::class.java)
                if (workspace != null) {
                    if (workspace.userIds?.containsKey(userId) == true) {
                        callback(true)
                        Log.d(
                            "Verificação de segurança de acesso do usuário",
                            "Verificação bem sucedida, usuário pode entrar."
                        )
                    } else {
                        callback(false)
                        Log.d(
                            "Verificação de segurança de acesso do usuário",
                            "Verificação mal sucedida, workspace está nulo. usuário não pode entrar."
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false)
                Log.d(
                    "Verificação de segurança de acesso do usuário",
                    "Verificação mal sucedida, processo cancelado."
                )
            }
        })
    }

    fun deleteWorkspaceFirebase(workspaceId: String, callback: (Boolean, String) -> Unit) {
        val reference =
            FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
        reference.removeValue()
            .addOnSuccessListener {
                // Operação de deleção bem-sucedida
                callback(true, "Dados deletados com sucesso!")
                Log.d("Teste deletar", "Dados deletados com sucesso.")
            }
            .addOnFailureListener { exception ->
                callback(false, "Falha ao deletar os dados!")
                Log.e("Teste deletar", "Erro ao deletar dados: ${exception.message}")
            }
    }

    suspend fun deleteWorkspaceRoom(workspace: Workspace, userId: String) {
        try {
            // Operação de exclusão bem-sucedida
            if (workspaceDao.getWorkspace(workspace.id) != null) {
                workspaceDao.delete(workspace)

                val workspaceAccess = workspaceDao.getWorkspaceAccess(workspace.id, userId)
                if (workspaceAccess != null) {
                    workspaceDao.deleteWorkspaceAccess(workspaceAccess.id)
                }
            }
        } catch (e: Exception) {
            // Tratar exceção
            Log.e("Deletar workspace", "Erro ao excluir workspace: ${e.message}")
        }
    }

    suspend fun deleteWorkspaceWithIdRoom(workspaceId: String, userId: String) {
        try {
            val workspaceAccess = workspaceDao.getWorkspaceAccess(workspaceId, userId)
            println("WorkspaceAccess for delete: ${workspaceAccess}")
            if (workspaceAccess != null) {
                workspaceDao.deleteWorkspaceAccess(workspaceAccess.id)
            }

            // Operação de exclusão bem-sucedida
            workspaceDao.deleteWorkspaceById(workspaceId)
        } catch (e: Exception) {
            // Tratar exceção
            Log.e("Deletar workspace", "Erro ao excluir workspace: ${e.message}")
        }
    }

    suspend fun deleteListWorkspaceRoom(listWorkspace: List<Workspace>) {
        try {
            workspaceDao.deleteList(listWorkspace)
            // Operação de exclusão bem-sucedida
        } catch (e: Exception) {
            // Tratar exceção
            Log.e("Deletar workspace", "Erro ao excluir workspace: ${e.message}")
        }
    }


    fun getSyncNotifications(
        userId: String,
        callback: (Boolean, List<SyncNotificationsModel>) -> Unit
    ) {
        val referenceSyncNotify =
            FirebaseDatabase.getInstance().getReference("syncNotification").child(userId)

        referenceSyncNotify.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val listSyncNotificationsModel = mutableListOf<SyncNotificationsModel>()
                try {
                    if (dataSnapshot.exists()) {
                        for (snapshot in dataSnapshot.children) {
                            val syncNotificationsModel =
                                snapshot.getValue(SyncNotificationsModel::class.java)
                            if (syncNotificationsModel != null) {
                                listSyncNotificationsModel.add(syncNotificationsModel)
                            }
                        }

                        callback(true, listSyncNotificationsModel)
                        Log.d(
                            "verifyExistsSyncNotify",
                            "Tem uma notificação para o usuario, uma ação para o sistema"
                        )
                    } else {
                        callback(false, emptyList())
                        Log.d(
                            "verifyExistsSyncNotify",
                            "Os dados não existem de notificações para o usuario"
                        )
                    }
                } catch (e: Exception) {
                    callback(false, emptyList())
                    Log.d(
                        "verifyExistsSyncNotify",
                        "Houve um erro inesperado: ${e.message}"
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(false, emptyList())
                Log.d(
                    "verifyExistsSyncNotify",
                    "Houve um erro ao tentar acessar notifications: ${error.message}"
                )
            }
        })
    }

    fun removeListWorkspaceMembers(
        workspaceId: String,
        listMembersToRemove: List<String>,
        callback: (Boolean) -> Unit
    ) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces")

        if (listMembersToRemove.isEmpty()) {
            println("Nenhum membro para remover.")
            callback(false)
            return
        }

        println("Workspace ID = $workspaceId")

        reference.child(workspaceId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    println("Dados do workspace não encontrados.")
                    callback(false)
                    return
                }

                println("Dados do workspace encontrados.")
                val workspaceModel = snapshot.getValue(WorkspaceModel::class.java)

                if (workspaceModel == null) {
                    println("Modelo de workspace não encontrado.")
                    callback(false)
                    return
                }

                val mapWorkspaceMembers = workspaceModel.userIds.toMutableMap()
                val tasks =
                    mutableListOf<Task<Void>>() // Lista para controlar todas as tarefas assíncronas

                // Remove todos os membros da lista do map
                listMembersToRemove.forEach { memberId ->
                    // Verifica se o membro é o criador do workspace e pula para o próximo
                    if (memberId == workspaceModel.creator) {
                        println("Membro $memberId é o criador do workspace. Ignorando a remoção.")
                        return@forEach  // Pula para o próximo membro
                    }

                    if (mapWorkspaceMembers.containsKey(memberId)) {
                        println("Removendo membro: $memberId")
                        mapWorkspaceMembers.remove(memberId)

                        // Adiciona a notificação de remoção para o membro
                        val syncNotification = SyncNotificationsModel(
                            "",
                            "delete_workspace",
                            "Você foi removido do grupo: ${workspaceModel.name}",
                            mapOf("workspaceId" to workspaceId, "userId" to memberId)
                        )

                        removeWorkspaceMember(memberId, workspaceId) { res ->
                            var control = true
                            if (!res) {
                                control = false
                                return@removeWorkspaceMember
                            }

                            addNotifyRemoveWorkspaceMember(
                                memberId,
                                syncNotification
                            ) { resNotify ->
                                if (!resNotify) {
                                    control = false
                                    return@addNotifyRemoveWorkspaceMember
                                }
                                println("Notificação bem sucedida!")
                            }

                            callback(control)
                            println("Remoção bem sucedida!")
                        }
                    } else {
                        println("Membro $memberId não encontrado no grupo.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Erro ao acessar o Firebase: ${error.message}")
                callback(false)
            }
        })
    }

    fun removeWorkspaceMember(memberId: String, workspaceId: String, callback: (Boolean) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces")

        // Cria o caminho específico para o userId e define como nulo para removê-lo
        val pathToRemove = "userIds/$memberId"
        reference.child(workspaceId).child(pathToRemove).setValue(null)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Membro $memberId removido com sucesso do mapa de membros.")
                } else {
                    println("Falha ao remover membro $memberId: ${task.exception?.message}")
                }
                callback(task.isSuccessful)
            }
    }

    suspend fun addNotifyRemoveWorkspaceMemberSuspend(
        memberId: String,
        syncNotification: SyncNotificationsModel
    ) {
        suspendCancellableCoroutine { continuation ->
            addNotifyRemoveWorkspaceMember(memberId, syncNotification) { result ->
                continuation.resume(result)
            }
        }
    }

    private fun addNotifyRemoveWorkspaceMember(
        memberId: String,
        syncNotification: SyncNotificationsModel,
        callback: (Boolean) -> Unit
    ) {
        val referenceSyncNotify = FirebaseDatabase.getInstance().getReference("syncNotification")

        val syncRef = referenceSyncNotify.child(memberId).push()
        syncNotification.id = syncRef.key ?: ""

        syncRef.setValue(syncNotification)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Notificação enviada com sucesso para o usuário $memberId.")
                } else {
                    println("Erro ao enviar notificação para o usuário $memberId: ${task.exception?.message}")
                }

                callback(task.isSuccessful)
            }
    }

    suspend fun removeListWorkspaceMembersRoom(
        workspaceId: String,
        listMembersToRemove: List<String>
    ) {
        listMembersToRemove.map { userId ->
            removeWorkspaceMemberRoom(workspaceId, userId)
        }
    }

    private suspend fun removeWorkspaceMemberRoom(workspaceId: String, userId: String) {
        val workspaceAccessUser = workspaceDao.getWorkspaceAccess(
            workspaceId,
            userId
        )

        if (workspaceAccessUser != null) {
            workspaceDao.deleteWorkspaceAccess(workspaceAccessUser.id)

            val listWorkspaceAccess =
                workspaceDao.getListWorkspaceAccessWithWorkspaceId(workspaceId)

            if (listWorkspaceAccess.isNotEmpty()) {
                workspaceDao.deleteWorkspaceById(workspaceId)
            }
        }
    }

    suspend fun loadListWorkspaceRoom(offUserId: String): List<Workspace> {
        return workspaceDao.getAllWorkspaces()
    }

    suspend fun getWorkspacesWithAccess(userId: String): List<WorkspaceWithAccess> {
        return workspaceDao.getListWorkspacesWithAccess(userId)
    }

    suspend fun getWorkspacesWithAccessNeedsSync(userId: String): List<WorkspaceWithAccess> {
        return workspaceDao.getWorkspacesWithAccessNeedsSync(userId)
    }

    suspend fun getUsersForWorkspace(workspaceId: String): List<AccessWithUser> {
        return workspaceDao.getUsersForWorkspace(workspaceId)
    }

    suspend fun updateWorkspaceRoom(workspace: Workspace) {
        try {
            Log.d("Update workspace", "Workspace: $workspace")
            workspaceDao.update(workspace)
        } catch (e: Exception) {
            Log.d("Update workspace", "Erro ao atualizar os dados: ${e.message}")
        }
    }

    suspend fun updateWorkspaceAccessIdsRoom(
        oldWorkspaceId: String,
        oldUserId: String,
        newWorkspaceId: String,
        newUserId: String
    ) {
        val workspaceAccess = workspaceDao.getWorkspaceAccess(oldWorkspaceId, oldUserId)

        if (workspaceAccess != null) {
            println("updateWorkspaceAccessIdsRoom : Existe $workspaceAccess")
            workspaceDao.updateWorkspaceAccessIds(
                workspaceAccess.id,
                newWorkspaceId,
                newUserId
            )
        }
    }

    suspend fun loadWorkspaceSuspend(workspaceId: String): WorkspaceModel =
        suspendCancellableCoroutine { continuation ->
            loadWorkspace(workspaceId) {
                continuation.resume(it)
            }
        }

    suspend fun checkExistsWorkspaceRoom(
        workspace: Workspace,
        workspaceId: String,
        userId: String
    ) {
        val verifyIfSaveWorkspaceFirebase = loadWorkspaceSuspend(workspaceId).modelIsEmpty()
        val verifyIfSaveWorkspace = loadWorkspaceRoom(workspaceId) == null

        if (verifyIfSaveWorkspaceFirebase && verifyIfSaveWorkspace) {
            saveWorkspaceRoom(workspace, userId, false)
            return
        } else if (verifyIfSaveWorkspace) {
            saveWorkspaceAccessRoom(workspaceId, userId)
        }
    }

    suspend fun saveWorkspaceAccessRoom(workspaceId: String, userId: String) {
        val verifyWorkspaceAccess = workspaceDao.getWorkspaceAccess(workspaceId, userId)
        if (verifyWorkspaceAccess == null) {
            val workspaceAccess = WorkspaceAccess(
                workspaceId = workspaceId,
                userId = userId
            )
            workspaceDao.insertWorkspaceAccess(workspaceAccess) // Insere o WorkspaceAccess
        }
    }

    fun verifyExistsWorkspaceByOffId(
        offWorkspaceId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces")
        reference.orderByChild("offWorkspaceId")
            .equalTo(offWorkspaceId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Verifica se algum dado foi encontrado
                    if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                        val firstChild = dataSnapshot.children.firstOrNull()
                        val workspaceId = firstChild?.key
                        println(workspaceId)
                        println("Verificação bem sucedida na verifyExistsWorkspaceByOffId")
                        callback(true, workspaceId)
                    } else {
                        println("Nenhum workspace encontrado com o offWorkspaceId fornecido")
                        callback(false, null)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("Falha na verificação verifyExistsWorkspaceByOffId: ${databaseError.message}")
                    callback(false, null)
                }
            })
    }

    suspend fun synchronizeWorkspace(
        userId: String,
        callback: SynchronizationWorkspaceCallback
    ) {
        isSynchronized = true
        println("Iniciando sincronização para o workspace: $userId")

        val mutex = userLocks.getOrPut(userId) { Mutex() }

        mutex.withLock {
            println("Entrando no lock de sincronização")

            if (shouldWaitForNextSync()) {
                delay(syncInterval - (System.currentTimeMillis() - lastSyncTime))
            }
            lastSyncTime = System.currentTimeMillis()
        }

        processSyncNotifications(userId)

        try {
            println("Passando pela sincronização")
            val (localData, onlineData) = fetchData(userId)
            println("localData - ${localData.size}, onlineData - ${onlineData.size}")
            val workspacesNeedsSync =
                WorkspaceModel.fromWorkspaceWithAccessList(getWorkspacesWithAccessNeedsSync(userId))

            val (missingOnlineIds, missingRoomIds) = identifyMissingData(
                workspacesNeedsSync,
                localData,
                onlineData
            )

            println("IDs ausentes - Online: ${missingOnlineIds.size}, Offline: ${missingRoomIds.size}")

            var saveOnlineResult = true
            var saveOfflineResult = true

            if (localData.size != onlineData.size) {
                println("localData - ${localData.map { it.id }}")
                println("onlineData - ${onlineData.map { it.id }}")

                if (missingOnlineIds.isNotEmpty()) {
                    saveOnlineResult = needsSaveOnlineData(missingOnlineIds, userId, localData)
                }

                if (missingRoomIds.isNotEmpty()) {
                    saveOfflineResult = needsSaveOfflineData(missingRoomIds, userId, onlineData)
                }
            }

            val updateSyncResult = updateNeedingUpdateData(userId, onlineData)

            if (saveOnlineResult && saveOfflineResult && updateSyncResult) {
                callback.onSuccess()
            } else {
                callback.onFailure("Erro inesperado")
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar: ${e.message}")
        } finally {
            userLocks.remove(userId)
            isSynchronized = false
        }
        println("Saindo do lock de sincronização")
    }

    private suspend fun handleSyncNotifications(
        userId: String,
        listSyncNotifications: List<SyncNotificationsModel>
    ) {
        if (listSyncNotifications.isEmpty()) return

        val referenceSyncNotify =
            FirebaseDatabase.getInstance().getReference("syncNotification").child(userId)

        // Use um conjunto para rastrear IDs de notificações já processadas
        val processedNotifications = mutableSetOf<String>()

        listSyncNotifications.forEach { syncNotification ->
            val getUserIdInDataMap = syncNotification.idDataMap["userId"]

            if (!getUserIdInDataMap.isNullOrEmpty() && getUserIdInDataMap == userId) {
                val notificationId = syncNotification.id
                if (processedNotifications.contains(notificationId)) {
                    // Pular notificações já processadas
                    return@forEach
                }

                processedNotifications.add(notificationId)

                when (syncNotification.command) {
                    "delete_workspace" -> {
                        val workspaceId = syncNotification.idDataMap["workspaceId"]
                        if (!workspaceId.isNullOrEmpty()) {
                            try {
                                val workspaceWithAccess =
                                    workspaceDao.getWorkspaceAccess(workspaceId, userId)
                                val allWorkspaceUsers =
                                    workspaceDao.getUsersForWorkspace(workspaceId)

                                if (workspaceWithAccess != null) {
                                    workspaceDao.deleteWorkspaceAccess(workspaceWithAccess.id)

                                    if (allWorkspaceUsers.size == 1) {
                                        workspaceDao.deleteWorkspaceById(workspaceId)
                                    }

                                    Log.d(
                                        "handleSyncNotifications",
                                        "Comando executado com sucesso!"
                                    )
                                } else {
                                    Log.d(
                                        "handleSyncNotifications",
                                        "Access ao workspace não encontrado."
                                    )
                                    // Erro específico, não usar retry
                                }

                                // Remove a notificação após o processamento
                                referenceSyncNotify.child(notificationId).removeValue()
                                    .addOnSuccessListener {
                                        Log.d(
                                            "handleSyncNotifications",
                                            "Notificação excluída com sucesso!"
                                        )
                                    }
                                    .addOnFailureListener {
                                        Log.d(
                                            "handleSyncNotifications",
                                            "Falha ao excluir notificação: ${it.message}"
                                        )
                                    }
                            } catch (e: Exception) {
                                Log.d("handleSyncNotifications", "Erro inesperado: ${e.message}")
                                // Trate outros erros conforme necessário
                            }
                        }
                    }

                    else -> {
                        Log.d("handleSyncNotifications", "Comando desconhecido na notificação.")
                    }
                }
            } else {
                Log.d(
                    "handleSyncNotifications",
                    "userId na notificação não corresponde ao usuário atual."
                )
            }
        }
    }

    // Função principal para obter e lidar com as notificações
    private suspend fun processSyncNotifications(userId: String) {
        val (successSyncNotify, listSyncNotify) = getSyncNotificationsSuspend(userId)
        if (successSyncNotify) {
            handleSyncNotifications(userId, listSyncNotify)
        } else {
            Log.d("processSyncNotifications", "Nenhuma notificação encontrada.")
        }
    }

    // Métodos auxiliares para refatoração
    private fun shouldWaitForNextSync(): Boolean {
        println("Tempo de execução da sincronização: ${System.currentTimeMillis() - lastSyncTime} ms")
        val currentTime = System.currentTimeMillis()
        return currentTime - lastSyncTime < syncInterval
    }

    private suspend fun fetchData(userId: String): Pair<List<WorkspaceModel>, List<WorkspaceModel>> =
        retry {
            withContext(Dispatchers.IO) {
                val localData = withContext(Dispatchers.IO) {
                    WorkspaceModel.fromWorkspaceWithAccessList(
                        getWorkspacesWithAccess(userId)
                    )
                }

                val onlineData = loadListWorkspace(userId)

                println("Dados carregados - Local: ${localData.size}, Online: ${onlineData.size}")

                return@withContext Pair(localData, onlineData)
            }
        }

    private suspend fun getSyncNotificationsSuspend(userId: String) = retry {
        suspendCancellableCoroutine { continuation ->
            getSyncNotifications(userId) { res, list ->
                continuation.resume(Pair(res, list))
            }
        }
    }

    private suspend fun loadListWorkspace(userId: String) =
        retry {
            suspendCancellableCoroutine { continuation ->
                loadListWorkspace(userId) { list ->
                    continuation.resume(list)
                }
            }
        }

    private fun identifyMissingData(
        workspacesNeedsSync: List<WorkspaceModel>,
        localData: List<WorkspaceModel>,
        onlineData: List<WorkspaceModel>
    ): Pair<Set<String>, Set<String>> {
        val workspacesNeedsSyncIds = workspacesNeedsSync.map { it.id }.toSet()
        val localIds = localData.map { it.id }.toSet()
        val onlineIds = onlineData.map { it.id }.toSet()

        println("onlineIds = ${onlineIds.size} - localIds ${localIds.size}")
        return Pair(workspacesNeedsSyncIds, onlineIds - localIds)
    }

    private suspend fun saveWorkspaceFirebaseSuspend(
        workspaceModel: WorkspaceModel,
        userId: String,
    ): Pair<Boolean, String> = retry {
        withContext(Dispatchers.IO) {
            val oldWorkspace = loadWorkspaceRoom(workspaceModel.id)

            if (oldWorkspace != null && oldWorkspace.id.isNotBlank()) {
                println("Salvando workspace no Firebase: ${workspaceModel.id}")

                val (success, firebaseId) = suspendCancellableCoroutine<Pair<Boolean, String>> { continuation ->
                    saveWorkspaceFirebase(userId, workspaceModel) { success, firebaseId ->
                        if (success && oldWorkspace.id != firebaseId) {
                            launch {
                                workspaceDao.updateSyncStatus(oldWorkspace.id, false)

                                val existingWorkspace = loadWorkspaceRoom(firebaseId)
                                if (existingWorkspace == null) {
                                    Log.d(
                                        "Update workspace id",
                                        "Update id | newId: $firebaseId , oldId: ${oldWorkspace.id}"
                                    )
                                    workspaceDao.updateId(firebaseId, oldWorkspace.id)
                                } else {
                                    Log.d(
                                        "Update workspace id",
                                        "New ID already exists, skipping update"
                                    )
                                }
                            }
                        } else {
                            Log.d("save workspace", "Falha no salvamento do workspace")
                        }
                        continuation.resume(Pair(success, firebaseId ?: ""))
                    }
                }

                return@withContext Pair(success, firebaseId)
            } else {
                Log.d("SaveWorkspace", "Workspace ID is blank, skipping save")
                return@withContext Pair(false, "")
            }
        }
    }

    private suspend fun needsSaveOnlineData(
        missingOnlineIds: Set<String>,
        userId: String,
        localData: List<WorkspaceModel>
    ): Boolean = retry {
        println("Iniciando salvamento de dados online. IDs faltando: ${missingOnlineIds.size}")

        if (missingOnlineIds.isEmpty()) return@retry true

        return@retry try {
            val deferredResult = coroutineScope {
                missingOnlineIds.map { id ->
                    val workspaceModel = localData.first { it.id == id }
                    async(Dispatchers.IO) {
                        saveWorkspaceFirebaseSuspend(
                            workspaceModel,
                            userId
                        )
                    }
                }
            }

            val results = deferredResult.awaitAll()

            results.forEach { (_, id) ->
                println("Workspace ID : $id")
            }

            println("Resultado do salvamento online: $results")
            results.all { (success, _) -> success }
        } catch (e: Exception) {
            println("Erro ao salvar os dados online de sincronização: ${e.message}")
            false
        }
    }

    private suspend fun needsSaveOfflineData(
        missingRoomIds: Set<String>,
        userId: String,
        onlineData: List<WorkspaceModel>
    ): Boolean = retry {
        if (missingRoomIds.isEmpty()) return@retry true

        return@retry try {
            missingRoomIds.map { workspaceId ->
                val workspaceSaveOff = workspaceDao.getWorkspace(workspaceId)

                if (workspaceSaveOff == null) {
                    val workspaceModel = onlineData.first { it.id == workspaceId }
                    val workspace = workspaceModel.toWorkspaceEntity(false)
                        .apply { firebaseId = workspaceId; needsSync = false }

                    workspaceDao.insert(workspace)
                    saveWorkspaceAccessRoom(workspace.id, userId)
                } else {
                    saveWorkspaceAccessRoom(workspaceId, userId)
                }
            }.all { true }
        } catch (e: Exception) {
            println("Erro ao salvar os dados offline de sincronização: ${e.message}")
            false
        }
    }

    private suspend fun updateWorkspaceSuspend(
        workspaceModel: WorkspaceModel
    ): Boolean = retry {
        suspendCoroutine { continuation ->
            updateWorkspace(workspaceModel) { success, _ ->
                continuation.resume(success)
            }
        }
    }

    private suspend fun updateNeedingUpdateData(
        userId: String,
        onlineData: List<WorkspaceModel>
    ): Boolean = retry {
        return@retry try {
            val listWorkspaceNeedingUpdate = getWorkspacesNeedingUpdate(userId)
            println("Workspace necessitando de atualização: ${listWorkspaceNeedingUpdate.size}")

            if (listWorkspaceNeedingUpdate.isEmpty()) return@retry true

            val listWorkspaceNeedingUpdateOffId =
                listWorkspaceNeedingUpdate.map { it.id }.toSet()

            val listUpdateOnlineData =
                onlineData.filter { it.id in listWorkspaceNeedingUpdateOffId }
            println("Total de elementos que necessitam de atualização: ${listWorkspaceNeedingUpdateOffId.size}")

            val updateResults = coroutineScope {
                listUpdateOnlineData.mapNotNull { workspaceModel ->
                    val workspaceEntity =
                        listWorkspaceNeedingUpdate.firstOrNull { it.id == workspaceModel.id }

                    if (workspaceEntity != null) {
                        val editWorkspaceModel = WorkspaceModel.fromEntity(workspaceEntity)
                            .apply { id = workspaceModel.id }
                        async(Dispatchers.IO) {
                            updateSyncStatus(editWorkspaceModel.id, false)
                            updateWorkspaceSuspend(editWorkspaceModel)
                        }
                    } else {
                        null
                    }
                }
            }.awaitAll()

            println("Cidadãos atualizados com sucesso no Firebase: $updateResults")
            updateResults.all { it } // Verifica se todas as atualizações foram bem-sucedidas
        } catch (e: Exception) {
            println("Erro ao atualizar cidadãos que necessitam de sincronização: ${e.message}")
            false
        }
    }

    interface SynchronizationWorkspaceCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }

    private suspend fun getWorkspacesNeedingUpdate(userId: String): List<Workspace> {
        return workspaceDao.getWorkspacesNeedingUpdate(userId)
    }

    suspend fun updateSyncStatus(
        workspaceId: String,
        needsSync: Boolean
    ) {
        workspaceDao.updateSyncStatus(workspaceId, needsSync)
    }

    suspend fun updateNeedingUpdateStatus(
        workspaceId: String,
        needsUpdate: Boolean
    ) {
        workspaceDao.updateNeedingUpdateStatus(workspaceId, needsUpdate)
    }

    suspend fun updateIdAndSyncStatus(
        firebaseId: String,
        offWorkspaceId: String,
        creator: String,
        needsSync: Boolean
    ) {
        workspaceDao.updateIdAndSyncStatus(firebaseId, offWorkspaceId, creator, needsSync)
    }

    private suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 1000, // 1 segundo
        maxDelay: Long = 10000, // 10 segundos
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block() // Tenta executar o bloco de código
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
                .coerceAtMost(maxDelay) // Aumenta o delay exponencialmente
        }
        return block() // Tenta uma última vez ou lança a exceção
    }
}


//                        val syncRef = referenceSyncNotify.child(memberId).push()
//                        syncNotification.id = syncRef.key ?: ""
//
//                        val notifyTask = syncRef.setValue(syncNotification)
//                        tasks.add(notifyTask)
//
//                        notifyTask.addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                println("Notificação enviada com sucesso para o usuário $memberId.")
//                            } else {
//                                println("Erro ao enviar notificação para o usuário $memberId: ${task.exception?.message}")
//                            }
//                        }
//
//                        // Cria o caminho específico para o userId e define como nulo para removê-lo
//                        val pathToRemove = "userIds/$memberId"
//                        val updateTask =
//                            reference.child(workspaceId).child(pathToRemove).setValue(null)
//                        tasks.add(updateTask)
//
//                        updateTask.addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                println("Membro $memberId removido com sucesso do mapa de membros.")
//                            } else {
//                                println("Falha ao remover membro $memberId: ${task.exception?.message}")
//                            }
//                        }