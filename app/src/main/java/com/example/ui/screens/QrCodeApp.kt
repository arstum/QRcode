package com.example.ui.screens

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.ui.theme.QRcodeTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.QrViewModel
import kotlinx.coroutines.launch

private data class NavTabMeta(
    val destination: AppDestination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val appNavTabs = listOf(
    NavTabMeta(
        destination = AppDestination.SCAN,
        selectedIcon = Icons.Filled.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_tab_scan"
    ),
    NavTabMeta(
        destination = AppDestination.GENERATE,
        selectedIcon = Icons.Filled.QrCode2,
        unselectedIcon = Icons.Outlined.QrCode2,
        testTag = "nav_tab_generate"
    ),
    NavTabMeta(
        destination = AppDestination.HISTORY,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    ),
    NavTabMeta(
        destination = AppDestination.SETTINGS,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeApp(viewModel: QrViewModel) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val generateState by viewModel.generateState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val localizedConfig = remember(baseConfig, preferences.appLanguage) {
        Configuration(baseConfig).apply {
            val locales = if (preferences.appLanguage == AppLanguage.SYSTEM) {
                LocaleList.getDefault()
            } else {
                LocaleList.forLanguageTags(preferences.appLanguage.tag)
            }
            setLocales(locales)
        }
    }
    val localizedContext = remember(baseContext, baseConfig, preferences.appLanguage) {
        val resourcesContext = baseContext.createConfigurationContext(localizedConfig)
        object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = resourcesContext.resources
        }
    }

    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (preferences.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig
    ) {
        QRcodeTheme(darkTheme = useDarkTheme) {
            val tabCount = appNavTabs.size
            val initialPage = remember { (Int.MAX_VALUE / 2).let { it - (it % tabCount) } }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { Int.MAX_VALUE }
            )
            val coroutineScope = rememberCoroutineScope()
            val currentTabIndex = ((pagerState.currentPage % tabCount) + tabCount) % tabCount
            val currentTab = appNavTabs[currentTabIndex].destination
            val currentRoute = currentTab.route

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(localizedContext) {
                viewModel.snackbarMessages.collect { uiText ->
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(uiText.asString(localizedContext))
                }
            }

            LaunchedEffect(currentTab, scanState.isCameraActive) {
                if (currentTab != AppDestination.SCAN && scanState.isCameraActive) {
                    viewModel.closeCameraScanner()
                }
            }

            val navigateToTab: (AppDestination) -> Unit = { dest ->
                if (dest != AppDestination.SCAN && scanState.isCameraActive) {
                    viewModel.closeCameraScanner()
                }
                val targetIndex = appNavTabs.indexOfFirst { it.destination == dest }
                    .coerceAtLeast(0)
                if (targetIndex != currentTabIndex) {
                    val delta = targetIndex - currentTabIndex
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + delta)
                    }
                }
            }

            val hideChromeForFullScreenCamera =
                currentTab == AppDestination.SCAN && scanState.isCameraActive

            BackHandler(enabled = currentTab != AppDestination.SCAN && !hideChromeForFullScreenCamera) {
                navigateToTab(AppDestination.SCAN)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (!hideChromeForFullScreenCamera) {
                            val appTitle = stringResource(R.string.app_name)
                            val tabLabel = stringResource(currentTab.labelRes)
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "$appTitle · $tabLabel",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (!useNavigationRail && !hideChromeForFullScreenCamera) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp
                            ) {
                                appNavTabs.forEach { tab ->
                                    val selected = currentRoute == tab.destination.route
                                    val label = stringResource(tab.destination.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.destination) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                maxLines = 1,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail && !hideChromeForFullScreenCamera) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                appNavTabs.forEach { tab ->
                                    val selected = currentRoute == tab.destination.route
                                    val label = stringResource(tab.destination.labelRes)
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.destination) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label, maxLines = 1) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !hideChromeForFullScreenCamera,
                            key = { it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag("main_menu_pager")
                        ) { page ->
                            val pageTabIndex = ((page % tabCount) + tabCount) % tabCount
                            when (appNavTabs[pageTabIndex].destination) {
                                AppDestination.GENERATE -> {
                                    GenerateScreen(
                                        state = generateState,
                                        preferences = preferences,
                                        onInputChange = { viewModel.updateGenerateInput(it) },
                                        onErrorCorrectionChange = { viewModel.updateErrorCorrection(it) },
                                        onToggleOptions = { viewModel.toggleGenerateOptions() },
                                        onPasteClipboard = { viewModel.pasteClipboardIntoGenerate() },
                                        onSaveClick = { viewModel.saveCurrentQrToGallery() },
                                        onCopyImageClick = { viewModel.copyCurrentQrImage() },
                                        onShareClick = { viewModel.shareCurrentQrImage(localizedContext) }
                                    )
                                }

                                AppDestination.SCAN -> {
                                    ScanScreen(
                                        state = scanState,
                                        onOpenCamera = { viewModel.openCameraScanner() },
                                        onCloseCamera = { viewModel.closeCameraScanner() },
                                        onToggleTorch = { viewModel.toggleTorch() },
                                        onToggleLens = { viewModel.toggleCameraLens() },
                                        onCameraQrDetected = { viewModel.onCameraQrDetected(it) },
                                        onPickImageUri = { viewModel.decodeQrFromImageUri(it) },
                                        onScanClipboard = { viewModel.scanFromClipboard() },
                                        onDismissResult = { viewModel.dismissScanResult() },
                                        onUseAsGenerate = { content ->
                                            viewModel.applyGeneratedTemplate(content)
                                            navigateToTab(AppDestination.GENERATE)
                                        },
                                        onShowMessage = { resId -> viewModel.showMessage(resId) }
                                    )
                                }

                                AppDestination.HISTORY -> {
                                    HistoryScreen(
                                        historyList = historyList,
                                        isHistoryPaused = preferences.pauseHistoryRecording,
                                        onItemClick = { item ->
                                            viewModel.applyGeneratedTemplate(item.content)
                                            navigateToTab(AppDestination.GENERATE)
                                        },
                                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                                        onClearAll = { viewModel.clearAllHistory() },
                                        onTogglePause = { viewModel.togglePauseHistory() },
                                        onShowMessage = { resId -> viewModel.showMessage(resId) }
                                    )
                                }

                                AppDestination.SETTINGS -> {
                                    SettingsScreen(
                                        preferences = preferences,
                                        onDarkModeInvertedChange = { viewModel.setDarkModeInvertedQr(it) },
                                        onPlaySoundChange = { viewModel.setPlaySoundOnScan(it) },
                                        onVibrateChange = { viewModel.setVibrateOnScan(it) },
                                        onPauseHistoryChange = { viewModel.setPauseHistoryRecording(it) },
                                        onDefaultErrorCorrectionChange = {
                                            viewModel.setDefaultErrorCorrection(it)
                                        },
                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                        onLanguageChange = { viewModel.setAppLanguage(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
