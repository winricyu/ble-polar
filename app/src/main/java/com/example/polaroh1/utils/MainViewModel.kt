package com.example.polaroh1.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.disposables.Disposable
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.PolarHrData


class MainViewModel(private val mContext: Application) : AndroidViewModel(mContext) {

    val mHR: MutableLiveData<PolarHrData> = MutableLiveData<PolarHrData>()
    val mPPG: MutableLiveData<Int> = MutableLiveData(0)
    val mPPI: MutableLiveData<Int> = MutableLiveData(0)
    val mACCCount: MutableLiveData<Int> = MutableLiveData(0)

    init {

    }

    val mPolarApi: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            mContext,
            PolarBleApi.ALL_FEATURES
        )
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
