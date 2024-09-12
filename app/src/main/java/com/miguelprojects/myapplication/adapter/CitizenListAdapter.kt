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

    private val listCitizenSelected = mutableListOf<CitizenModel>()

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

//        if (citizen.image_id != null && citizen.image_id != 0) {
//            holder.image.setImageResource(citizen.image_id!!)
//        } else {
//            holder.image.setImageResource(R.drawable.baseline_account_circle_dark_24)
//        }

        val ageFormat = ConvertManager.calculateSimpleAge(citizen.birthdate)
        holder.name.text = citizen.name
        holder.age.text = ageFormat
        holder.image.setImageResource(CitizenManager.getCitizenImage(citizen.birthdate, citizen.sex))

        holder.itemView.setOnClickListener {
            if (activeAction) {
                if (listCitizenSelected.contains(citizen)) {
                    listCitizenSelected.remove(citizen)
                    holder.itemView.setBackgroundResource(R.drawable.rounded_background_light_gray)
                } else {
                    listCitizenSelected.add(citizen)
                    holder.itemView.setBackgroundResource(R.drawable.rounded_background_green_light_active)
                }
                notifyDataSetChanged()
            }

            citizenOnClickListener.clickListener(citizen, listCitizenSelected)
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
        listCitizenSelected.clear() // Limpa a lista de cidadãos selecionados
        citizenList = citizenList.toMutableList() // Cria uma nova lista para resetar os dados
        notifyDataSetChanged() // Atualiza o RecyclerView
    }
}
