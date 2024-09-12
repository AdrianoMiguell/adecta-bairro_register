package com.miguelprojects.myapplication.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R

class CitizenViewHolder (view: View): RecyclerView.ViewHolder (view) {
    val image: ImageView = view.findViewById(R.id.image_citizen)
    val name: TextView = view.findViewById(R.id.text_name_citizen)
    val age: TextView = view.findViewById(R.id.text_age_citizen)
}