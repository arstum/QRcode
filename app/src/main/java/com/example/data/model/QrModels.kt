package com.example.data.model

enum class QrHistoryType {
    GENERATED,
    SCANNED
}

enum class QrErrorCorrection(
    val code: String,
    val label: String,
    val percent: String
) {
    L("L", "低 (L)", "7%"),
    M("M", "中 (M)", "15%"),
    Q("Q", "较高 (Q)", "25%"),
    H("H", "高 (H)", "30%");

    companion object {
        fun fromCode(code: String): QrErrorCorrection =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: M
    }
}

enum class AppThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

data class QrPreferences(
    val darkModeInvertedQr: Boolean = false,
    val playSoundOnScan: Boolean = true,
    val vibrateOnScan: Boolean = true,
    val pauseHistoryRecording: Boolean = false,
    val defaultErrorCorrection: QrErrorCorrection = QrErrorCorrection.M,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

enum class DecodedContentKind(val label: String) {
    URL("网址链接"),
    WIFI("Wi-Fi 配置"),
    EMAIL("电子邮箱"),
    PHONE("电话号码"),
    TEXT("纯文本")
}

data class ScannedResult(
    val content: String,
    val sourceLabel: String,
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
