package com.example.polaroh1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.text.InputFilter
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bomdic.gomoreedgekit.GoMoreEdgeKit
import com.bomdic.gomoreedgekit.StressSleepParam
import com.bomdic.gomoreedgekit.StressSleepResult
import com.example.polaroh1.repository.RepositoryKit
import com.example.polaroh1.repository.entity.*
import com.example.polaroh1.utils.MainViewModel
import com.example.polaroh1.utils.MainViewModelFactory
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.reactivestreams.Publisher
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS = 11
        private const val MAX_LENGTH = 8
        private const val DEVICE_ID = "DEVICE_ID"
    }

    private val mViewModel by lazy {
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
    private var mRecording = MutableLiveData<Boolean>()
    private var mConnectionStartTime = 0L

    private var mPPGDisposable: Disposable? = null
    private var mPPIDisposable: Disposable? = null
    private var mACCDisposable: Disposable? = null
    private var mDataLock: Boolean = false
    private var mTimer: Timer? = null

    private lateinit var mCollectDataJob: Job

    private fun analyzeRecords() {
        lifecycleScope.launch(Dispatchers.IO) {

            RepositoryKit.queryAllRecords().run {

                runOnUiThread {
                    tv_record_log.text = getString(
                        R.string.records_log,
                        this.size,
                        mViewModel.deviceDisconnectCounts.value?.size ?: 0
                    )

                }
            }
        }
    }

    private suspend fun insertHRList(recordId: Long) {
        println("MainActivity.insertHRList , recordId = [${recordId}]")
        mViewModel.currentHRList.value?.run {
            //無資料時寫入空值
            if (isEmpty()) {
                RepositoryKit.insertHR(HREntity(recordId = recordId))
                return@run
            }
            RepositoryKit.insertHRList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private suspend fun insertPPGList(recordId: Long) {
        println("MainActivity.insertPPGList , recordId = [${recordId}]")
        mViewModel.currentPPGList.value?.run {
            //無資料時寫入空值
            if (isEmpty()) {
                RepositoryKit.insertPPG(PPGEntity(recordId = recordId))
                return@run
            }
            RepositoryKit.insertPPGList(*this.asSequence().onEachIndexed { index, it ->
                it.recordId = recordId
                updateSdkPPGRaw(it.ppg0)
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private fun getSdkActivityInfo(hr: Int): FloatArray {

        return GoMoreEdgeKit.getActivityInfoExt(
            intArrayOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()),
            floatArrayOf(hr.toFloat())
        )
    }


    //TODO GoMoreEdgeKit.updatePPGRaw 每秒呼叫, 帶入所有ppg, 回傳result 寫入DB
    private fun updateSdkPPGRaw(ppg: Int) {
        if (ppg < 0) return

        //hrArray 首次給空陣列, 之後取 stressSleepResult?.hrArray
        //rmssdArray 首次給空陣列, 之後取 stressSleepResult?.rmssdArray
        val hrArr = mViewModel.stressSleepResult.value?.hrArray ?: intArrayOf()
        val rmssdArr = mViewModel.stressSleepResult.value?.rmssdArray ?: intArrayOf()
        val updatePPGResult = GoMoreEdgeKit.updatePPGRaw(
            LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt(),
            StressSleepParam(
                ppg.toFloat(),
                0f,
                135,
                floatArrayOf(0f, 0f, 0f),
                0,
                hrArr,
                rmssdArr
            ),
            StressSleepResult()
        )

//        mViewModel.stressSleepResult.value?.apply {

        //TODO 寫入DB
        updatePPGResult?.also {

            if ((it.hrArray.isEmpty()) or (it.rmssdArray.isEmpty())) {
                return@also
            }

            lifecycleScope.launch(Dispatchers.IO) {
                RepositoryKit.insertSleep(
                    SleepEntity(
                        stress = it.stress,
                        hrArray = it.hrArray.toList(),
                        ppiArray = it.ppiArray.toList(),
                        ppiLen = it.ppiLen,
                        rmssdArray = it.rmssdArray.toList()
                    )
                )
            }

            mViewModel.stressSleepResult.postValue(updatePPGResult)
        }
//        }
    }

    private suspend fun insertPPIList(recordId: Long) {
        println("MainActivity.insertPPIList , recordId = [${recordId}]")
        mViewModel.currentPPIList.value?.run {
            //無資料時寫入空值
            if (isEmpty()) {
                RepositoryKit.insertPPI(PPIEntity(recordId = recordId))
                return@run
            }
            RepositoryKit.insertPPIList(*this.asSequence().onEach {
                it.recordId = recordId
            }.toList().toTypedArray())
            this.clear()
        }
    }

    private suspend fun insertACCList(recordId: Long) {
        println("MainActivity.insertACCList , recordId = [${recordId}]")
        mViewModel.currentACCList.value?.run {
            //無資料時寫入空值
            if (isEmpty()) {
                RepositoryKit.insertACC(ACCEntity(recordId = recordId))
                return@run
            }
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
                    val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    val recordId =
                        RepositoryKit.insertRecord(RecordEntity(timestamp = Date(timestamp)))

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

                    delay(1000)
                }

            }
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        //處理裝置連線狀態
        mPolarStatus.observe(this) { status ->
            println("ericyu - MainActivity.mPolarStatus: $status")
            progress_connection.isVisible =
                (status == PolarStatus.CONNECTING) or (status == PolarStatus.SEARCHING)
            switch_connect.setTextColor(getColor(if (status == PolarStatus.DISCONNECTED) R.color.error else android.R.color.black))

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
                    mConnectionStartTime = 0
                }
                PolarStatus.CONNECTED -> {

                    mTimer = Timer("connection_timer").apply {
                        schedule(object : TimerTask() {
                            override fun run() {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    tv_device_info.text =
                                        "連線時間: ${
                                            SimpleDateFormat("HH:mm:ss").apply {
                                                timeZone = TimeZone.getTimeZone("UTC")
                                            }
                                                .format(Date(SystemClock.elapsedRealtime() - mConnectionStartTime))
                                        }"
                                }
                            }
                        }, 0, 1000)
                    }
                    switch_connect.text = "已連線"
                    mConnectionStartTime = SystemClock.elapsedRealtime()

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
        mViewModel.deviceId.observe(this) {
            if (it.isNotBlank()) {
                FirebaseCrashlytics.getInstance().setCustomKey("DeviceId", it)
            }

            tv_device_id.text = if (it.isBlank()) "--------" else it
        }

        mViewModel.stressSleepResult.observe(this) {
            //TODO 列印SDK結果
            it?.apply {
                println("stressSleepResult, stress:$stress, ppiArray:${ppiArray.toList()}, ppiLen:$ppiLen")
                tv_sdk_value.text = "stress:$stress, ppiLen:$ppiLen"
            }
        }


        //連接Polar裝置
        switch_connect.setOnCheckedChangeListener { switch, isChecked ->
            println("ericyu - MainActivity.setOnCheckedChangeListener isChecked:$isChecked")

            if (isChecked) {

                val deviceId = edt_device.text?.trim().toString()


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
                mViewModel.deviceId.value?.takeIf { it.isNotBlank() }?.run {
                    mViewModel.polarApi.disconnectFromDevice(this)
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
            btn_download.isEnabled = !recording
            btn_clear.isEnabled = !recording

            //顯示紀錄狀態或資料筆數
            if (recording) {
                tv_record_log.text = "記錄中..."
            } else {
                analyzeRecords()
            }
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
                if (RepositoryKit.queryAllRecords().isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "無資料", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                downloadFile()
            }
        }

        mViewModel.polarApi.setAutomaticReconnection(true)
        mViewModel.polarApi.setApiLogger {
            println("ericyu - MainActivity.setApiLogger: $it")
        }

        mViewModel.polarApi.setApiCallback(object : PolarBleApiCallback() {

            override fun blePowerStateChanged(powered: Boolean) {
                super.blePowerStateChanged(powered)
                println("ericyu - MainActivity.blePowerStateChanged, powered = [${powered}]")
                switch_connect.isEnabled = powered
                /*Toast.makeText(
                    this@MainActivity,
                    "藍芽 ${if (powered) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()*/

            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                super.deviceConnected(polarDeviceInfo)
                println("ericyu - MainActivity.deviceConnected, polarDeviceInfo = [${polarDeviceInfo}]")
                mViewModel.deviceId.value = polarDeviceInfo.deviceId
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
                mViewModel.deviceId.value = ""
                mViewModel.deviceDisconnectCounts.value?.add(1)
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
                //取得ACC
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
                //取得PPG
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
                //取得PPI
                tv_ppi_value.text = "Ready..."
                /*mPPIDisposable = mViewModel.polarApi.startOhrPPIStreaming(identifier)
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

                    }*/

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
                //取得HR
                tv_hr_value.text = "${data.hr}"
                if (!switch_record.isChecked) return
                if (mDataLock) return

                //TODO GoMoreEdgeKit.getActivityInfo 每秒呼叫, 取得float array 寫入DB
                /*GoMoreEdgeKit.getActivityInfo(
                    intArrayOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toInt()),
                    floatArrayOf(data.hr.toFloat())
                )*/



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

        //TODO 測試寫入GoMoreEdgeKit
        GoMoreEdgeKit.initialize()
        GoMoreEdgeKit.healthIndexInitUser(mViewModel.userInfo, 1609372799)

        when {
            (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) and
                    (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) and
                    (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED) and
                    (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED) -> {
                println("ericyu - MainActivity.onCreate, permissions granted")

            }

            else -> {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ), Companion.REQUEST_LOCATION_PERMISSIONS
                )
            }
        }

        //取得暫存device id
        mSharedPreferences.getString(DEVICE_ID, "")?.takeIf { !it.isNullOrBlank() }?.run {
            FirebaseCrashlytics.getInstance().setCustomKey("DeviceId", this)
            edt_device.setText(this)
        }

        tv_version.text = "v${BuildConfig.VERSION_NAME}"

        //預設狀態
//        btn_download.isEnabled = false
//        btn_clear.isEnabled = false
        analyzeRecords()

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
            //RepositoryKit.clearAllTables()
            RepositoryKit.clearAllTableEntries()
        }
        mViewModel.clearData()
        tv_hr_value.text = "--"
        tv_ppg_value.text = "--"
        tv_ppi_value.text = "--"
        tv_acc_value.text = "--"
        tv_record_log.text = getString(R.string.records_log, 0, 0)
        progress_file_output.progress = 0
        progress_file_output.max = 0
    }

    private fun downloadFile() {

        lifecycleScope.launch(Dispatchers.Main) {
            group_loading.isVisible = true
            progress_file_output.progress = 0
            progress_file_output.max = 0
            tv_loading.text = "處理中"
            progress_file_loading.isVisible = true
            /*repeat(3) {
                delay(1000)
                tv_loading.text = "${tv_loading.text}."
            }*/

            RepositoryKit.queryRecordAndDetailAsync().apply {
                observe(this@MainActivity) {
                    println("ericyu - MainActivity.downloadFile, result:${it.size}")
                    removeObservers(this@MainActivity)
                    //設定 loading progress max
                    progress_file_output.max = it.size
                    progress_file_loading.isVisible = false
                    runBlocking {
                        writeToCSV(it)
                    }
                }
            }
        }
    }


    private suspend fun writeToCSV(list: List<RecordAndDetail>) {
        lifecycleScope.launch(Dispatchers.IO) {
            var file: File? = null
            val filePath = this@MainActivity.filesDir.toString() + "/images/polar_data.csv"
            file = File(filePath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdir()
            }
            val fileUri =
                FileProvider.getUriForFile(this@MainActivity, "polaroh1.fileprovider", file)

            csvWriter().openAsync(file) {
                writeRow(listOf("TIMESTAMP", "HR", "PPG", "ACC_X", "ACC_Y", "ACC_Z"))

                list.onEachIndexed { index, detail ->

                    writeRow(
                        listOf(
                            detail.record.timestamp.time,
                            "[${detail.hrList.joinToString()}]",
                            "[${detail.ppgList.joinToString()}]",
                            "[${detail.accXList.joinToString()}]",
                            "[${detail.accYList.joinToString()}]",
                            "[${detail.accZList.joinToString()}]"
                        )
                    )
                    //顯示CSV格式轉換處理進度
                    withContext(Dispatchers.Main) {
                        progress_file_output.progress = index + 1
                        tv_loading.text =
                            "${progress_file_output.progress}/${progress_file_output.max}"
                    }
                }
            }

            if (file.exists()) {

                runOnUiThread {
                    group_loading.isVisible = false
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        val createtime =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).run {
                                format(Date(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)))
                            }
//                        val createDate = createtime.split(" ").firstOrNull() ?: ""
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "PolarOH1 [${edt_device.text.toString() ?: ""}] ${
                                createtime.split(" ").firstOrNull() ?: ""
                            } 追蹤紀錄"
                        )
                        putExtra(Intent.EXTRA_TEXT, "建立時間: $createtime")
                        type = "text/csv"
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(shareIntent, "分享檔案"))

                }

            }

        }
    }

}

/*
fun GoMoreEdgeKit.Kit.updatePPGRawExt(
    currentTime: kotlin.Int,
    stressSleepParam: com.bomdic.gomoreedgekit.StressSleepParam,
    stressSleepResult: com.bomdic.gomoreedgekit.StressSleepResult
): StressSleepResult? {

//    println("ericyu - <top>.updatePPGRawExt, currentTime = [${currentTime}], ppgRaw1 = [${stressSleepParam}], hrArray = [${stressSleepParam.hrArray.size}], rmssdArray = [${stressSleepParam.rmssdArray.size}]")
    return GoMoreEdgeKit.updatePPGRaw(currentTime, stressSleepParam, stressSleepResult)
}
*/

fun GoMoreEdgeKit.Kit.getActivityInfoExt(
    timestampList: kotlin.IntArray,
    hrList: kotlin.FloatArray
): FloatArray {
    println("ericyu - <top>.getActivityInfoExt, timestampList = ${timestampList.toList()}, hrList = ${hrList.toList()}")
    return getActivityInfo(timestampList, hrList)
}



