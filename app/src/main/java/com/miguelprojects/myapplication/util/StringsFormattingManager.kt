package com.miguelprojects.myapplication.util

import java.text.Normalizer
import kotlin.random.Random

object StringsFormattingManager {
    private val listLastNames = listOf(
        "Silva",
        "Santos",
        "Oliveira",
        "Pereira",
        "Costa",
        "Ferreira",
        "Rodrigues",
        "Almeida"
    )

    private val listFirstNames = listOf("Lucas", "André", "João", "Carlos", "Miguel", "Ana", "Maria", "Paula", "Beatriz", "Renata")

    fun String?.formattedOrDefault(default: String = "Não informado"): String {
        return this?.ifEmpty { default } ?: default
    }
    fun String?.formatCpf(): String {
        return this?.let {
            if (it.length == 11) {
                it.replace(Regex("(\\d{3})(\\d{3})(\\d{3})(\\d{2})"), "$1.$2.$3-$4")
            } else {
                it
            }
        } ?: "inválido"
    }

    fun String?.formatCep(): String {
        return this?.let {
            if (it.length == 8) {
                it.replace(Regex("(\\d{5})(\\d{3})"), "$1-$2")
            } else {
                it
            }
        } ?: "inválido"
    }

    fun String?.formatSus(): String {
        return this?.let {
            if (it.length == 15) {
                it.replace(Regex("(\\d{3})(\\d{4})(\\d{4})(\\d{4})"), "$1 $2 $3 $4")
            } else {
                it
            }
        } ?: "inválido"
    }

    fun String.formatTelephone(): String {
        return when (this.length) {
            10 -> {
                // Formato para número fixo: (XX) XXXX-XXXX
                "(${this.substring(0, 2)}) ${this.substring(2, 6)}-${this.substring(6, 10)}"
            }
            11 -> {
                // Formato para número celular: (XX) XXXXX-XXXX
                "(${this.substring(0, 2)}) ${this.substring(2, 7)}-${this.substring(7, 11)}"
            }
            else -> {
                this
            }
        }
    }

    fun String?.isValidCpf(): Boolean {
        if (this == null || this.length != 11) return false

        if (Regex("(\\d)\\1{10}").matches(this)) return false

        val numbers = this.map { it.toString().toInt() }

        val dv1 = (0..8).map { (10 - it) * numbers[it] }.sum() % 11
        val dv1Final = if (dv1 < 2) 0 else 11 - dv1

        val dv2 = (0..9).map { (11 - it) * numbers[it] }.sum() % 11
        val dv2Final = if (dv2 < 2) 0 else 11 - dv2

        return numbers[9] == dv1Final && numbers[10] == dv2Final
    }

    fun String.isValidTelephone(): Boolean {
        // Aceita formatos com 10 ou 11 dígitos, com ou sem DDD
        return Regex("^\\d{10,11}\$").matches(this)
    }

    fun String?.isValidCep(): Boolean {
        return this?.length == 8
    }

    fun Long.isValidBirthdate(minAge: Int = 0, maxAge: Int = 120): Boolean {
        val currentDate = System.currentTimeMillis()
        val age = (currentDate - this) / (365.25 * 24 * 60 * 60 * 1000).toLong() // Calcula a idade aproximada em anos
        return age in minAge..maxAge
    }

    fun String?.isValidSus(): Boolean {
        return this?.length == 15 && this.all { it.isDigit() }
    }

    fun String.isValidState(): Boolean {
        val validStates = setOf(
            "Acre", "Alagoas", "Amapá", "Amazonas", "Bahia", "Ceará",
            "Distrito Federal", "Espírito Santo", "Goiás", "Maranhão",
            "Mato Grosso", "Mato Grosso do Sul", "Minas Gerais", "Pará",
            "Paraíba", "Paraná", "Pernambuco", "Piauí", "Rio de Janeiro",
            "Rio Grande do Norte", "Rio Grande do Sul", "Rondônia",
            "Roraima", "Santa Catarina", "São Paulo", "Sergipe", "Tocantins"
        ).map { it.removeAccents().lowercase() }.toSet()

        return this.removeAccents().lowercase() in validStates
    }

    fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
            .replace(" ", "")
    }

    fun String.isValidRegisterNumber(): Boolean {
        // Exemplo genérico de verificação
        return this.isNotEmpty() && this.length == 8 && this.all { it.isDigit() }
    }


    fun generateCPF(): String {
        val cpfNumbers = MutableList(9) { Random.nextInt(0, 10) }
        cpfNumbers.add(calculateVerifierDigit(cpfNumbers, 10))
        cpfNumbers.add(calculateVerifierDigit(cpfNumbers, 11))

        return cpfNumbers.joinToString("")
    }

    fun calculateVerifierDigit(numbers: List<Int>, weight: Int): Int {
        var sum = 0
        for (i in numbers.indices) {
            sum += numbers[i] * (weight - i)
        }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    fun generateSex(): String {
        val listSex = listOf("f", "m", "")
        return listSex.random()
    }

    fun generateRandomName(): String {
        val firstName = listFirstNames.random()
        val lastName = listLastNames.random()

        return "$firstName $lastName"
    }

    fun generateRandomManName(): String {
        val firstName = listFirstNames.take(5).random()
        val lastName = listLastNames.random()

        return "$firstName $lastName"
    }

    fun generateRandomWomanName(): String {
        val firstName = listFirstNames.drop(5).random()
        val lastName = listLastNames.random()

        return "$firstName $lastName"
    }

    fun generatePhoneNumber(): String {
        val ddd = Random.nextInt(10, 100) // Gera o DDD entre 10 e 99
        val firstDigit = Random.nextInt(2, 10) // Primeiro dígito do número, geralmente 9 para celular
        val numberPart1 = Random.nextInt(1000, 10000) // Gera a primeira parte do número, com 4 dígitos
        val numberPart2 = Random.nextInt(1000, 10000) // Gera a segunda parte do número, com 4 dígitos

        return "$ddd$firstDigit$numberPart1$numberPart2"
    }

    fun generateSUSNumber(): String {
        // Gera um número aleatório com 15 dígitos
        val susNumber = StringBuilder()
        for (i in 1..15) {
            susNumber.append(Random.nextInt(0, 10)) // Adiciona um dígito aleatório (0-9)
        }
        return susNumber.toString()
    }

    fun String.convertCapitalizeWord(): String {
        return this.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

}