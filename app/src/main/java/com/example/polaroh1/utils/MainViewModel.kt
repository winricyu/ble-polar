package com.example.polaroh1.utils

import android.app.Application
import androidx.lifecycle.*
import com.bomdic.gomoreedgekit.data.GMStressSleep
import com.bomdic.gomoreedgekit.data.GMUserInfo
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
    val userInfo: GMUserInfo = GMUserInfo(31, 1, 172f, 73f, 192, 60)
    var stressSleepResult: MutableLiveData<GMStressSleep?> = MutableLiveData(GMStressSleep())
    val polarApi: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            mContext,
            PolarBleApi.ALL_FEATURES
        )
    }

    fun insertRecord(recordEntity: RecordEntity): Long {
        return RepositoryKit.insertRecord(recordEntity)
    }

    fun insertPPG(ppgEntity: PPGEntity) {
        RepositoryKit.insertPPG(ppgEntity)
    }

    fun insertPPGList(recordId: Long, vararg samples: PPGEntity) {
        RepositoryKit.insertPPGList(*samples.asSequence().onEach {
            it.recordId = recordId
        }.toList().toTypedArray())
    }

    fun insertACC(accEntity: ACCEntity) {
        RepositoryKit.insertACC(accEntity)
    }

    fun insertACCList(recordId: Long, vararg samples: ACCEntity) {
        RepositoryKit.insertACCList(*samples.asSequence().onEach {
            it.recordId = recordId
        }.toList().toTypedArray())
    }

    fun insertSleep(sleepEntity: SleepEntity) {
        RepositoryKit.insertSleep(sleepEntity)
    }

    fun insertHR(hrEntity: HREntity) {
        RepositoryKit.insertHR(hrEntity)
    }

    fun queryRecordCountAsync(): LiveData<Int> {
        return RepositoryKit.queryRecordCountAsync()
    }

    fun queryRecordCount(): Int {
        return RepositoryKit.queryRecordCount()
    }

    fun clearAllTableEntries() {
        RepositoryKit.clearAllTableEntries()
    }

    /*fun queryRecordDetailByCountAsync(limit: Int, offset: Int):LiveData<List<RecordAndDetail>>{
        return RepositoryKit.queryRecordDetailByCountAsync(limit,offset)
    }*/


    suspend fun queryRecordByCountAsync(limit: Int, offset: Int): LiveData<List<RecordEntity>> {
        return withContext(Dispatchers.IO) {
            RepositoryKit.queryRecordByCountAsync(limit, offset)
        }
    }

    suspend fun queryRecordByCount(limit: Int, offset: Int): List<RecordEntity> {
        println("MainViewModel.queryRecordByCount , limit = [${limit}], offset = [${offset}]")
        return withContext(Dispatchers.IO) {
            RepositoryKit.queryRecordByCount(limit, offset)
        }
    }

    suspend fun queryAllRecordAsync(): LiveData<List<RecordEntity>> {
        return withContext(Dispatchers.IO) {
            RepositoryKit.queryAllRecordsAsync()
        }
    }

    fun clearData() {
        currentHRList.value?.clear()
        currentPPGList.value?.clear()
        currentPPIList.value?.clear()
        currentACCList.value?.clear()
        deviceDisconnectCounts.value?.clear()
    }

    var recordChannel: ReceiveChannel<List<RecordEntity>>?=null


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
