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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.polaroh1.repository.RepositoryKit
import com.example.polaroh1.repository.entity.*
import com.example.polaroh1.utils.MainViewModel
import com.example.polaroh1.utils.MainViewModelFactory
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.reactivestreams.Publisher
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.model.*
import java.text.SimpleDateFormat
import java.util.*


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
    private var mPPIDisposable: Disposable? = null
    private var mACCDisposable: Disposable? = null
    private var mDataLock: Boolean = false
    private var mTimer: Timer? = null

    private lateinit var mCollectDataJob: Job

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

    private suspend fun insertHRList(recordId: Long) {
        println("MainActivity.insertHRList , recordId = [${recordId}]")
        mViewModel.currentHRList.value?.run {
//            sb.append("HR:${this.size}, ")
            RepositoryKit.insertHRList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private suspend fun insertPPGList(recordId: Long) {
        println("MainActivity.insertPPGList , recordId = [${recordId}]")
        mViewModel.currentPPGList.value?.run {
//            sb.append("PPG:${this.size}, ")
            RepositoryKit.insertPPGList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private suspend fun insertPPIList(recordId: Long) {
        println("MainActivity.insertPPIList , recordId = [${recordId}]")
        mViewModel.currentPPIList.value?.run {
            //sb.append("PPI:${this.size}, ")
            RepositoryKit.insertPPIList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private suspend fun insertACCList(recordId: Long) {
        println("MainActivity.insertACCList , recordId = [${recordId}]")
        mViewModel.currentACCList.value?.run {
            //sb.append("ACC:${this.size}")
            RepositoryKit.insertACCList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private fun initCollectDataJob() {
        mCollectDataJob =
            lifecycleScope.launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {

                //Polar OH1 可使用12小時
                repeat(43200) { repeatCount ->

                    val recordId =
                        RepositoryKit.insertRecord(RecordEntity(timestamp = Date(System.currentTimeMillis())))

                    runBlocking {
                        mDataLock = true
                        println("MainActivity.initCollectDataJob runBlocking 1 mDataLock:$mDataLock")
                        val hr = async { insertHRList(recordId) }
                        hr.await()
                        val ppg = async { insertPPGList(recordId) }
                        ppg.await()
                        val ppi = async { insertPPIList(recordId) }
                        ppi.await()
                        val acc = async { insertACCList(recordId) }
                        acc.await()
                        println("MainActivity.initCollectDataJob runBlocking mDataLock:$mDataLock")
                    }
                    mDataLock = false
//                    val sb = StringBuilder()
//                    sb.append("ericyu - MainActivity.repeat $repeatCount, ")
                    //TODO 每秒收集HR, PPG, PPI, ACC 暫存資料寫入DB

                    /* mViewModel.currentHRList.value?.run {
                         sb.append("HR:${this.size}, ")
                         RepositoryKit.insertHRList(*this.asSequence().onEach {
                             it.recordId = recordId
                         }.toList().toTypedArray())
                         this.clear()
                     }*/
                    /*mViewModel.currentPPGList.value?.run {
                        sb.append("PPG:${this.size}, ")
                        RepositoryKit.insertPPGList(*this.asSequence().onEach {
                            it.recordId = recordId
                        }.toList().toTypedArray())
                        this.clear()
                    }*/
                    /*mViewModel.currentPPIList.value?.run {
                        sb.append("PPI:${this.size}, ")
                        RepositoryKit.insertPPIList(*this.asSequence().onEach {
                            it.recordId = recordId
                        }.toList().toTypedArray())
                        this.clear()
                    }*/
                    /*mViewModel.currentACCList.value?.run {
                        sb.append("ACC:${this.size}")
                        RepositoryKit.insertACCList(*this.asSequence().onEach {
                            it.recordId = recordId
                        }.toList().toTypedArray())
                        this.clear()
                    }*/
                    delay(1000)
                }

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
                    mTimer?.cancel()
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

                    mTimer = Timer("connection_timer").apply {
                        schedule(object : TimerTask() {
                            override fun run() {
                                tv_device_info.text =
                                    "連線時間: ${
                                        SimpleDateFormat("HH:mm:ss").apply {
                                            timeZone = TimeZone.getTimeZone("UTC")
                                        }
                                            .format(Date(SystemClock.elapsedRealtime() - mConnectionStartTime))
                                    }"
                            }

                        }, 0, 1000)
                    }
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
                    mTimer?.cancel()
                }
            }
        }

        mViewModel.currentHRList.observe(this) { list ->
            tv_hr_value.text = "${list.lastOrNull()?.hr}"
        }
        mViewModel.currentPPGList.observe(this) { list ->
            tv_ppg_value.text = "${list.size}"
        }
        mViewModel.currentPPIList.observe(this) { list ->
            tv_ppi_value.text = "${list.size}"
        }
        mViewModel.currentACCList.observe(this) { list ->
            tv_acc_value.text = "${list.size}"
        }


        //連接Polar裝置
        switch_connect.setOnCheckedChangeListener { switch, isChecked ->
            println("ericyu - MainActivity.setOnCheckedChangeListener isChecked:$isChecked")

            if (isChecked) {

                val deviceId = edt_device.text?.trim().toString()

                /*if (deviceId.isBlank()) {
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

                } else {*/
                if (deviceId.isBlank() or (deviceId.length < MAX_LENGTH)) {
                    input_layout.error = "請輸入 Device ID 共 $MAX_LENGTH 碼"
                    switch.isChecked = !isChecked
                    return@setOnCheckedChangeListener
                }
                if (input_layout.error.isNullOrBlank()) {
                    mPolarStatus.value = PolarStatus.SEARCHING
                    mViewModel.polarApi.connectToDevice(deviceId)
                }
//                }

            } else {
                if (mDeviceId.isNotBlank()) {
                    mViewModel.polarApi.disconnectFromDevice(mDeviceId)
                }
                switch_record.isChecked = false
                progress_connection.isVisible = false
                mViewModel.polarApi.cleanup()
                mPolarStatus.value = PolarStatus.IDLE
            }

        }

        //開始擷取資料
        switch_record.setOnCheckedChangeListener { _, isChecked ->
            mRecording.value = isChecked
            if (isChecked) {
                initCollectDataJob()
                mCollectDataJob.start()
            } else {
                mCollectDataJob.cancel()

            }

        }

        mRecording.observe(this) { recording ->
            //TODO 測試下載
            //btn_download.isEnabled = !recording
            btn_clear.isEnabled = !recording
        }

        //輸入框
        edt_device.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        edt_device.addTextChangedListener {
            println("ericyu - MainActivity.addTextChangedListener, ${it?.toString()}")
            input_layout.error = null
        }
        edt_device.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                switch_connect.isChecked = true
                true
            }
            false
        }

        btn_clear.setOnClickListener {
            lifecycleScope.launch {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("警告")
                    .setMessage("所有資料將被刪除且無法復原")
                    .setPositiveButton(
                        "刪除"
                    ) { dialog, which ->
                        println("ericyu - MainActivity.setPositiveButton, $which")
                        clearCacheAndDatabase()
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

        btn_download.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val size = RepositoryKit.queryAllRecords().size
                println("ericyu - MainActivity.btn_download queryAllRecords:$size")
                if (size <= 0) return@launch
                downloadFile()
            }
        }


        mViewModel.polarApi.setApiLogger {
            println("ericyu - MainActivity.setApiLogger: $it")
        }

        mViewModel.polarApi.setApiCallback(object : PolarBleApiCallback() {

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
                mACCDisposable = mViewModel.polarApi.requestAccSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarAccelerometerData>> { accSetting: PolarSensorSetting ->
                        mViewModel.polarApi.startAccStreaming(
                            identifier,
                            accSetting.maxSettings()
                        )
                    })
                    .observeOn(Schedulers.io())
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
                            //每???ms收到資料
                            println("ericyu - MainActivity.acc, onRecieved ${it.size}, mDataLock:${mDataLock}")
                            if (!switch_record.isChecked) return@subscribe
                            if (mDataLock) return@subscribe
                            mViewModel.currentACCList.apply {
                                this.value?.addAll(it)
                                this.postValue(this.value)
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
                mPPGDisposable = mViewModel.polarApi.requestPpgSettings(identifier)
                    .toFlowable()
                    .flatMap(Function<PolarSensorSetting, Publisher<PolarOhrPPGData>> { ppgSettings: PolarSensorSetting ->
                        mViewModel.polarApi.startOhrPPGStreaming(
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
                            //每150ms收到資料
                            println("ericyu - MainActivity.ppg, onRecieved ${it.size}, mDataLock:${mDataLock}")
                            if (!switch_record.isChecked) return@subscribe
                            if (mDataLock) return@subscribe

                            mViewModel.currentPPGList.apply {
                                this.value?.addAll(it)
                                this.postValue(this.value)
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
                mPPIDisposable = mViewModel.polarApi.startOhrPPIStreaming(identifier)
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

                        //每5000ms收到資料
                        println("ericyu - MainActivity.ppi, onRecieve ${it.size}, mDataLock:${mDataLock}")
                        if (!switch_record.isChecked) return@subscribe
                        if (mDataLock) return@subscribe

                        mViewModel.currentPPIList.apply {
                            this.value?.addAll(it)
                            this.postValue(this.value)
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
                println("ericyu - MainActivity.hrNotificationReceived, identifier = [${identifier}], hr = [${data.hr}], mDataLock:${mDataLock}")
                //TODO 取得HR
                tv_hr_value.text = "${data.hr}"
                if (!switch_record.isChecked) return
                if (mDataLock) return

                mViewModel.currentHRList.apply {
                    this.value?.add(HREntity(hr = data.hr))
                    this.postValue(this.value)
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
        mViewModel.polarApi.foregroundEntered()
    }

    override fun onPause() {
        super.onPause()
        println("ericyu - MainActivity.onPause")
        mViewModel.polarApi.backgroundEntered()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("ericyu - MainActivity.onDestroy")
        mViewModel.polarApi.shutDown()
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

    //清除資料庫和暫存
    private fun clearCacheAndDatabase() {
        lifecycleScope.launch(context = Dispatchers.IO) {
            RepositoryKit.clearAllTables()
        }
        mViewModel.clearData()
        tv_hr_value.text = "--"
        tv_ppg_value.text = "--"
        tv_ppi_value.text = "--"
        tv_acc_value.text = "--"
    }

    private fun downloadFile() {
        println("ericyu - MainActivity.downloadFile")
        /*lifecycleScope.launch(Dispatchers.IO) {
            RepositoryKit.queryAllRecords().onEach { record ->


            }
        }*/
        lifecycleScope.launch(Dispatchers.Main) {

            RepositoryKit.queryRecordAndDetailAsync().observe(this@MainActivity) {
                println("ericyu - MainActivity.downloadFile, result:${it.size}")
            }


        }

    }

}
