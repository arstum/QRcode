package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.QrDatabase
import com.example.data.local.QrHistoryEntity
import com.example.data.local.QrPreferencesDataStore
import com.example.data.model.AppLanguage
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> if (args.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *args.toTypedArray())
        }
    }
}

enum class AppDestination(val route: String, @param:StringRes val labelRes: Int) {
    SCAN("scan", R.string.tab_scan),
    GENERATE("generate", R.string.tab_generate),
    HISTORY("history", R.string.tab_history),
    SETTINGS("settings", R.string.tab_settings)
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

    private val _snackbarMessages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val snackbarMessages: SharedFlow<UiText> = _snackbarMessages.asSharedFlow()

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
                    ?: return@withContext Result.failure<String>(IllegalStateException("qr_gen_failed"))
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
                _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_saved_gallery))
            }.onFailure { err ->
                if (err.message == "qr_gen_failed") {
                    _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_qr_gen_failed))
                } else {
                    _snackbarMessages.tryEmit(
                        UiText.StringResource(
                            R.string.msg_save_failed,
                            listOf(err.localizedMessage ?: context.getString(R.string.error_unknown))
                        )
                    )
                }
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
                    ?: return@withContext Result.failure<Unit>(IllegalStateException("qr_gen_failed"))
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
                _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_copied_image))
            }.onFailure { err ->
                if (err.message == "qr_gen_failed") {
                    _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_qr_gen_failed))
                } else {
                    _snackbarMessages.tryEmit(
                        UiText.StringResource(
                            R.string.msg_copy_failed,
                            listOf(err.localizedMessage ?: context.getString(R.string.error_unknown))
                        )
                    )
                }
            }
        }
    }

    fun shareCurrentQrImage(hostContext: Context = getApplication()) {
        val state = _generateState.value
        if (state.inputText.isBlank()) return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                QrCodeEngine.renderExportBitmap(state.inputText, state.errorCorrection)
            }
            if (bitmap == null) {
                _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_qr_gen_failed))
                return@launch
            }
            val result = try {
                QrMediaHelper.shareQrBitmap(
                    context = hostContext,
                    bitmap = bitmap,
                    content = state.inputText,
                    chooserTitle = hostContext.getString(R.string.chooser_share_qr)
                )
            } finally {
                bitmap.recycle()
            }
            result.onSuccess {
                repository.recordHistoryIfActive(
                    content = state.inputText,
                    type = QrHistoryType.GENERATED,
                    errorCorrection = state.errorCorrection
                )
            }.onFailure {
                _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_share_failed))
            }
        }
    }

    fun pasteClipboardIntoGenerate() {
        val text = QrMediaHelper.readClipboardText(getApplication())
        if (text.isNullOrBlank()) {
            _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_clipboard_no_text))
        } else {
            updateGenerateInput(text, recordDebounced = true)
            _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_clipboard_pasted))
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
        handleDecodedQr(content, sourceLabelRes = R.string.scan_source_camera)
    }

    fun decodeQrFromImageUri(
        uri: Uri,
        @StringRes sourceLabelRes: Int = R.string.scan_source_gallery
    ) {
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
                _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_no_qr_in_image))
            } else {
                handleDecodedQr(decoded, sourceLabelRes = sourceLabelRes)
            }
        }
    }

    fun scanFromClipboard() {
        val context = getApplication<Application>()
        val imageUri = QrMediaHelper.readClipboardImageUri(context)
        if (imageUri != null) {
            decodeQrFromImageUri(imageUri, sourceLabelRes = R.string.scan_source_clipboard_image)
            return
        }
        val text = QrMediaHelper.readClipboardText(context)
        if (!text.isNullOrBlank()) {
            handleDecodedQr(text, sourceLabelRes = R.string.scan_source_clipboard_text)
        } else {
            _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_clipboard_empty))
        }
    }

    private fun handleDecodedQr(content: String, @StringRes sourceLabelRes: Int) {
        triggerScanFeedback()
        _scanState.update {
            it.copy(
                activeResult = ScannedResult(content = content, sourceLabelRes = sourceLabelRes)
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
            _snackbarMessages.tryEmit(UiText.StringResource(R.string.msg_history_cleared))
        }
    }

    fun togglePauseHistory() {
        val next = !preferences.value.pauseHistoryRecording
        setPauseHistoryRecording(next)
    }

    fun setPauseHistoryRecording(paused: Boolean) {
        viewModelScope.launch {
            repository.setPauseHistoryRecording(paused)
            _snackbarMessages.tryEmit(
                UiText.StringResource(
                    if (paused) R.string.msg_history_paused else R.string.msg_history_resumed
                )
            )
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

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setAppLanguage(language) }
    }

    fun showMessage(@StringRes resId: Int) {
        _snackbarMessages.tryEmit(UiText.StringResource(resId))
    }
}
