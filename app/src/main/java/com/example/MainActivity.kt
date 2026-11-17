package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MovieFilter
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTab
import com.example.ui.PromptToonViewModel
import com.example.ui.components.ExportVideoDialog
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.PromptToonTheme
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before UI, ViewModels, or any Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            PromptToonTheme {
                Surface(
                    color = StudioObsidian,
                    modifier = Modifier.fillMaxSize()
                ) {
                    PromptToonApp()
                }
            }
        }
    }
}

@Composable
private fun PromptToonApp(
    viewModel: PromptToonViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val project by viewModel.project.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val selectedSceneIndex by viewModel.selectedSceneIndex.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val isComposerExpanded by viewModel.isComposerExpanded.collectAsStateWithLifecycle()
    val isPlayingAll by viewModel.isPlayingAll.collectAsStateWithLifecycle()
    val fadeAlpha by viewModel.playbackFadeAlpha.collectAsStateWithLifecycle()
    val playbackElapsedSec by viewModel.playbackElapsedSec.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.attachMusic(uri)
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    if (currentTab == AppTab.PROJECTS) {
        BackHandler {
            viewModel.selectTab(AppTab.STUDIO)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = StudioObsidian,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = StudioSlate,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.STUDIO) {
                                        Icons.Rounded.MovieFilter
                                    } else {
                                        Icons.Outlined.MovieFilter
                                    },
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioObsidian,
                                selectedTextColor = ElectricCoral,
                                indicatorColor = ElectricCoral,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_studio_tab")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.PROJECTS,
                            onClick = { viewModel.selectTab(AppTab.PROJECTS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.PROJECTS) {
                                        Icons.Rounded.Folder
                                    } else {
                                        Icons.Outlined.Folder
                                    },
                                    contentDescription = stringResource(R.string.nav_projects)
                                )
                            },
                            label = {
                                Text(
                                    text = if (savedProjects.isEmpty()) {
                                        stringResource(R.string.nav_projects)
                                    } else {
                                        "${stringResource(R.string.nav_projects)} (${savedProjects.size})"
                                    }
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StudioObsidian,
                                selectedTextColor = ElectricCoral,
                                indicatorColor = ElectricCoral,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_projects_tab")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = StudioSlate,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentTab == AppTab.STUDIO,
                            onClick = { viewModel.selectTab(AppTab.STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.STUDIO) {
                                        Icons.Rounded.MovieFilter
                                    } else {
                                        Icons.Outlined.MovieFilter
                                    },
                                    contentDescription = stringResource(R.string.nav_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_studio)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = StudioObsidian,
                                selectedTextColor = ElectricCoral,
                                indicatorColor = ElectricCoral,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == AppTab.PROJECTS,
                            onClick = { viewModel.selectTab(AppTab.PROJECTS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == AppTab.PROJECTS) {
                                        Icons.Rounded.Folder
                                    } else {
                                        Icons.Outlined.Folder
                                    },
                                    contentDescription = stringResource(R.string.nav_projects)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_projects)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = StudioObsidian,
                                selectedTextColor = ElectricCoral,
                                indicatorColor = ElectricCoral,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        AppTab.STUDIO -> StudioScreen(
                            project = project,
                            selectedPreset = selectedPreset,
                            selectedSceneIndex = selectedSceneIndex,
                            generationState = generationState,
                            isComposerExpanded = isComposerExpanded,
                            isPlayingAll = isPlayingAll,
                            fadeAlpha = fadeAlpha,
                            playbackElapsedSec = playbackElapsedSec,
                            onToggleComposer = viewModel::setComposerExpanded,
                            onPromptChange = viewModel::updatePrompt,
                            onSelectDurationPreset = viewModel::selectDurationPreset,
                            onCustomDurationChange = viewModel::updateCustomDuration,
                            onSelectStyle = viewModel::updateStyle,
                            onApplyStarter = viewModel::applyStarter,
                            onGenerateClick = viewModel::generateStoryboardAndScenes,
                            onPauseGeneration = viewModel::pauseGeneration,
                            onResumeGeneration = viewModel::resumeGeneration,
                            onNewProjectClick = viewModel::startNewProject,
                            onSelectScene = viewModel::selectScene,
                            onRegenerateScene = viewModel::regenerateSingleScene,
                            onMoveScene = viewModel::moveScene,
                            onDeleteScene = viewModel::deleteScene,
                            onUpdateScene = viewModel::updateSceneDetails,
                            onTogglePlayAll = viewModel::togglePlayAll,
                            onToggleCaptions = viewModel::toggleCaptions,
                            onToggleVoiceover = viewModel::toggleVoiceover,
                            onCycleVoiceSpeed = viewModel::cycleVoiceSpeed,
                            onPickMusic = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            onRemoveMusic = viewModel::removeMusic,
                            onOpenExport = {
                                viewModel.cancelOrResetExport()
                                showExportDialog = true
                            }
                        )

                        AppTab.PROJECTS -> ProjectsScreen(
                            projects = savedProjects,
                            activeProjectId = project.id,
                            onOpenProject = viewModel::loadProject,
                            onDeleteProject = viewModel::deleteSavedProject,
                            onCreateNew = viewModel::startNewProject
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportVideoDialog(
            exportState = exportState,
            captionsEnabled = project.captionsEnabled,
            onStartExport = { webView -> viewModel.startVideoExport(webView) },
            onDownload = viewModel::downloadExportedVideo,
            onShare = viewModel::shareExportedVideo,
            onClose = {
                viewModel.cancelOrResetExport()
                showExportDialog = false
            }
        )
    }
}
