package com.miguelprojects.myapplication.util

import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.miguelprojects.myapplication.R

object StyleSystemManager {

    fun changeNavigationBarStyleWithColor(context: Context ,window: Window) {
        // Alterar a cor da barra de status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(context, R.color.tert_ebony_clay)
        }

        // Alterar a cor da barra de navegação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = ContextCompat.getColor(context, R.color.tert_ebony_clay)
        }
    }

    // Função para verificar se uma cor é clara
    fun isColorLight(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) >= 0.5
    }

}