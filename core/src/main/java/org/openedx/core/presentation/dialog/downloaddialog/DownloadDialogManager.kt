package org.openedx.core.presentation.dialog.downloaddialog

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.CourseInteractor
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.DownloadCoursePreview
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.system.StorageManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.foundation.presentation.rememberWindowSize

interface DownloadDialogListener {
    fun onCancelClick()
    fun onConfirmClick()
}

interface DownloadDialog {
    var listener: DownloadDialogListener?
}

class DownloadDialogManager(
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val interactor: CourseInteractor,
    private val workerController: DownloadWorkerController
) {

    companion object {
        const val MAX_CELLULAR_SIZE = 104857600 // 100MB
        const val DOWNLOAD_SIZE_FACTOR = 2 // Multiplier to match required disk size

        val listMaxSize: Dp
            @Composable
            get() {
                val configuration = LocalConfiguration.current
                val windowSize = rememberWindowSize()
                return when {
                    configuration.orientation == Configuration.ORIENTATION_PORTRAIT || windowSize.isTablet -> {
                        200.dp
                    }

                    else -> {
                        88.dp
                    }
                }
            }
    }

    private val uiState = MutableSharedFlow<DownloadDialogUIState>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            uiState.collect { state ->
                val dialog = when {
                    state.isDownloadFailed -> DownloadErrorDialogFragment.newInstance(
                        dialogType = DownloadErrorDialogType.DOWNLOAD_FAILED,
                        uiState = state
                    )

                    state.isAllBlocksDownloaded -> DownloadConfirmDialogFragment.newInstance(
                        dialogType = DownloadConfirmDialogType.REMOVE,
                        uiState = state
                    )

                    !networkConnection.isOnline() -> DownloadErrorDialogFragment.newInstance(
                        dialogType = DownloadErrorDialogType.NO_CONNECTION,
                        uiState = state
                    )

                    StorageManager.getFreeStorage() < state.sizeSum * DOWNLOAD_SIZE_FACTOR -> {
                        DownloadStorageErrorDialogFragment.newInstance(
                            uiState = state
                        )
                    }

                    corePreferences.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected() -> {
                        DownloadErrorDialogFragment.newInstance(
                            dialogType = DownloadErrorDialogType.WIFI_REQUIRED,
                            uiState = state
                        )
                    }

                    !corePreferences.videoSettings.wifiDownloadOnly && !networkConnection.isWifiConnected() -> {
                        DownloadConfirmDialogFragment.newInstance(
                            dialogType = DownloadConfirmDialogType.DOWNLOAD_ON_CELLULAR,
                            uiState = state
                        )
                    }

                    state.sizeSum >= MAX_CELLULAR_SIZE -> DownloadConfirmDialogFragment.newInstance(
                        dialogType = DownloadConfirmDialogType.CONFIRM,
                        uiState = state
                    )

                    else -> null
                }

                val dialogListener = object : DownloadDialogListener {
                    override fun onCancelClick() {
                        state.onDismissClick()
                    }

                    override fun onConfirmClick() {
                        state.onConfirmClick()
                    }
                }
                if (dialog != null) {
                    dialog.listener = dialogListener
                    dialog.show(state.fragmentManager, dialog::class.java.simpleName)
                } else {
                    state.onConfirmClick()
                    state.saveDownloadModels()
                }
            }
        }
    }

    fun showPopup(
        subSectionsBlocks: List<Block>,
        courseId: String,
        isBlocksDownloaded: Boolean,
        onlyVideoBlocks: Boolean = false,
        fragmentManager: FragmentManager,
        removeDownloadModels: (blockId: String, courseId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
        onDismissClick: () -> Unit = {},
        onConfirmClick: () -> Unit = {},
    ) {
        createDownloadItems(
            subSectionsBlocks = subSectionsBlocks,
            courseId = courseId,
            fragmentManager = fragmentManager,
            isBlocksDownloaded = isBlocksDownloaded,
            onlyVideoBlocks = onlyVideoBlocks,
            removeDownloadModels = removeDownloadModels,
            saveDownloadModels = saveDownloadModels,
            onDismissClick = onDismissClick,
            onConfirmClick = onConfirmClick
        )
    }

    fun showPopup(
        coursePreview: DownloadCoursePreview,
        isBlocksDownloaded: Boolean,
        fragmentManager: FragmentManager,
        removeDownloadModels: (blockId: String, courseId: String) -> Unit,
        saveDownloadModels: () -> Unit,
        onDismissClick: () -> Unit = {},
        onConfirmClick: () -> Unit = {},
    ) {
        createCourseDownloadItems(
            coursePreview = coursePreview,
            fragmentManager = fragmentManager,
            isBlocksDownloaded = isBlocksDownloaded,
            removeDownloadModels = removeDownloadModels,
            saveDownloadModels = saveDownloadModels,
            onDismissClick = onDismissClick,
            onConfirmClick = onConfirmClick
        )
    }

    fun showRemoveDownloadModelPopup(
        downloadDialogItem: DownloadDialogItem,
        fragmentManager: FragmentManager,
        removeDownloadModels: () -> Unit,
    ) {
        coroutineScope.launch {
            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = listOf(downloadDialogItem),
                    isAllBlocksDownloaded = true,
                    isDownloadFailed = false,
                    sizeSum = downloadDialogItem.size,
                    fragmentManager = fragmentManager,
                    removeDownloadModels = removeDownloadModels,
                    saveDownloadModels = {}
                )
            )
        }
    }

    fun showDownloadFailedPopup(
        downloadModel: List<DownloadModel>,
        fragmentManager: FragmentManager,
    ) {
        createDownloadItems(
            downloadModels = downloadModel,
            fragmentManager = fragmentManager,
        )
    }

    private fun createDownloadItems(
        downloadModels: List<DownloadModel>,
        fragmentManager: FragmentManager,
    ) {
        coroutineScope.launch {
            val courseIds = downloadModels.map { it.courseId }.distinct()
            val blockIds = downloadModels.map { it.id }
            val notDownloadedSubSections = mutableListOf<Block>()
            val allDownloadDialogItems = mutableListOf<DownloadDialogItem>()

            courseIds.forEach { courseId ->
                val courseStructure = interactor.getCourseStructureFromCache(courseId)
                val allSubSectionBlocks =
                    courseStructure.blockData.filter { it.type == BlockType.SEQUENTIAL }

                allSubSectionBlocks.forEach { subSectionBlock ->
                    val verticalBlocks =
                        courseStructure.blockData.filter { it.id in subSectionBlock.descendants }
                    val blocks = courseStructure.blockData.filter {
                        it.id in verticalBlocks.flatMap { it.descendants } && it.id in blockIds
                    }
                    val totalSize = blocks.sumOf { it.getFileSize() }

                    if (blocks.isNotEmpty()) notDownloadedSubSections.add(subSectionBlock)
                    if (totalSize > 0) {
                        allDownloadDialogItems.add(
                            DownloadDialogItem(
                                title = subSectionBlock.displayName,
                                size = totalSize
                            )
                        )
                    }
                }
            }

            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = allDownloadDialogItems,
                    isAllBlocksDownloaded = false,
                    isDownloadFailed = true,
                    sizeSum = allDownloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = {},
                    saveDownloadModels = {
                        coroutineScope.launch {
                            workerController.saveModels(downloadModels)
                        }
                    }
                )
            )
        }
    }

    private fun createDownloadItems(
        subSectionsBlocks: List<Block>,
        courseId: String,
        fragmentManager: FragmentManager,
        isBlocksDownloaded: Boolean,
        onlyVideoBlocks: Boolean,
        removeDownloadModels: (blockId: String, courseId: String) -> Unit,
        saveDownloadModels: (blockId: String) -> Unit,
        onDismissClick: () -> Unit = {},
        onConfirmClick: () -> Unit = {},
    ) {
        coroutineScope.launch {
            val courseStructure = interactor.getCourseStructure(courseId, false)
            val downloadModelIds = interactor.getAllDownloadModels().map { it.id }

            val downloadDialogItems = subSectionsBlocks.mapNotNull { subSectionBlock ->
                val verticalBlocks =
                    courseStructure.blockData.filter { it.id in subSectionBlock.descendants }
                val blocks = verticalBlocks.flatMap { verticalBlock ->
                    courseStructure.blockData.filter {
                        it.id in verticalBlock.descendants &&
                                (isBlocksDownloaded == (it.id in downloadModelIds)) &&
                                (!onlyVideoBlocks || it.type == BlockType.VIDEO)
                    }
                }
                val size = blocks.sumOf { it.getFileSize() }
                if (size > 0) {
                    DownloadDialogItem(
                        title = subSectionBlock.displayName,
                        size = size
                    )
                } else {
                    null
                }
            }

            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = downloadDialogItems,
                    isAllBlocksDownloaded = isBlocksDownloaded,
                    isDownloadFailed = false,
                    sizeSum = downloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = {
                        subSectionsBlocks.forEach {
                            removeDownloadModels(
                                it.id,
                                courseId
                            )
                        }
                    },
                    saveDownloadModels = { subSectionsBlocks.forEach { saveDownloadModels(it.id) } },
                    onDismissClick = onDismissClick,
                    onConfirmClick = onConfirmClick,
                )
            )
        }
    }

    private fun createCourseDownloadItems(
        coursePreview: DownloadCoursePreview,
        fragmentManager: FragmentManager,
        isBlocksDownloaded: Boolean,
        removeDownloadModels: (blockId: String, courseId: String) -> Unit,
        saveDownloadModels: () -> Unit,
        onDismissClick: () -> Unit = {},
        onConfirmClick: () -> Unit = {},
    ) {
        coroutineScope.launch {
            val downloadDialogItems = listOf(
                DownloadDialogItem(
                    title = coursePreview.name,
                    size = coursePreview.totalSize,
                    icon = Icons.Default.School
                )
            )

            uiState.emit(
                DownloadDialogUIState(
                    downloadDialogItems = downloadDialogItems,
                    isAllBlocksDownloaded = isBlocksDownloaded,
                    isDownloadFailed = false,
                    sizeSum = downloadDialogItems.sumOf { it.size },
                    fragmentManager = fragmentManager,
                    removeDownloadModels = {
                        coroutineScope.launch {
                            val downloadModels = interactor.getAllDownloadModels().filter {
                                it.courseId == coursePreview.id
                            }
                            downloadModels.forEach {
                                removeDownloadModels(
                                    it.id,
                                    coursePreview.id
                                )
                            }
                        }
                    },
                    saveDownloadModels = saveDownloadModels,
                    onDismissClick = onDismissClick,
                    onConfirmClick = onConfirmClick,
                )
            )
        }
    }
}
