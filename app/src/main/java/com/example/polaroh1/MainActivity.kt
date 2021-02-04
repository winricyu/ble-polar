package com.example.polaroh1

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.text.InputFilter
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.datastore.core.DataStore
import androidx.datastore.createDataStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.polaroh1.repository.RepositoryKit
import com.example.polaroh1.repository.entity.ACCEntity
import com.example.polaroh1.repository.entity.HREntity
import com.example.polaroh1.repository.entity.PPGEntity
import com.example.polaroh1.repository.entity.PPIEntity
import com.example.polaroh1.utils.MainViewModel
import com.example.polaroh1.utils.MainViewModelFactory
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.model.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.prefs.Preferences


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS = 11
        private const val MAX_LENGTH = 8
        private const val DEVICE_ID = "DEVICE_ID"
    }

    private val mViewModel by lazy {
//        ViewModelProvider(this).get(MainViewModel::class.java)
        ViewModelProvider(this, mViewModelFactory).get(MainViewModel::class.java)

    }
    private val mViewModelFactory by lazy {
        MainViewModelFactory(application)
    }


    private val mWakeLock by lazy {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name))
    }

    private val mSharedPreferences by lazy {
        getSharedPreferences("local_storage", MODE_PRIVATE)
    }

    private val mPolarStatus = MutableLiveData<PolarStatus>()

    private var mDeviceId = ""
    private var mRecording = MutableLiveData<Boolean>()
    private var mConnectionStartTime = 0L

    private var mPPGDisposable: Disposable? = null
    private var mHRDisposable: Disposable? = null
    private var mPPIDisposable: Disposable? = null
    private var mACCDisposable: Disposable? = null

    private lateinit var mACCFlowable: Flowable<PolarAccelerometerData>
    private lateinit var mPPGFlowable: Flowable<PolarOhrPPGData>


    private fun getPackageInstallSource(packageName: String): String? {
        return try {
            packageManager.getInstallerPackageName(packageName)
//            packageManager.getInstallSourceInfo(packageName).originatingPackageName
        } catch (e: NoSuchMethodError) {
            e.localizedMessage
            null
        } catch (e: Exception) {
            e.localizedMessage
            null
        }
    }

    override fun onStart() {
        super.onStart()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /*
        //TODO 開啟APP安裝來源
//        val WECHAT = "com.tencent.mm"
//        val DYACO = "com.dyaco.xinglv"
//        val QIYI = "com.qiyi.video"
//        val SNAPCHAT = "com.snapchat.android"
        val launchAppStore = View.OnClickListener {
            (it as TextView).text?.toString()?.let { installedApp ->

                getPackageInstallSource(installedApp).let { source ->

                    when {

                        source.isNullOrBlank().not() -> {
                            //TODO 用安裝來源AppStore開啟的App頁面
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                `package` = "$source"
                                data = Uri.parse("market://details?id=$installedApp")
                            })
                        }
                        else -> {
                            //TODO 開啟任意 App Store
                            Toast.makeText(this, "找不到安裝來源", Toast.LENGTH_SHORT).show()
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("market://details?id=$installedApp")
                            })

                        }

                    }


                }
            }
        }


        btn_1.setOnClickListener(launchAppStore)
        btn_2.setOnClickListener(launchAppStore)
        btn_3.setOnClickListener(launchAppStore)*/

        //處理裝置連線狀態
        mPolarStatus.observe(this) { status ->
            println("ericyu - MainActivity.mPolarStatus: $status")
            //switch_record.isVisible = status == PolarStatus.CONNECTED
            progress_connection.isVisible =
                (status == PolarStatus.CONNECTING) or (status == PolarStatus.SEARCHING)

            when (status) {
                PolarStatus.IDLE -> {
                    switch_connect.text = "未連線"
                    tv_device_id.text = "--------"
                }
                PolarStatus.SEARCHING -> {
                    switch_connect.text = "搜尋中..."
                    tv_device_id.text = "--------"
                }
                PolarStatus.CONNECTING -> {
                    switch_connect.text = "連線中..."
                    tv_device_id.text = mDeviceId
                    mConnectionStartTime = 0
                }
                PolarStatus.CONNECTED -> {
                    switch_connect.text = "已連線"
                    mConnectionStartTime = SystemClock.elapsedRealtime()
                    tv_device_id.text = mDeviceId
                }
                PolarStatus.FAIL -> {
                    switch_connect.text = "連線失敗"
                    tv_device_id.text = "--------"
                }
                PolarStatus.DISCONNECTED -> {
                    switch_connect.text = "已斷線"
                    mConnectionStartTime = 0
                }
            }
        }

        mViewModel.mHR.observe(this) {
            tv_hr_value.text = it.hr.toString()
            val duration = (SystemClock.elapsedRealtime() - mConnectionStartTime)
            tv_device_info.text =
                "連線時間: ${
                    SimpleDateFormat("HH:mm:ss").apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .format(Date(duration))
                }"
        }


        mViewModel.mPPG.observe(this) { count ->
            tv_ppg_value.text = "$count"
        }

        mViewModel.mPPI.observe(this) { count ->
            tv_ppi_value.text = "$count"
        }

        mViewModel.mACCCount.observe(this) { count ->
            tv_acc_value.text = "$count"
        }


        //連接Polar裝置
        switch_connect.setOnCheckedChangeListener { switch, isChecked ->
            println("ericyu - MainActivity.setOnCheckedChangeListener isChecked:$isChecked")

            if (isChecked) {

                val deviceId = edt_device.text?.trim().toString()

                if (deviceId.isBlank()) {
                    mPolarStatus.value = PolarStatus.SEARCHING
                    mViewModel.mPolarApi.autoConnectToDevice(-50, "180D", null).subscribe(
                        {
                            println("ericyu - auto connect search complete")
                        },
                        {
                            println("ericyu - throwable: $it")
                            mPolarStatus.value = PolarStatus.FAIL
                        }
                    )

                } else {
                    if (deviceId.length < MAX_LENGTH) {
                        input_layout.error = "Device Id 長度為 $MAX_LENGTH"
                        switch.isChecked = !isChecked
                        return@setOnCheckedChangeListener
                    }

                    if (input_layout.error.isNullOrBlank()) {
                        mPolarStatus.value = PolarStatus.SEARCHING
                        mViewModel.mPolarApi.connectToDevice(deviceId)
                    }
                }

            } else {
                if (mDeviceId.isNotBlank()) {
                    mViewModel.mPolarApi.disconnectFromDevice(mDeviceId)
                }
                progress_connection.isVisible = false
                mViewModel.mPolarApi.cleanup()
                mPolarStatus.value = PolarStatus.IDLE
            }

        }

        //開始擷取資料
        switch_record.setOnCheckedChangeListener { _, isChecked ->
            mRecording.value = isChecked
        }

        mRecording.observe(this) { recording ->
//            group_record.isVisible = recording
            btn_download.isEnabled = !recording
            btn_clear.isEnabled = !recording
        }

        //輸入框
        edt_device.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        edt_device.addTextChangedListener {
            println("ericyu - MainActivity.addTextChangedListener, ${it?.toString()}")
            input_layout.error = null
        }

        btn_clear.setOnClickListener {
            lifecycleScope.launch {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("所有資料將被刪除且無法復原")
                    .setPositiveButton(
                        "刪除"
                    ) { dialog, which ->
                        println("ericyu - MainActivity.setPositiveButton, $which")
                        lifecycleScope.launch(context = Dispatchers.IO) {
                            RepositoryKit.clearAllData()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(
                        "取消"
                    ) { dialog, which ->
                        println("ericyu - MainActivity.setPositiveButton, $which")
                        dialog.dismiss()
                    }.show()
            }
        }

        mViewModel.mPolarApi.setApiCallback(object : PolarBleApiCallback() {

            override fun blePowerStateChanged(powered: Boolean) {
                super.blePowerStateChanged(powered)
                println("ericyu - MainActivity.blePowerStateChanged, powered = [${powered}]")
                switch_connect.isEnabled = powered
                Toast.makeText(
                    this@MainActivity,
                    "藍芽 ${if (powered) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()

            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                super.deviceConnected(polarDeviceInfo)
                println("ericyu - MainActivity.deviceConnected, polarDeviceInfo = [${polarDeviceInfo}]")
                mDeviceId = polarDeviceInfo.deviceId
                mSharedPreferences.edit().putString(DEVICE_ID, polarDeviceInfo.deviceId).apply()
                mPolarStatus.value = PolarStatus.CONNECTED
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                super.deviceConnecting(polarDeviceInfo)
                println("ericyu - MainActivity.deviceConnecting, polarDeviceInfo = [${polarDeviceInfo}]")
                mPolarStatus.value = PolarStatus.CONNECTING
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                super.deviceDisconnected(polarDeviceInfo)
                println("ericyu - MainActivity.deviceDisconnected, polarDeviceInfo = [${polarDeviceInfo}]")
                mDeviceId = ""
//                tv_hr_value.text = "--"
//                tv_ppg_value.text = "--"
//                tv_ppi_value.text = "--"
//                tv_acc_value.text = "--"
                mPPGDisposable?.dispose()
                mPPIDisposable?.dispose()
                mACCDisposable?.dispose()
                mPolarStatus.value = PolarStatus.DISCONNECTED
            }

            override fun ecgFeatureReady(identifier: String) {
                super.ecgFeatureReady(identifier)
                println("ericyu - MainActivity.ecgFeatureReady, identifier = [${identifier}]")
            }

            override fun accelerometerFeatureReady(identifier: String) {
                super.accelerometerFeatureReady(identifier)
                println("ericyu - MainActivity.accelerometerFeatureReady, identifier = [${identifier}]")
                //TODO 取得ACC
                tv_acc_value.text = "Ready..."
                mACCDisposable = mViewModel.mPolarApi.requestAccSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarAccelerometerData>> { accSetting: PolarSensorSetting ->
                        mViewModel.mPolarApi.startAccStreaming(identifier, accSetting.maxSettings())
                    })
                    .observeOn(Schedulers.io())
                    //.buffer(1000,TimeUnit.SECONDS)
                    .map {
                        it.samples.asSequence().mapIndexed { index, sample ->
                            sample.run {
                                ACCEntity(
                                    timestamp = Date(it.timeStamp),
                                    index = index,
                                    x = x,
                                    y = y,
                                    z = z
                                )
                            }
                        }.toList()
                    }
                    .subscribe(
                        {
                            //Callback 每???收到資料
                            println("ericyu - MainActivity.acc, onRecieved ${it.size}")
                            if (!switch_record.isChecked) return@subscribe
                            RepositoryKit.insertACCList(*it.toTypedArray())
                            mViewModel.mACCCount.apply {
                                this.postValue(this.value?.plus(it.size))
                            }
                        },
                        {
                            Log.e(
                                MainActivity::class.java.simpleName,
                                "ericyu - MainActivity.acc, onError ${it.localizedMessage}"
                            )
                        },
                        {
                            println("ericyu - MainActivity.acc, onComplete")
                        }
                    )

            }

            override fun ppgFeatureReady(identifier: String) {
                super.ppgFeatureReady(identifier)
                println("ericyu - MainActivity.ppgFeatureReady, identifier = [${identifier}]")
                //TODO 取得PPG
                tv_ppg_value.text = "Ready..."
                mPPGDisposable = mViewModel.mPolarApi.requestPpgSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarOhrPPGData>> { ppgSettings: PolarSensorSetting ->
                        mViewModel.mPolarApi.startOhrPPGStreaming(
                            identifier,
                            ppgSettings.maxSettings()
                        )
                    })
                    .observeOn(Schedulers.io())
                    .map {
                        it.samples.asSequence().mapIndexed { index, sample ->
                            sample.run {
                                PPGEntity(
                                    timestamp = Date(it.timeStamp),
                                    index = index,
                                    ppg0 = ppg0,
                                    ppg1 = ppg1,
                                    ppg2 = ppg2,
                                    ambient = ambient,
                                    ambient2 = ambient2,
                                    ppgDataSamples = ppgDataSamples,
                                    status = status
                                )
                            }
                        }.toList()
                    }
                    .subscribe(
                        {
                            //Callback 每150ms收到資料
                            println("ericyu - MainActivity.ppg, onRecieved ${it.size}")
                            if (!switch_record.isChecked) return@subscribe
                            RepositoryKit.insertPPGList(*it.toTypedArray())
                            mViewModel.mPPG.apply {
                                this.postValue(this.value?.plus(it.size))
                            }

                        },
                        {
                            Log.e(
                                MainActivity::class.java.simpleName,
                                "ericyu - MainActivity.ppg, onError ${it.localizedMessage}"
                            )
                        },
                        {
                            println("ericyu - MainActivity.ppg onComplete")
                        }
                    )


            }

            override fun ppiFeatureReady(identifier: String) {
                super.ppiFeatureReady(identifier)
                println("ericyu - MainActivity.ppiFeatureReady, identifier = [${identifier}]")
                //TODO 取得PPI
                tv_ppi_value.text = "Ready..."
                mPPIDisposable = mViewModel.mPolarApi.startOhrPPIStreaming(identifier)
                    .observeOn(Schedulers.io())
                    .map {
                        it.samples.asSequence().mapIndexed { index, sample ->
                            sample.run {
                                PPIEntity(
                                    timestamp = Date(it.timeStamp),
                                    index = index,
                                    ppi = ppi,
                                    hr = hr,
                                    blockerBit = blockerBit,
                                    errorEstimate = errorEstimate,
                                    skinContactStatus = skinContactStatus,
                                    skinContactSupported = skinContactSupported
                                )
                            }
                        }.toList()
                    }
                    .subscribe {

                        //Callback 每5秒收到資料
                        println("ericyu - MainActivity.ppi, onRecieve ${it.size}")
                        if (!switch_record.isChecked) return@subscribe
                        RepositoryKit.insertPPIList(*it.toTypedArray())
                        mViewModel.mPPI.apply {
                            this.postValue(this.value?.plus(it.size))
                        }

                    }

            }

            override fun biozFeatureReady(identifier: String) {
                super.biozFeatureReady(identifier)
                println("ericyu - MainActivity.biozFeatureReady, identifier = [${identifier}]")
            }

            override fun hrFeatureReady(identifier: String) {
                super.hrFeatureReady(identifier)
                println("ericyu - MainActivity.hrFeatureReady, identifier = [${identifier}]")
                tv_hr_value.text = "Ready..."
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                super.disInformationReceived(identifier, uuid, value)
                println("ericyu - MainActivity.disInformationReceived, identifier = [${identifier}], uuid = [${uuid}], value = [${value}]")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                super.batteryLevelReceived(identifier, level)
                println("ericyu - MainActivity.batteryLevelReceived, identifier = [${identifier}], level = [${level}]")
                progress_battery.progress = level
                tv_device_battery.text = "$level%"
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                super.hrNotificationReceived(identifier, data)
                println("ericyu - MainActivity.hrNotificationReceived, identifier = [${identifier}], hr = [${data.hr}]")

                //TODO 取得HR
                mViewModel.mHR.value = data

                if (!switch_record.isChecked) return
                data.run {
                    RepositoryKit.insertHR(
                        HREntity(
                            hr = hr,
                            rrs = rrs,
                            rrsMs = rrsMs,
                            contactStatus = contactStatus,
                            contactStatusSupported = contactStatusSupported,
                            rrAvailable = rrAvailable
                        )
                    )
                }

            }

            override fun polarFtpFeatureReady(identifier: String) {
                super.polarFtpFeatureReady(identifier)
                println("ericyu - MainActivity.polarFtpFeatureReady, identifier = [${identifier}]")
            }
        })

    }

    override fun onResume() {
        super.onResume()
        println("ericyu - MainActivity.onResume")
        mViewModel.mPolarApi.foregroundEntered()
    }

    override fun onPause() {
        super.onPause()
        println("ericyu - MainActivity.onPause")
        mViewModel.mPolarApi.backgroundEntered()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("ericyu - MainActivity.onDestroy")
        mViewModel.mPolarApi.shutDown()
        mWakeLock.release()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mWakeLock.acquire(86400000)

        when {
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) and (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) -> {
                println("ericyu - MainActivity.onCreate, permissions granted")

            }

            else -> {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), Companion.REQUEST_LOCATION_PERMISSIONS
                )
            }
        }

        //取得暫存device id
        mSharedPreferences.getString(DEVICE_ID, "")?.takeIf { !it.isNullOrBlank() }?.run {
            edt_device.setText(this)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("ericyu - MainActivity.onRequestPermissionsResult, requestCode = [${requestCode}], permissions = [${permissions.toList()}], grantResults = [${grantResults.toList()}]")
        if (permissions.toList()
                .all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        ) {
            println("ericyu - MainActivity.onRequestPermissionsResult PERMISSION_GRANTED ")
        } else {
            requestPermissions(permissions, REQUEST_LOCATION_PERMISSIONS)
        }
    }


}
