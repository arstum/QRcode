package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.DecodedContentKind
import com.example.data.model.ScannedResult
import com.example.qr.QrCodeEngine
import com.example.qr.QrMediaHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.ScanUiState
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onOpenCamera: () -> Unit,
    onCloseCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLens: () -> Unit,
    onCameraQrDetected: (String) -> Unit,
    onPickImageUri: (android.net.Uri) -> Unit,
    onScanClipboard: () -> Unit,
    onDismissResult: () -> Unit,
    onUseAsGenerate: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cameraPermissionDenied by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermissionDenied = false
            onOpenCamera()
        } else {
            cameraPermissionDenied = true
            onShowMessage("需要相机权限才能使用实时扫码")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickImageUri(uri)
        }
    }

    val launchCameraWithPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            cameraPermissionDenied = false
            onOpenCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchGalleryPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (state.isCameraActive) {
        BackHandler {
            onCloseCamera()
        }
        LiveCameraScannerView(
            isTorchEnabled = state.isTorchEnabled,
            useFrontCamera = state.useFrontCamera,
            onClose = onCloseCamera,
            onToggleTorch = onToggleTorch,
            onToggleLens = onToggleLens,
            onPickFromGallery = launchGalleryPicker,
            onQrDetected = onCameraQrDetected,
            onCameraError = { msg ->
                onShowMessage(msg)
                onCloseCamera()
            },
            modifier = modifier
        )
    } else {
        ScanHubView(
            isDecodingImage = state.isDecodingImage,
            cameraPermissionDenied = cameraPermissionDenied,
            onCameraScanClick = launchCameraWithPermission,
            onGalleryScanClick = launchGalleryPicker,
            onClipboardScanClick = onScanClipboard,
            modifier = modifier
        )
    }

    val result = state.activeResult
    if (result != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissResult,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ScannedResultSheetContent(
                result = result,
                onCopyText = {
                    QrMediaHelper.copyTextToClipboard(context, "QR Result", result.content)
                    onShowMessage("已复制识别结果")
                },
                onOpenUrl = {
                    QrMediaHelper.openExternalUrl(context, result.content).onFailure {
                        onShowMessage("无法打开该链接")
                    }
                },
                onShareText = {
                    QrMediaHelper.shareText(context, result.content).onFailure {
                        onShowMessage("无法打开系统分享")
                    }
                },
                onGenerateQr = {
                    onDismissResult()
                    onCloseCamera()
                    onUseAsGenerate(result.content)
                },
                onClose = onDismissResult
            )
        }
    }
}

@Composable
private fun ScanHubView(
    isDecodingImage: Boolean,
    cameraPermissionDenied: Boolean,
    onCameraScanClick: () -> Unit,
    onGalleryScanClick: () -> Unit,
    onClipboardScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "选择扫描方式",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "所有识别均在本地离线完成，不上传任何图片或内容",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Primary action: Camera Scan (mirrors screenshot 1)
            Button(
                onClick = onCameraScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("scan_camera_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.scan_camera_btn),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (cameraPermissionDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "相机权限被拒绝。您可以点击上方按钮重新授权，或使用下方的相册图片扫描。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            OrDivider()

            // Gallery Image Scan (Zero-permission Photo Picker)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = !isDecodingImage, onClick = onGalleryScanClick)
                    .testTag("scan_gallery_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isDecodingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "正在解析图片中的二维码…",
                            style = MaterialTheme.typography.titleSmall
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.scan_gallery_btn),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OrDivider()

            // Clipboard Scan
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClipboardScanClick)
                    .testTag("scan_clipboard_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.scan_clipboard_btn),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = stringResource(R.string.scan_or_divider),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun LiveCameraScannerView(
    isTorchEnabled: Boolean,
    useFrontCamera: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleLens: () -> Unit,
    onPickFromGallery: () -> Unit,
    onQrDetected: (String) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(boundCamera, isTorchEnabled) {
        boundCamera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(isTorchEnabled)
            }
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                            analyzeImageProxy(imageProxy, onQrDetected)
                        }
                    }

                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            }.onFailure {
                onCameraError("无法启动相机预览，请改用相册图片扫描")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
            boundCamera = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzerExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ScannerReticleOverlay(
            frameColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize()
        )

        // Top floating toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭相机",
                        tint = Color.White
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!useFrontCamera) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = onToggleTorch,
                            modifier = Modifier.testTag("camera_torch_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) {
                                    Icons.Outlined.FlashlightOn
                                } else {
                                    Icons.Outlined.FlashlightOff
                                },
                                contentDescription = if (isTorchEnabled) "关闭闪光灯" else "打开闪光灯",
                                tint = if (isTorchEnabled) Color(0xFF9DF0B8) else Color.White
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(
                        onClick = onToggleLens,
                        modifier = Modifier.testTag("camera_flip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FlipCameraAndroid,
                            contentDescription = "切换摄像头",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom prompt & quick gallery switch
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "将二维码置于框内即可自动识别",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("camera_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("从相册选择图片")
            }
        }
    }
}

private fun analyzeImageProxy(
    imageProxy: ImageProxy,
    onQrDetected: (String) -> Unit
) {
    try {
        val plane = imageProxy.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height
        val yBytes = ByteArray(width * height)

        if (rowStride == width) {
            buffer.get(yBytes, 0, yBytes.size)
        } else {
            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                buffer.position(y * rowStride)
                buffer.get(row, 0, minOf(rowStride, buffer.remaining()))
                System.arraycopy(row, 0, yBytes, y * width, width)
            }
        }

        val decoded = QrCodeEngine.decodeYuvFrame(yBytes, width, height)
        if (!decoded.isNullOrBlank()) {
            onQrDetected(decoded)
        }
    } finally {
        imageProxy.close()
    }
}

@Composable
private fun ScannerReticleOverlay(
    frameColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "scan_line")
    val lineProgress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_y"
    )

    Canvas(modifier = modifier) {
        val boxSize = minOf(size.width, size.height) * 0.66f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val cornerRadius = 20.dp.toPx()

        drawRect(color = Color.Black.copy(alpha = 0.48f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        val strokeWidth = 4.dp.toPx()
        val arm = 28.dp.toPx()
        val bracketColor = Color(0xFF83D5A1)

        // Four crisp corner brackets
        // Top-Left
        drawLine(bracketColor, Offset(left, top + arm), Offset(left, top), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, top), Offset(left + arm, top), strokeWidth, StrokeCap.Round)
        // Top-Right
        drawLine(bracketColor, Offset(left + boxSize - arm, top), Offset(left + boxSize, top), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + boxSize, top), Offset(left + boxSize, top + arm), strokeWidth, StrokeCap.Round)
        // Bottom-Left
        drawLine(bracketColor, Offset(left, top + boxSize - arm), Offset(left, top + boxSize), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left, top + boxSize), Offset(left + arm, top + boxSize), strokeWidth, StrokeCap.Round)
        // Bottom-Right
        drawLine(bracketColor, Offset(left + boxSize - arm, top + boxSize), Offset(left + boxSize, top + boxSize), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + boxSize, top + boxSize - arm), Offset(left + boxSize, top + boxSize), strokeWidth, StrokeCap.Round)

        // Subtle animated scan beam
        val lineY = top + boxSize * lineProgress
        drawLine(
            color = frameColor.copy(alpha = 0.85f),
            start = Offset(left + 16.dp.toPx(), lineY),
            end = Offset(left + boxSize - 16.dp.toPx(), lineY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(left, top),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun ScannedResultSheetContent(
    result: ScannedResult,
    onCopyText: () -> Unit,
    onOpenUrl: () -> Unit,
    onShareText: () -> Unit,
    onGenerateQr: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${result.sourceLabel} · ${result.kind.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("scan_result_close")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭识别结果"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = result.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFamily
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(14.dp)
                    .testTag("scan_result_content")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (result.kind == DecodedContentKind.URL) {
                Button(
                    onClick = onOpenUrl,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("scan_result_open_url")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打开链接")
                }
            }

            FilledTonalButton(
                onClick = onCopyText,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("scan_result_copy")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("复制文本")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onGenerateQr,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("scan_result_to_generate")
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("转为二维码")
            }

            OutlinedButton(
                onClick = onShareText,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("scan_result_share")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享内容")
            }
        }
    }
}
