package com.miguelprojects.myapplication.ui.activitys.users

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityChooseImageProfileBinding
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.StyleSystemManager

class ChooseImageProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseImageProfileBinding
    private var imageResult = 0
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChooseImageProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        userId = intent.getStringExtra("user_id") ?: ""

        if (userId.isEmpty()) {
            binding.layoutNavbarTop.navigation.visibility = View.GONE
        } else {
            DrawerConfigurator(
                this,
                0,
                0,
                mapOf("userId" to userId)
            ).configureSimpleTopNavigation()
        }

        binding.image1.setOnClickListener {
            if (imageResult == 1) {
                imageResult = 0
                binding.image1.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()
                imageResult = 1
                binding.image1.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image2.setOnClickListener {
            if (imageResult == 2) {
                imageResult = 0
                binding.image2.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()
                imageResult = 2
                binding.image2.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image3.setOnClickListener {
            if (imageResult == 3) {
                imageResult = 0
                binding.image3.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()
                imageResult = 3
                binding.image3.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image4.setOnClickListener {
            if (imageResult == 4) {
                imageResult = 0
                binding.image4.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()
                imageResult = 4
                binding.image4.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image5.setOnClickListener {
            if (imageResult == 5) {
                imageResult = 0
                binding.image5.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()

                imageResult = 5
                binding.image5.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image6.setOnClickListener {
            if (imageResult == 6) {
                imageResult = 0
                binding.image6.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()

                imageResult = 6
                binding.image6.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image7.setOnClickListener {
            if (imageResult == 7) {
                imageResult = 0
                binding.image7.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()

                imageResult = 7
                binding.image7.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.image8.setOnClickListener {
            if (imageResult == 8) {
                imageResult = 0
                binding.image8.setBackgroundResource(R.color.transparent)
            } else {
                resetBackColor()

                imageResult = 8
                binding.image8.setBackgroundResource(R.color.green_light_2)
            }
        }

        binding.buttonChoose.setOnClickListener {
            val intent = Intent()
            intent.putExtra("imageResult", imageResult)
            setResult(IMAGE_CODE, intent)
            finish()  // Finaliza a atividade e retorna o resultado para a RegisterActivity
        }
    }

    private fun resetBackColor() {
        when (imageResult) {
            1 -> {
                binding.image1.setBackgroundResource(R.color.transparent)
            }

            2 -> {
                binding.image2.setBackgroundResource(R.color.transparent)
            }

            3 -> {
                binding.image3.setBackgroundResource(R.color.transparent)
            }

            4 -> {
                binding.image4.setBackgroundResource(R.color.transparent)
            }

            5 -> {
                binding.image5.setBackgroundResource(R.color.transparent)
            }

            6 -> {
                binding.image6.setBackgroundResource(R.color.transparent)
            }

            7 -> {
                binding.image7.setBackgroundResource(R.color.transparent)
            }

            8 -> {
                binding.image8.setBackgroundResource(R.color.transparent)
            }
        }
    }

    private companion object {
        private const val IMAGE_CODE = 99
    }
}