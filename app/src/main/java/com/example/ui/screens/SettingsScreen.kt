package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppThemeMode
import com.example.data.model.QrErrorCorrection
import com.example.data.model.QrPreferences

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: QrPreferences,
    onDarkModeInvertedChange: (Boolean) -> Unit,
    onPlaySoundChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onPauseHistoryChange: (Boolean) -> Unit,
    onDefaultErrorCorrectionChange: (QrErrorCorrection) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: QR Generation (mirrors screenshot 3)
            SettingsSectionCard(
                title = "二维码生成",
                icon = Icons.Outlined.QrCode2
            ) {
                SettingsCheckboxRow(
                    title = "在深色模式下生成黑色背景上的白色二维码",
                    description = "在深色模式下，界面预览将显示深色背景上的浅色前景色二维码。某些扫描仪可能难以扫描此类反色二维码；您通过“保存”“复制图片”和“分享”导出的二维码将始终保持白色背景和黑色前景色。",
                    checked = preferences.darkModeInvertedQr,
                    onCheckedChange = onDarkModeInvertedChange,
                    testTag = "settings_dark_inverted_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "默认容错纠错等级",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QrErrorCorrection.entries.forEach { level ->
                        FilterChip(
                            selected = preferences.defaultErrorCorrection == level,
                            onClick = { onDefaultErrorCorrectionChange(level) },
                            label = { Text("${level.label} · ${level.percent}") },
                            modifier = Modifier.testTag("settings_ec_${level.code.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "应用外观主题",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AppThemeMode.entries.size
                            ),
                            selected = preferences.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label)
                        }
                    }
                }
            }

            // Section 2: QR Scanning (mirrors screenshot 3)
            SettingsSectionCard(
                title = "二维码扫描",
                icon = Icons.Outlined.QrCodeScanner
            ) {
                SettingsCheckboxRow(
                    title = "扫描成功时播放声音",
                    description = "成功扫描到二维码时播放清晰的提示音。",
                    checked = preferences.playSoundOnScan,
                    onCheckedChange = onPlaySoundChange,
                    testTag = "settings_sound_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsCheckboxRow(
                    title = "扫描成功时振动提示",
                    description = "成功识别二维码时触发轻触振动反馈。",
                    checked = preferences.vibrateOnScan,
                    onCheckedChange = onVibrateChange,
                    testTag = "settings_vibrate_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsCheckboxRow(
                    title = "暂停历史记录",
                    description = "开启后，新生成或新扫描的二维码将不会记录到本地历史列表。",
                    checked = preferences.pauseHistoryRecording,
                    onCheckedChange = onPauseHistoryChange,
                    testTag = "settings_pause_history_checkbox"
                )
            }

            // Section 3: App Info
            SettingsSectionCard(
                title = "关于 QRcode",
                icon = Icons.Outlined.Info
            ) {
                Text(
                    text = "QRcode · 离线二维码生成器与扫描器",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "版本 v${BuildConfig.VERSION_NAME} · 纯离线本地运行 · 不上传任何用户数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "目标架构：arm64-v8a (64-bit) · 适配 Android 15",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsCheckboxRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
