package org.openedx.course.presentation.offline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.exception.safeDivBy
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.FileType
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogItem
import org.openedx.core.presentation.dialog.downloaddialog.DownloadDialogManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureGot
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.utils.FileUtil
import org.openedx.course.domain.interactor.CourseInteractor
import kotlin.text.get

class CourseOfflineViewModel(
    val courseId: String,
    val courseTitle: String,
    val courseInteractor: CourseInteractor,
    private val preferencesManager: CorePreferences,
    private val downloadDialogManager: DownloadDialogManager,
    private val fileUtil: FileUtil,
    private val networkConnection: NetworkConnection,
    private val courseNotifier: CourseNotifier,
    private val resourceManager: ResourceManager,
    coreAnalytics: CoreAnalytics,
    downloadDao: DownloadDao,
    workerController: DownloadWorkerController,
) : BaseDownloadViewModel(
    courseId,
    downloadDao,
    preferencesManager,
    workerController,
    coreAnalytics
) {
    private val _uiState = MutableStateFlow(
        CourseOfflineUIState(
            isHaveDownloadableBlocks = false,
            largestDownloads = emptyList(),
            isDownloading = false,
            isAllDownloaded = false,
            readyToDownloadSize = 0L,
            downloadedSize = 0L,
            progressBarValue = 0f,
            isInitialLoading = true
        )
    )
    val uiState: StateFlow<CourseOfflineUIState>
        get() = _uiState.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()
    private var offlineDataJob: Job? = null
    init {
        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                val isDownloading = it.any { it.value.isWaitingOrDownloading }
                _uiState.update { it.copy(isDownloading = isDownloading) }
            }
        }
        collectCourseNotifier()
        refreshOfflineData()
    }

    fun downloadAllBlocks(fragmentManager: FragmentManager) {

        viewModelScope.launch {
            val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
            val downloadModels = courseInteractor.getAllDownloadModels()
            val subSectionsBlocks = allBlocks.values.filter { it.type == BlockType.SEQUENTIAL }
            val notDownloadedSubSectionBlocks = subSectionsBlocks.mapNotNull { subSection ->
                val verticalBlocks = allBlocks.values.filter { it.id in subSection.descendants }
                val notDownloadedBlocks = courseStructure.blockData.filter { block ->
                    block.id in verticalBlocks.flatMap { it.descendants } &&
                            block.isDownloadable &&
                            downloadModels.none { it.id == block.id }
                }
                if (notDownloadedBlocks.isNotEmpty()) subSection else null
            }

            downloadDialogManager.showPopup(
                subSectionsBlocks = notDownloadedSubSectionBlocks,
                courseId = courseId,
                isBlocksDownloaded = false,
                fragmentManager = fragmentManager,
                removeDownloadModels = ::removeDownloadModels,
                saveDownloadModels = { blockId ->
                    saveDownloadModels(fileUtil.getExternalAppDir().path, courseId, blockId)
                }
            )
        }

    }

    fun removeDownloadModel(downloadModel: DownloadModel, fragmentManager: FragmentManager) {
        val icon = when (downloadModel.type) {
            FileType.VIDEO -> Icons.Outlined.SmartDisplay
            else -> Icons.AutoMirrored.Outlined.InsertDriveFile
        }
        val downloadDialogItem = DownloadDialogItem(
            title = downloadModel.title,
            size = downloadModel.size,
            icon = icon
        )
        downloadDialogManager.showRemoveDownloadModelPopup(
            downloadDialogItem = downloadDialogItem,
            fragmentManager = fragmentManager,
            removeDownloadModels = {
                super.removeBlockDownloadModel(downloadModel.id)
            }
        )
    }

    fun deleteAll(fragmentManager: FragmentManager) {
        viewModelScope.launch {
            val downloadModels =
                courseInteractor.getAllDownloadModels().filter { it.courseId == courseId }
            val totalSize = downloadModels.sumOf { it.size }
            val downloadDialogItem = DownloadDialogItem(
                title = courseTitle,
                size = totalSize,
            )
            downloadDialogManager.showRemoveDownloadModelPopup(
                downloadDialogItem = downloadDialogItem,
                fragmentManager = fragmentManager,
                removeDownloadModels = {
                    downloadModels.forEach { super.removeBlockDownloadModel(it.id) }
                }
            )

        }
    }

    fun removeDownloadModel() {
        viewModelScope.launch {
            courseInteractor.getAllDownloadModels()
                .filter { it.courseId == courseId && it.downloadedState.isWaitingOrDownloading }
                .forEach { removeBlockDownloadModel(it.id) }
        }
    }

    private suspend fun initDownloadFragment(courseStructure: CourseStructure) {
        setBlocks(courseStructure.blockData)
        allBlocks.values
            .filter { it.type == BlockType.SEQUENTIAL }
            .forEach { addDownloadableChildrenForSequentialBlock(it) }
    }


    private fun getOfflineData() {

        viewModelScope.launch {
                val courseStructure = courseInteractor.getCourseStructureFromCache(courseId)
                val totalDownloadableSize = getFilesSize(courseStructure.blockData)
                courseInteractor.getDownloadModels().collect { downloadModels ->
                    val courseDownloadModels = downloadModels.filter { it.courseId == courseId }
                    val completedDownloads =
                        courseDownloadModels.filter { it.downloadedState.isDownloaded }
                    val downloadedBlocks = courseStructure.blockData.filter {
                        it.id in completedDownloads.map { it.id }
                    }
                    val allDownloadableBlocks = courseStructure.blockData.filter { it.isDownloadable }
                    val courseDownloadModelsMap = courseDownloadModels.associateBy { it.id }
                    val isAllDownloaded = allDownloadableBlocks.isNotEmpty() &&
                            allDownloadableBlocks.all { block ->
                                courseDownloadModelsMap[block.id]?.downloadedState?.isDownloaded == true
                            }
                    val isHaveDownloadableBlocks = allDownloadableBlocks.isNotEmpty()

                    updateUIState(
                        totalDownloadableSize,
                        completedDownloads,
                        downloadedBlocks,
                        isAllDownloaded,
                        isHaveDownloadableBlocks
                    )
                }

        }

    }

    private fun updateUIState(
        totalDownloadableSize: Long,
        completedDownloads: List<DownloadModel>,
        downloadedBlocks: List<Block>,
        isAllDownloaded: Boolean,
        isHaveDownloadableBlocks: Boolean,
    ) {
        val downloadedSize = getFilesSize(downloadedBlocks).toFloat()
        val realDownloadedSize = completedDownloads.sumOf { it.size }
        val largestDownloads = completedDownloads
            .sortedByDescending { it.size }
            .take(n = 5)
        val progressBarValue = downloadedSize.safeDivBy(totalDownloadableSize.toFloat())
        val readyToDownloadSize = if (progressBarValue >= 1) {
            0
        } else {
            totalDownloadableSize - realDownloadedSize
        }
        _uiState.update {
            it.copy(
                isHaveDownloadableBlocks = isHaveDownloadableBlocks,
                isAllDownloaded = isAllDownloaded,
                largestDownloads = largestDownloads,
                readyToDownloadSize = readyToDownloadSize,
                downloadedSize = realDownloadedSize,
                progressBarValue = progressBarValue
            )
        }
    }

    private fun getFilesSize(blocks: List<Block>): Long {
        return blocks.filter { it.isDownloadable }.sumOf {
            when (it.downloadableType) {
                FileType.VIDEO -> {
                    it.studentViewData?.encodedVideos
                        ?.getPreferredVideoInfoForDownloading(preferencesManager.videoSettings.videoDownloadQuality)
                        ?.fileSize ?: 0
                }

                //FileType.X_BLOCK -> it.offlineDownload?.fileSize ?: 0
                else -> 0
            }
        }
    }

    private fun collectCourseNotifier() {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureGot -> if (event.courseId == courseId) refreshOfflineData()
                    is CourseStructureUpdated -> if (event.courseId == courseId) refreshOfflineData()
                }
            }
        }
    }
    private fun refreshOfflineData() {
        offlineDataJob?.cancel()
        offlineDataJob = viewModelScope.launch {
            runCatching {
                // Single source of truth for this refresh cycle
                val courseStructure = courseInteractor.getCourseStructure(courseId, isNeedRefresh = false)
                initDownloadFragment(courseStructure)
                collectOfflineData(courseStructure)
            }.onFailure {
                _uiState.update { state ->
                    state.copy(isInitialLoading = false)
                }            }
        }
    }
    private suspend fun collectOfflineData(courseStructure: CourseStructure) {
        val totalDownloadableSize = getFilesSize(courseStructure.blockData)
        var isFirstEmission = true

        courseInteractor.getDownloadModels().collect { downloadModels ->
            val courseDownloadModels = downloadModels.filter { it.courseId == courseId }
            val completedDownloads = courseDownloadModels.filter { it.downloadedState.isDownloaded }
            val downloadedBlocks = courseStructure.blockData.filter { block ->
                block.id in completedDownloads.map { it.id }
            }
            val allDownloadableBlocks = courseStructure.blockData.filter { it.isDownloadable }
            val courseDownloadModelsMap = courseDownloadModels.associateBy { it.id }
            val isAllDownloaded = allDownloadableBlocks.isNotEmpty() &&
                    allDownloadableBlocks.all { block ->
                        courseDownloadModelsMap[block.id]?.downloadedState?.isDownloaded == true
                    }

            // Only set isInitialLoading = false on FIRST emission
            if (isFirstEmission) {
                _uiState.update { state ->
                    state.copy(isInitialLoading = false)
                }
                isFirstEmission = false
            }

            updateUIState(
                totalDownloadableSize = totalDownloadableSize,
                completedDownloads = completedDownloads,
                downloadedBlocks = downloadedBlocks,
                isAllDownloaded = isAllDownloaded,
                isHaveDownloadableBlocks = allDownloadableBlocks.isNotEmpty()
            )
        }
    }
}
