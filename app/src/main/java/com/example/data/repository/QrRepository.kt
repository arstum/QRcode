package com.example.data.repository

import com.example.data.local.QrHistoryDao
import com.example.data.local.QrHistoryEntity
import com.example.data.local.QrPreferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.QrErrorCorrection
import com.example.data.model.QrHistoryType
import com.example.data.model.QrPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class QrRepository(
    private val historyDao: QrHistoryDao,
    private val preferencesDataStore: QrPreferencesDataStore
) {
    val historyFlow: Flow<List<QrHistoryEntity>> = historyDao.observeAll()
    val preferencesFlow: Flow<QrPreferences> = preferencesDataStore.preferencesFlow

    suspend fun recordHistoryIfActive(
        content: String,
        type: QrHistoryType,
        errorCorrection: QrErrorCorrection = QrErrorCorrection.M
    ) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        val prefs = preferencesDataStore.preferencesFlow.first()
        if (prefs.pauseHistoryRecording) return

        val latest = historyDao.getLatest()
        if (latest != null && latest.content == trimmed && latest.type == type) {
            return
        }

        historyDao.insert(
            QrHistoryEntity(
                content = trimmed,
                type = type,
                errorCorrection = errorCorrection.code,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun setDarkModeInvertedQr(enabled: Boolean) {
        preferencesDataStore.setDarkModeInvertedQr(enabled)
    }

    suspend fun setPlaySoundOnScan(enabled: Boolean) {
        preferencesDataStore.setPlaySoundOnScan(enabled)
    }

    suspend fun setVibrateOnScan(enabled: Boolean) {
        preferencesDataStore.setVibrateOnScan(enabled)
    }

    suspend fun setPauseHistoryRecording(paused: Boolean) {
        preferencesDataStore.setPauseHistoryRecording(paused)
    }

    suspend fun setDefaultErrorCorrection(level: QrErrorCorrection) {
        preferencesDataStore.setDefaultErrorCorrection(level)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        preferencesDataStore.setThemeMode(mode)
    }
}
