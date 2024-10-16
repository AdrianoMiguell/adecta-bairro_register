package com.miguelprojects.myapplication.util

import com.miguelprojects.myapplication.R

object CitizenManager {
    fun getCitizenImage(birthdate: Long, sex: String): Int {
        if (sex.isEmpty() || birthdate == 0L) {
            return R.drawable.baseline_account_circle_dark_24
        }

        println("Entrou para decidir o image_id")
        val age = ConvertManager.calculateLongAge(birthdate)

        return when (age) {
            in 0 .. 2 -> {
                R.drawable.icon_baby
            }
            in 2..12 -> {
                if (sex == "f")
                    R.drawable.icon_girl
                else
                    R.drawable.icon_boy
            }
            in 13..18 -> {
                if (sex == "f")
                    R.drawable.icon_teem_girl
                else
                    R.drawable.icon_teem_boy
            }
            in 18..60 -> {
                if (sex == "f")
                    R.drawable.icon_woman
                else
                    R.drawable.icon_man
            }
            in 60..125 -> {
                if (sex == "f")
                    R.drawable.icon_gramma
                else
                    R.drawable.icon_grandfather
            }
            else -> {
                R.drawable.baseline_account_circle_dark_24
            }
        }

    }
}