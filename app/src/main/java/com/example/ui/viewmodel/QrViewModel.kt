package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QrDatabase
import com.example.data.local.QrHistoryEntity
import com.example.data.local.QrPreferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.QrErrorCorrection
import com.example.data.model.QrHistoryType
import com.example.data.model.QrPreferences
import com.example.data.model.ScannedResult
import com.example.data.repository.QrRepository
import com.example.qr.QrCodeEngine
import com.example.qr.QrMatrix
import com.example.qr.QrMediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppDestination(val route: String, val label: String) {
    GENERATE("generate", "生成"),
    SCAN("scan", "扫描"),
    HISTORY("history", "历史记录"),
    SETTINGS("settings", "设置")
}

data class GenerateUiState(
    val inputText: String = "",
    val errorCorrection: QrErrorCorrection = QrErrorCorrection.M,
    val qrMatrix: QrMatrix? = null,
    val isOptionsExpanded: Boolean = false
)

data class ScanUiState(
    val isCameraActive: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val useFrontCamera: Boolean = false,
    val isDecodingImage: Boolean = false,
    val activeResult: ScannedResult? = null
)

class QrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QrRepository by lazy {
        val db = QrDatabase.getInstance(application)
        val prefs = QrPreferencesDataStore(application)
        QrRepository(db.qrHistoryDao(), prefs)
    }

    val preferences: StateFlow<QrPreferences> = repository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QrPreferences()
    )

    val historyList: StateFlow<List<QrHistoryEntity>> = repository.historyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _generateState = MutableStateFlow(GenerateUiState())
    val generateState: StateFlow<GenerateUiState> = _generateState.asStateFlow()

    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var generateHistoryJob: Job? = null
    private var lastCameraScanContent: String? = null
    private var lastCameraScanTimeMs: Long = 0L

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _generateState.update { state ->
                    if (state.inputText.isBlank()) {
                        state.copy(errorCorrection = prefs.defaultErrorCorrection)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun updateGenerateInput(text: String, recordDebounced: Boolean = true) {
        val ec = _generateState.value.errorCorrection
        val matrix = QrCodeEngine.encodeMatrix(text, ec)
        _generateState.update {
            it.copy(inputText = text, qrMatrix = matrix)
        }
        generateHistoryJob?.cancel()
        if (recordDebounced && text.isNotBlank()) {
            generateHistoryJob = viewModelScope.launch {
                delay(1400L)
                repository.recordHistoryIfActive(
                    content = text,
                    type = QrHistoryType.GENERATED,
                    errorCorrection = _generateState.value.errorCorrection
                )
            }
        }
    }

    fun updateErrorCorrection(level: QrErrorCorrection) {
        val currentText = _generateState.value.inputText
        val matrix = QrCodeEngine.encodeMatrix(currentText, level)
        _generateState.update {
            it.copy(errorCorrection = level, qrMatrix = matrix)
        }
    }

    fun toggleGenerateOptions() {
        _generateState.update { it.copy(isOptionsExpanded = !it.isOptionsExpanded) }
    }

    fun applyGeneratedTemplate(content: String) {
        updateGenerateInput(content, recordDebounced = true)
    }

    fun saveCurrentQrToGallery() {
        val state = _generateState.value
        if (state.inputText.isBlank()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.Default) {
                val bitmap = QrCodeEngine.renderExportBitmap(state.inputText, state.errorCorrection)
                    ?: return@withContext Result.failure<String>(IllegalStateException("二维码生成失败"))
                try {
                    QrMediaHelper.saveQrBitmapToGallery(context, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            result.onSuccess {
                repository.recordHistoryIfActive(
                    content = state.inputText,
                    type = QrHistoryType.GENERATED,
                    errorCorrection = state.errorCorrection
                )
                _snackbarMessages.tryEmit("已保存到相册 (Pictures/QRcode)")
            }.onFailure { err ->
                _snackbarMessages.tryEmit("保存失败：${err.message ?: "未知错误"}")
            }
        }
    }

    fun copyCurrentQrImage() {
        val state = _generateState.value
        if (state.inputText.isBlank()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.Default) {
                val bitmap = QrCodeEngine.renderExportBitmap(state.inputText, state.errorCorrection)
                    ?: return@withContext Result.failure<Unit>(IllegalStateException("二维码生成失败"))
                try {
                    QrMediaHelper.copyQrBitmapToClipboard(context, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            result.onSuccess {
                repository.recordHistoryIfActive(
                    content = state.inputText,
                    type = QrHistoryType.GENERATED,
                    errorCorrection = state.errorCorrection
                )
                _snackbarMessages.tryEmit("已复制二维码图片到剪贴板")
            }.onFailure { err ->
                _snackbarMessages.tryEmit("复制失败：${err.message ?: "未知错误"}")
            }
        }
    }

    fun shareCurrentQrImage() {
        val state = _generateState.value
        if (state.inputText.isBlank()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = withContext(Dispatchers.Default) {
                val bitmap = QrCodeEngine.renderExportBitmap(state.inputText, state.errorCorrection)
                    ?: return@withContext Result.failure<Unit>(IllegalStateException("二维码生成失败"))
                try {
                    QrMediaHelper.shareQrBitmap(context, bitmap, state.inputText)
                } finally {
                    bitmap.recycle()
                }
            }
            result.onSuccess {
                repository.recordHistoryIfActive(
                    content = state.inputText,
                    type = QrHistoryType.GENERATED,
                    errorCorrection = state.errorCorrection
                )
            }.onFailure {
                _snackbarMessages.tryEmit("无法打开系统分享")
            }
        }
    }

    fun pasteClipboardIntoGenerate() {
        val text = QrMediaHelper.readClipboardText(getApplication())
        if (text.isNullOrBlank()) {
            _snackbarMessages.tryEmit("剪贴板中没有可用文本")
        } else {
            updateGenerateInput(text, recordDebounced = true)
            _snackbarMessages.tryEmit("已粘贴剪贴板内容")
        }
    }

    fun openCameraScanner() {
        _scanState.update { it.copy(isCameraActive = true, isTorchEnabled = false) }
    }

    fun closeCameraScanner() {
        _scanState.update { it.copy(isCameraActive = false, isTorchEnabled = false) }
    }

    fun toggleTorch() {
        _scanState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
    }

    fun toggleCameraLens() {
        _scanState.update {
            it.copy(
                useFrontCamera = !it.useFrontCamera,
                isTorchEnabled = false
            )
        }
    }

    fun onCameraQrDetected(content: String) {
        val now = System.currentTimeMillis()
        if (_scanState.value.activeResult != null) return
        if (content == lastCameraScanContent && now - lastCameraScanTimeMs < 2000L) return

        lastCameraScanContent = content
        lastCameraScanTimeMs = now
        handleDecodedQr(content, sourceLabel = "摄像头扫描")
    }

    fun decodeQrFromImageUri(uri: Uri, sourceLabel: String = "相册图片") {
        viewModelScope.launch {
            _scanState.update { it.copy(isDecodingImage = true) }
            val decoded = withContext(Dispatchers.Default) {
                val bitmap: Bitmap = QrMediaHelper.loadBitmapFromUri(getApplication(), uri)
                    ?: return@withContext null
                try {
                    QrCodeEngine.decodeBitmap(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            _scanState.update { it.copy(isDecodingImage = false) }
            if (decoded.isNullOrBlank()) {
                _snackbarMessages.tryEmit("未在该图片中识别到二维码")
            } else {
                handleDecodedQr(decoded, sourceLabel = sourceLabel)
            }
        }
    }

    fun scanFromClipboard() {
        val context = getApplication<Application>()
        val imageUri = QrMediaHelper.readClipboardImageUri(context)
        if (imageUri != null) {
            decodeQrFromImageUri(imageUri, sourceLabel = "剪贴板图片")
            return
        }
        val text = QrMediaHelper.readClipboardText(context)
        if (!text.isNullOrBlank()) {
            handleDecodedQr(text, sourceLabel = "剪贴板内容")
        } else {
            _snackbarMessages.tryEmit("剪贴板中没有图片或文本")
        }
    }

    private fun handleDecodedQr(content: String, sourceLabel: String) {
        triggerScanFeedback()
        _scanState.update {
            it.copy(
                activeResult = ScannedResult(content = content, sourceLabel = sourceLabel)
            )
        }
        viewModelScope.launch {
            repository.recordHistoryIfActive(
                content = content,
                type = QrHistoryType.SCANNED
            )
        }
    }

    fun dismissScanResult() {
        _scanState.update { it.copy(activeResult = null) }
    }

    private fun triggerScanFeedback() {
        val prefs = preferences.value
        if (prefs.playSoundOnScan) {
            runCatching {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 160)
                viewModelScope.launch {
                    delay(250)
                    runCatching { toneGen.release() }
                }
            }
        }
        if (prefs.vibrateOnScan) {
            runCatching {
                val context = getApplication<Application>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(VibratorManager::class.java)
                    manager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Vibrator::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(60L)
                    }
                }
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackbarMessages.tryEmit("历史记录已清空")
        }
    }

    fun togglePauseHistory() {
        val next = !preferences.value.pauseHistoryRecording
        setPauseHistoryRecording(next)
    }

    fun setPauseHistoryRecording(paused: Boolean) {
        viewModelScope.launch {
            repository.setPauseHistoryRecording(paused)
            _snackbarMessages.tryEmit(if (paused) "已暂停记录历史" else "已恢复记录历史")
        }
    }

    fun setDarkModeInvertedQr(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkModeInvertedQr(enabled) }
    }

    fun setPlaySoundOnScan(enabled: Boolean) {
        viewModelScope.launch { repository.setPlaySoundOnScan(enabled) }
    }

    fun setVibrateOnScan(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrateOnScan(enabled) }
    }

    fun setDefaultErrorCorrection(level: QrErrorCorrection) {
        viewModelScope.launch {
            repository.setDefaultErrorCorrection(level)
            updateErrorCorrection(level)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun showMessage(msg: String) {
        _snackbarMessages.tryEmit(msg)
    }
}
