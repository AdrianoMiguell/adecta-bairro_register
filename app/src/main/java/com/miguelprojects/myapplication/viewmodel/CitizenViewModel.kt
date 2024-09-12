package com.miguelprojects.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.room.entity.Citizen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CitizenViewModel(
    private val repository: CitizenRepository,
) : ViewModel() {
    private val _citizenModel = MutableLiveData<CitizenModel>()
    val citizenModel: LiveData<CitizenModel> get() = _citizenModel

    private val _citizenListModel = MutableLiveData<List<CitizenModel>>()
    val citizenListModel: LiveData<List<CitizenModel>> get() = _citizenListModel

    private val _citizenListRoom = MutableLiveData<List<Citizen>>()
    val citizenListRoom: LiveData<List<Citizen>> get() = _citizenListRoom

    private var citizenListener: ChildEventListener? = null
    private val citizenIds = mutableSetOf<String>()

    private val standardCategory = mutableMapOf<String, Any>(
        "searchValue" to "",
        "sexCategory" to "t",
        "limitValue" to 50,
        "searchCategory" to "",
        "ageCategory" to 0,
        "orderAlphabet" to true
    )

    private var filtersCitizenList = mutableMapOf<String, Any>(
        "searchValue" to "",
        "sexCategory" to "t",
        "limitValue" to 50,
        "searchCategory" to "",
        "ageCategory" to 0,
        "orderAlphabet" to true
    )

    fun saveCitizenData(
        workspaceId: String,
        citizenModel: CitizenModel,
        callback: (Boolean, String) -> Unit
    ) {
        repository.saveCitizen(workspaceId, citizenModel) { res, citizenId ->
            callback(res, citizenId)
        }
    }

    fun saveCitizenRoom(citizen: Citizen, callback: (String) -> Unit) {
        viewModelScope.launch {
            val citizenId = repository.saveCitizenRoom(citizen)
            callback(citizenId)
        }
    }

    fun loadCitizenDataRoom(offCitizenId: String, callback: (Citizen?) -> Unit) {
        viewModelScope.launch {
            val citizenData = repository.loadCitizenRoom(offCitizenId)
            if (citizenData != null) {
                Log.d("Room load citizen", "Dados do cidadão encontrados!")
            } else {
                Log.d("Room load citizen", "Dados não encontrados do cidadão!")
            }
            callback(citizenData)
        }
    }

    fun loadCitizenData(citizenId: String, callback: (Boolean) -> Unit) {
        repository.loadCitizen(citizenId) { citizen ->
            _citizenModel.value = citizen
            if (citizen.id.isNotEmpty()) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun loadListCitizens(workspaceId: String, limit: Int, orderAlphabet: Boolean) {
        filtersCitizenList = standardCategory
        repository.loadListCitizens(workspaceId, limit, orderAlphabet) { listCitizens ->
            val sortedList = if (orderAlphabet) {
                listCitizens.sortedBy { it.name }.take(limit)
            } else {
                listCitizens.sortedByDescending { it.name }.take(limit)
            }
            _citizenListModel.value = sortedList
        }
    }

    fun sizeListCitizens(workspaceId: String, limit: Int, callback: (Int) -> Unit) {
        repository.loadListCitizens(workspaceId, limit, true) { list ->
            callback(list.size)
        }
    }

    fun loadListCitizensRoom(
        offWorkspaceId: String,
        limit: Int,
        orderAlphabet: Boolean,
        callback: (List<Citizen>) -> Unit
    ) {
        viewModelScope.launch {
            filtersCitizenList = standardCategory

            val listCitizens = repository.loadListCitizensRoom(offWorkspaceId, limit, orderAlphabet)
            val sortedList = if (orderAlphabet) {
                listCitizens.sortedBy { it.name }.take(limit)
            } else {
                listCitizens.sortedByDescending { it.name }.take(limit)
            }
            _citizenListRoom.value = sortedList
            callback(sortedList)
        }
    }

    fun getAllCitizensNotNeedingSync(offWorkspaceId: String, callback: (List<Citizen>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getAllCitizensNotNeedingSync(offWorkspaceId)
            callback(list)
        }
    }

    fun updateListCitizens(newCitizenModel: CitizenModel) {
        val orderAlphabet = filtersCitizenList["orderAlphabet"] as? Boolean?

        val currentList = _citizenListModel.value.orEmpty().toMutableList()
        currentList.add(newCitizenModel)
        val orderCurrentList = if (orderAlphabet != null && !orderAlphabet) {
            currentList.sortedByDescending { it.name }
        } else {
            currentList.sortedBy { it.name }
        }
        _citizenListModel.value = orderCurrentList
    }

    fun updateCitizenInList(updatedCitizen: CitizenModel, workspaceId: String) {
        val orderAlphabet = filtersCitizenList["orderAlphabet"] as? Boolean?

        val currentList = _citizenListModel.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedCitizen.id }
        if (index != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val citizenEntity = Citizen.fromCitizenModel(updatedCitizen, workspaceId).apply {
                    needsUpdate = false
                    needsSync = false
                    isDelete = false
                }

                repository.updateCitizenRoom(citizenEntity)
            }

            currentList[index] = updatedCitizen
            val orderCurrentList = if (orderAlphabet != null && !orderAlphabet) {
                currentList.sortedByDescending { it.name }
            } else {
                currentList.sortedBy { it.name }
            }
            _citizenListModel.value = orderCurrentList
        }
    }

    fun removeCitizenInList(removedCitizen: CitizenModel, workspaceId: String) {
        val currentList = _citizenListModel.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == removedCitizen.id }
        if (index != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val citizenEntity = Citizen.fromCitizenModel(removedCitizen, workspaceId).apply {
                    needsSync = false
                    needsUpdate = false
                    isDelete = false
                }

                repository.deleteCitizenRoom(citizenEntity)
            }

            currentList.removeAt(index)
            _citizenListModel.value = currentList
        }
    }

    fun offRemoveCitizenInList(removedCitizen: CitizenModel) {
        val currentList = _citizenListModel.value.orEmpty().toMutableList()
        println(currentList)
        val index = currentList.indexOfFirst { it.id == removedCitizen.id }
        if (index != -1) {
            currentList.removeAt(index)
            _citizenListModel.value = currentList
        }
    }

    fun searchCitizenByFieldFirebase(
        workspaceId: String,
        mapCategory: Map<String, Any>,
        callback: (Boolean, List<CitizenModel>) -> Unit
    ) {
        filtersCitizenList = mapCategory.toMutableMap()

        try {
            repository.searchCitizenByFieldFirebase(workspaceId, mapCategory) { list ->
                Log.d("searchCitizenByFieldFirebase", "Tudo ok ao pesquisar")
                _citizenListModel.value = list
                callback(true, list)
            }
        } catch (e: Exception) {
            Log.d("searchCitizenByFieldFirebase", "Erro ao pesquisar: ${e.message}")
            _citizenListModel.value = emptyList()
            callback(false, emptyList())
        }
    }

    fun searchCitizenByFieldRoom(
        offWorkspaceId: String,
        mapCategory: Map<String, Any>,
        callback: (Boolean, List<Citizen>) -> Unit
    ) {
        viewModelScope.launch {
            filtersCitizenList = mapCategory.toMutableMap()

            try {
                val list = repository.searchCitizenByFieldRoom(
                    offWorkspaceId,
                    filtersCitizenList
                )
                callback(true, list)
            } catch (e: Exception) {
                println("Erro ao pesquisar: ${e.message}")
                callback(false, emptyList())
            }
        }
    }

    fun updateCitizenFirebase(
        citizenModel: CitizenModel,
        workspaceId: String,
        callback: (Boolean, String) -> Unit
    ) {
        repository.updateCitizenFirebase(citizenModel, workspaceId) { res, message ->
            callback(res, message)
        }
    }

    fun updateCitizenRoom(citizen: Citizen, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateCitizenRoom(citizen)
            callback(res)
        }
    }

    fun deleteCitizenFirebase(
        citizenModel: CitizenModel,
        workspaceId: String,
        callback: (Boolean) -> Unit
    ) {
        try {
            repository.deleteCitizenFirebase(citizenModel, workspaceId) { res ->
                callback(res)
                Log.e(
                    "deleteCitizenFirebase", if (res) {
                        "Tudo ok"
                    } else {
                        "Erro ao excluir cidadao"
                    }
                )
            }
        } catch (e: Exception) {
            callback(false)
            Log.e("deleteCitizenFirebase", "Erro ao excluir cidadao: ${e.message}")
        }
    }

    fun deleteCitizenRoom(citizen: Citizen) {
        viewModelScope.launch {
            try {
                repository.deleteCitizenRoom(citizen)
                // Operação de exclusão bem-sucedida
            } catch (e: Exception) {
                // Tratar exceção
                Log.e("deleteCitizenRoom", "Erro ao excluir cidadao: ${e.message}")
            }
        }
    }

    fun deleteListCitizenFirebase(
        workspaceId: String,
        listCitizenModel: List<CitizenModel>,
        callback: (Boolean) -> Unit
    ) {
        if (listCitizenModel.isEmpty()) {
            callback(true) // Retorne true imediatamente se não houver cidadãos para deletar
            return
        }

        try {
            // Lista para armazenar resultados das deleções
            val deleteResults = mutableListOf<Deferred<Boolean>>()

            // Inicia a deleção de cada cidadão de forma assíncrona
            CoroutineScope(Dispatchers.IO).launch {
                listCitizenModel.forEach { citizenModel ->
                    val deleteResult = async {
                        suspendCancellableCoroutine<Boolean> { continuation ->
                            repository.deleteCitizenFirebase(citizenModel, workspaceId) { res ->
                                continuation.resume(res)
                            }
                        }
                    }
                    deleteResults.add(deleteResult)
                }

                // Aguarde até que todas as deleções sejam concluídas
                val allResults = deleteResults.awaitAll()

                // Verifica se todas as deleções foram bem-sucedidas
                if (allResults.all { it }) {
                    callback(true) // Todos foram deletados com sucesso
                } else {
                    callback(false) // Alguma deleção falhou
                }
            }
        } catch (e: Exception) {
            Log.d("deleteListCitizenFirebase", "Ação de deletar mal sucedida")
            callback(false)
        }
    }

    fun deleteListCitizenRoom(
        listCitizen: List<Citizen>,
        isConnected: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = if (isConnected) {
                    repository.deleteListCitizensRoom(listCitizen)
                    true
                } else {
                    if (listCitizen.isEmpty()) {
                        true
                    } else {
                        listCitizen.all { citizen ->
                            repository.scheduleDeleteCitizenRoom(citizen.id, true)
                        }
                    }
                }
                callback(result)
            } catch (e: Exception) {
                Log.d("deleteListCitizenRoom", "Ação de deletar mal sucedida", e)
                callback(false)
            }
        }
    }


    fun restoreListCitizenFirebase(
        workspaceId: String,
        listCitizenModel: List<CitizenModel>,
        callback: (Boolean) -> Unit
    ) {
        val totalCitizens = listCitizenModel.size
        var restoreCount = 0

        if (totalCitizens == 0) {
            callback(true) // Se não há cidadãos para deletar, retorne true imediatamente
            return
        }

        try {
            for (citizenModel in listCitizenModel) {
                repository.updateActiveCitizenFirebase(
                    citizenModel,
                    workspaceId,
                    true
                ) { res ->
                    if (!res) {
                        callback(false)
                        return@updateActiveCitizenFirebase
                    }

                    restoreCount++
                    if (restoreCount == totalCitizens) {
                        callback(true) // Chama o callback apenas após todos os cidadãos serem deletados
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("deleteListCitizenFirebase", "Ação de deletar mal sucedida")
            callback(false)
        }
    }

    fun restoreListCitizenRoom(
        listCitizen: List<Citizen>,
        needsUpdate: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (listCitizen.isEmpty()) {
                    callback(true) // Nenhuma ação necessária, retorne true
                    return@launch
                }

                // Acumular resultados da deleção
                var allDeleted = true

                for (citizen in listCitizen) {
                    try {
                        repository.inactiveCitizenRoom(citizen.id, true, needsUpdate)
                    } catch (e: Exception) {
                        allDeleted = false
                        Log.d("inactiveCitizenRoom", "Erro no inactiveCitizenRoom: ${e.message}")
                        break // Interrompe o loop ao encontrar uma falha
                    }
                }

                callback(allDeleted) // Retorna true se todos foram deletados, caso contrário, false
            } catch (e: Exception) {
                Log.d("deleteListCitizenRoom", "Ação de deletar mal sucedida", e)
                callback(false)
            }
        }
    }

    fun restoreCitizensFirebaseList(
        listCitizenModel: List<CitizenModel>,
        workspaceId: String,
        active: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var allSucceeded = true // Flag para rastrear o sucesso de todas as operações

                // Processar a lista de cidadãos
                listCitizenModel.forEach { citizenModel ->
                    val result = suspendCancellableCoroutine<Boolean> { continuation ->
                        repository.updateActiveCitizenFirebase(
                            citizenModel,
                            workspaceId,
                            active
                        ) { res ->
                            continuation.resume(res)
                        }
                    }

                    if (!result) {
                        allSucceeded = false
                        return@forEach // Interrompe o loop se qualquer operação falhar
                    }
                }

                // Chamar o callback com o resultado final
                Log.e("updateActiveCitizenFirebaseList", "atualizações mal sucedida!")
                callback(allSucceeded)
            } catch (e: Exception) {
                Log.e("updateActiveCitizenFirebaseList", "Erro ao atualizar cidadão: ${e.message}")
                callback(false) // Retorna false em caso de exceção
            }
        }
    }


    fun updateActiveCitizenFirebase(
        citizenModel: CitizenModel,
        workspaceId: String,
        active: Boolean,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Operação de exclusão bem-sucedida
                repository.updateActiveCitizenFirebase(citizenModel, workspaceId, active) { res ->
                    callback(res)
                    Log.e(
                        "inactiveCitizenFirebase", if (res) {
                            "Tudo ok"
                        } else {
                            "Erro desconhecido ao inativar"
                        }
                    )
                }
            } catch (e: Exception) {
                // Tratar exceção
                Log.e("inactiveCitizenFirebase", "Erro ao excluir cidadao: ${e.message}")
            }
        }
    }

    fun inActiveCitizenRoom(offCitizenId: String, active: Boolean, needsUpdate: Boolean) {
        viewModelScope.launch {
            try {
                // Operação de exclusão bem-sucedida
                repository.inactiveCitizenRoom(offCitizenId, active, needsUpdate)
            } catch (e: Exception) {
                // Tratar exceção
                Log.e("Deletar citizen - view model", "Erro ao excluir cidadao: ${e.message}")
            }
        }
    }

    fun loadInactiveCitizenFirebase(workspaceId: String) {
        repository.loadInactiveCitizenFirebase(workspaceId) { list ->
            _citizenListModel.value = list
        }
    }

    fun loadInactiveCitizenRoom(offWorkspaceId: String, callback: (List<Citizen>) -> Unit) {
        viewModelScope.launch {
            val list = repository.loadInactiveCitizenRoom(offWorkspaceId)
            callback(list)
        }
    }

    fun verifyExistsCitizenByOffId(
        workspaceId: String,
        offCitizenId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        repository.verifyExistsCitizenByOffId(workspaceId, offCitizenId) { res, citizenId ->
            callback(res, citizenId)
        }
    }

    private fun areMapsEqual(): Boolean {
        return standardCategory.all { (key, value) ->
            filtersCitizenList[key] == value
        }
    }

    // Função de extensão para verificar se um ID está na lista de cidadãos
    private fun List<CitizenModel>.containsCitizenId(id: String): Boolean {
        return this.any { it.id == id }
    }

    fun startListFirebaseListener(workspaceId: String) {
        if (workspaceId.isEmpty()) {
            Log.d("loadListFirebaseListener", "Workspace Id está vazio.")
            return
        }

        citizenListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val newCitizen = snapshot.getValue(CitizenModel::class.java)
                    newCitizen?.let {
                        if (!repository.isSync) {
                            // Verifique se a sincronização está ocorrendo
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(300) // Aguarde 300ms antes de prosseguir para evitar duplicações

                                // Carregar a lista de cidadãos do Firebase
                                repository.loadAllListCitizens(workspaceId) { listFirebase ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        // Carregar a lista de cidadãos do Room
                                        val listRoom =
                                            repository.loadAllListCitizensRoom(workspaceId)

                                        // Verifique se as listas são diferentes em tamanho
                                        if (listRoom.size != listFirebase.size) {
                                            // Verifique se o cidadão já está salvo no Room
                                            val existingCitizen =
                                                repository.loadCitizenRoom(newCitizen.id)

                                            if (existingCitizen == null) {
                                                // Salvar o cidadão no Room se não estiver salvo
                                                val citizenEntity = Citizen.fromCitizenModel(
                                                    newCitizen,
                                                    workspaceId
                                                )
                                                citizenEntity.needsSync = false
                                                citizenEntity.needsUpdate = false

                                                println("Salvando esse dado: $citizenEntity do id = ${newCitizen.id}")
                                                repository.saveCitizenRoom(citizenEntity)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val limit = filtersCitizenList["limitValue"] as? Int ?: 50
                        if (_citizenListModel.value?.size != limit && newCitizen.active) {
                            if (!areMapsEqual()) {
                                val searchCategory =
                                    filtersCitizenList["searchCategory"] as? String ?: ""
                                val ageCategory: Int =
                                    filtersCitizenList["ageCategory"] as? Int ?: 0
                                val sexCategory: String =
                                    filtersCitizenList["sexCategory"] as? String ?: ""
                                val searchValue = filtersCitizenList["searchValue"] ?: ""

                                var filterAndSearch = true
                                var stringSearchValue = searchValue.toString().trim()

                                if (searchCategory.isNotEmpty() && stringSearchValue.isNotEmpty() && sexCategory != "t" && ageCategory != 0
                                ) {
                                    filterAndSearch = repository.filterAndSearchDataCitizens(
                                        it,
                                        searchCategory,
                                        ageCategory,
                                        sexCategory,
                                        stringSearchValue
                                    )
                                }

                                // Verifique se o cidadão já está na lista antes de adicionar
                                val currentCitizens = _citizenListModel.value ?: emptyList()
                                if (filterAndSearch && !currentCitizens.containsCitizenId(it.id) && citizenIds.add(
                                        it.id
                                    )
                                ) {
                                    updateListCitizens(it)
                                }
                            } else if (!(_citizenListModel.value ?: emptyList()).containsCitizenId(
                                    it.id
                                ) && citizenIds.add(it.id)
                            ) {
                                updateListCitizens(it)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Erro inesperado: ${e.message}")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val changedCitizen = snapshot.getValue(CitizenModel::class.java)
                changedCitizen?.let { updateCitizenInList(it, workspaceId) }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removeCitizen = snapshot.getValue(CitizenModel::class.java)
                removeCitizen?.let { removeCitizenInList(it, workspaceId) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Reordenar a lista se necessário
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WorkspaceMainFragment", "Database error: ${error.message}")
            }
        }

        val citizensRef =
            FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
                .child("citizens")
        citizensRef.addChildEventListener(citizenListener!!)
    }

    fun cancelListFirebaseListener(workspaceId: String) {
        val citizensRef =
            FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
                .child("citizens")

        citizenListener?.let {
            citizensRef.removeEventListener(it)
            citizenListener = null
        }
    }

    fun updateSyncStatus(offCitizenId: String, needsSync: Boolean, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateSyncStatus(offCitizenId, needsSync)
            callback(res)
        }
    }

    fun scheduleDeleteCitizenRoom(offCitizenId: String, isDelete: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateSyncDelete(offCitizenId, isDelete)
            } catch (e: Exception) {
                Log.e("scheduleDeleteCitizenRoom", "Erro ao excluir cidadao: ${e.message}")
            }
        }
    }

    fun getCitizensNeedingDelete(offWorkspaceId: String, callback: (List<Citizen>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getCitizensNeedingDelete(offWorkspaceId)
            callback(list)
        }
    }

    fun updateSyncDelete(offCitizenId: String, isDelete: Boolean) {
        viewModelScope.launch {
            repository.updateSyncDelete(offCitizenId, isDelete)
        }
    }

    fun getWorkspaceIdSynchronized(callback: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            var workspaceId: String

            repeat(5) {
                workspaceId = repository.workspaceIdSynchronized
                if (workspaceId.isNotEmpty()) {
                    callback(true, workspaceId)
                    return@launch
                } else {
                    delay(1000) // Aguarda 1 segundo antes de tentar novamente
                }
            }

            // Se chegar aqui, significa que não conseguiu obter o workspaceId em 5 tentativas
            callback(false, "")
        }
    }
}

