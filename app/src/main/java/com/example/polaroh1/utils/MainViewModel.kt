package com.example.polaroh1.utils

import android.app.Application
import androidx.lifecycle.*
import com.example.polaroh1.repository.entity.ACCEntity
import com.example.polaroh1.repository.entity.HREntity
import com.example.polaroh1.repository.entity.PPGEntity
import com.example.polaroh1.repository.entity.PPIEntity
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiDefaultImpl


class MainViewModel(private val mContext: Application) : AndroidViewModel(mContext) {

    val currentHRList: MutableLiveData<MutableList<HREntity>> = MutableLiveData(mutableListOf())
    val currentPPGList: MutableLiveData<MutableList<PPGEntity>> = MutableLiveData(mutableListOf())
    val currentPPIList: MutableLiveData<MutableList<PPIEntity>> = MutableLiveData(mutableListOf())
    val currentACCList: MutableLiveData<MutableList<ACCEntity>> = MutableLiveData(mutableListOf())
    val polarApi: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            mContext,
            PolarBleApi.ALL_FEATURES
        )
    }
    fun clearData(){
        currentHRList.value?.clear()
        currentPPGList.value?.clear()
        currentPPIList.value?.clear()
        currentACCList.value?.clear()
    }
}

class MainViewModelFactory(private val mContext: Application) :
    ViewModelProvider.AndroidViewModelFactory(mContext) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(mContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}