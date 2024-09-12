package com.miguelprojects.myapplication.adapter.listener

import com.miguelprojects.myapplication.model.CitizenModel

class CitizenOnClickListener (val clickListener: (citizenModel: CitizenModel, listCitizenSelected: List<CitizenModel>) -> Unit) {
    fun onClick(citizenModel: CitizenModel, listCitizenSelected: List<CitizenModel>) = clickListener
}