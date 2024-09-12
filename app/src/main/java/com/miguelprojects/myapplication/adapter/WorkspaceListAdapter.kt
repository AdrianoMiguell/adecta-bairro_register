package com.miguelprojects.myapplication.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.listener.WorkspaceOnClickListener
import com.miguelprojects.myapplication.adapter.viewholder.WorkspaceViewHolder
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.util.ConvertManager

class WorkspaceListAdapter(
    private var workspaceList: List<WorkspaceModel>,
    private val workspaceOnClickListener: WorkspaceOnClickListener
) : RecyclerView.Adapter<WorkspaceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkspaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycleview_workspace, parent, false)
        return WorkspaceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return workspaceList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: WorkspaceViewHolder, position: Int) {
        val workspace = workspaceList[position]
        val total_number_participantes = workspace.userIds.size

        holder.title.text = ConvertManager.capitalizeWords(workspace.name)
        holder.describe.text = workspace.description

        when {
            total_number_participantes > 1 -> {
                holder.number_participants.text =
                    "$total_number_participantes Participantes no grupo"
            }

            total_number_participantes == 1 -> {
                holder.number_participants.text =
                    "$total_number_participantes Participante no grupo"
            }

            else -> {
                holder.number_participants.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            workspaceOnClickListener.clickListener(workspace)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<WorkspaceModel>) {
        workspaceList = newList
        notifyDataSetChanged()
    }

    fun removeItemById(itemId: String?) {
        itemId ?: return
        val position = workspaceList.indexOfFirst { it.id == itemId }

        if (position != -1) {
            workspaceList = workspaceList.toMutableList().apply {
                removeAt(position)
            }
            notifyItemRemoved(position)
        }
    }

}
