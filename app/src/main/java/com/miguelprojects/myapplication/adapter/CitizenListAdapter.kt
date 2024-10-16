package com.miguelprojects.myapplication.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.listener.CitizenOnClickListener
import com.miguelprojects.myapplication.adapter.viewholder.CitizenViewHolder
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.util.CitizenManager
import com.miguelprojects.myapplication.util.ConvertManager

class CitizenListAdapter(
    private var citizenList: List<CitizenModel>,
    private var activeAction: Boolean,
    private val citizenOnClickListener: CitizenOnClickListener
) :
    RecyclerView.Adapter<CitizenViewHolder>() {

    private val setCitizenSelected = mutableSetOf<CitizenModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitizenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recyclerview_citizen, parent, false)
        return CitizenViewHolder(view)
    }

    override fun getItemCount(): Int {
        return citizenList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: CitizenViewHolder, position: Int) {
        val citizen = citizenList[position]

        val ageFormat = ConvertManager.calculateSimpleAge(citizen.birthdate)
        holder.name.text = citizen.name
        holder.age.text = ageFormat
        holder.image.setImageResource(
            CitizenManager.getCitizenImage(
                citizen.birthdate,
                citizen.sex
            )
        )
        // Definir o fundo com base no estado de seleção
        if (setCitizenSelected.contains(citizen)) {
            holder.itemView.setBackgroundResource(R.drawable.rounded_background_green_light_active)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.rounded_background_light_gray)
        }

        holder.itemView.setOnClickListener {
            if (activeAction) {
                if (setCitizenSelected.contains(citizen)) {
                    setCitizenSelected.remove(citizen)
                    holder.itemView.setBackgroundResource(R.drawable.rounded_background_light_gray)
                } else {
                    setCitizenSelected.add(citizen)
                    holder.itemView.setBackgroundResource(R.drawable.rounded_background_green_light_active)
                }
                notifyItemChanged(position)
            }

            citizenOnClickListener.clickListener(citizen, setCitizenSelected.toList())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<CitizenModel>) {
        citizenList = newList
        notifyDataSetChanged()
    }

    fun removeItemById(itemId: String?) {
        itemId ?: return
        val position = citizenList.indexOfFirst { it.id == itemId }

        if (position != -1) {
            citizenList = citizenList.toMutableList().apply {
                removeAt(position)
            }
            notifyItemRemoved(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelectionAndList() {
        setCitizenSelected.clear()
        citizenList = citizenList.toMutableList() // Cria uma nova lista para resetar os dados
        notifyDataSetChanged() // Atualiza o RecyclerView
    }

    fun getSelectedCitizens(): List<CitizenModel> = setCitizenSelected.toList()
}
