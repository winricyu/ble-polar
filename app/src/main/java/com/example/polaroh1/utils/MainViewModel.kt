package com.example.polaroh1.utils

import android.app.Application
import androidx.lifecycle.*
import com.example.polaroh1.repository.RepositoryKit
import com.example.polaroh1.repository.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.withContext
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiDefaultImpl


class MainViewModel(private val mContext: Application) : AndroidViewModel(mContext) {

    val currentHRList: MutableLiveData<MutableList<HREntity>> = MutableLiveData(mutableListOf())
    val rawPPGList: MutableLiveData<List<PPGEntity>> = MutableLiveData(listOf())
    val rawHR: MutableLiveData<Int> = MutableLiveData()
    val currentPPGList: MutableLiveData<MutableList<PPGEntity>> = MutableLiveData(mutableListOf())
    val currentPPIList: MutableLiveData<MutableList<PPIEntity>> = MutableLiveData(mutableListOf())
    val rawACCList: MutableLiveData<List<ACCEntity>> = MutableLiveData(listOf())
    val currentACCList: MutableLiveData<MutableList<ACCEntity>> = MutableLiveData(mutableListOf())
    val deviceDisconnectCounts: MutableLiveData<MutableList<Int>> = MutableLiveData(mutableListOf())
    val deviceId: MutableLiveData<String> = MutableLiveData("")
    var recordChannel: ReceiveChannel<List<RecordEntity>>?=null
    var exportBatchCount= 0
    var exportFileCount= 0


    val polarApi: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            mContext,
            PolarBleApi.ALL_FEATURES
        )
    }

    fun insertRecord(recordEntity: RecordEntity): Long {
        return RepositoryKit.insertRecord(recordEntity)
    }

    fun queryRecordCountAsync(): LiveData<Int> {
        return RepositoryKit.queryRecordCountAsync()
    }


    suspend fun queryRecordByCount(limit: Int, offset: Int): List<RecordEntity> {
        println("MainViewModel.queryRecordByCount , limit = [${limit}], offset = [${offset}]")
        return withContext(Dispatchers.IO) {
            RepositoryKit.queryRecordByCount(limit, offset)
        }
    }

    fun clearData() {
        currentHRList.value?.clear()
        currentPPGList.value?.clear()
        currentPPIList.value?.clear()
        currentACCList.value?.clear()
        deviceDisconnectCounts.value?.clear()
    }

    fun clearAllTableEntries() {
        RepositoryKit.clearAllTableEntries()
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
