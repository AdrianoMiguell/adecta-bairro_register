package com.miguelprojects.myapplication.repository

import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.room.dao.CitizenDao
import com.miguelprojects.myapplication.room.entity.Citizen
import com.miguelprojects.myapplication.util.ConvertManager
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
import kotlin.random.Random

class CitizenRepository(private val citizenDao: CitizenDao) {
    private val workspaceLocks = ConcurrentHashMap<String, Mutex>()
    private var lastSyncTime: Long = 0 // Armazena o tempo do último salvamento
    private val syncInterval = 1000L // Intervalo de espera em milissegundos (ex: 5 segundos)
    var workspaceIdSynchronized = ""
    var isSync = false

    private val ageRanges = mapOf(
        0 to Int.MIN_VALUE..Int.MAX_VALUE, // Todas as idades
        1 to 0..10,
        2 to 10..20,
        3 to 20..30,
        4 to 30..40,
        5 to 40..60,
        6 to 60..80,
        7 to 80..Int.MAX_VALUE
    )

    private fun generateCitizenNumber(): String {
        val random = Random(System.currentTimeMillis())
        return (1..8)
            .map { random.nextInt(0, 10) }
            .joinToString("")
    }

    private fun isCitizenNumberUnique(
        workspaceId: String,
        isConnect: Boolean,
        number: String,
        callback: (Boolean) -> Unit
    ) {
        if (isConnect) {
            val reference =
                FirebaseDatabase.getInstance().getReference("workspaces/$workspaceId/citizens")
            reference.orderByChild("numberregister").equalTo(number)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        callback(!snapshot.exists())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(false) // Assume que não é único em caso de erro
                    }
                })
        } else {
            val res = citizenDao.verifyNumberRegister(number)
            callback(res == 0)
        }
    }

    private fun generateUniqueCitizenNumber(
        workspaceId: String,
        isConnect: Boolean,
        callback: (String) -> Unit
    ) {
        val newNumber = generateCitizenNumber()
        isCitizenNumberUnique(workspaceId, isConnect, newNumber) { isUnique ->
            if (isUnique) {
                callback(newNumber)
            } else {
                generateUniqueCitizenNumber(workspaceId, isConnect, callback) // Tentar novamente
            }
        }
    }

    fun saveCitizen(
        workspaceId: String,
        citizenModel: CitizenModel,
        callback: (Boolean, String) -> Unit
    ) {
        var citizenId = citizenModel.id.ifEmpty { null }

        if (workspaceId.isEmpty()) {
            callback(false, "Workspace id não informado!")
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("workspaces")
            .child(workspaceId).child("citizens").push()
        citizenModel.id = reference.key ?: ""

        generateUniqueCitizenNumber(workspaceId, true) { numbering ->
            if (citizenModel.numberregister.isEmpty()) {
                citizenModel.numberregister = numbering
            }

            reference.setValue(citizenModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                        if (citizenId != null && citizenModel.id != citizenId) {
////                          Citizen foi salvo antes no room, agora vamos atualizar a sua id
//                            CoroutineScope(Dispatchers.IO).launch {
//                                citizenDao.updateCitizenId(citizenModel.id, citizenId)
//                            }
//                        }
                        Log.d("Save citizen", "Dados salvos com sucesso!")
                        callback(true, citizenModel.id)
                    } else {
                        Log.d("Save citizen", "Dados não foram salvos!")
                        callback(false, "")
                    }
                }
                .addOnFailureListener { exception ->
                    callback(false, "")
                    Log.d("Save citizen", "Dados não foram salvos! ${exception.message}")
                }
        }
    }

    suspend fun saveCitizenRoom(citizen: Citizen): String {
        if (citizen.id.isEmpty()) {
            val id = UUID.randomUUID().toString()
            citizen.id = id
        } else if (citizen.firebaseId!!.isEmpty()) {
            citizen.firebaseId = citizen.id
        }

        if (citizen.numberregister.isEmpty()) {
            val uniqueNumber = generateCitizenNumber()
            citizen.numberregister = uniqueNumber
        }

        citizenDao.insert(citizen)

        return citizen.id
    }

    suspend fun loadCitizenRoom(offCitizenId: String): Citizen? {
        return citizenDao.getCitizen(offCitizenId)
    }

    fun loadCitizen(citizenId: String, callback: (CitizenModel) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces").child(citizenId)
            .child("citizens")
        reference.get().addOnSuccessListener { snapshot ->
            val citizen = snapshot.getValue(CitizenModel::class.java)
            if (citizen != null) {
                println("Dados : $citizen")
                callback(citizen)
            } else {
                callback(CitizenModel())
            }
        }.addOnFailureListener { exception ->
            println("Erro ao carregar os dados: ${exception.message}")
            callback(CitizenModel())
        }
    }

    fun loadListCitizens(
        workspaceId: String,
        limit: Int,
        orderAlphabet: Boolean,
        callback: (List<CitizenModel>) -> Unit
    ) {
        if (workspaceId.isEmpty()) {
            callback(emptyList())
            println("Nenhum workspaceId")
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("workspaces")
            .child(workspaceId).child("citizens")
        reference.orderByChild("active").equalTo(true).limitToLast(limit)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val citizenList = mutableListOf<CitizenModel>()
                    if (snapshot.exists()) {
                        Log.d(
                            "LoadListCitizens",
                            "Snapshot exists. Children count: ${snapshot.childrenCount}"
                        )
                        for (citizenSnapshot in snapshot.children) {
                            try {
                                // Verifique o tipo de dado antes de converter
                                if (citizenSnapshot.value is Map<*, *>) {
                                    val citizen = citizenSnapshot.getValue(CitizenModel::class.java)
                                    if (citizen != null) {
                                        citizenList.add(citizen)
                                    } else {
                                        Log.e("LoadListCitizens", "Dados inválidos para cidadão")
                                    }
                                } else {
                                    Log.e(
                                        "LoadListCitizens",
                                        "Tipo de dado inesperado: ${citizenSnapshot.value?.javaClass?.kotlin}"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("LoadListCitizens", "Erro ao converter dados: ${e.message}")
                                Log.e(
                                    "LoadListCitizens",
                                    "Tipo de dado encontrado: ${citizenSnapshot.value?.javaClass?.kotlin}"
                                )
                            }
                        }
                        Log.d("LoadListCitizens", "Citizen list size: ${citizenList.size}")

                        val sortedList = if (orderAlphabet) {
                            citizenList.sortedBy { it.name }
                        } else {
                            citizenList.sortedByDescending { it.name }
                        }

                        println("Order da lista: ${sortedList.size}")
                        callback(sortedList)
                    } else {
                        Log.d("LoadListCitizens", "No data found at the reference")
                        callback(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LoadListCitizens", "Error loading data: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    fun loadAllListCitizens(
        workspaceId: String,
        callback: (List<CitizenModel>) -> Unit
    ) {
        if (workspaceId.isEmpty()) {
            callback(emptyList())
            println("Nenhum workspaceId")
            return
        }

        val reference = FirebaseDatabase.getInstance().getReference("workspaces")
            .child(workspaceId).child("citizens")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val citizenList = mutableListOf<CitizenModel>()
                if (snapshot.exists()) {
                    for (citizenSnapshot in snapshot.children) {
                        try {
                            // Verifique o tipo de dado antes de converter
                            if (citizenSnapshot.value is Map<*, *>) {
                                val citizen = citizenSnapshot.getValue(CitizenModel::class.java)
                                if (citizen != null) {
                                    citizenList.add(citizen)
                                } else {
                                    Log.e("LoadListCitizens", "Dados inválidos para cidadão")
                                }
                            } else {
                                Log.e(
                                    "LoadListCitizens",
                                    "Tipo de dado inesperado: ${citizenSnapshot.value?.javaClass?.kotlin}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("LoadListCitizens", "Erro ao converter dados: ${e.message}")
                            Log.e(
                                "LoadListCitizens",
                                "Tipo de dado encontrado: ${citizenSnapshot.value?.javaClass?.kotlin}"
                            )
                        }
                    }

                    callback(citizenList)
                } else {
                    Log.d("LoadListCitizens", "No data found at the reference")
                    callback(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoadListCitizens", "Error loading data: ${error.message}")
                callback(emptyList())
            }
        })
    }

    suspend fun getAllCitizensNotNeedingSync(offWorkspaceId: String): List<Citizen> {
        return citizenDao.getCitizensNotNeedingSync(offWorkspaceId)
    }

    private suspend fun loadOfflineCitizenListNeedsSync(offWorkspaceId: String): List<Citizen> {
        return citizenDao.getCitizensNeedingSync(offWorkspaceId)
    }

    suspend fun loadAllListCitizensRoom(workspaceId: String): List<Citizen> {
        return citizenDao.getAllCitizens(workspaceId)
    }

    suspend fun loadListCitizensRoom(
        offWorkspaceId: String,
        limit: Int,
        orderAlphabet: Boolean,
    ): List<Citizen> {
        val list = citizenDao.getCitizensForWorkspace(offWorkspaceId, limit)
        if (orderAlphabet) {
            list.sortedBy { it.name }
        } else {
            list.sortedByDescending { it.name }
        }
        return list
    }

    private fun ifBetweenInIntervalAge(age: Int, categoryAge: Int): Boolean {
        return when (categoryAge) {
            1 -> {
                age in 0..10
            }

            2 -> {
                age in 10..20
            }

            3 -> {
                age in 20..30
            }

            4 -> {
                age in 30..40
            }

            5 -> {
                age in 40..60
            }

            6 -> {
                age in 60..80
            }

            7 -> {
                age in 80..Int.MAX_VALUE
            }

            else -> true
        }
    }

    private val fieldSearchAccessors: Map<String, (CitizenModel) -> String> = mapOf(
        "name" to { citizen -> citizen.name },
        "telephone" to { citizen -> citizen.telephone },
        "cpf" to { citizen -> citizen.cpf },
        "sus" to { citizen -> citizen.sus!! },
        "numberregister" to { citizen -> citizen.numberregister },
        "fathername" to { citizen -> citizen.fathername },
        "mothername" to { citizen -> citizen.mothername },
        "birthplace" to { citizen -> citizen.birthplace },
        "cep" to { citizen -> citizen.cep },
        "state" to { citizen -> citizen.state },
        "city" to { citizen -> citizen.city },
        "neighborhood" to { citizen -> citizen.neighborhood },
        "street" to { citizen -> citizen.street },
        "numberhouse" to { citizen -> citizen.numberhouse.toString() },
        "addons" to { citizen -> citizen.addons!! },
    )

    fun filterAndSearchDataCitizens(
        citizenModel: CitizenModel,
        searchCategory: String,
        categoryAge: Int,
        categorySex: String,
        searchValue: String
    ): Boolean {
        val accessor = fieldSearchAccessors[searchCategory]
        val valueToCheck = accessor?.invoke(citizenModel)

        if (searchValue.isNotEmpty() && (valueToCheck == null || !valueToCheck.contains(
                searchValue.trim(),
                ignoreCase = true
            ))
        ) {
            return false
        }

        if (categorySex != "t" && citizenModel.sex != categorySex) {
            return false
        }

        if (categoryAge != 0) {
            val age = ConvertManager.calculateLongAge(citizenModel.birthdate)
            return ifBetweenInIntervalAge(age, categoryAge)
        }

        return true
    }

    fun searchCitizenByFieldFirebase(
        workspaceId: String,
        filtersMap: Map<String, Any>,
        callback: (List<CitizenModel>) -> Unit
    ) {
        try {
            val searchCategory =
                filtersMap["searchCategory"] as? String ?: return callback(emptyList())
            val categoryAge: Int = filtersMap["ageCategory"] as? Int ?: return callback(emptyList())
            val categorySex: String =
                filtersMap["sexCategory"] as? String ?: return callback(emptyList())
            val searchValue = filtersMap["searchValue"] ?: return callback(emptyList())
            val limit = filtersMap["limitValue"] as? Int ?: 50
            val orderAlphabet =
                filtersMap["orderAlphabet"] as? Boolean ?: return callback(emptyList())

            val reference = FirebaseDatabase.getInstance().getReference("workspaces")
                .child(workspaceId).child("citizens")

            reference.orderByChild("active").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var listCitizens = mutableListOf<CitizenModel>()

                        for (snapshot in dataSnapshot.children) {
                            val citizenModel = snapshot.getValue(CitizenModel::class.java)

                            if (citizenModel != null) {
                                if (!citizenModel.active) {
                                    continue
                                }

                                if (searchCategory == "name" && searchValue.toString()
                                        .isEmpty() && categorySex == "t" && categoryAge == 0
                                ) {
                                    listCitizens.add(citizenModel)
                                } else {
                                    val filterAndSearch = filterAndSearchDataCitizens(
                                        citizenModel,
                                        searchCategory,
                                        categoryAge,
                                        categorySex,
                                        searchValue.toString()
                                    )

                                    if (filterAndSearch) {
                                        println("Item passou na pesquisa | ${citizenModel.id}")
                                        listCitizens.add(citizenModel)
                                    }
                                }
                            }
                        }

                        // Randomiza a lista e seleciona os 50 primeiros
                        println("Total da lista: ${listCitizens.size}")

                        val sortedList = if (orderAlphabet) {
                            listCitizens.sortedBy { it.name }.take(limit)
                        } else {
                            listCitizens.sortedByDescending { it.name }.take(limit)
                        }

                        println("Order da lista: ${sortedList.size}")
                        callback(sortedList)
                    }

                    override fun onCancelled(e: DatabaseError) {
                        // Trate o erro
                        println("Erro no search: ${e.message}")
                        callback(emptyList())
                    }
                })
        } catch (e: Exception) {
            println("Erro ao realizar a pesquisa: ${e.message}")
            callback(emptyList())
        }
    }

    suspend fun searchCitizenByFieldRoom(
        offWorkspaceId: String,
        filtersMap: Map<String, Any>
    ): List<Citizen> {
        val fieldName = filtersMap["searchCategory"] as? String ?: return emptyList()
        val ageCategory: Int = filtersMap["ageCategory"] as? Int ?: return emptyList()
        val sexCategory: String =
            filtersMap["sexCategory"] as? String ?: return emptyList()
        val searchValue = filtersMap["searchValue"] ?: return emptyList()
        val limitValue = filtersMap["limitValue"] as? Int ?: 50
        val orderAlphabet =
            filtersMap["orderAlphabet"] as? Boolean ?: return emptyList()

        val currentTimestamp = System.currentTimeMillis()

        // Obter o intervalo de idade com base na categoria
        val ageRange = ageRanges[ageCategory]

        // Montar a consulta SQL dinamicamente
        val queryBuilder =
            StringBuilder("SELECT * FROM citizen WHERE workspaceId = ? AND $fieldName LIKE ? AND active = 1")

        // Adicionar a condição de idade, se existir
        ageRange?.let {
            queryBuilder.append(" AND (CAST(? AS INTEGER) - birthdate) / (1000 * 60 * 60 * 24 * 365.25) BETWEEN ? AND ?")
        }

        // Adicionar a condição de sexo, se definido

        if (sexCategory != "t") {
            if (sexCategory.isEmpty()) {
                queryBuilder.append(" AND sex = ''") // Supondo que 'sex' seja o nome da coluna no banco de dados
            } else {
                queryBuilder.append(" AND sex = ?") // Supondo que 'sex' seja o nome da coluna no banco de dados
            }
        }

        if (orderAlphabet) {
            queryBuilder.append(" ORDER BY ? ASC") // Supondo que 'sex' seja o nome da coluna no banco de dados
        } else {
            queryBuilder.append(" ORDER BY ? DESC") // Supondo que 'sex' seja o nome da coluna no banco de dados
        }

        queryBuilder.append(" LIMIT ?") // Adicionar espaço antes de LIMIT

        // Criar o array de argumentos
        val args = mutableListOf<Any>().apply {
            add(offWorkspaceId)
            add("%$searchValue%")
            ageRange?.let {
                add(currentTimestamp)
                add(ageRange.first)
                add(ageRange.last)
            }
            if (sexCategory.isNotEmpty() && sexCategory != "t") {
                add(sexCategory)
            }
            add(searchValue)
            add(limitValue)
        }

        val finalQuery = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())

        val list = citizenDao.getCitizensByField(finalQuery)

        val sortedList = if (orderAlphabet) {
            list.sortedBy { it.name }
        } else {
            list.sortedByDescending { it.name }
        }

        return sortedList
    }

    fun updateCitizenFirebase(
        citizenModel: CitizenModel,
        workspaceId: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (workspaceId.isEmpty() || citizenModel.id.isEmpty()) {
            callback(false, "Workspace id não informado!")
            return
        } else {
            val reference = FirebaseDatabase.getInstance().getReference("workspaces")
                .child(workspaceId).child("citizens").child(citizenModel.id)

            reference.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result.exists()) {
                        try {
                            reference.updateChildren(citizenModel.toMap())
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        callback(true, "Atualização bem-sucedida!")
                                    } else {
                                        callback(false, "Erro ao atualizar os dados no banco!")
                                    }
                                }.addOnFailureListener {
                                    callback(false, "Erro ao atualizar os dados no banco!")
                                }
                        } catch (e: Exception) {
                            callback(false, "Erro ao atualizar os dados")
                            Log.d("Update citizen", "Erro ao atualizar os dados")
                        }
                    } else {
                        callback(false, "Cidadão não encontrado para atualização!")
                    }
                } else {
                    callback(false, "Erro ao verificar a existência do cidadão!")
                }
            }.addOnFailureListener {
                callback(false, "Erro ao verificar a existência do cidadão!")
            }
        }
    }

    suspend fun updateCitizenRoom(
        citizen: Citizen,
    ): Boolean {
        return try {
            citizenDao.update(citizen)
            true
        } catch (e: Exception) {
            Log.d("Update Room - citizen", "Erro ao atualizar os dados")
            false
        }
    }

    fun updateActiveCitizenFirebase(
        citizenModel: CitizenModel, workspaceId: String, active: Boolean,
        callback: (Boolean) -> Unit
    ) {
        if (citizenModel.id.isEmpty() || workspaceId.isEmpty()) {
            println("Erro ao captar o citizen id ou o workspaceId")
            return
        }

        val reference =
            FirebaseDatabase.getInstance().getReference("workspaces/${workspaceId}/citizens")
        reference.child(citizenModel.id).updateChildren(mapOf("active" to active))
            .addOnCompleteListener {
                Log.d("inactiveCitizenFirebase", "Tudo ocorreu ok")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.d("inactiveCitizenFirebase", "${e.message}")
                callback(false)
            }
    }

    suspend fun inactiveCitizenRoom(offCitizenId: String, active: Boolean, needsUpdate: Boolean) {
        try {
            citizenDao.updateIsActive(offCitizenId, active, needsUpdate)
        } catch (e: Exception) {
            Log.d("inactiveCitizenRoom", "Erro : ${e.message}")
        }
    }

    fun loadInactiveCitizenFirebase(
        workspaceId: String,
        callback: (List<CitizenModel>) -> Unit
    ) {
        val reference = FirebaseDatabase.getInstance().getReference("workspaces").child(workspaceId)
            .child("citizens")

        reference.orderByChild("active").equalTo(false).limitToFirst(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val citizenList = mutableListOf<CitizenModel>()

                    for (snapshot in dataSnapshot.children) {
                        val citizenModel = snapshot.getValue(CitizenModel::class.java)
                        if (citizenModel != null) {
                            citizenList.add(citizenModel)
                        }
                    }

                    callback(citizenList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(
                        "Erro loadInactiveCitizenFirebase",
                        "Erro ao captar os dados: ${error.message}"
                    )
                    callback(emptyList())
                }

            })
    }

    suspend fun loadInactiveCitizenRoom(offWorkspaceId: String): List<Citizen> {
        return citizenDao.getCitizensInactive(offWorkspaceId)
    }

    fun deleteCitizenFirebase(
        citizenModel: CitizenModel,
        workspaceId: String,
        callback: (Boolean) -> Unit
    ) {
        try {
            val reference =
                FirebaseDatabase.getInstance().getReference("workspaces/${workspaceId}/citizens")
            reference.child(citizenModel.id).removeValue()
                .addOnCompleteListener {
                    Log.d("deleteCitizenFirebase", "Tudo ocorreu ok")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.d("deleteCitizenFirebase", "${e.message}")
                    callback(false)
                }
        } catch (e: Exception) {
            Log.d("deleteCitizenFirebase", "Erro ao executar delete: ${e.message}")
        }
    }

    suspend fun scheduleDeleteCitizenRoom(offCitizenId: String, isDelete: Boolean): Boolean {
        try {
            val res = citizenDao.updateIsDelete(offCitizenId, isDelete)
            return res > 0
            // Operação de exclusão bem-sucedida
        } catch (e: Exception) {
            // Tratar exceção
            return false
            Log.e("deletar citizen - repository", "Erro ao excluir cidadao: ${e.message}")
        }
    }

    suspend fun deleteCitizenRoom(citizen: Citizen) {
        try {
            citizenDao.delete(citizen)
            // Operação de exclusão bem-sucedida
        } catch (e: Exception) {
            // Tratar exceção
            Log.e("deleteCitizenRoom - repository", "Erro ao excluir cidadao: ${e.message}")
        }
    }

    suspend fun deleteListCitizensRoom(listCitizens: List<Citizen>) {
        try {
            citizenDao.deleteListCitizens(listCitizens)
            Log.e("deleteListCitizensRoom", "Operação bem sucedida, ${listCitizens.size} deletados")
        } catch (e: Exception) {
            Log.e("deleteListCitizensRoom", "Erro ao excluir cidadao: ${e.message}")
        }
    }

    fun verifyExistsCitizenByOffId(
        workspaceId: String,
        offCitizenId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val reference =
            FirebaseDatabase.getInstance().getReference("workspaces/${workspaceId}/citizens")
        reference.orderByChild("id").equalTo(offCitizenId.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                        val firstChild = dataSnapshot.children.firstOrNull()
                        val citizenId = firstChild?.key
                        println("Verificação bem sucedida na verifyExistsWorkspaceByOffId")
                        if (!citizenId.isNullOrEmpty()) {
                            callback(true, citizenId)
                        } else {
                            callback(false, null)
                        }
                    } else {
                        println("Nenhum workspace encontrado com o offWorkspaceId fornecido")
                        callback(false, null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Erro ao executar a ação de busca dos dados do cidadão.")
                    callback(false, null)
                }

            })
    }

    suspend fun synchronizeCitizen(
        userId: String,
        workspaceId: String,
        callback: SynchronizationCitizenCallback
    ) {
        println("Iniciando sincronização para o workspace: $workspaceId")
        isSync = true
        val mutex = workspaceLocks.getOrPut(workspaceId) { Mutex() }

        // Usando o Mutex apenas para proteger a seção crítica
        mutex.withLock {
            // Verificação de tempo de sincronização e atualização do timestamp
            if (shouldWaitForNextSync()) {
                delay(syncInterval - (System.currentTimeMillis() - lastSyncTime))
            }
            lastSyncTime = System.currentTimeMillis()
        }

        // Verificação se o workspace está salvo no Firebase
        var currentWorkspaceId = workspaceId
        val workspaceExists = checkIfWorkspaceExists(workspaceId)

        if (!workspaceExists) {
            println("!workspaceExists")
            val (saved, newWorkspaceId) = saveWorkspaceToFirebase(userId, currentWorkspaceId)

            if (!saved) {
                println("Falha ao salvar o workspace no Firebase")
                callback.onFailure("Falha ao salvar o workspace no Firebase.")
                isSync = false
                return
            } else {
                currentWorkspaceId = newWorkspaceId
                workspaceIdSynchronized = newWorkspaceId
                println("Novo workspace id = $currentWorkspaceId")
            }
        }

        try {
            // Todo o resto é feito fora do lock
            println("Passando pela sincronização")
            val (localData, onlineData) = fetchData(currentWorkspaceId)

            val listCitizensNeedingSync =
                CitizenModel.fromEntityList(citizenDao.getCitizensNeedingSync(workspaceId))

            println("localData - ${localData.size}, onlineData - ${onlineData.size}")

            val (missingOnlineIds, missingRoomIds) = identifyMissingData(
                listCitizensNeedingSync,
                localData,
                onlineData
            )

            println("IDs ausentes - Online: ${missingOnlineIds.size}, Offline: ${missingRoomIds.size}")

            // Operações de sincronização fora do mutex
            val deleteResult = citizensNeedingDelete(currentWorkspaceId, onlineData)

            val saveOnlineResult =
                if (localData.size != onlineData.size && missingOnlineIds.isNotEmpty()) {
                    needsSaveOnlineData(missingOnlineIds, currentWorkspaceId, localData)
                } else true

            val saveOfflineResult =
                if (localData.size != onlineData.size && missingRoomIds.isNotEmpty()) {
                    needsSaveOfflineData(missingRoomIds, currentWorkspaceId, onlineData)
                } else true

            val updateSyncResult = updateCitizenNeedingUpdate(currentWorkspaceId, onlineData)

            if (saveOnlineResult && saveOfflineResult && updateSyncResult && deleteResult) {
                callback.onSuccess()
            } else {
                callback.onFailure("Erro inesperado")
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar: ${e.message}")
            callback.onFailure(e.message ?: "Erro desconhecido")
        } finally {
            // Remover o lock fora do mutex para evitar conflitos durante a execução das operações
            isSync = false
            workspaceLocks.remove(currentWorkspaceId)
            println("Sincronização concluída para o workspace: $currentWorkspaceId")
        }
    }

    // Função para verificar se o workspace existe no Firebase
    private suspend fun checkIfWorkspaceExists(workspaceId: String): Boolean {
        val workspace = citizenDao.verifyExistsWorkspaceFirebaseId(workspaceId)
        return workspace != null
    }

    // Função para salvar o workspace no Firebase
    private suspend fun saveWorkspaceToFirebase(
        workspaceId: String,
        userId: String
    ): Pair<Boolean, String> = retry {
        try {
            var control = true
            var newWorkspaceId = ""

            // Log inicial
            Log.d(
                "saveWorkspaceToFirebase",
                "Iniciando processo de salvamento para Workspace ID: $workspaceId"
            )

            val workspaceEntity = citizenDao.getWorkspaceReference(workspaceId)

            if (workspaceEntity != null) {
                Log.d(
                    "saveWorkspaceToFirebase",
                    "Workspace encontrado no banco de dados local: ${workspaceEntity.id}"
                )

                val reference = FirebaseDatabase.getInstance().getReference("workspaces").push()
                val workspace = WorkspaceModel.fromEntity(workspaceEntity)

                workspace.id = reference.key ?: ""
                Log.d("saveWorkspaceToFirebase", "Novo ID do Workspace gerado: ${workspace.id}")

                if (workspace.userIds.isEmpty()) {
                    workspace.userIds[userId] = true
                    Log.d("saveWorkspaceToFirebase", "Adicionado usuário $userId ao Workspace")
                }

                // Verificar se o modelo está vazio
                if (!workspace.modelIsEmpty()) {
                    Log.e(
                        "saveWorkspaceToFirebase",
                        "Modelo de workspace está vazio ou com dados faltando"
                    )
                    return@retry Pair(false, "")
                }

                // Usar corrotina para aguardar a conclusão do setValue
                val taskResult = suspendCancellableCoroutine<Boolean> { continuation ->
                    reference.setValue(workspace)
                        .addOnCompleteListener { task ->
                            control = task.isSuccessful
                            if (task.isSuccessful) {
                                Log.d(
                                    "saveWorkspaceToFirebase",
                                    "Dados de workspace salvos no Firebase com sucesso."
                                )
                                newWorkspaceId = workspace.id
                            } else {
                                Log.e(
                                    "saveWorkspaceToFirebase",
                                    "Falha ao salvar dados de workspace no Firebase."
                                )
                            }
                            continuation.resume(control)
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "saveWorkspaceToFirebase",
                                "Erro ao salvar dados de workspace no Firebase: ${e.message}"
                            )
                            control = false
                            continuation.resume(false)
                        }
                }

                // Se a tarefa foi bem-sucedida, atualize o banco de dados local
                if (taskResult) {
                    Log.d(
                        "saveWorkspaceToFirebase",
                        "Atualizando banco de dados local com o novo ID do workspace."
                    )
                    citizenDao.updateSyncWorkspace(workspace.id, false)
                    citizenDao.updateWorkspaceId(newWorkspaceId, workspaceEntity.id)
                } else {
                    Log.e(
                        "saveWorkspaceToFirebase",
                        "Falha na tarefa de salvamento do workspace no Firebase."
                    )
                }

            } else {
                Log.e(
                    "saveWorkspaceToFirebase",
                    "Workspace não encontrado no banco de dados local para ID: $workspaceId"
                )
                control = false
            }

            Pair(control, newWorkspaceId)
        } catch (e: Exception) {
            Log.e("saveWorkspaceToFirebase", "Exceção capturada: ${e.message}")
            Pair(false, "")
        }
    }

    private fun shouldWaitForNextSync(): Boolean {
        println("Tempo de execução da sincronização: ${System.currentTimeMillis() - lastSyncTime} ms")
        val currentTime = System.currentTimeMillis()
        return currentTime - lastSyncTime < syncInterval
    }

    private suspend fun fetchData(workspaceId: String): Pair<List<CitizenModel>, List<CitizenModel>> =
        retry {
            withContext(Dispatchers.IO) {
                // Executando ambas operações de IO em paralelo para maior eficiência
                val localDataDeferred =
                    async { CitizenModel.fromEntityList(citizenDao.getAllCitizens(workspaceId)) }

                val onlineDataDeferred = async { loadAllListCitizensSuspend(workspaceId) }

                val localData = localDataDeferred.await()
                val onlineData = onlineDataDeferred.await()

                println("Dados carregados - Local: ${localData.size}, Online: ${onlineData.size}")

                Pair(localData, onlineData)
            }
        }

    private suspend fun loadAllListCitizensSuspend(workspaceId: String): List<CitizenModel> =
        retry {
            suspendCancellableCoroutine { continuation ->
                loadAllListCitizens(workspaceId) { list ->
                    if (continuation.isActive) {
                        continuation.resume(list)
                    }
                }
            }
        }

    private fun identifyMissingData(
        listCitizensNeedingSync: List<CitizenModel>,
        localData: List<CitizenModel>,
        onlineData: List<CitizenModel>
    ): Pair<Set<String>, Set<String>> {
        val citizensNeedingSyncIds = listCitizensNeedingSync.map { it.id }.toSet()
        val localIds = localData.map { it.id }.toSet()
        val onlineIds = onlineData.map { it.id }.toSet()

        println("citizensNeedingSyncIds - ${citizensNeedingSyncIds.size}, onlineIds = ${onlineIds.size} - localIds ${localIds.size}")
        return Pair(citizensNeedingSyncIds, onlineIds - localIds)
    }

    private suspend fun saveCitizenSuspend(
        workspaceId: String,
        citizenModel: CitizenModel
    ): Pair<Boolean, String> = retry {
        withContext(Dispatchers.IO) {
            val oldCitizen = loadCitizenRoom(citizenModel.id)

            if (citizenModel.id.isNotBlank()) {
                println("Salvando cidadão no Firebase: ${citizenModel.id}")

                val (success, firebaseId) = suspendCancellableCoroutine<Pair<Boolean, String>> { continuation ->
                    saveCitizen(workspaceId, citizenModel) { success, firebaseId ->
                        if (continuation.isActive) {
                            continuation.resume(Pair(success, firebaseId ?: ""))
                        }
                    }
                }

                if (success) {
                    val existingCitizen = citizenDao.getCitizen(firebaseId)

                    if (existingCitizen == null) {
                        Log.d(
                            "Update citizen id",
                            "Update id | newId: $firebaseId , oldId: ${oldCitizen?.id}"
                        )
                        citizenDao.updateCitizenId(firebaseId, oldCitizen?.id ?: "")
                        updateSyncStatus(firebaseId, false)
                    } else {
                        Log.d(
                            "Update citizen id",
                            "New ID already exists, skipping update"
                        )
                    }
                }

                Pair(success, firebaseId)
            } else {
                Log.d("SaveCitizen", "Citizen ID is blank, skipping save")
                Pair(false, "")
            }
        }
    }

    private suspend fun needsSaveOnlineData(
        missingOnlineIds: Set<String>,
        workspaceId: String,
        localData: List<CitizenModel>
    ): Boolean = retry {
        println("Iniciando salvamento de dados online. IDs faltando: ${missingOnlineIds.size}")

        if (missingOnlineIds.isEmpty()) return@retry true

        try {
            coroutineScope {
                val deferredResults = missingOnlineIds.map { id ->
                    val citizenModel = localData.first { it.id == id }
                    async(Dispatchers.IO) { saveCitizenSuspend(workspaceId, citizenModel) }
                }

                val results = deferredResults.awaitAll()
                results.all { (success, _) -> success }
            }
        } catch (e: Exception) {
            println("Erro ao salvar os dados online de sincronização: ${e.message}")
            false
        }
    }

    private suspend fun needsSaveOfflineData(
        missingRoomIds: Set<String>,
        workspaceId: String,
        onlineData: List<CitizenModel>
    ): Boolean = retry {
        if (missingRoomIds.isEmpty()) return@retry true

        try {
            missingRoomIds.map { id ->
                val citizenModel = onlineData.first { it.id == id }
                val citizen = Citizen.fromCitizenModel(citizenModel, workspaceId)
                    .apply { firebaseId = id; needsSync = false }
                citizenDao.insert(citizen)
            }.all { true }
        } catch (e: Exception) {
            println("Erro ao salvar os dados offline de sincronização: ${e.message}")
            false
        }
    }

    private suspend fun updateCitizenFirebaseSuspend(
        citizenModel: CitizenModel,
        workspaceId: String
    ): Boolean = retry {
        suspendCancellableCoroutine { continuation ->
            updateCitizenFirebase(citizenModel, workspaceId) { success, _ ->
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            }
        }
    }

    private suspend fun updateCitizenNeedingUpdate(
        workspaceId: String,
        onlineData: List<CitizenModel>
    ): Boolean = retry {
        try {
            val listCitizenNeedingUpdate =
                getCitizensNeedingUpdate(workspaceId) // Função para obter a lista de cidadãos que precisam de atualização

            println("Cidadãos necessitando de atualização: ${listCitizenNeedingUpdate.size}")

            if (listCitizenNeedingUpdate.isEmpty()) return@retry true

            val listCitizenNeedingUpdateOffId = listCitizenNeedingUpdate.map { it.id }.toSet()

            val listNeedingUpdateOnlineData =
                onlineData.filter { it.id in listCitizenNeedingUpdateOffId }

            println("Total de elementos que necessitam de atualização: ${listCitizenNeedingUpdateOffId.size}")

            val updateResults = coroutineScope {
                listNeedingUpdateOnlineData.mapNotNull { citizenModel ->
                    val citizenEntity =
                        listCitizenNeedingUpdate.firstOrNull { it.id == citizenModel.id }
                    if (citizenEntity != null) {
                        val editWorkspaceModel =
                            CitizenModel.fromEntity(citizenEntity)
                                .apply { id = citizenModel.id }

                        async(Dispatchers.IO) {
                            updateNeedsUpdateStatus(editWorkspaceModel.id, false)
                            updateCitizenFirebaseSuspend(
                                editWorkspaceModel,
                                workspaceId
                            )
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

    private suspend fun citizensNeedingDelete(
        workspaceId: String,
        onlineData: List<CitizenModel>,
    ): Boolean = retry {
        try {
            val listCitizenNeedingDelete =
                getCitizensNeedingDelete(workspaceId) // Função para obter a lista de cidadãos que precisam ser deletados

            val listCitizensNotNeedingSync =
                getAllCitizensNotNeedingSync(workspaceId)

            if (listCitizenNeedingDelete.isEmpty()) return@retry true

            val listCitizenNeedingDeleteId = listCitizenNeedingDelete.map { it.id }.toSet()

            val citizensToDeleteOnline = onlineData.filter { it.id in listCitizenNeedingDeleteId }
            // Filtra os cidadãos em localData que têm needsSync = false e cujo ID não está presente em onlineData
            val citizensToDeleteOff = listCitizensNotNeedingSync.filter { localCitizen ->
                !localCitizen.needsSync && localCitizen.id !in onlineData.map { it.id }
            }

            println("Iniciando processo de exclusão de cidadãos. Total a excluir: ${citizensToDeleteOnline.size}")
            println("Total de elementos que necessitam de exclusão: ${citizensToDeleteOnline.size}")

            val deletionResults = coroutineScope {
                citizensToDeleteOnline.map { citizenModel ->
                    async(Dispatchers.IO) {
                        val successDeleteFirebase =
                            deleteCitizenFirebaseSuspend(citizenModel, workspaceId)
//                        val successDeleteRoom = deleteCitizenRoomSuspend(citizenModel, workspaceId)
                        successDeleteFirebase
                    }
                }.awaitAll()

                citizensToDeleteOff.map { citizen ->
                    async(Dispatchers.IO) {
                        val successDeleteRoom = citizenDao.delete(citizen) != 0
                        successDeleteRoom
                    }
                }.awaitAll()
            }

            println("Cidadãos deletados com sucesso: $deletionResults")
            deletionResults.all { it } // Verifica se todas as exclusões foram bem-sucedidas
        } catch (e: Exception) {
            println("Erro ao deletar cidadãos: ${e.message}")
            false
        }
    }

    private suspend fun deleteCitizenFirebaseSuspend(
        citizenModel: CitizenModel,
        workspaceId: String
    ): Boolean =
        retry {
            suspendCoroutine { continuation ->
                deleteCitizenFirebase(citizenModel, workspaceId) { success ->
                    continuation.resume(success)
                }
            }
        }

    private suspend fun deleteCitizenRoomSuspend(
        citizenModel: CitizenModel,
        workspaceId: String
    ): Boolean = retry {
        withContext(Dispatchers.IO) {
            try {
                citizenDao.delete(Citizen.fromCitizenModel(citizenModel, workspaceId))
                true
            } catch (e: Exception) {
                println("Erro ao deletar cidadão no banco de dados local: ${e.message}")
                false
            }
        }
    }

    // Em um arquivo chamado SynchronizationCallback.kt ou no mesmo arquivo do ViewModel
    interface SynchronizationCitizenCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }

    private suspend fun getCitizensNeedingUpdate(offWorkspaceId: String): List<Citizen> {
        return citizenDao.getCitizensNeedingUpdate(offWorkspaceId)
    }

    suspend fun updateSyncStatus(offCitizenId: String, needsSync: Boolean): Boolean {
        val res = citizenDao.updateSyncStatus(offCitizenId, needsSync)
        return res > 0
    }

    suspend fun updateNeedsUpdateStatus(offCitizenId: String, needsSync: Boolean): Boolean {
        val res = citizenDao.updateNeedsUpdateStatus(offCitizenId, needsSync)
        return res > 0
    }

    suspend fun getCitizensNeedingDelete(offWorkspaceId: String): List<Citizen> {
        return citizenDao.getCitizensNeedingDelete(offWorkspaceId)
    }

    suspend fun updateSyncDelete(offCitizenId: String, isDelete: Boolean) {
        citizenDao.updateIsDelete(offCitizenId, isDelete)
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