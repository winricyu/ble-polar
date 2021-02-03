package com.example.polaroh1

import android.Manifest
import android.content.Context
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.polaroh1.repository.RepositoryKit
import com.example.polaroh1.repository.entity.ACCEntity
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import polar.com.sdk.api.PolarBleApi
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.model.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS = 11

        //private const val DEVICE_ID = "76643020"
        private const val MAX_LENGTH = 8

    }

    private val mPolarApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            this,
            PolarBleApi.ALL_FEATURES
        )
    }

    private val mWakeLock by lazy {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getString(R.string.app_name))
    }

    private val mPolarStatus = MutableLiveData<PolarStatus>()
    private val mHR = MutableLiveData<PolarHrData>()
    private val mPPG: MutableLiveData<MutableList<PolarOhrPPGData>> =
        MutableLiveData(mutableListOf())
    private val mPPI: MutableLiveData<MutableList<PolarOhrPPIData>> =
        MutableLiveData(mutableListOf())
    private val mACCCount: MutableLiveData<Int> = MutableLiveData(0)
    private var mDeviceId = ""
    private var mRecording = MutableLiveData<Boolean>()
    private var mConnectionStartTime = 0L

    private var mPPGDisposable: Disposable? = null
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
                    switch_record.isChecked = true
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

        mHR.observe(this) {
            tv_hr_value.text = it.hr.toString()
            val duration = (SystemClock.elapsedRealtime() - mConnectionStartTime)
            tv_device_info.text =
                "連線時間: ${
                    SimpleDateFormat("HH:mm:ss").apply { timeZone = TimeZone.getTimeZone("UTC") }
                        .format(Date(duration))
                }"
        }


        mPPG.observe(this) { list ->
            tv_ppg_value.text = "count ${list.asSequence().flatMap { it.samples }.toList().size}"
        }

        mPPI.observe(this) { list ->
            tv_ppi_value.text = "count ${list.asSequence().flatMap { it.samples }.toList().size}"
        }

        mACCCount.observe(this) { count ->
            println("ericyu - MainActivity.tv_acc_value: $count")
            tv_acc_value.text = "$count"
        }


        //連接Polar裝置
        switch_connect.setOnCheckedChangeListener { switch, isChecked ->
            println("ericyu - MainActivity.setOnCheckedChangeListener isChecked:$isChecked")

            if (isChecked) {

                val deviceId = edt_device.text?.trim().toString()

                if (deviceId.isBlank()) {
                    mPolarStatus.value = PolarStatus.SEARCHING
                    mPolarApi.autoConnectToDevice(-50, "180D", null).subscribe(
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
                        mPolarApi.connectToDevice(deviceId)
                    }
                }

            } else {
                if (mDeviceId.isNotBlank()) {
                    mPolarApi.disconnectFromDevice(mDeviceId)
                }
                progress_connection.isVisible = false
                mPolarApi.cleanup()
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

        mPolarApi.setApiCallback(object : PolarBleApiCallback() {

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
                tv_hr_value.text = ""
                tv_ppg_value.text = ""
                tv_ppi_value.text = ""
                tv_acc_value.text = ""
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
                tv_acc_value.text = "loading..."
                mACCDisposable = mPolarApi.requestAccSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarAccelerometerData>> { accSetting: PolarSensorSetting ->
                        mPolarApi.startAccStreaming(identifier, accSetting.maxSettings())
                    })
                    .observeOn(Schedulers.io())
                    //.buffer(1000,TimeUnit.SECONDS)
                    .map {
                        it.samples.asSequence().mapIndexed { index, sample ->
                            ACCEntity(
                                timestamp = Date(it.timeStamp),
                                index = index,
                                x = sample.x,
                                y = sample.y,
                                z = sample.z
                            )
                        }.toList()
                    }
                    .subscribe(
                        {
                            //Callback 每???收到資料
                            println("ericyu - MainActivity.acc, onRecieve ${it.size}")
                            if (!switch_record.isChecked) return@subscribe
                            RepositoryKit.insertAll(*it.toTypedArray())
                            mACCCount.apply {
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
                tv_ppg_value.text = "loading..."

                //TODO 取得PPG
                mPPGDisposable = mPolarApi.requestPpgSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarOhrPPGData>> { ppgSettings: PolarSensorSetting ->
                        mPolarApi.startOhrPPGStreaming(
                            identifier,
                            ppgSettings.maxSettings()
                        )
                    })
//                    .throttleFirst(2000, TimeUnit.MILLISECONDS, Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                        {
                            //Callback 每150ms收到資料
                            println("ericyu - MainActivity.ppg, onRecieve ${it.samples.size}")
                            mPPG.value?.add(it)
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
                tv_ppi_value.text = "loading..."
                mPPIDisposable = mPolarApi.startOhrPPIStreaming(identifier)
                    .observeOn(Schedulers.io())
                    .subscribe {

                        //Callback 每5秒收到資料
                        println("ericyu - MainActivity.ppi, onRecieve ${it.samples.size}")
                        mPPI.value?.add(it)

                    }

            }

            override fun biozFeatureReady(identifier: String) {
                super.biozFeatureReady(identifier)
                println("ericyu - MainActivity.biozFeatureReady, identifier = [${identifier}]")
            }

            override fun hrFeatureReady(identifier: String) {
                super.hrFeatureReady(identifier)
                println("ericyu - MainActivity.hrFeatureReady, identifier = [${identifier}]")
                tv_hr_value.text = "loading..."
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
                mHR.value = data

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
        mPolarApi.foregroundEntered()
    }

    override fun onPause() {
        super.onPause()
        println("ericyu - MainActivity.onPause")
        mPolarApi.backgroundEntered()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("ericyu - MainActivity.onDestroy")
        mPolarApi.shutDown()
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
