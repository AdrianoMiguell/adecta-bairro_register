package com.miguelprojects.myapplication.model

data class SyncNotificationsModel(
    var id: String = "",
    var command: String = "",
    var notification: String = "",
    var idDataMap: Map<String, String> = emptyMap()
) {
    // Construtor padrão necessário para deserialização pelo Firebase
    constructor() : this("", "", "", emptyMap())

    // Adicione métodos adicionais, se necessário
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "command" to command,
            "notification" to notification,
            "idDataMap" to idDataMap
        )
    }
}
