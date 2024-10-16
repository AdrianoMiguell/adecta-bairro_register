package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityJoinWorkspaceBinding
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.WorkspaceRequestViewModel

class JoinWorkspaceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJoinWorkspaceBinding
    private lateinit var workspaceRequestViewModel: WorkspaceRequestViewModel
    private var userId: String = ""
    private var workspaceCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityJoinWorkspaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)
        DrawerConfigurator(this, 0, 0, mapOf("userId" to userId)).configureSimpleTopNavigation()

        getExtraValues()
        startTools()
        initializeApp()
    }

    private fun initializeApp() {
        if (NetworkChangeReceiver().isNetworkConnected(this)) {
            binding.layoutJoinWorkspace.visibility = View.VISIBLE
            binding.layoutOffConnection.visibility = View.GONE
            setClickListeners()
        } else {
            binding.layoutJoinWorkspace.visibility = View.GONE
            binding.layoutOffConnection.visibility = View.VISIBLE
        }
    }

    private fun startTools() {
        workspaceRequestViewModel = ViewModelProvider(this)[WorkspaceRequestViewModel::class.java]
    }

    private fun getExtraValues() {
        userId = intent.getStringExtra("userId") ?: ""

        verifyIfEmptyValues()
    }

    private fun verifyIfEmptyValues() {
        if (userId.isEmpty()) {
            Toast.makeText(
                this,
                "Erro ao acessar a página. Por favor, Reporte esse problema!",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("verifyIfEmptyValues", "Erro, dados necessários estão vazios")
            finish()
        }
    }

    private fun setClickListeners() {
        binding.buttonJoin.setOnClickListener {
            binding.buttonJoin.isEnabled = false

            if (NetworkChangeReceiver().isNetworkConnected(this)) {
                workspaceCode = binding.editCodeJoin.text.toString()
//                Código inválido
                if (workspaceCode.length != 8) {
                    Toast.makeText(
                        this,
                        "Código invalido. Certifique-se de ter digitado corretamente!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.buttonJoin.isEnabled = true
                    }, 1000)
                } else {
                    workspaceRequestViewModel.sendRequestJoin(
                        workspaceCode,
                        userId
                    ) { res, message ->
                        if (res) {
                            finish()
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.buttonJoin.isEnabled = true
                            }, 1000)
                            binding.editCodeJoin.setText("")
                            println("Erro ao enviar solicitação para entrar no grupo. Reporte esse problema!")
                        }
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    R.string.text_needs_connection,
                    Toast.LENGTH_SHORT
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.buttonJoin.isEnabled = true
                }, 1000)
            }
        }
    }
}