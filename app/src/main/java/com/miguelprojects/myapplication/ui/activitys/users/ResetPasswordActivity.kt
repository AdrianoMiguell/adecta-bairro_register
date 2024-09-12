package com.miguelprojects.myapplication.ui.activitys.users

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.miguelprojects.myapplication.R

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var resetPasswordButton: Button
    private lateinit var backButton: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        emailEditText = findViewById(R.id.emailEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        backButton = findViewById(R.id.buttonBack)
        mAuth = FirebaseAuth.getInstance()

        backButton.setOnClickListener {
            finish()
        }

        resetPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isNotEmpty()) {
                resetPasswordButton.isEnabled = false
                resetPassword(email)
            } else {
                Toast.makeText(this, "Por favor, insira seu email.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword(email: String) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ResetPasswordActivity", "Email de redefinição de senha enviado.")
                    Toast.makeText(
                        this,
                        "Email de redefinição de senha enviado.",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetPasswordButton.isEnabled = true
                    finish()
                } else {
                    Log.d("ResetPasswordActivity", "Erro ao enviar email de redefinição de senha.", task.exception)
                    Toast.makeText(
                        this,
                        "Erro ao enviar email de redefinição de senha.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}