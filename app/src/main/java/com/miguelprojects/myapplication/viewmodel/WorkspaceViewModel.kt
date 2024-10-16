package com.miguelprojects.myapplication.viewmodel

import WorkspaceRepository
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.miguelprojects.myapplication.model.SyncNotificationsModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.room.entity.AccessWithUser
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.room.entity.WorkspaceWithAccess
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val repository: WorkspaceRepository,
) : ViewModel() {
    private val _workspaceModel = MutableLiveData<WorkspaceModel>()
    val workspaceModel: LiveData<WorkspaceModel> get() = _workspaceModel

    private val _workspaceListModel = MutableLiveData<List<WorkspaceModel>>()
    val workspaceListModel: LiveData<List<WorkspaceModel>> get() = _workspaceListModel

    private val _workspaceListRoom = MutableLiveData<List<Workspace>>()
    val workspaceListRoom: LiveData<List<Workspace>> get() = _workspaceListRoom

    private val _workspacesWithAccess = MutableLiveData<List<WorkspaceWithAccess>>()
    val workspacesWithAccess: LiveData<List<WorkspaceWithAccess>> get() = _workspacesWithAccess

    private val workspacesRef = FirebaseDatabase.getInstance().getReference("workspaces")
    private var workspaceListener: ChildEventListener? = null

    private val workspaceIds = mutableSetOf<String>()

    fun saveData(userId: String, workspace: WorkspaceModel, callback: (Boolean, String) -> Unit) {
        repository.saveWorkspaceFirebase(userId, workspace, callback)
    }

//    fun saveOffWorkspaceIdFirebase(userId: String, workspaceModel: WorkspaceModel) {
//        repository.saveOffWorkspaceIdFirebase(workspaceModel, userId)
//    }

    fun setListData(workspace: WorkspaceModel) {
        val currentList = _workspaceListModel.value ?: emptyList()
        val updateList = currentList.toMutableList()
        updateList.add(workspace)
        Log.d("TESTE SETListData", "Update List: ${updateList.size}")
        Log.d("TESTE Current List", "Update List: ${currentList.size}")
        _workspaceListModel.postValue(updateList)
    }

    fun loadListData(userId: String) {
        if (userId.isNotEmpty()) {
            repository.loadListWorkspace(userId) { list ->
                val orderList = list.sortedBy { it.name }.toList()
                _workspaceListModel.value = orderList
            }
            Log.d("Teste Workspace view model - Load List", "Lista pega com sucesso!")
        } else {
            Log.d("Teste Workspace view model - Load List", "User id nulo")
        }
    }

    fun updateData(workspaceModel: WorkspaceModel, callback: (Boolean, String) -> Unit) {
        repository.updateWorkspace(workspaceModel) { res, message ->
            callback(res, message)
        }
    }

    fun updateWorkspaceRoom(workspace: Workspace) {
        viewModelScope.launch {
            try {
                repository.updateWorkspaceRoom(workspace)
            } catch (e: Exception) {
                Log.d("Workspace View Model - Update", "Erro: ${e.message}")
            }
        }
    }

    fun saveDataRoom(
        workspace: Workspace,
        userId: String,
        needsSync: Boolean,
        callback: (String) -> Unit
    ) {
        viewModelScope.launch {
            val workspaceId = repository.saveWorkspaceRoom(workspace, userId, needsSync)
            callback(workspaceId)
        }
    }

    fun loadListDataRoom(userId: String) {
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val list = repository.loadListWorkspaceRoom(userId)
                    _workspaceListRoom.value = list
                    println("Tamanho da lista em workspace view model: ${list.size}")
                } catch (e: Exception) {
                    println("Erro ao executar o loadListDataRoom")
                    return@launch
                }
            }
        } else {
            println("User Id está vazio!")
        }
    }

    fun loadData(workspaceId: String) {
        repository.loadWorkspace(workspaceId) { workpace ->
            _workspaceModel.value = workpace
        }
    }

    //    colocar função de pegar os dados do workspace off
    fun loadDataRoom(workspaceId: String, callback: (Workspace?) -> Unit) {
        viewModelScope.launch {
            val workspace = repository.loadWorkspaceRoom(workspaceId)
            callback(workspace)
        }
    }

    fun getWorkspacesWithAccess(userId: String) {
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    val list = repository.getWorkspacesWithAccess(userId)
                    val orderList = list.sortedBy { it.workspace.name }.toList()
                    _workspacesWithAccess.value = orderList
                    println("Tamanho da lista em workspace view model: ${orderList.size}")
                } catch (e: Exception) {
                    println("erro no getWorkspacesWithAccess: ${e.message}")
                }
            }
        } else {
            println("User Id está vazio!")
        }
    }

    fun getUsersForWorkspace(workspaceId: String, callback: (List<AccessWithUser>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getUsersForWorkspace(workspaceId)
            callback(list)
        }
    }

    fun getSyncNotifications(userId: String, callback: (Boolean, List<SyncNotificationsModel>) -> Unit) {
        repository.getSyncNotifications(userId) { success, listSyncNotify ->
            callback(success, listSyncNotify)
        }
    }

    fun removeWorkspaceMembers(workspaceId: String, listMembersToRemove: List<String>) {
        println("Total de users para remover: ${listMembersToRemove.size}")
        repository.removeListWorkspaceMembers(workspaceId, listMembersToRemove) { success ->
            if (success) {
                viewModelScope.launch {
                    repository.removeListWorkspaceMembersRoom(workspaceId, listMembersToRemove)
                }
            }
        }
    }

    fun updateListWorkspaces(
        workspace: Workspace,
        newWorkspaceModel: WorkspaceModel,
        userId: String
    ) {
        println("Passando pelo updateListWorkspaces - ${workspace.id}")
        val currentList = _workspaceListModel.value.orEmpty().toMutableList()
        currentList.add(newWorkspaceModel)
        _workspaceListModel.value = currentList
    }

    fun updateWorkspaceInList(updatedWorkspace: WorkspaceModel, workspace: Workspace) {
        val currentList = _workspaceListModel.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedWorkspace.id }
        if (index != -1) {
            currentList[index] = updatedWorkspace
            viewModelScope.launch {
                repository.updateWorkspaceRoom(workspace)
            }
            _workspaceListModel.value = currentList
        }
    }

    fun updateListWorkspaceRoom(newWorkspace: Workspace) {
        val listWorkspaceUpdated = (_workspaceListRoom.value ?: emptyList()).toMutableList()
        Log.d(
            "Teste Workspace view model - updateListWorkspace",
            "Teste lista antiga: ${listWorkspaceUpdated.size}"
        )
        listWorkspaceUpdated.add(newWorkspace)
        Log.d(
            "Teste Workspace view model - updateListWorkspace",
            "Teste lista nova: ${listWorkspaceUpdated.size}"
        )
        _workspaceListRoom.value = listWorkspaceUpdated
    }

    fun checkExistsWorkspaceRoom(workspace: Workspace, newWorkspaceId: String, userId: String) {
        viewModelScope.launch {
            repository.checkExistsWorkspaceRoom(workspace, newWorkspaceId, userId)
        }
    }

    fun accessSecurityCheck(userId: String, workspaceId: String, callback: (Boolean) -> Unit) {
        repository.accessSecurityCheckUser(userId, workspaceId) { res ->
            if (res) {
                callback(true)
                Log.d("Teste Workspace view model - accessSecurityCheck", "Acesso permitido")
            } else {
                Log.d("Teste Workspace view model - accessSecurityCheck", "Acesso negado")
                callback(false)
            }
        }
    }

    fun deleteWorkspaceFirebase(workspaceId: String, callback: (Boolean, String) -> Unit) {
        repository.deleteWorkspaceFirebase(workspaceId) { res, message ->
            callback(res, message)
        }
    }

    fun deleteWorkspaceRoom(workspace: Workspace, userId: String) {
        viewModelScope.launch {
            try {
                repository.deleteWorkspaceRoom(workspace, userId)
            } catch (e: Exception) {
                println("Erro ao executar o loadListDataRoom")
                return@launch
            }
        }
    }

    fun removeWorkspaceMember(memberId: String, workspaceId: String, callback: (Boolean) -> Unit) {
        repository.removeWorkspaceMember(memberId, workspaceId) { res ->
            callback(res)
        }
    }

    fun removeWorkspaceInList(removedWorkspace: WorkspaceModel, userId: String) {
        val currentList = _workspaceListModel.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == removedWorkspace.id }
        if (index != -1) {
            currentList.removeAt(index)
            viewModelScope.launch {
                repository.deleteWorkspaceWithIdRoom(removedWorkspace.id, userId)
            }
            _workspaceListModel.value = currentList
        }
    }

    fun verifyExistsWorkspaceByOffId(
        workspaceId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.verifyExistsWorkspaceByOffId(workspaceId) { res, workspaceId ->
            callback(res, workspaceId)
        }
    }

    private fun checkIfIsSynchronized(): Boolean {
        return repository.isSynchronized
    }

    // Função de extensão para verificar se um ID está na lista de workspaces
    private fun List<WorkspaceModel>.containsWorkspaceId(id: String): Boolean {
        return this.any { it.id == id }
    }

    fun loadListFirebaseListener(userId: String) {
        workspaceListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val workspaceModel = snapshot.getValue(WorkspaceModel::class.java)

                // Verifique se workspaceModel não é nulo
                if (workspaceModel != null) {
                    val workspaceEntity = workspaceModel.toWorkspaceEntity(false)

                    // Verifique se o usuário tem acesso e se o ID não está na lista atual
                    val currentWorkspaces = workspaceListModel.value ?: emptyList()
                    if (workspaceModel.userIds[userId] == true && !currentWorkspaces.containsWorkspaceId(
                            workspaceModel.id
                        )
                    ) {
                        workspaceIds.add(workspaceModel.id) // Adiciona o ID à lista
                        println("updateListWorkspaces  -  Novo item adicionado ")

//                        if (!checkIfIsSynchronized()) {
//                            println("Adicionando novo item pelo Child Event Listener")
                        updateListWorkspaces(workspaceEntity, workspaceModel, userId)
//                        }
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val workspaceChanged = snapshot.getValue(WorkspaceModel::class.java)

                if (workspaceChanged != null) {
                    val workspaceEntity = workspaceChanged.toWorkspaceEntity(false)
                    updateWorkspaceInList(workspaceChanged, workspaceEntity)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val workspaceRemoved = snapshot.getValue(WorkspaceModel::class.java)
                if (workspaceRemoved != null) {
                    workspaceIds.remove(workspaceRemoved.id)
                    removeWorkspaceInList(workspaceRemoved, userId)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Reordenar a lista se necessário
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WorkspaceViewModel", "Database error: ${error.message}")
            }
        }

        workspacesRef.addChildEventListener(workspaceListener!!)
    }

    fun cancelFirebaseListener() {
        workspaceListener?.let {
            workspacesRef.removeEventListener(it)
            workspaceListener = null
        }
    }

}
