package com.example.data.model

import androidx.annotation.StringRes
import com.example.R

enum class QrHistoryType {
    GENERATED,
    SCANNED
}

enum class QrErrorCorrection(
    val code: String,
    @param:StringRes val labelRes: Int,
    val percent: String
) {
    L("L", R.string.ec_low, "7%"),
    M("M", R.string.ec_medium, "15%"),
    Q("Q", R.string.ec_quartile, "25%"),
    H("H", R.string.ec_high, "30%");

    companion object {
        fun fromCode(code: String): QrErrorCorrection =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: M
    }
}

enum class AppThemeMode(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark)
}

enum class AppLanguage(val tag: String, @param:StringRes val labelRes: Int) {
    SYSTEM("", R.string.lang_system),
    ZH_CN("zh-CN", R.string.lang_zh_cn),
    ZH_TW("zh-TW", R.string.lang_zh_tw),
    EN("en", R.string.lang_en),
    JA("ja", R.string.lang_ja),
    KO("ko", R.string.lang_ko),
    ES("es", R.string.lang_es);

    companion object {
        fun fromTag(tag: String): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: SYSTEM
    }
}

data class QrPreferences(
    val darkModeInvertedQr: Boolean = false,
    val playSoundOnScan: Boolean = true,
    val vibrateOnScan: Boolean = true,
    val pauseHistoryRecording: Boolean = false,
    val defaultErrorCorrection: QrErrorCorrection = QrErrorCorrection.M,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM
)

enum class DecodedContentKind(@param:StringRes val labelRes: Int) {
    URL(R.string.kind_url),
    WIFI(R.string.kind_wifi),
    EMAIL(R.string.kind_email),
    PHONE(R.string.kind_phone),
    TEXT(R.string.kind_text)
}

data class ScannedResult(
    val content: String,
    @param:StringRes val sourceLabelRes: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val kind: DecodedContentKind
        get() = when {
            content.startsWith("http://", ignoreCase = true) ||
                content.startsWith("https://", ignoreCase = true) -> DecodedContentKind.URL
            content.startsWith("WIFI:", ignoreCase = true) -> DecodedContentKind.WIFI
            content.startsWith("mailto:", ignoreCase = true) -> DecodedContentKind.EMAIL
            content.startsWith("tel:", ignoreCase = true) -> DecodedContentKind.PHONE
            else -> DecodedContentKind.TEXT
        }
}
