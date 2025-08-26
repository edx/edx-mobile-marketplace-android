package org.openedx.course.presentation.outline

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.openedx.core.BlockType
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseBannerType
import org.openedx.core.domain.model.CourseComponentStatus
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesBannerInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.extension.isInternetError
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.db.DownloadDao
import org.openedx.core.module.download.BaseDownloadViewModel
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.settings.calendarsync.CalendarSyncDialogType
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CalendarSyncEvent.CreateCalendarSyncEvent
import org.openedx.core.system.notifier.CourseDatesShifted
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseOpenBlock
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.core.system.notifier.RefreshPLSBanner
import org.openedx.core.utils.FileUtil
import org.openedx.core.utils.Logger
import org.openedx.course.R
import org.openedx.course.data.storage.CoursePreferences
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.CourseRouter
import org.openedx.core.R as CoreR

class CourseOutlineViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val courseNotifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val preferencesManager: CorePreferences,
    private val coursePreferences: CoursePreferences,
    private val analytics: CourseAnalytics,
    val courseRouter: CourseRouter,
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
    private val logger = Logger(TAG)

    val isCourseNestedListEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    private val _uiState = MutableStateFlow<CourseOutlineUIState>(CourseOutlineUIState.Loading)
    val uiState: StateFlow<CourseOutlineUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    private val _resumeBlockId = MutableSharedFlow<String>()
    val resumeBlockId: SharedFlow<String>
        get() = _resumeBlockId.asSharedFlow()

    private val _canShowPLSBanner = MutableStateFlow(false)
    val canShowPLSBanner: StateFlow<Boolean> = _canShowPLSBanner

    private var resumeSectionBlock: Block? = null
    private var resumeVerticalBlock: Block? = null

    private val isCourseExpandableSectionsEnabled get() = config.getCourseUIConfig().isCourseDropdownNavigationEnabled

    private val courseSubSections = mutableMapOf<String, MutableList<Block>>()
    private val subSectionsDownloadsCount = mutableMapOf<String, Int>()
    val courseSubSectionUnit = mutableMapOf<String, Block?>()

    init {
        viewModelScope.launch {
            courseNotifier.notifier.collect { event ->
                when (event) {
                    is CourseStructureUpdated -> {
                        if (event.courseId == courseId) {
                            getCourseData()
                        }
                    }

                    is CourseOpenBlock -> {
                        _resumeBlockId.emit(event.blockId)
                    }

                    is RefreshPLSBanner -> {
                        _canShowPLSBanner.value =
                            coursePreferences.canShowPLSBanner(courseId, event.bannerType)
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadModelsStatusFlow.collect {
                if (_uiState.value is CourseOutlineUIState.CourseData) {
                    val state = _uiState.value as CourseOutlineUIState.CourseData
                    _uiState.value = CourseOutlineUIState.CourseData(
                        courseStructure = state.courseStructure,
                        downloadedState = it.toMap(),
                        resumeComponent = state.resumeComponent,
                        resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
                        courseSubSections = courseSubSections,
                        courseSectionsState = state.courseSectionsState,
                        subSectionsDownloadsCount = subSectionsDownloadsCount,
                        datesBannerInfo = state.datesBannerInfo
                    )
                }
            }
        }

        getCourseData()
    }

    override fun saveDownloadModels(folder: String, id: String) {
        if (preferencesManager.videoSettings.wifiDownloadOnly) {
            if (networkConnection.isWifiConnected()) {
                super.saveDownloadModels(folder, id)
            } else {
                viewModelScope.launch {
                    _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.course_can_download_only_with_wifi)))
                }
            }
        } else {
            super.saveDownloadModels(folder, id)
        }
    }

    fun getCourseData() {
        getCourseDataInternal()
    }

    fun switchCourseSections(blockId: String): Boolean {
        return if (_uiState.value is CourseOutlineUIState.CourseData) {
            val state = _uiState.value as CourseOutlineUIState.CourseData
            val courseSectionsState = state.courseSectionsState.toMutableMap()
            courseSectionsState[blockId] = !(state.courseSectionsState[blockId] ?: false)

            _uiState.value = CourseOutlineUIState.CourseData(
                courseStructure = state.courseStructure,
                downloadedState = state.downloadedState,
                resumeComponent = state.resumeComponent,
                resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
                courseSubSections = courseSubSections,
                courseSectionsState = courseSectionsState,
                subSectionsDownloadsCount = subSectionsDownloadsCount,
                datesBannerInfo = state.datesBannerInfo
            )

            courseSectionsState[blockId] ?: false

        } else {
            false
        }
    }

    fun onPLSBannerViewed() {
        logPLSBannerEvents(CourseAnalyticsEvent.PLS_BANNER_VIEWED)
    }

    fun onDismissPLSBanner(bannerType: String) {
        _canShowPLSBanner.value = false
        coursePreferences.markPLSBannerDismissed(courseId, bannerType)
        viewModelScope.launch { courseNotifier.send(RefreshPLSBanner(bannerType)) }
        logPLSBannerEvents(CourseAnalyticsEvent.PLS_BANNER_DISMISSED)
    }

    private fun getCourseDataInternal() {
        viewModelScope.launch {
            val courseStructureFlow = interactor.getCourseStructureFlow(courseId, false)
                .catch { emit(null) }
            val courseStatusFlow = interactor.getCourseStatusFlow(courseId)
            val courseDatesFlow = interactor.getCourseDatesFlow(courseId)
            combine(
                courseStructureFlow,
                courseStatusFlow,
                courseDatesFlow
            ) { courseStructure, courseStatus, courseDatesResult ->
                Triple(courseStructure, courseStatus, courseDatesResult)
            }.catch { e ->
                handleCourseDataError(e)
            }.collect { (courseStructure, courseStatus, courseDates) ->
                if (courseStructure == null) return@collect
                val blocks = courseStructure.blockData
                val datesBannerInfo = courseDates.courseBanner

                checkIfCalendarOutOfDate(courseDates.datesSection.values.flatten())

                initializeCourseData(blocks, courseStructure, courseStatus, datesBannerInfo)
            }
        }
    }

    private suspend fun initializeCourseData(
        blocks: List<Block>,
        courseStructure: CourseStructure,
        courseStatus: CourseComponentStatus,
        datesBannerInfo: CourseDatesBannerInfo,
    ) {
        setBlocks(blocks)
        courseSubSections.clear()
        courseSubSectionUnit.clear()
        val sortedStructure = courseStructure.copy(blockData = sortBlocks(blocks))
        initDownloadModelsStatus()

        _uiState.value = CourseOutlineUIState.CourseData(
            courseStructure = sortedStructure,
            downloadedState = getDownloadModelsStatus(),
            resumeComponent = getResumeBlock(blocks, courseStatus.lastVisitedBlockId),
            resumeUnitTitle = resumeVerticalBlock?.displayName ?: "",
            courseSubSections = courseSubSections,
            courseSectionsState = getCourseSectionExpandedState(sortedStructure.blockData),
            subSectionsDownloadsCount = subSectionsDownloadsCount,
            datesBannerInfo = datesBannerInfo,
        )
        _canShowPLSBanner.value =
            coursePreferences.canShowPLSBanner(courseId, datesBannerInfo.bannerType.name)
    }

    private suspend fun handleCourseDataError(e: Throwable) {
        logger.e(throwable = e, metadata = mapOf("courseId" to courseId))
        _uiState.value = CourseOutlineUIState.Error
        val errorMessage = when {
            e.isInternetError() -> CoreR.string.core_error_no_connection
            else -> CoreR.string.core_error_unknown_error
        }
        _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(errorMessage)))
    }

    private fun sortBlocks(blocks: List<Block>): List<Block> {
        val resultBlocks = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        blocks.forEach { block ->
            if (block.type == BlockType.CHAPTER) {
                resultBlocks.add(block)
                processDescendants(block, blocks)
            }
        }
        return resultBlocks
    }

    private fun processDescendants(block: Block, blocks: List<Block>) {
        block.descendants.forEach { descendantId ->
            val sequentialBlock = blocks.find { it.id == descendantId } ?: return@forEach
            addSequentialBlockToSubSections(block, sequentialBlock)
            courseSubSectionUnit[sequentialBlock.id] =
                sequentialBlock.getFirstDescendantBlock(blocks)
            subSectionsDownloadsCount[sequentialBlock.id] =
                sequentialBlock.getDownloadsCount(blocks)
            addDownloadableChildrenForSequentialBlock(sequentialBlock)
        }
    }

    private fun addSequentialBlockToSubSections(block: Block, sequentialBlock: Block) {
        courseSubSections.getOrPut(block.id) { mutableListOf() }.add(sequentialBlock)
    }

    private fun getResumeBlock(
        blocks: List<Block>,
        continueBlockId: String,
    ): Block? {
        val resumeBlock = blocks.firstOrNull { it.id == continueBlockId }
        resumeVerticalBlock =
            blocks.getVerticalBlocks().find { it.descendants.contains(resumeBlock?.id) }
        resumeSectionBlock =
            blocks.getSequentialBlocks().find { it.descendants.contains(resumeVerticalBlock?.id) }
        return resumeBlock
    }

    fun resetCourseDatesBanner(onResetDates: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                getCourseData()
                courseNotifier.send(CourseDatesShifted)
                onResetDates(true)
            } catch (e: Exception) {
                logger.e(throwable = e, metadata = mapOf("courseId" to courseId))
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_dates_shift_dates_unsuccessful_msg)))
                }
                onResetDates(false)
            }
        }
    }

    fun openBlock(fragmentManager: FragmentManager, blockId: String) {
        viewModelScope.launch {
            val courseStructure = interactor.getCourseStructure(courseId, false)
            val blocks = courseStructure.blockData
            getResumeBlock(blocks, blockId)
            resumeBlock(fragmentManager, blockId)
        }
    }

    private fun resumeBlock(fragmentManager: FragmentManager, blockId: String) {
        resumeSectionBlock?.let { subSection ->
            resumeCourseTappedEvent(subSection.id)
            resumeVerticalBlock?.let { unit ->
                if (isCourseExpandableSectionsEnabled) {
                    courseRouter.navigateToCourseContainer(
                        fm = fragmentManager,
                        courseId = courseId,
                        unitId = unit.id,
                        componentId = blockId,
                        mode = CourseViewMode.FULL
                    )
                } else {
                    courseRouter.navigateToCourseSubsections(
                        fragmentManager,
                        courseId = courseId,
                        subSectionId = subSection.id,
                        mode = CourseViewMode.FULL,
                        unitId = unit.id,
                        componentId = blockId
                    )
                }
            }
        }
    }

    private fun getCourseSectionExpandedState(blockData: List<Block>): Map<String, Boolean> {
        val expandedState = mutableMapOf<String, Boolean>()

        // Open only the first incomplete section (if any)
        blockData.firstOrNull { !it.isCompleted() }?.id
            ?.let { expandedState[it] = true }

        // Merge in any existing overrides (existing takes precedence)
        val existingState = (_uiState.value as? CourseOutlineUIState.CourseData)
            ?.courseSectionsState
            .orEmpty()
        expandedState.putAll(existingState)

        return expandedState
    }

    fun viewCertificateTappedEvent() {
        analytics.logEvent(
            CourseAnalyticsEvent.VIEW_CERTIFICATE.eventName,
            buildMap {
                put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.VIEW_CERTIFICATE.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
            }
        )
    }

    private fun resumeCourseTappedEvent(blockId: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.RESUME_COURSE_CLICKED.eventName,
                buildMap {
                    put(
                        CourseAnalyticsKey.NAME.key,
                        CourseAnalyticsEvent.RESUME_COURSE_CLICKED.biValue
                    )
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                }
            )
        }
    }

    fun sequentialClickedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
            analytics.sequentialClickedEvent(
                courseId,
                currentState.courseStructure.name,
                blockId,
                blockName
            )
        }
    }

    fun logUnitDetailViewedEvent(blockId: String, blockName: String) {
        val currentState = uiState.value
        if (currentState is CourseOutlineUIState.CourseData) {
            analytics.logEvent(
                CourseAnalyticsEvent.UNIT_DETAIL.eventName,
                buildMap {
                    put(CourseAnalyticsKey.NAME.key, CourseAnalyticsEvent.UNIT_DETAIL.biValue)
                    put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                    put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                    put(CourseAnalyticsKey.BLOCK_ID.key, blockId)
                    put(CourseAnalyticsKey.BLOCK_NAME.key, blockName)
                }
            )
        }
    }

    private fun checkIfCalendarOutOfDate(courseDates: List<CourseDateBlock>) {
        viewModelScope.launch {
            courseNotifier.send(
                CreateCalendarSyncEvent(
                    courseDates = courseDates,
                    dialogType = CalendarSyncDialogType.NONE.name,
                    checkOutOfSync = true,
                )
            )
        }
    }

    fun downloadBlocks(
        blocksIds: List<String>,
        fragmentManager: FragmentManager,
        context: Context,
    ) {
        if (blocksIds.find { isBlockDownloading(it) } != null) {
            courseRouter.navigateToDownloadQueue(fm = fragmentManager)
            return
        }
        blocksIds.forEach { blockId ->
            if (isBlockDownloaded(blockId)) {
                removeDownloadModels(blockId)
            } else {
                saveDownloadModels(
                    FileUtil(context).getExternalAppDir().path, blockId
                )
            }
        }
    }

    private fun logPLSBannerEvents(event: CourseAnalyticsEvent) {
        analytics.logEvent(
            event.eventName,
            buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(CourseAnalyticsKey.BANNER_TYPE.key, CourseBannerType.RESET_DATES.name)
                put(CourseAnalyticsKey.SCREEN_NAME.key, CourseAnalyticsKey.COURSE_DASHBOARD.key)
            }
        )
    }

    companion object {
        private const val TAG = "CourseOutlineViewModel"
    }
}
