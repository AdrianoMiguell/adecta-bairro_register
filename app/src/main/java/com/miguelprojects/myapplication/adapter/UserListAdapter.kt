package com.miguelprojects.myapplication.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.listener.UserOnClickListener
import com.miguelprojects.myapplication.adapter.viewholder.UserViewHolder
import com.miguelprojects.myapplication.model.UserModel

class UserListAdapter(
    private var userList: List<UserModel>,
    private var itemClicked: Boolean?,
    private val creatorWorkspace: String?,
    private val userOnClickListener: UserOnClickListener
) : RecyclerView.Adapter<UserViewHolder>() {
    private var selectedItemId = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycleview_notification_incoming_orders, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.image.setImageResource(R.drawable.baseline_account_circle_dark_24)
        holder.name.text = user.username
        holder.email.text = user.email
        holder.itemView.setBackgroundResource(R.color.transparent)

        if (user.id == creatorWorkspace) {
            val greenColor =
                ContextCompat.getColor(holder.itemView.context, R.color.main_green_haze)
            holder.name.setTextColor(greenColor)
            holder.name.text = "${user.username} (Criador)"
            holder.itemView.isEnabled = false
        }

        if (itemClicked == true) {
            holder.itemView.setOnClickListener {
                if (selectedItemId.contains(user.id)) {
                    selectedItemId.remove(user.id)
//                    Log.d("Teste de seleção de item", "Item removido da lista de ids")
                } else {
                    selectedItemId.add(user.id)
//                    Log.d("Teste de seleção de item", "Item adicionado na lista de ids")
                }

                userOnClickListener.clickListener(user)
                notifyDataSetChanged() // Notifica o RecyclerView sobre a mudança de dados
            }

            if (selectedItemId.contains(user.id)) {
                holder.itemView.setBackgroundResource(R.drawable.rounded_background_green_light_active)
            } else {
                holder.itemView.setBackgroundResource(R.color.transparent)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<UserModel>) {
        userList = newList
        notifyDataSetChanged()
    }
}