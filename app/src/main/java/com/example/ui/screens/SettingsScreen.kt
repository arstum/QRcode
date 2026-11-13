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
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AppLanguage
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
    onLanguageChange: (AppLanguage) -> Unit,
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
            // Section 1: Interface Language
            SettingsSectionCard(
                title = stringResource(R.string.settings_language_title),
                icon = Icons.Outlined.Language
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = preferences.appLanguage == lang,
                            onClick = { onLanguageChange(lang) },
                            label = { Text(stringResource(lang.labelRes)) },
                            modifier = Modifier.testTag("settings_lang_${lang.name.lowercase()}")
                        )
                    }
                }
            }

            // Section 2: QR Generation
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_generate),
                icon = Icons.Outlined.QrCode2
            ) {
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_dark_inverted_title),
                    description = stringResource(R.string.settings_dark_inverted_desc),
                    checked = preferences.darkModeInvertedQr,
                    onCheckedChange = onDarkModeInvertedChange,
                    testTag = "settings_dark_inverted_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = stringResource(R.string.settings_default_ec_title),
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
                            label = {
                                Text("${stringResource(level.labelRes)} · ${level.percent}")
                            },
                            modifier = Modifier.testTag("settings_ec_${level.code.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = stringResource(R.string.settings_theme_title),
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
                            Text(
                                text = stringResource(mode.labelRes),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Section 3: QR Scanning
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_scan),
                icon = Icons.Outlined.QrCodeScanner
            ) {
                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_sound_title),
                    description = stringResource(R.string.settings_sound_desc),
                    checked = preferences.playSoundOnScan,
                    onCheckedChange = onPlaySoundChange,
                    testTag = "settings_sound_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_vibrate_title),
                    description = stringResource(R.string.settings_vibrate_desc),
                    checked = preferences.vibrateOnScan,
                    onCheckedChange = onVibrateChange,
                    testTag = "settings_vibrate_checkbox"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SettingsCheckboxRow(
                    title = stringResource(R.string.settings_pause_history_title),
                    description = stringResource(R.string.settings_pause_history_desc),
                    checked = preferences.pauseHistoryRecording,
                    onCheckedChange = onPauseHistoryChange,
                    testTag = "settings_pause_history_checkbox"
                )
            }

            // Section 4: App Info
            SettingsSectionCard(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Outlined.Info
            ) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_version_line, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.about_arch_line),
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
    icon: ImageVector,
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
