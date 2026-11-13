package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppThemeMode
import com.example.data.model.QrErrorCorrection
import com.example.data.model.QrPreferences
import com.example.qr.QrMatrix
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.GenerateUiState
import kotlin.math.ceil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(
    state: GenerateUiState,
    preferences: QrPreferences,
    onInputChange: (String) -> Unit,
    onErrorCorrectionChange: (QrErrorCorrection) -> Unit,
    onToggleOptions: () -> Unit,
    onPasteClipboard: () -> Unit,
    onSaveClick: () -> Unit,
    onCopyImageClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemDark = isSystemInDarkTheme()
    val isAppDark = when (preferences.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }
    val useInvertedColors = isAppDark && preferences.darkModeInvertedQr

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Content input field matching screenshot's clean top box
            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp, max = 168.dp)
                    .testTag("generate_input_field"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.generate_input_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                trailingIcon = {
                    if (state.inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { onInputChange("") },
                            modifier = Modifier.testTag("generate_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "清空输入"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onPasteClipboard,
                            modifier = Modifier.testTag("generate_paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "粘贴剪贴板内容"
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Character length & Options row (mirrors screenshot 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.generate_length_label, state.inputText.length),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .testTag("generate_length_text")
                )

                TextButton(
                    onClick = onToggleOptions,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("generate_options_toggle")
                ) {
                    Text(
                        text = "${stringResource(R.string.generate_options)} (${state.errorCorrection.code})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (state.isOptionsExpanded) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expandable Options panel
            AnimatedVisibility(visible = state.isOptionsExpanded) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "容错纠错等级",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QrErrorCorrection.entries.forEach { level ->
                                FilterChip(
                                    selected = state.errorCorrection == level,
                                    onClick = { onErrorCorrectionChange(level) },
                                    label = { Text("${level.label} · ${level.percent}") },
                                    modifier = Modifier.testTag("ec_chip_${level.code.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "快捷模板",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val templates = listOf(
                                "网址" to "https://",
                                "Wi-Fi" to "WIFI:T:WPA;S:网络名称;P:密码;;",
                                "邮箱" to "mailto:example@domain.com",
                                "电话" to "tel:+8613800138000"
                            )
                            templates.forEach { (title, sample) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onInputChange(sample) }
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QR Code Preview Box
            val qrCardBg = if (useInvertedColors) Color(0xFF0A0A0A) else Color.White
            val qrForeground = if (useInvertedColors) Color.White else Color.Black

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 310.dp)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (state.qrMatrix != null) {
                            qrCardBg
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(16.dp)
                    .testTag("qr_preview_container"),
                contentAlignment = Alignment.Center
            ) {
                if (state.qrMatrix != null) {
                    QrMatrixCanvas(
                        matrix = state.qrMatrix,
                        foreground = qrForeground,
                        background = qrCardBg,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("qr_code_canvas")
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.generate_empty_tip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Action Bar (Save / Copy / Share)
            val hasQr = state.qrMatrix != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onSaveClick,
                    enabled = hasQr,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("generate_save_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.generate_save),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onCopyImageClick,
                    enabled = hasQr,
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 48.dp)
                        .testTag("generate_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.generate_copy))
                }

                OutlinedButton(
                    onClick = onShareClick,
                    enabled = hasQr,
                    modifier = Modifier
                        .weight(0.95f)
                        .heightIn(min = 48.dp)
                        .testTag("generate_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.generate_share))
                }
            }

            if (useInvertedColors && hasQr) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "当前为深色反色预览；保存、复制或分享的图片将始终保持白底黑码以确保最佳识别率。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QrMatrixCanvas(
    matrix: QrMatrix,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(color = background)
        val dim = minOf(size.width, size.height)
        val modulePx = dim / matrix.size.toFloat()
        val offsetX = (size.width - dim) / 2f
        val offsetY = (size.height - dim) / 2f
        val drawSize = ceil(modulePx.toDouble()).toFloat() + 0.5f

        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = foreground,
                        topLeft = Offset(
                            x = offsetX + x * modulePx,
                            y = offsetY + y * modulePx
                        ),
                        size = Size(drawSize, drawSize)
                    )
                }
            }
        }
    }
}
