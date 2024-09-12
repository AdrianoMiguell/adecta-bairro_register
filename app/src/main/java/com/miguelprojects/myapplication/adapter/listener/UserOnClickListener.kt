package com.miguelprojects.myapplication.adapter.listener

import com.miguelprojects.myapplication.model.UserModel

class UserOnClickListener (val clickListener: (userModel: UserModel)  -> Unit) {
    fun onClick(userModel: UserModel) = clickListener
}