package com.miguelprojects.myapplication.util

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.CreateEditWorkspaceActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.JoinWorkspaceActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.WorkspaceMainActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.WorkspaceMembersActivity
import com.miguelprojects.myapplication.ui.activitys.users.SettingActivity
import com.miguelprojects.myapplication.ui.activitys.users.UserSupportActivity

class DrawerConfigurator(
    private val activity: AppCompatActivity,
    private val userModel: UserModel,
    private val drawerLayoutId: Int,
    private val navigationViewId: Int,
    kitMapValuesString: Map<String, String>
) {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var headerView: View
    private lateinit var buttonClose: ImageView
    private val networkChangeReceiver = NetworkChangeReceiver()
    private val userId = kitMapValuesString["userId"]
    private val workspaceId = kitMapValuesString["workspaceId"]

    fun configureDrawerAndNavigation() {
        initializeValues()
        updateUserData()
        startFunctions()
    }

    fun configureSimpleTopNavigation() {
        buttonClose = activity.findViewById(R.id.button_open_menu)
        buttonClose.setImageResource(R.drawable.baseline_arrow_back_24)
        buttonClose.setOnClickListener {
            activity.finish()
        }

        startGeneralOnClickNavigation()
    }

    private fun initializeValues() {
        drawerLayout = activity.findViewById(drawerLayoutId)
        navigationView = activity.findViewById(navigationViewId)

        headerView = navigationView.getHeaderView(0)
        buttonClose = headerView.findViewById(R.id.button_close_menu)

        usernameTextView = headerView.findViewById(R.id.username_navbar)
        emailTextView = headerView.findViewById(R.id.email_navbar)
    }


    private fun startFunctions() {
        drawerToggle = ActionBarDrawerToggle(
            activity, drawerLayout,
            R.string.abrir, R.string.fechar
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Customize this based on activity if needed
        when (activity) {
            is MainActivity -> navigationView.setCheckedItem(R.id.home_topnav)
            is SettingActivity -> navigationView.setCheckedItem(R.id.settings_topnav)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            handleNavigationItemSelected(item)
        }

        setupDrawerToggle()
        startGeneralOnClickNavigation()

        val infoItem = navigationView.menu.findItem(R.id.informacoes_workspace_topnav)
        val usersItem = navigationView.menu.findItem(R.id.users_workspace_topnav)

        if (!workspaceId.isNullOrEmpty()) {
            try {
                infoItem.isVisible = true
                usersItem.isVisible = true
            } catch (e: Exception) {
                Log.d("Erro isVisible - navigation", "Erro ao tornar visível o item. $e")
            }
        } else {
            try {
                infoItem.isVisible = false
                usersItem.isVisible = false
            } catch (e: Exception) {
                Log.d("Erro isVisible - navigation", "Erro ao tornar invisivel o item. $e")
            }
        }
    }

    private fun startGeneralOnClickNavigation() {
        activity.findViewById<View>(R.id.title_app)?.setOnClickListener {
            navigateToHome()
        }
        activity.findViewById<View>(R.id.image_app)?.setOnClickListener {
            navigateToHome()
        }
        activity.findViewById<View>(R.id.image_account)?.setOnClickListener {
            navigateToSettings()
        }
    }

    private fun updateUserData() {
        usernameTextView.text = userModel.username
        emailTextView.text = userModel.email
    }

    private fun setupDrawerToggle() {
        buttonClose.setOnClickListener { drawerLayout.closeDrawer(GravityCompat.START) }
        activity.findViewById<View>(R.id.button_open_menu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun handleNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home_topnav -> {
                navigateToHome()
                true
            }

            R.id.join_group_topnav -> {
                navigateToJoinGroup()
                false
            }

            R.id.informacoes_workspace_topnav -> {
                navigateToWorkspaceInfo()
                false
            }

            R.id.users_workspace_topnav -> {
                navigateToWorkspaceUsers()
                false
            }

            R.id.settings_topnav -> {
                navigateToSettings()
                true
            }

            R.id.suport_topnav -> {
                navigateToSupport()
                false
            }

            else -> false
        }
    }

    private fun navigateToHome() {
        closeDrawerWithDelay()
        if (activity !is MainActivity) {
            startActivity(MainActivity::class.java, null)
        }
    }

    private fun navigateToWorkspaceInfo() {
        closeDrawerWithDelay()
        if (activity !is CreateEditWorkspaceActivity) {
            val extras = Bundle().apply {
                putString("workspaceId", workspaceId)
                putString("userId", userId)
            }
            startActivity(CreateEditWorkspaceActivity::class.java, extras)
        }
    }

    private fun navigateToWorkspaceUsers() {
        closeDrawerWithDelay()
        if (activity !is WorkspaceMembersActivity) {
            val extras = Bundle().apply {
                putString("workspaceId", workspaceId)
                putString("userId", userId)
            }
            startActivity(WorkspaceMembersActivity::class.java, extras)
        }
    }

    private fun navigateToJoinGroup() {
        closeDrawerWithDelay()
        if (networkChangeReceiver.isNetworkConnected(activity) && activity !is JoinWorkspaceActivity) {
            val extras = Bundle().apply {
                putString("userId", userId)
            }
            startActivity(JoinWorkspaceActivity::class.java, extras)
        } else {
            Toast.makeText(activity, "Falha na conexão com a rede!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSettings() {
        closeDrawerWithDelay()
        if (activity !is SettingActivity) {
            val extras = Bundle().apply {
                putString("userId", userId)
            }
            startActivity(SettingActivity::class.java, extras)
        }
    }

    private fun navigateToSupport() {
        closeDrawerWithDelay()
        if (activity !is SettingActivity) {
            val extras = Bundle().apply {
                putString("userId", userId)
            }
            startActivity(UserSupportActivity::class.java, extras)
        }
    }

    private fun startActivity(activityClass: Class<*>, extras: Bundle? = null) {
        val intent = Intent(activity, activityClass)
        extras?.let { intent.putExtras(it) }

        // Adicione flags apenas para MainActivity
        if (activityClass == MainActivity::class.java) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        activity.startActivity(intent)

        if (activity !is MainActivity && activity !is WorkspaceMainActivity) {
            activity.finish()
        }
    }

    private fun closeDrawerWithDelay() {
        if (::drawerLayout.isInitialized) {
            Handler(Looper.getMainLooper()).postDelayed({
                drawerLayout.closeDrawer(GravityCompat.START)
            }, 250)
        }
    }
}

