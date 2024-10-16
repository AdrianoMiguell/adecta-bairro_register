package com.miguelprojects.myapplication.ui.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivitySplashScreenBinding
import com.miguelprojects.myapplication.ui.activitys.users.LoginActivity
import com.miguelprojects.myapplication.util.StyleSystemManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!verifyIfAfterDate()) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 1000)
        } else {
            binding.textSupport.visibility = View.VISIBLE
            Toast.makeText(this, "Acesso temporário suspenso! Entre em contato com o suporte.", Toast.LENGTH_LONG).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun verifyIfAfterDate(): Boolean {
        val fixedDate = LocalDate.parse("2024-10-30", DateTimeFormatter.ISO_DATE)

        val dateActual = LocalDate.now()

        return dateActual.isAfter(fixedDate)
    }
}