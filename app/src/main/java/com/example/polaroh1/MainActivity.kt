package com.example.polaroh1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
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
import androidx.room.RoomWarnings
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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.sync.Semaphore
import org.reactivestreams.Publisher
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.model.*
import java.math.MathContext
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS = 11
        private const val MAX_LENGTH = 8
        private const val STORAGE_DEVICE_ID = "DEVICE_ID"
        private const val STORAGE_SETTING_CSV_EXPORT_COUNTS = "SETTING_CSV_EXPORT_COUNTS"
        private const val CSV_MAX_ROW = 3600
        private const val FILE_EXPORT_REQUEST_CODE = 123
        private val DATE_FORMAT = SimpleDateFormat("MMdd")
        private val PATTERN_WHITE_SPACE = "\\s".toRegex()

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

    private var mTimer: Timer? = null
    private val mPPGSemaphore = Semaphore(1)
    private val mHRSemaphore = Semaphore(1)
    private val mACCSemaphore = Semaphore(1)

    private lateinit var mCollectDataJob: Job

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        println("MainActivity.onActivityResult , requestCode = [${requestCode}], resultCode = [${resultCode}], data = [${data?.data}]")

        if ((requestCode == FILE_EXPORT_REQUEST_CODE) and (resultCode == Activity.RESULT_OK)) {

            data?.data?.apply fileUri@{
                lifecycleScope.launch(Dispatchers.IO) {
                    contentResolver.takePersistableUriPermission(
                        this@fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    mViewModel.recordChannel?.receiveOrNull()?.apply {
                        println("MainActivity.onActivityResult receiveOrNull:${this.size}, ${mViewModel.exportFileCount}/${mViewModel.exportBatchCount}")
                        writeCSVFile(this@fileUri, this)
                    }

                }
            }
        }


    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        println("MainActivity.onCreate")
        setContentView(R.layout.activity_main)
        mWakeLock.acquire(86400000)

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

        //取得本地暫存 device id
        mSharedPreferences.getString(STORAGE_DEVICE_ID, "")?.takeIf { !it.isNullOrBlank() }?.run {
            FirebaseCrashlytics.getInstance().setCustomKey("DeviceId", this)
            edt_device.setText(this)
        }
        //取得本地暫存 CSV單檔資料筆數設定值
        mSharedPreferences.getInt(STORAGE_SETTING_CSV_EXPORT_COUNTS, CSV_MAX_ROW).apply {
            edt_export.setText(this.toString())
        }

        tv_version.text = "v${BuildConfig.VERSION_NAME}"

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

        mViewModel.rawPPGList.observe(this) { list ->
            lifecycleScope.launch(Dispatchers.IO) {
                collectPPGRawData(list)
            }
        }

        mViewModel.rawHR.observe(this) {
            lifecycleScope.launch(Dispatchers.IO) {
                collectHRRawData(it)
            }
        }

        mViewModel.rawACCList.observe(this) {
            lifecycleScope.launch(Dispatchers.IO) {
                collectACCRawData(it)
            }
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


        //連接Polar裝置
        switch_connect.setOnCheckedChangeListener { switch, isChecked ->

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
        edt_device.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH), InputFilter.AllCaps())
        edt_device.addTextChangedListener {
            input_layout.error = null
        }
        edt_device.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                switch_connect.isChecked = true
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
                        clearCacheAndDatabase()
                        dialog.dismiss()
                    }
                    .setNegativeButton(
                        "取消"
                    ) { dialog, which ->
                        dialog.dismiss()
                    }.show()
            }
        }

        btn_download.setOnClickListener {

            //儲存CSV單檔資料筆數設定值
            edt_export.text.toString().takeIf { !it.isNullOrBlank() }?.apply {
                mSharedPreferences.edit().putInt(STORAGE_SETTING_CSV_EXPORT_COUNTS, this.toInt())
                    .apply()
            }

            mViewModel.queryRecordCountAsync().apply {
                observe(this@MainActivity) {
                    removeObservers(this@MainActivity)
                    if (it == 0) {
                        Toast.makeText(this@MainActivity, "無資料", Toast.LENGTH_SHORT).show()
                        return@observe
                    }
                    group_loading.isVisible = true
                    tv_loading.text = "處理中"
                    progress_file_loading.isVisible = true
                    downloadFile(it)
                }
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
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                super.deviceConnected(polarDeviceInfo)
                println("ericyu - MainActivity.deviceConnected, polarDeviceInfo = [${polarDeviceInfo}]")
                mViewModel.deviceId.value = polarDeviceInfo.deviceId
                mSharedPreferences.edit().putString(STORAGE_DEVICE_ID, polarDeviceInfo.deviceId)
                    .apply()
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
                            println("ericyu - MainActivity.acc, onRecieved ${it.size}")
                            if (!switch_record.isChecked) return@subscribe
                            mViewModel.rawACCList.postValue(it)
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
                            println("ericyu - MainActivity.ppg, onRecieved ${it.size}")

                            if (!switch_record.isChecked) return@subscribe
                            mViewModel.rawPPGList.postValue(it)
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
                //取得HR
                tv_hr_value.text = "${data.hr}"
                if (!switch_record.isChecked) return
                mViewModel.rawHR.postValue(data.hr)
            }

            override fun polarFtpFeatureReady(identifier: String) {
                super.polarFtpFeatureReady(identifier)
                println("ericyu - MainActivity.polarFtpFeatureReady, identifier = [${identifier}]")
            }
        })

        analyzeRecords()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.toList()
                .all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        ) {
        } else {
            requestPermissions(permissions, REQUEST_LOCATION_PERMISSIONS)
        }
    }

    private fun analyzeRecords() {

        mViewModel.queryRecordCountAsync().apply {
            observe(this@MainActivity) {
                println("MainActivity.queryRecordCountAsync analyzeRecords ${it}")
                removeObservers(this@MainActivity)
                tv_record_log.text = getString(R.string.records_log, it)
            }
        }
    }

    private fun initCollectDataJob() {
        mCollectDataJob =
            lifecycleScope.launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {

                //Polar OH1 可使用12小時
                repeat(43200) { repeatCount ->
                    val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                    //val recordId = mViewModel.insertRecord(RecordEntity(timestamp = Date(timestamp)))
                    val recordEntity = RecordEntity(timestamp = Date(timestamp))

                    runBlocking {
                        println("MainActivity.initCollectDataJob runBlocking 1")

                        //insertHRList(recordId)
                        try {
                            mHRSemaphore.acquire()
                            mViewModel.currentHRList.value?.apply {
                                recordEntity.hrList = this.map { it.hr }
                                clear()
                            }
                        } finally {
                            mHRSemaphore.release()
                        }

                        //insertPPGList(recordId)
                        try {
                            mPPGSemaphore.acquire()
                            mViewModel.currentPPGList.value?.apply {
                                recordEntity.ppg1List = this.map { it.ppg0 }
                                recordEntity.ppg2List = this.map { it.ppg1 }
                                recordEntity.ppg3List = this.map { it.ppg2 }
                                recordEntity.ambient1List = this.map { it.ambient }
                                clear()
                            }
                        } finally {
                            mPPGSemaphore.release()
                        }

                        //insertACCList(recordId)
                        try {
                            mACCSemaphore.acquire()
                            mViewModel.currentACCList.value?.apply {
                                recordEntity.accXList = this.map { it.x }
                                recordEntity.accYList = this.map { it.y }
                                recordEntity.accZList = this.map { it.z }
                                clear()
                            }
                        } finally {
                            mACCSemaphore.release()
                        }

                        mViewModel.insertRecord(recordEntity)

                        println("MainActivity.initCollectDataJob runBlocking 2")
                    }
                    //mDataLock = false

                    delay(1000)
                }

            }
    }

    //收集 HR raw data
    private suspend fun collectHRRawData(hr: Int) {
        println("MainActivity.collectHRRawData , hr = [${hr}]")
        withContext(Dispatchers.IO) {
            try {
                mHRSemaphore.acquire()
                mViewModel.currentHRList.value?.apply {
                    this.add(HREntity(hr = hr))
                    mViewModel.currentHRList.postValue(this)
                }
            } finally {
                mHRSemaphore.release()
            }
        }
    }

    //收集 ACC raw data
    private suspend fun collectACCRawData(list: List<ACCEntity>) {
        println("MainActivity.collectACCRawData , list = [${list.size}]")
        withContext(Dispatchers.IO) {
            try {
                mACCSemaphore.acquire()
                mViewModel.currentACCList.value?.apply {
                    this.addAll(list)
                    mViewModel.currentACCList.postValue(this)
                }
            } finally {
                mACCSemaphore.release()
            }
        }
    }

    //收集PPG raw data
    private suspend fun collectPPGRawData(list: List<PPGEntity>) {
        println("MainActivity.collectPPGRawData , list = [${list.size}] ,${mViewModel.currentPPGList.value?.size}")
        withContext(Dispatchers.IO) {
            try {
                mPPGSemaphore.acquire()
                mViewModel.currentPPGList.value?.apply {
                    this.addAll(list)
                    mViewModel.currentPPGList.postValue(this)
                }
            } finally {
                mPPGSemaphore.release()
            }
        }
    }

    //清除資料庫和暫存
    private fun clearCacheAndDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            mViewModel.clearAllTableEntries()
        }
        mViewModel.clearData()
        tv_hr_value.text = "--"
        tv_ppg_value.text = "--"
        tv_acc_value.text = "--"
        tv_record_log.text = getString(R.string.records_log, 0)
        progress_file_output.progress = 0
        progress_file_output.max = 0
    }

    //下載
    private fun downloadFile(totalRecord: Int) {
        println("MainActivity.downloadFile , totalRecord = [${totalRecord}]")
        mViewModel.exportBatchCount = 0
        mViewModel.exportFileCount = 0
        progress_file_output.progress = 0
        progress_file_output.max = 0

        val fileRecordLimit = edt_export.text.toString().toIntOrNull() ?: CSV_MAX_ROW
        println("MainActivity.downloadFile, fileRecordLimit:$fileRecordLimit")
        runBlocking {
            lifecycleScope.launch(Dispatchers.IO) {
                mViewModel.exportBatchCount =
                    (totalRecord.toFloat() / fileRecordLimit).toBigDecimal(
                        MathContext.DECIMAL32
                    ).setScale(0, RoundingMode.UP).toInt()
                progress_file_output.max = mViewModel.exportBatchCount

                println("MainActivity.downloadFile, batchCount:${mViewModel.exportBatchCount}")
                var repeat = 0

                mViewModel.recordChannel =
                    lifecycleScope.produce(Dispatchers.IO) {

                        println("MainActivity.downloadFile, recordChannel")

                        while (repeat <= mViewModel.exportBatchCount) {
                            println("MainActivity.downloadFile, repeat:$repeat")
                            delay(500)
                            val result =
                                withContext(Dispatchers.IO) {

                                    mViewModel.queryRecordByCount(
                                        fileRecordLimit,
                                        repeat * fileRecordLimit
                                    )
                                }
                            repeat++
                            //println("MainActivity.downloadFile, result:${result.size}")
                            send(result)

                        }
                    }

                createCSVFile("PolarOH1_${DATE_FORMAT.format(Date())}_${mViewModel.exportFileCount + 1}.csv")

            }
        }
    }

    //建立CSV檔案
    private fun createCSVFile(fileName: String) {
        println("MainActivity.saveFile , fileName = [${fileName}]")
        val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
        exportIntent.type = "text/csv"
        exportIntent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(exportIntent, FILE_EXPORT_REQUEST_CODE)
    }

    //資料寫入CSV檔案
    private suspend fun writeCSVFile(documentUri: Uri, list: List<RecordEntity>?) {
        println("MainActivity.writeCSV , documentUri = [${documentUri}], list = [${list?.size}] ,last:${list?.lastOrNull()?.id}")
        if (list.isNullOrEmpty()) return
        withContext(Dispatchers.IO) {

            val outputStream = this@MainActivity.contentResolver?.openOutputStream(documentUri)
                ?: return@withContext

            println("MainActivity.writeCSV START")
            csvWriter().openAsync(outputStream) {
                writeRow(
//                    listOf(
                    "TIMESTAMP",
                    "HR",
                    "PPG_1",
                    "PPG_2",
                    "PPG_3",
                    "AMBIENT_1",
                    "ACC_X",
                    "ACC_Y",
                    "ACC_Z"
//                    )
                )

                list.onEachIndexed { _, detail ->

                    writeRow(
//                        listOf(
                        detail.timestamp.time,
                        "[${detail.hrList.firstOrNull() ?: 0}]",
                        "[${detail.ppg1List.joinToString().replace(PATTERN_WHITE_SPACE, "")}]",
                        "[${detail.ppg2List.joinToString().replace(PATTERN_WHITE_SPACE, "")}]",
                        "[${detail.ppg3List.joinToString().replace(PATTERN_WHITE_SPACE, "")}]",
                        "[${
                            detail.ambient1List.joinToString().replace(PATTERN_WHITE_SPACE, "")
                        }]",
                        "[${detail.accXList.joinToString().replace(PATTERN_WHITE_SPACE, "")}]",
                        "[${detail.accYList.joinToString().replace(PATTERN_WHITE_SPACE, "")}]",
                        "[${detail.accZList.joinToString().replace(PATTERN_WHITE_SPACE, "")}]"
//                        )
                    )

                    //顯示CSV格式轉換處理進度
                    /*withContext(Dispatchers.Main) {
                        progress_file_output.progress += 1
                        tv_loading.text =
                            "${progress_file_output.progress}/${progress_file_output.max}"
                        progress_file_loading.isVisible =
                            progress_file_output.progress != progress_file_output.max
                    }*/

                }


            }
            println("MainActivity.writeCSV END")

            //顯示CSV格式轉換處理進度
            withContext(Dispatchers.Main) {
                progress_file_output.progress = mViewModel.exportFileCount + 1
                tv_loading.text = "${progress_file_output.progress}/${progress_file_output.max}"
                progress_file_loading.isVisible =
                    progress_file_output.progress != progress_file_output.max
            }

            mViewModel.exportFileCount++
            if (mViewModel.exportFileCount < mViewModel.exportBatchCount) {
                createCSVFile("PolarOH1_${DATE_FORMAT.format(Date())}_${mViewModel.exportFileCount + 1}.csv")
            }


        }

    }


    private suspend fun insertSampleRecords(counts: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val records = RepositoryKit.queryRecordByCount(10, 0)
            val new = RecordEntity()
            records.last().apply {
                new.hrList = hrList
                new.ppg1List = ppg1List
                new.ppg2List = ppg2List
                new.ppg3List = ppg3List
                new.ambient1List = ambient1List
                new.accXList = accXList
                new.accYList = accYList
                new.accZList = accZList
            }
            for (i in 1..counts) {
                RepositoryKit.insertRecord(new)
            }
        }
    }
}

