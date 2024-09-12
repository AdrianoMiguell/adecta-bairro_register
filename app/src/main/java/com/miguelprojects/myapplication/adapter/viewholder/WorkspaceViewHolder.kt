package com.miguelprojects.myapplication.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R

class WorkspaceViewHolder(view: View): RecyclerView.ViewHolder (view) {
    val image: ImageView = view.findViewById(R.id.image_workspace)
    val title: TextView = view.findViewById(R.id.title_workspace)
    val describe: TextView = view.findViewById(R.id.text_describe_workspace)
    val number_participants: TextView = view.findViewById(R.id.text_number_participants)
}