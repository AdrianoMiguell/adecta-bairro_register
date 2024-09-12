package com.miguelprojects.myapplication.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object ConvertManager {

    fun convertStringForDate(dateString: String): Long {
        // Converta o texto para um objeto Date
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        val newDate: Date? = formatter.parse(dateString)
        val timestamp: Long? = newDate?.time
        return timestamp ?: 0L
    }

    fun convertLongForString(dateLong: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return formatter.format(dateLong)
    }

    fun generateRandomBirthdateTimestamp(): Long {
        // Obter a data atual
        val currentDate = Calendar.getInstance()

        // Data 120 anos atrás
        val pastDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, -120)
        }

        // Converter ambas as datas para timestamps (em milissegundos)
        val currentTimestamp = currentDate.timeInMillis
        val pastTimestamp = pastDate.timeInMillis

        // Gerar um timestamp aleatório entre pastTimestamp e currentTimestamp
        val randomTimestamp = Random.nextLong(pastTimestamp, currentTimestamp)

        return randomTimestamp
    }

    fun calculateFullAge(birthdateTimestamp: Long): String {
        val birthdateCalendar = Calendar.getInstance().apply {
            timeInMillis = birthdateTimestamp
        }

        val today = Calendar.getInstance()

        var years = today.get(Calendar.YEAR) - birthdateCalendar.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birthdateCalendar.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - birthdateCalendar.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months -= 1
            val lastMonth = if (today.get(Calendar.MONTH) == 0) 11 else today.get(Calendar.MONTH) - 1
            val daysInLastMonth = Calendar.getInstance().apply {
                set(Calendar.MONTH, lastMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }.get(Calendar.DAY_OF_MONTH)
            days += daysInLastMonth
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        val sb = StringBuilder()

        if (years > 0) {
            sb.append("$years ${if (years == 1) "ano" else "anos"}")
        }
        if (months > 0) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("$months ${if (months == 1) "mês" else "meses"}")
        }
        if (days > 0) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("$days ${if (days == 1) "dia" else "dias"}")
        }

        return sb.toString()
    }

    fun calculateSimpleAge(birthdateTimestamp: Long): String {
        val birthdateCalendar = Calendar.getInstance().apply {
            timeInMillis = birthdateTimestamp
        }

        val today = Calendar.getInstance()

        var years = today.get(Calendar.YEAR) - birthdateCalendar.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birthdateCalendar.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - birthdateCalendar.get(Calendar.DAY_OF_MONTH)

        // Ajustar dias e meses negativos
        if (days < 0) {
            months -= 1
            val daysInLastMonth = Calendar.getInstance().apply {
                set(Calendar.MONTH, today.get(Calendar.MONTH) - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }.get(Calendar.DAY_OF_MONTH)
            days += daysInLastMonth
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        val sb = StringBuilder()

        when {
            years > 0 -> {
                sb.append("$years ${if (years == 1) "ano" else "anos"}")
            }
            months > 0 -> {
                sb.append("$months ${if (months == 1) "mês" else "meses"}")
            }
            else -> {
                sb.append("$days ${if (days == 1) "dia" else "dias"}")
            }
        }

        return sb.toString()
    }

    fun calculateLongAge(birthdateTimestamp: Long): Int {
        val birthdateCalendar = Calendar.getInstance().apply {
            timeInMillis = birthdateTimestamp
        }

        val today = Calendar.getInstance()

        var years = today.get(Calendar.YEAR) - birthdateCalendar.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birthdateCalendar.get(Calendar.MONTH)
        var days = today.get(Calendar.DAY_OF_MONTH) - birthdateCalendar.get(Calendar.DAY_OF_MONTH)

        // Ajustar dias e meses negativos
        if (days < 0) {
            months -= 1
            val daysInLastMonth = Calendar.getInstance().apply {
                set(Calendar.MONTH, today.get(Calendar.MONTH) - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }.get(Calendar.DAY_OF_MONTH)
            days += daysInLastMonth
        }

        if (months < 0) {
            years -= 1
            months += 12
        }

        return years
    }

    fun capitalizeWords(words: String): String {
        return words.split(" ").joinToString(" ") { it.capitalize() }
    }

}