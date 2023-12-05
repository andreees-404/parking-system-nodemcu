package com.domaintest.parkingservice.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MessageViewModel : ViewModel(){
    private val _message = MutableLiveData<String>()

    val message: LiveData<String> get() = _message



    fun setMessage(msg: String){
        _message.value = msg
    }

    fun clearMsg(){
        _message.value = ""
    }
}