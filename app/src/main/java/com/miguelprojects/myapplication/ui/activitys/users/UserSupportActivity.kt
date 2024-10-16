package com.miguelprojects.myapplication.ui.activitys.users

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityUserSupportBinding
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.PrivacyTermsActivity
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.StyleSystemManager

class UserSupportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserSupportBinding
    private lateinit var supportName: String
    private lateinit var supportEmail: String
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
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        setOnClickListeners()
    }


    private fun getExtraData() {
        userId = intent.getStringExtra("userId") ?: ""
        supportEmail = getString(R.string.support_email)
        supportName = getString(R.string.support_name)

        if (userId.isEmpty()) {
            toastMessage("Erro ao carregar os dados. Por favor, reporte esse problema!")
            println("Erro ao carregar os dados!")
            finish()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun setOnClickListeners() {
        binding.buttonSendFeedback.setOnClickListener {
            val recipient = arrayOf(supportEmail)
            val subject = "Feedback - ADECTA"
            val body = "Feedback positivo / negativo"

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Somente aplicativos de e-mail respondem a essa Intent
                putExtra(Intent.EXTRA_EMAIL, recipient)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Escolha um aplicativo para enviar este e-mail"))
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao enviar o email. Tente novamente!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.buttonReportProblem.setOnClickListener {
            val recipient = arrayOf(supportEmail)
            val subject = "Reclamações - ADECTA"
            val body = "Olá, estou com um problema com o aplicativo. [...]"

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Somente aplicativos de e-mail respondem a essa Intent
                putExtra(Intent.EXTRA_EMAIL, recipient)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Escolha um aplicativo para enviar este e-mail"))
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao enviar o email. Tente novamente!", Toast.LENGTH_SHORT)
                    .show()
            }
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