package com.qtekfun.ultimatefiles.ui.main

import com.qtekfun.ultimatefiles.core.model.FileItem
import com.qtekfun.ultimatefiles.core.model.Favorite
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import android.app.Activity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qtekfun.ultimatefiles.R
import com.qtekfun.ultimatefiles.ui.components.connectionMessage
import com.qtekfun.ultimatefiles.ui.components.ConnectionSnackbarVisuals
import com.qtekfun.ultimatefiles.ui.components.ConnectionAwareSnackbar
import com.qtekfun.ultimatefiles.core.util.RemoteAccounts
import com.qtekfun.ultimatefiles.core.model.PanelId
import com.qtekfun.ultimatefiles.core.model.StorageVolume
import com.qtekfun.ultimatefiles.data.system.IntentFactory
import com.qtekfun.ultimatefiles.ui.browser.BrowserEvent
import com.qtekfun.ultimatefiles.ui.browser.BrowserViewModel
import com.qtekfun.ultimatefiles.ui.analysis.SizeAnalysisScreen
import com.qtekfun.ultimatefiles.ui.analysis.SizeAnalysisState
import com.qtekfun.ultimatefiles.ui.analysis.SizeAnalysisViewModel
import com.qtekfun.ultimatefiles.ui.components.AccountEdit
import com.qtekfun.ultimatefiles.ui.components.AddAccountDialog
import com.qtekfun.ultimatefiles.ui.components.BatteryHintDialog
import com.qtekfun.ultimatefiles.ui.components.InterruptedTransfersDialog
import com.qtekfun.ultimatefiles.ui.components.NameInputDialog
import com.qtekfun.ultimatefiles.ui.components.rememberIgnoringBatteryOptimizations
import com.qtekfun.ultimatefiles.ui.components.requestIgnoreBatteryOptimizations
import com.qtekfun.ultimatefiles.ui.components.ConflictDialog
import com.qtekfun.ultimatefiles.ui.components.DropActionDialog
import com.qtekfun.ultimatefiles.ui.components.TransferProgressBar
import com.qtekfun.ultimatefiles.ui.components.dockBottomInsets
import com.qtekfun.ultimatefiles.ui.components.isTransferBarShown
import androidx.compose.foundation.layout.consumeWindowInsets
import com.qtekfun.ultimatefiles.data.thumbnail.ThumbnailLoader
import com.qtekfun.ultimatefiles.ui.browser.LocalThumbnailLoader
import com.qtekfun.ultimatefiles.ui.dualpanel.DualPanelScaffold
import com.qtekfun.ultimatefiles.ui.dualpanel.PanelEntry
import com.qtekfun.ultimatefiles.ui.history.HistoryScreen
import com.qtekfun.ultimatefiles.ui.history.HistoryViewModel
import com.qtekfun.ultimatefiles.ui.settings.SettingsScreen
import com.qtekfun.ultimatefiles.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import com.qtekfun.ultimatefiles.data.system.IncomingFiles
import com.qtekfun.ultimatefiles.domain.usecase.ArchivePaths
import org.koin.mp.KoinPlatform

private enum class AppScreen { BROWSER, HISTORY, SETTINGS, ANALYSIS }

/** Root screen: navigation drawer + dual panel, plus the dialogs and bar shared by both panels. */
@Composable
fun MainScreen() {
    val koin = remember { KoinPlatform.getKoin() }
    val viewModel = viewModel<MainViewModel>(factory = viewModelFactory { initializer { koin.get<MainViewModel>() } })
    val settingsViewModel = viewModel<SettingsViewModel>(factory = viewModelFactory { initializer { koin.get<SettingsViewModel>() } })
    val historyViewModel = viewModel<HistoryViewModel>(factory = viewModelFactory { initializer { koin.get<HistoryViewModel>() } })
    var screen by rememberSaveable { mutableStateOf(AppScreen.BROWSER) }
    val analysisViewModel = viewModel<SizeAnalysisViewModel>(factory = viewModelFactory { initializer { koin.get<SizeAnalysisViewModel>() } })
    // What is being analysed is kept apart from the view model so that the screen can start again after the process died.
    var analysisPath by rememberSaveable { mutableStateOf<String?>(null) }
    var analysisLabel by rememberSaveable { mutableStateOf("") }
    fun startAnalysis(path: String, label: String) {
        analysisPath = path
        analysisLabel = label
        screen = AppScreen.ANALYSIS
        analysisViewModel.start(path, label)
    }
    fun leaveAnalysis() {
        analysisViewModel.reset()
        analysisPath = null
        screen = AppScreen.BROWSER
    }
    var showAddAccount by rememberSaveable { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<StorageVolume?>(null) }
    var accountToRename by remember { mutableStateOf<StorageVolume?>(null) }
    var accountToEdit by remember { mutableStateOf<EditableAccount?>(null) }
    val unreachable by viewModel.unreachable.collectAsStateWithLifecycle()
    BackHandler(enabled = screen != AppScreen.BROWSER && screen != AppScreen.ANALYSIS) { screen = AppScreen.BROWSER }
    // Back goes up one folder inside the analysis first, and only then leaves it.
    BackHandler(enabled = screen == AppScreen.ANALYSIS) { if (!analysisViewModel.up()) leaveAnalysis() }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val panels = state.panels.map { id -> key(id) { PanelEntry(id, rememberBrowserViewModel(id, viewModel)) } }
    /** Opens the edit form of the account with this id (from a notice about a server that could not be used). */
    fun editAccountById(accountId: String) {
        val volume = state.volumes.firstOrNull { RemoteAccounts.idOfVolume(it.id) == accountId } ?: return
        viewModel.loadForEdit(volume) { accountToEdit = it }
    }
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val pendingDrop by viewModel.pendingDrop.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The headers of the browser and of the drawer are filled with the accent, so the status bar icons over them are light.
    val view = LocalView.current
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val overHeader = screen == AppScreen.BROWSER
    val darkIcons = !darkSurface && !overHeader
    LaunchedEffect(darkIcons) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }

    // Closing a panel can be undone for a few seconds.
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.closedPanels.collect { closed ->
            val result = snackbar.showSnackbar(
                message = resources.getString(R.string.panel_closed),
                actionLabel = resources.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restorePanel(closed)
        }
    }

    // A copy that failed because a server could not be used says why, and offers to edit the account.
    LaunchedEffect(viewModel) {
        viewModel.connectionNotices.collect { notice ->
            snackbar.showSnackbar(
                ConnectionSnackbarVisuals(
                    message = resources.connectionMessage(notice.problem.kind, notice.accountLabel, notice.problem.detail),
                    retryLabel = null,
                    editLabel = resources.getString(R.string.connection_edit_account),
                    accountId = notice.problem.accountId,
                ),
            )
        }
    }

    // Panels showing an account that was just edited reload it, so they do not keep using the old connection's listing.
    LaunchedEffect(viewModel) {
        viewModel.accountChanges.collect { id ->
            panels.forEach { entry ->
                val path = entry.viewModel.state.value.currentPath.orEmpty()
                if (listOf("dav", "sftp", "smb").any { path.startsWith("$it://$id") }) entry.viewModel.onEvent(BrowserEvent.Refresh)
            }
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) viewModel.refreshVolumes()
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::addStorage)
    }

    fun openInActivePanel(path: String) {
        panels.firstOrNull { it.id == state.activePanel }?.viewModel?.onEvent(BrowserEvent.Navigate(path))
        scope.launch { drawerState.close() }
    }

    /** A folder opens; a file is shown, selected, in its own folder. */
    fun openFavorite(favorite: Favorite) {
        if (favorite.isDirectory) {
            openInActivePanel(favorite.path)
            return
        }
        val item = FileItem(favorite.path, favorite.name, false, 0L, 0L, null)
        panels.firstOrNull { it.id == state.activePanel }?.viewModel?.onEvent(BrowserEvent.Reveal(item))
        scope.launch { drawerState.close() }
    }

    // An archive another app sent here opens as a folder in the active panel.
    val incomingFiles = remember { koin.get<IncomingFiles>() }
    val incoming by incomingFiles.pending.collectAsStateWithLifecycle()
    LaunchedEffect(incoming) {
        incoming?.let { path ->
            screen = AppScreen.BROWSER
            openInActivePanel(ArchivePaths.rootOf(path))
            incomingFiles.consume()
        }
    }

    CompositionLocalProvider(LocalThumbnailLoader provides remember { koin.get<ThumbnailLoader>() }) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = screen == AppScreen.BROWSER,
            drawerContent = {
                AppDrawer(
                    volumes = state.volumes,
                    unreachable = unreachable,
                    shortcuts = state.shortcuts,
                    favorites = state.favorites,
                    onOpen = ::openInActivePanel,
                    onOpenFavorite = ::openFavorite,
                    onRemoveFavorite = viewModel::removeFavorite,
                    onAddStorage = { pickFolder.launch(null) },
                    onAddAccount = {
                        showAddAccount = true
                        scope.launch { drawerState.close() }
                    },
                    onRemoveAccount = { accountToRemove = it },
                        onRenameAccount = { accountToRename = it },
                        onEditAccount = { volume -> viewModel.loadForEdit(volume) { accountToEdit = it } },
                    onShowHistory = {
                        screen = AppScreen.HISTORY
                        scope.launch { drawerState.close() }
                    },
                    onShowSettings = {
                        screen = AppScreen.SETTINGS
                        scope.launch { drawerState.close() }
                    },
                    onAnalyze = { volume ->
                        startAnalysis(volume.rootPath, volume.label)
                        scope.launch { drawerState.close() }
                    },
                    onEject = {
                        try {
                            context.startActivity(KoinPlatform.getKoin().get<IntentFactory>().ejectSettings())
                        } catch (e: ActivityNotFoundException) {
                            // No storage settings screen on this device; nothing else can be done safely.
                        }
                    },
                )
            },
        ) {
            Column(Modifier.fillMaxSize()) {
                // The progress bar below already keeps clear of the dock, so the screen above must not do it twice.
                val dockInsets = dockBottomInsets
                Box(
                    Modifier
                        .weight(1f)
                        .then(if (isTransferBarShown(transfer.progress)) Modifier.consumeWindowInsets(dockInsets) else Modifier),
                ) {
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).zIndex(1f)) { data ->
                        ConnectionAwareSnackbar(data, onEditAccount = ::editAccountById)
                    }
                    when (screen) {
                        AppScreen.BROWSER -> DualPanelScaffold(
                            panels = panels,
                            startPanel = state.startPanel,
                            endPanel = state.endPanel,
                            activePanel = state.activePanel,
                            barPosition = state.panelBarPosition,
                            onActivePanelChange = viewModel::setActivePanel,
                            onShowPanel = viewModel::showPanel,
                            onAddPanel = {
                                val active = panels.firstOrNull { it.id == state.activePanel }
                                viewModel.addPanel(active?.viewModel?.state?.value?.currentPath)
                            },
                            onClosePanel = { id ->
                                viewModel.closePanel(id, panels.firstOrNull { it.id == id }?.viewModel?.state?.value?.currentPath)
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onAnalyze = ::startAnalysis,
                            onEditAccount = ::editAccountById,
                        )
                        AppScreen.ANALYSIS -> {
                            val analysis by analysisViewModel.state.collectAsStateWithLifecycle()
                            LaunchedEffect(analysis) {
                                // After the process died the view model is empty again: start over, or leave if nothing was being analysed.
                                if (analysis is SizeAnalysisState.Idle) {
                                    analysisPath?.let { analysisViewModel.start(it, analysisLabel) } ?: run { screen = AppScreen.BROWSER }
                                }
                            }
                            SizeAnalysisScreen(
                                state = analysis,
                                onBack = { if (!analysisViewModel.up()) leaveAnalysis() },
                                onOpen = analysisViewModel::open,
                                onCancel = ::leaveAnalysis,
                                onRetry = analysisViewModel::start,
                                onShowInPanel = { item ->
                                    panels.firstOrNull { it.id == state.activePanel }?.viewModel?.onEvent(BrowserEvent.Reveal(item))
                                    leaveAnalysis()
                                },
                                onOpenInPanel = { item ->
                                    panels.firstOrNull { it.id == state.activePanel }?.viewModel?.onEvent(BrowserEvent.Navigate(item.path))
                                    leaveAnalysis()
                                },
                            )
                        }
                        AppScreen.HISTORY -> {
                            val entries by historyViewModel.entries.collectAsStateWithLifecycle()
                            val running by historyViewModel.transfer.collectAsStateWithLifecycle()
                            HistoryScreen(
                                entries = entries,
                                running = running,
                                onTogglePause = historyViewModel::togglePause,
                                onCancelRunning = historyViewModel::cancelRunning,
                                onClear = historyViewModel::clear,
                                onBack = { screen = AppScreen.BROWSER },
                            )
                        }
                        AppScreen.SETTINGS -> {
                            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
                            preferences?.let { current ->
                                val outcome by settingsViewModel.outcome.collectAsStateWithLifecycle()
                                val accountCount by settingsViewModel.accountCount.collectAsStateWithLifecycle()
                                SettingsScreen(
                                    preferences = current,
                                    accountCount = accountCount,
                                    outcome = outcome,
                                    onExport = settingsViewModel::exportBackup,
                                    onImport = settingsViewModel::importBackup,
                                    onDismissOutcome = settingsViewModel::dismissOutcome,
                                    onThemeMode = settingsViewModel::setThemeMode,
                                    onDynamicColor = settingsViewModel::setDynamicColor,
                                    onAccentColor = settingsViewModel::setAccentColor,
                                    onVerifyCopies = settingsViewModel::setVerifyCopies,
                                    onThumbnailsOnNetwork = settingsViewModel::setThumbnailsOnNetwork,
                                    onPanelBarPosition = settingsViewModel::setPanelBarPosition,
                                    onBack = { screen = AppScreen.BROWSER },
                                )
                            }
                        }
                    }
                }
                TransferProgressBar(
                    progress = transfer.progress,
                    paused = transfer.paused,
                    onTogglePause = viewModel::togglePauseTransfers,
                    onCancel = viewModel::cancelTransfers,
                )
            }
        }
    }

    val interrupted by viewModel.interrupted.collectAsStateWithLifecycle()
    if (interrupted.isNotEmpty()) {
        InterruptedTransfersDialog(
            tasks = interrupted,
            onResume = viewModel::resumeInterrupted,
            onDiscard = viewModel::discardInterrupted,
        )
    }

    val batteryExempt by rememberIgnoringBatteryOptimizations()
    val hints = remember { context.getSharedPreferences("hints", android.content.Context.MODE_PRIVATE) }
    var batteryHintAnswered by remember { mutableStateOf(hints.getBoolean(KEY_BATTERY_HINT, false)) }
    if (transfer.active != null && !batteryExempt && !batteryHintAnswered) {
        BatteryHintDialog(
            onAllow = {
                batteryHintAnswered = true
                hints.edit().putBoolean(KEY_BATTERY_HINT, true).apply()
                requestIgnoreBatteryOptimizations(context)
            },
            onDismiss = {
                batteryHintAnswered = true
                hints.edit().putBoolean(KEY_BATTERY_HINT, true).apply()
            },
        )
    }

    if (showAddAccount) {
        AddAccountDialog(
            onConnect = viewModel::connectAccount,
            onConnectSftp = viewModel::connectSftp,
            onConnectSmb = viewModel::connectSmb,
            onCancel = viewModel::cancelConnect,
            onDismiss = { showAddAccount = false },
        )
    }
    accountToEdit?.let { editable ->
        AddAccountDialog(
            onConnect = viewModel::connectAccount,
            onConnectSftp = viewModel::connectSftp,
            onConnectSmb = viewModel::connectSmb,
            onCancel = viewModel::cancelConnect,
            onDismiss = { accountToEdit = null },
            edit = AccountEdit(
                account = editable.account,
                usesKey = editable.usesKey,
                onUpdateNextcloud = { server, label, trust, signIn, openBrowser, onDone ->
                    viewModel.updateNextcloud(editable.account, server, label, trust, signIn, openBrowser, onDone)
                },
                onUpdateSftp = { host, port, user, secret, key, useKey, label, pinned, onDone ->
                    viewModel.updateSftp(editable.account, host, port, user, secret, key, useKey, label, pinned, onDone)
                },
                onUpdateSmb = { host, port, share, domain, user, password, label, onDone ->
                    viewModel.updateSmb(editable.account, host, port, share, domain, user, password, label, onDone)
                },
            ),
        )
    }
    accountToRename?.let { volume ->
        NameInputDialog(
            title = R.string.action_rename,
            initialName = volume.label,
            onConfirm = {
                viewModel.renameAccount(volume, it)
                accountToRename = null
            },
            onDismiss = { accountToRename = null },
        )
    }
    accountToRemove?.let { volume ->
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = { Text(stringResource(R.string.account_remove_title)) },
            text = { Text(stringResource(R.string.account_remove_message, volume.label)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeAccount(volume)
                    accountToRemove = null
                }) { Text(stringResource(R.string.account_remove)) }
            },
            dismissButton = { TextButton(onClick = { accountToRemove = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    transfer.conflict?.let { prompt ->
        ConflictDialog(
            prompt = prompt,
            onDecision = viewModel::answerConflict,
            onCancelTransfer = viewModel::cancelTransfers,
        )
    }
    pendingDrop?.let { drop ->
        DropActionDialog(
            itemCount = drop.items.size,
            onCopy = { viewModel.confirmDrop(copy = true) },
            onMove = { viewModel.confirmDrop(copy = false) },
            onDismiss = viewModel::dismissDrop,
        )
    }
}

@Composable
private fun rememberBrowserViewModel(panel: PanelId, main: MainViewModel): BrowserViewModel {
    val koin = remember { KoinPlatform.getKoin() }
    // Each panel has a store of its own in [main] so that closing one disposes of just its state holder.
    val owner = remember(panel) {
        object : ViewModelStoreOwner {
            override val viewModelStore = main.storeFor(panel)
        }
    }
    return viewModel(
        viewModelStoreOwner = owner,
        key = panel.value.toString(),
        factory = viewModelFactory {
            initializer { koin.get<BrowserViewModel>(parameters = { parametersOf(panel) }) }
        },
    )
}

private const val KEY_BATTERY_HINT = "battery_hint_answered"
