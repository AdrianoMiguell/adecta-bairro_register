package com.miguelprojects.myapplication.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R

class UserViewHolder (view: View): RecyclerView.ViewHolder (view) {
    val image: ImageView = view.findViewById(R.id.image_user_solicitor)
    val name: TextView = view.findViewById(R.id.text_name_user_solicitor)
    val email: TextView = view.findViewById(R.id.text_email_user_solicitor)
//    val button_selection: ImageView = view.findViewById(R.id.image_user_selection)
}