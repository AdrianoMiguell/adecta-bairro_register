package com.miguelprojects.myapplication.util

class SpinnerOptions {
    val mapCategoryAge = mapOf(
        0 to "Idades",
        1 to "0 - 10 anos",
        2 to "10 - 20 anos",
        3 to "20 - 30 anos",
        4 to "30 - 40 anos",
        5 to "40 - 60 anos",
        6 to "60 - 80 anos",
        7 to "80+ anos"
    )

    val mapCategorySex = mapOf(
        "t" to "Gêneros",
        "f" to "Feminino",
        "m" to "Masculino",
        "" to "Indefinido"
    )

    val mapCaregorySearch = mapOf(
        "name" to "Nome",
        "telephone" to "Telefone",
        "cpf" to "CPF",
        "sus" to "SUS",
        "numberregister" to "Registro",
        "fathername" to "Nome do pai",
        "mothername" to "Nome da mãe",
        "birthplace" to "Lugar de origem",
        "cep" to "CEP",
        "state" to "Estado",
        "city" to "Cidade",
        "neighborhood" to "Bairro",
        "street" to "Rua",
        "numberhouse" to "N° da casa"
    )

    val mapOfLimits = mapOf(
        50 to "Exibir 50 resultados",
        20 to "Exibir 20 resultados",
        10 to "Exibir 10 resultados",
        5 to "Exibir 5 resultados"
    )

    val mapOrderAlphabetic = mapOf(
        true to "A a Z",  // Para ordem crescente
        false to "Z a A"  // Para ordem decrescente
    )
}
