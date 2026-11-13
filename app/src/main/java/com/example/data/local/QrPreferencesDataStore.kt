package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.QrErrorCorrection
import com.example.data.model.QrPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.qrDataStore: DataStore<Preferences> by preferencesDataStore(name = "qrcode_preferences")

class QrPreferencesDataStore(private val context: Context) {

    private object Keys {
        val DARK_MODE_INVERTED_QR = booleanPreferencesKey("dark_mode_inverted_qr")
        val PLAY_SOUND_ON_SCAN = booleanPreferencesKey("play_sound_on_scan")
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
        val PAUSE_HISTORY = booleanPreferencesKey("pause_history_recording")
        val DEFAULT_EC = stringPreferencesKey("default_error_correction")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val preferencesFlow: Flow<QrPreferences> = context.qrDataStore.data.map { prefs ->
        QrPreferences(
            darkModeInvertedQr = prefs[Keys.DARK_MODE_INVERTED_QR] ?: false,
            playSoundOnScan = prefs[Keys.PLAY_SOUND_ON_SCAN] ?: true,
            vibrateOnScan = prefs[Keys.VIBRATE_ON_SCAN] ?: true,
            pauseHistoryRecording = prefs[Keys.PAUSE_HISTORY] ?: false,
            defaultErrorCorrection = QrErrorCorrection.fromCode(prefs[Keys.DEFAULT_EC] ?: "M"),
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                AppThemeMode.entries.firstOrNull { it.name == name }
            } ?: AppThemeMode.SYSTEM,
            appLanguage = AppLanguage.fromTag(prefs[Keys.APP_LANGUAGE] ?: "")
        )
    }

    suspend fun setDarkModeInvertedQr(enabled: Boolean) {
        context.qrDataStore.edit { it[Keys.DARK_MODE_INVERTED_QR] = enabled }
    }

    suspend fun setPlaySoundOnScan(enabled: Boolean) {
        context.qrDataStore.edit { it[Keys.PLAY_SOUND_ON_SCAN] = enabled }
    }

    suspend fun setVibrateOnScan(enabled: Boolean) {
        context.qrDataStore.edit { it[Keys.VIBRATE_ON_SCAN] = enabled }
    }

    suspend fun setPauseHistoryRecording(paused: Boolean) {
        context.qrDataStore.edit { it[Keys.PAUSE_HISTORY] = paused }
    }

    suspend fun setDefaultErrorCorrection(level: QrErrorCorrection) {
        context.qrDataStore.edit { it[Keys.DEFAULT_EC] = level.code }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.qrDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.qrDataStore.edit { it[Keys.APP_LANGUAGE] = language.tag }
    }
}
