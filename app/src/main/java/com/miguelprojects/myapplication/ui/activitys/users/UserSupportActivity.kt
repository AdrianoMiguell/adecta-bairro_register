package com.miguelprojects.myapplication.ui.activitys.users

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityUserSupportBinding
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.PrivacyTermsActivity
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.StyleSystemManager

class UserSupportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSupportBinding
    private val supportEmail = R.string.support_email
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        initializeApp()
    }

    private fun initializeApp() {
        getExtraData()

        DrawerConfigurator(
            this,
            UserModel(),
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        setOnClickListeners()
    }


    private fun getExtraData() {
        userId = intent.getStringExtra("userId") ?: ""

        if (userId.isEmpty()) {
            toastMessage("Erro ao carregar os dados. Por favor, reporte esse problema!")
            println("Erro ao carregar os dados!")
            finish()
        }
    }

    private fun setOnClickListeners() {
        binding.buttonSendFeedback.setOnClickListener {
//            val emailIntent = Intent(Intent.ACTION_SEND)
//            emailIntent.type = "text/plain"
//            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
//            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - ADECTA")
//            emailIntent.putExtra(Intent.EXTRA_TEXT, "Feedback positivo / negativo")
//
//            try {
//                startActivity(Intent.createChooser(emailIntent, "Abrindo Email ..."))
//            } catch (e: Exception) {
//                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
//            }
        }

        binding.buttonReportProblem.setOnClickListener {
//            val emailIntent = Intent(Intent.ACTION_SEND)
//            emailIntent.type = "text/plain"
//            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
//            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Reclamações - ADECTA")
//            emailIntent.putExtra(
//                Intent.EXTRA_TEXT,
//                "Olá, estou com um problema com o aplicativo. [...]"
//            )
//
//            try {
//                startActivity(
//                    Intent.createChooser(
//                        emailIntent,
//                        "Email de reclamação do cliente ..."
//                    )
//                )
//            } catch (e: Exception) {
//                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
//            }
        }

        binding.termsOfServiceAndPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyTermsActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }
    }

    private fun toastMessage(mes: String) {
        Toast.makeText(this, mes, Toast.LENGTH_SHORT).show()
    }
}