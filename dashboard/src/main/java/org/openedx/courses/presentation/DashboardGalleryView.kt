package org.openedx.courses.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseAssignments
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.CourseDatesCalendarSync
import org.openedx.core.domain.model.CourseEnrollments
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CourseStatus
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.DashboardCourseList
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.domain.model.Pagination
import org.openedx.core.domain.model.Progress
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OfflineModeDialog
import org.openedx.core.ui.OpenEdXBrandButton
import org.openedx.core.ui.PurchasesFulfillmentCompletedDialog
import org.openedx.core.ui.TextIcon
import org.openedx.core.ui.UpgradeErrorDialog
import org.openedx.core.ui.UpgradeToAccessView
import org.openedx.core.ui.UpgradeToAccessViewType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.utils.TimeUtils
import org.openedx.dashboard.R
import java.util.Date
import org.openedx.core.R as CoreR

@Composable
fun DashboardGalleryView(
    fragmentManager: FragmentManager,
) {
    val windowSize = rememberWindowSize()
    val viewModel: DashboardGalleryViewModel = koinViewModel { parametersOf(windowSize) }
    val updating by viewModel.updating.collectAsState(false)
    val uiMessage by viewModel.uiMessage.collectAsState(null)
    val uiState by viewModel.uiState.collectAsState(DashboardGalleryUIState.Loading)
    val iapUiState by viewModel.iapUiState.collectAsState(IAPUIState.Clear)

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.updateCourses(isUpdating = false)
        }
    }

    DashboardGalleryView(
        uiMessage = uiMessage,
        uiState = uiState,
        iapUiState = iapUiState,
        updating = updating,
        apiHostUrl = viewModel.apiHostUrl,
        hasInternetConnection = viewModel.hasInternetConnection,
        onAction = { action ->
            when (action) {
                DashboardGalleryScreenAction.SwipeRefresh -> {
                    viewModel.updateCourses()
                    viewModel.refreshPushBadgeCount()
                }

                DashboardGalleryScreenAction.ViewAll -> {
                    viewModel.navigateToAllEnrolledCourses(fragmentManager)
                }

                DashboardGalleryScreenAction.Reload -> {
                    viewModel.getCourses()
                }

                DashboardGalleryScreenAction.NavigateToDiscovery -> {
                    viewModel.navigateToDiscovery()
                }

                is DashboardGalleryScreenAction.OpenCourse -> {
                    viewModel.navigateToCourseOutline(
                        fragmentManager = fragmentManager,
                        enrolledCourse = action.enrolledCourse
                    )
                }

                is DashboardGalleryScreenAction.NavigateToDates -> {
                    viewModel.navigateToCourseOutline(
                        fragmentManager = fragmentManager,
                        enrolledCourse = action.enrolledCourse,
                        openDates = true
                    )
                }

                is DashboardGalleryScreenAction.OpenBlock -> {
                    viewModel.navigateToCourseOutline(
                        fragmentManager = fragmentManager,
                        enrolledCourse = action.enrolledCourse,
                        resumeBlockId = action.blockId
                    )
                }
            }
        },
        onIAPAction = { action, course, iapException ->
            viewModel.processIAPAction(
                fragmentManager, action, course, iapException
            )
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DashboardGalleryView(
    uiMessage: UIMessage?,
    uiState: DashboardGalleryUIState,
    iapUiState: IAPUIState?,
    updating: Boolean,
    apiHostUrl: String,
    onAction: (DashboardGalleryScreenAction) -> Unit,
    onIAPAction: (IAPAction, EnrolledCourse?, IAPException?) -> Unit = { _, _, _ -> },
    hasInternetConnection: Boolean
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = updating,
        onRefresh = { onAction(DashboardGalleryScreenAction.SwipeRefresh) }
    )
    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutForLandscape()
                .padding(paddingValues),
            color = MaterialTheme.appColors.background
        ) {
            Box(
                Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (uiState) {
                        is DashboardGalleryUIState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.appColors.primary
                            )
                        }

                        is DashboardGalleryUIState.Courses -> {
                            UserCourses(
                                modifier = Modifier.fillMaxSize(),
                                userCourses = uiState.userCourses,
                                apiHostUrl = apiHostUrl,
                                openCourse = {
                                    onAction(DashboardGalleryScreenAction.OpenCourse(it))
                                },
                                onViewAllClick = {
                                    onAction(DashboardGalleryScreenAction.ViewAll)
                                },
                                navigateToDates = {
                                    onAction(DashboardGalleryScreenAction.NavigateToDates(it))
                                },
                                resumeBlockId = { course, blockId ->
                                    onAction(
                                        DashboardGalleryScreenAction.OpenBlock(
                                            course,
                                            blockId
                                        )
                                    )
                                },
                                onIAPAction = onIAPAction,
                            )
                            LaunchedEffect(uiState.userCourses.enrollments.courses) {
                                if (uiState.userCourses.enrollments.courses.isNotEmpty()) {
                                    onIAPAction(IAPAction.ACTION_UNFULFILLED, null, null)
                                }
                            }
                        }

                        is DashboardGalleryUIState.Empty -> {
                            NoCoursesInfo(
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                            FindACourseButton(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter),
                                findACourseClick = {
                                    onAction(DashboardGalleryScreenAction.NavigateToDiscovery)
                                }
                            )
                        }
                    }

                    PullRefreshIndicator(
                        updating,
                        pullRefreshState,
                        Modifier.align(Alignment.TopCenter)
                    )
                }
                if (!isInternetConnectionShown && !hasInternetConnection) {
                    OfflineModeDialog(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        onDismissCLick = {
                            isInternetConnectionShown = true
                        },
                        onReloadClick = {
                            isInternetConnectionShown = true
                            onAction(DashboardGalleryScreenAction.SwipeRefresh)
                        }
                    )
                }
                when (iapUiState) {
                    is IAPUIState.PurchasesFulfillmentCompleted -> {
                        PurchasesFulfillmentCompletedDialog(onConfirm = {
                            onIAPAction(IAPAction.ACTION_COMPLETION, null, null)
                        }, onDismiss = {
                            onIAPAction(IAPAction.ACTION_CLOSE, null, null)
                        })
                    }

                    is IAPUIState.Error -> {
                        UpgradeErrorDialog(
                            title = stringResource(id = CoreR.string.iap_error_title),
                            description = stringResource(id = CoreR.string.iap_course_not_fullfilled),
                            confirmText = stringResource(id = CoreR.string.core_cancel),
                            onConfirm = {
                                onIAPAction(
                                    IAPAction.ACTION_ERROR_CLOSE,
                                    null,
                                    null
                                )
                            },
                            dismissText = stringResource(id = CoreR.string.iap_get_help),
                            onDismiss = {
                                onIAPAction(
                                    IAPAction.ACTION_GET_HELP,
                                    null,
                                    iapUiState.iapException
                                )
                            }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun UserCourses(
    modifier: Modifier = Modifier,
    userCourses: CourseEnrollments,
    apiHostUrl: String,
    openCourse: (EnrolledCourse) -> Unit,
    navigateToDates: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit,
    resumeBlockId: (enrolledCourse: EnrolledCourse, blockId: String) -> Unit,
    onIAPAction: (IAPAction, EnrolledCourse?, IAPException?) -> Unit = { _, _, _ -> },
) {
    Column(
        modifier = modifier
            .padding(vertical = 12.dp)
    ) {
        val primaryCourse = userCourses.primary
        if (primaryCourse != null) {
            PrimaryCourseCard(
                isIAPEnabled = userCourses.configs.iapConfig.isEnabled,
                primaryCourse = primaryCourse,
                apiHostUrl = apiHostUrl,
                navigateToDates = navigateToDates,
                resumeBlockId = resumeBlockId,
                openCourse = openCourse,
                onIAPAction = onIAPAction,
            )
        }
        if (userCourses.enrollments.courses.isNotEmpty()) {
            SecondaryCourses(
                courses = userCourses.enrollments.courses,
                courseCount = userCourses.enrollments.pagination.count,
                hasNextPage = userCourses.enrollments.pagination.next.isNotEmpty(),
                apiHostUrl = apiHostUrl,
                onCourseClick = openCourse,
                onViewAllClick = onViewAllClick
            )
        }
    }
}

@Composable
private fun SecondaryCourses(
    courses: List<EnrolledCourse>,
    courseCount: Int,
    hasNextPage: Boolean,
    apiHostUrl: String,
    onCourseClick: (EnrolledCourse) -> Unit,
    onViewAllClick: () -> Unit
) {
    val windowSize = rememberWindowSize()
    val itemsCount = if (windowSize.isTablet) 7 else 5
    val rows = if (windowSize.isTablet) 2 else 1
    val height = if (windowSize.isTablet) 322.dp else 152.dp
    val items = courses.take(itemsCount)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextIcon(
            modifier = Modifier.padding(horizontal = 18.dp),
            text = stringResource(R.string.dashboard_view_all_with_count, courseCount + 1),
            textStyle = MaterialTheme.appTypography.titleSmall,
            icon = Icons.Default.ChevronRight,
            color = MaterialTheme.appColors.textDark,
            iconModifier = Modifier.size(22.dp),
            onClick = onViewAllClick
        )
        LazyHorizontalGrid(
            modifier = Modifier
                .fillMaxSize()
                .height(height),
            rows = GridCells.Fixed(rows),
            contentPadding = PaddingValues(horizontal = 18.dp),
            content = {
                items(items) {
                    CourseListItem(
                        course = it,
                        apiHostUrl = apiHostUrl,
                        onCourseClick = onCourseClick
                    )
                }
                if (hasNextPage) {
                    item {
                        ViewAllItem(
                            onViewAllClick = onViewAllClick
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ViewAllItem(
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(152.dp)
            .padding(4.dp)
            .clickable(
                onClickLabel = stringResource(id = R.string.dashboard_view_all),
                onClick = {
                    onViewAllClick()
                }
            ),
        backgroundColor = MaterialTheme.appColors.cardViewBackground,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(id = R.drawable.dashboard_ic_book),
                tint = MaterialTheme.appColors.textFieldBorder,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.dashboard_view_all),
                style = MaterialTheme.appTypography.titleSmall,
                color = MaterialTheme.appColors.textDark
            )
        }
    }
}

@Composable
private fun CourseListItem(
    course: EnrolledCourse,
    apiHostUrl: String,
    onCourseClick: (EnrolledCourse) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(152.dp)
            .padding(4.dp)
            .clickable {
                onCourseClick(course)
            },
        backgroundColor = MaterialTheme.appColors.cardViewBackground,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 2.dp
    ) {
        Box {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(apiHostUrl + course.course.courseImage)
                        .error(CoreR.drawable.core_no_image_course)
                        .placeholder(CoreR.drawable.core_no_image_course)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )
                Text(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    text = course.course.name,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textDark,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    minLines = 2
                )
            }
//            if (!course.course.coursewareAccess?.errorCode.isNullOrEmpty()) {
//                Lock()
//            }
        }
    }
}

@Composable
private fun AssignmentItem(
    modifier: Modifier = Modifier,
    painter: Painter,
    title: String?,
    info: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            tint = MaterialTheme.appColors.textDark,
            contentDescription = null
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val infoTextStyle = if (title.isNullOrEmpty()) {
                MaterialTheme.appTypography.titleSmall
            } else {
                MaterialTheme.appTypography.labelSmall
            }
            Text(
                text = info,
                color = MaterialTheme.appColors.textDark,
                style = infoTextStyle
            )
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    color = MaterialTheme.appColors.textDark,
                    style = MaterialTheme.appTypography.titleSmall
                )
            }
        }
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            tint = MaterialTheme.appColors.textDark,
            contentDescription = null
        )
    }
}

@Composable
private fun PrimaryCourseCard(
    isIAPEnabled: Boolean,
    primaryCourse: EnrolledCourse,
    apiHostUrl: String,
    navigateToDates: (EnrolledCourse) -> Unit,
    resumeBlockId: (enrolledCourse: EnrolledCourse, blockId: String) -> Unit,
    openCourse: (EnrolledCourse) -> Unit,
    onIAPAction: (IAPAction, EnrolledCourse?, IAPException?) -> Unit = { _, _, _ -> },
) {
    val orientation = LocalConfiguration.current.orientation

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .padding(2.dp),
        backgroundColor = MaterialTheme.appColors.cardViewBackground,
        shape = MaterialTheme.appShapes.courseImageShape,
        elevation = 2.dp
    ) {
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .clickable {
                            openCourse(primaryCourse)
                        }
                        .height(IntrinsicSize.Min)
                ) {
                    PrimaryCourseCaption(
                        modifier = Modifier.weight(1f),
                        primaryCourse = primaryCourse,
                        apiHostUrl = apiHostUrl,
                        imageHeight = null,
                    )
                    PrimaryCourseButtons(
                        modifier = Modifier.weight(1f),
                        primaryCourse = primaryCourse,
                        navigateToDates = navigateToDates,
                        resumeBlockId = resumeBlockId,
                        openCourse = openCourse,
                        adjustHeight = true,
                        isIAPEnabled = isIAPEnabled,
                        onIAPAction = onIAPAction,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier.clickable {
                        openCourse(primaryCourse)
                    }
                ) {
                    PrimaryCourseCaption(
                        primaryCourse = primaryCourse,
                        apiHostUrl = apiHostUrl,
                    )
                    PrimaryCourseButtons(
                        primaryCourse = primaryCourse,
                        navigateToDates = navigateToDates,
                        resumeBlockId = resumeBlockId,
                        openCourse = openCourse,
                        isIAPEnabled = isIAPEnabled,
                        onIAPAction = onIAPAction,
                    )
                }
            }
        }
    }
}

/**
 * Manages and displays a dynamic list of primary course card buttons for up to four views: Due,
 * Future, Upgrade, and Resume. The views are prioritized in the following order: Due, Future,
 * Upgrade, and Resume.
 *
 * If all four views are active, the Future view is omitted to ensure only three buttons
 * are shown. Unavailable views are automatically excluded from the list, preserving the
 * established priority.
 *
 * Additionally, the visible buttons adhere to a specific color scheme, respecting the
 * priority order of the colors: caution, info, and brand. The brand color is fixed for the
 * Resume view.
 */
@Composable
private fun PrimaryCourseButtons(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse,
    adjustHeight: Boolean = false,
    navigateToDates: (EnrolledCourse) -> Unit,
    resumeBlockId: (enrolledCourse: EnrolledCourse, blockId: String) -> Unit,
    openCourse: (EnrolledCourse) -> Unit,
    isIAPEnabled: Boolean,
    onIAPAction: (IAPAction, EnrolledCourse?, IAPException?) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val viewsList = mutableListOf<@Composable () -> Unit>()

    val pastAssignments = primaryCourse.courseAssignments?.pastAssignments
    if (!pastAssignments.isNullOrEmpty()) {
        viewsList.add {
            val nearestAssignment = pastAssignments.maxBy { it.date }
            val title = if (pastAssignments.size == 1) {
                nearestAssignment.title
            } else {
                stringResource(R.string.dashboard_assignment_due_default)
            }
            AssignmentItem(
                modifier = Modifier
                    .background(MaterialTheme.appColors.primaryCardCautionBackground)
                    .clickable {
                        if (pastAssignments.size == 1) {
                            resumeBlockId(primaryCourse, nearestAssignment.blockId)
                        } else {
                            navigateToDates(primaryCourse)
                        }
                    },
                painter = rememberVectorPainter(Icons.Default.Warning),
                title = title,
                info = pluralStringResource(
                    R.plurals.dashboard_past_due_assignment,
                    pastAssignments.size,
                    pastAssignments.size
                )
            )
        }
    }

    val futureAssignments = primaryCourse.courseAssignments?.futureAssignments
    if (!futureAssignments.isNullOrEmpty()) {
        viewsList.add {
            val nearestAssignment = futureAssignments.minBy { it.date }
            val title = if (futureAssignments.size == 1) nearestAssignment.title else null
            AssignmentItem(
                modifier = Modifier
                    .background(
                        if (!pastAssignments.isNullOrEmpty())
                            MaterialTheme.appColors.primaryCardInfoBackground
                        else
                            MaterialTheme.appColors.primaryCardCautionBackground
                    )
                    .clickable {
                        if (futureAssignments.size == 1) {
                            resumeBlockId(primaryCourse, nearestAssignment.blockId)
                        } else {
                            navigateToDates(primaryCourse)
                        }
                    },
                painter = painterResource(id = CoreR.drawable.ic_core_chapter_icon),
                title = title,
                info = stringResource(
                    R.string.dashboard_assignment_due,
                    nearestAssignment.assignmentType ?: "",
                    TimeUtils.getAssignmentFormattedDate(context, nearestAssignment.date)
                )
            )
        }
    }

    if (primaryCourse.isUpgradeable && isIAPEnabled) {
        viewsList.add {
            UpgradeToAccessView(
                type = UpgradeToAccessViewType.GALLERY,
                iconPadding = PaddingValues(end = 12.dp),
                padding = PaddingValues(vertical = 16.dp, horizontal = 14.dp)
            ) {
                onIAPAction(
                    IAPAction.ACTION_USER_INITIATED,
                    primaryCourse,
                    null
                )
            }
        }
    }

    viewsList.add {
        ResumeButton(
            primaryCourse = primaryCourse,
            onClick = {
                if (primaryCourse.courseStatus == null) {
                    openCourse(primaryCourse)
                } else {
                    resumeBlockId(
                        primaryCourse,
                        primaryCourse.courseStatus?.lastVisitedBlockId ?: ""
                    )
                }
            }
        )
    }

    // Remove Future Assignments if all buttons are available to show a maximum of three buttons.
    if (viewsList.size == 4) {
        viewsList.removeAt(1)
    }

    Column(modifier = modifier) {
        var titleModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 16.dp)
        if (adjustHeight) {
            titleModifier = titleModifier.weight(1f)
        }

        PrimaryCourseTitle(
            modifier = titleModifier,
            primaryCourse = primaryCourse,
        )
        viewsList.forEach { view ->
            Divider()
            view()
        }
    }
}

@Composable
private fun PrimaryCourseCaption(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse,
    imageHeight: Dp? = 140.dp,
    apiHostUrl: String,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        val imageModifier = imageHeight?.let {
            Modifier
                .height(it)
                .fillMaxWidth()
        } ?: Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .weight(1f)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(apiHostUrl + primaryCourse.course.courseImage)
                .error(CoreR.drawable.core_no_image_course)
                .placeholder(CoreR.drawable.core_no_image_course)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            progress = primaryCourse.progress.value,
            color = MaterialTheme.appColors.progressBarColor,
            backgroundColor = MaterialTheme.appColors.progressBarBackgroundColor
        )
    }
}

@Composable
private fun ResumeButton(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .heightIn(min = 60.dp)
            .background(MaterialTheme.appColors.primaryButtonBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (primaryCourse.courseStatus == null) {
            Icon(
                imageVector = Icons.Default.School,
                tint = MaterialTheme.appColors.primaryButtonText,
                contentDescription = null
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.dashboard_start_course),
                color = MaterialTheme.appColors.primaryButtonText,
                style = MaterialTheme.appTypography.titleSmall
            )
        } else {
            Icon(
                imageVector = Icons.Default.School,
                tint = MaterialTheme.appColors.primaryButtonText,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_resume_course),
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.labelSmall
                )
                Text(
                    text = primaryCourse.courseStatus?.lastVisitedUnitDisplayName ?: "",
                    color = MaterialTheme.appColors.primaryButtonText,
                    style = MaterialTheme.appTypography.titleSmall
                )
            }
        }
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            tint = MaterialTheme.appColors.primaryButtonText,
            contentDescription = null
        )
    }
}

@Composable
private fun PrimaryCourseTitle(
    modifier: Modifier = Modifier,
    primaryCourse: EnrolledCourse
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = primaryCourse.course.org,
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textFieldHint
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = primaryCourse.course.name,
            style = MaterialTheme.appTypography.titleLarge,
            color = MaterialTheme.appColors.textDark,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textFieldHint,
            text = TimeUtils.getCourseFormattedDate(
                LocalContext.current,
                Date(),
                primaryCourse.auditAccessExpires,
                primaryCourse.course.start,
                primaryCourse.course.end,
                primaryCourse.course.startType,
                primaryCourse.course.startDisplay
            )
        )
    }
}

@Composable
private fun FindACourseButton(
    modifier: Modifier = Modifier,
    findACourseClick: () -> Unit
) {
    OpenEdXBrandButton(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 20.dp),
        text = stringResource(id = R.string.dashboard_find_a_course),
        onClick = {
            findACourseClick()
        }
    )
}

@Composable
private fun NoCoursesInfo(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dashboard_ic_book),
                tint = MaterialTheme.appColors.textFieldBorder,
                contentDescription = null
            )
            Spacer(Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_title")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dashboard_all_courses_empty_title),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                modifier = Modifier
                    .testTag("txt_empty_state_description")
                    .fillMaxWidth(),
                text = stringResource(id = R.string.dashboard_all_courses_empty_description),
                color = MaterialTheme.appColors.textDark,
                style = MaterialTheme.appTypography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val mockCourseDateBlock = CourseDateBlock(
    title = "Homework 1: ABCD",
    description = "After this date, course content will be archived",
    date = TimeUtils.iso8601ToDate("2024-05-31T15:08:07Z")!!,
    assignmentType = "Homework"
)
private val mockCourseAssignments =
    CourseAssignments(listOf(mockCourseDateBlock), listOf(mockCourseDateBlock, mockCourseDateBlock))
private val mockCourse = EnrolledCourse(
    auditAccessExpires = Date(),
    created = "created",
    certificate = Certificate(""),
    mode = "mode",
    isActive = true,
    progress = Progress(4, 10),
    courseStatus = CourseStatus("", emptyList(), "", "Unit name"),
    courseAssignments = mockCourseAssignments,
    course = EnrolledCourseData(
        id = "id",
        name = "Looooooooooooooooooooong Course name",
        number = "",
        org = "Org",
        start = Date(),
        startDisplay = "",
        startType = "",
        end = Date(),
        upgradeDeadline = "",
        subscriptionId = "",
        coursewareAccess = CoursewareAccess(
            true,
            "",
            "",
            "",
            "",
            "",
        ),
        media = null,
        courseImage = "",
        courseAbout = "",
        courseSharingUtmParameters = CourseSharingUtmParameters("", ""),
        courseUpdates = "",
        courseHandouts = "",
        discussionUrl = "",
        videoOutline = "",
        isSelfPaced = false
    ),
    productInfo = null
)

private val mockPagination = Pagination(10, "", 4, "1")
private val mockDashboardCourseList = DashboardCourseList(
    pagination = mockPagination,
    courses = listOf(mockCourse, mockCourse, mockCourse, mockCourse, mockCourse, mockCourse)
)

private val mockUserCourses = CourseEnrollments(
    enrollments = mockDashboardCourseList,
    configs = AppConfig(CourseDatesCalendarSync(true, true, true, true)),
    primary = mockCourse
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ViewAllItemPreview() {
    OpenEdXTheme {
        ViewAllItem(
            onViewAllClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun DashboardGalleryViewPreview() {
    OpenEdXTheme {
        DashboardGalleryView(
            uiState = DashboardGalleryUIState.Courses(mockUserCourses),
            iapUiState = null,
            apiHostUrl = "",
            uiMessage = null,
            updating = false,
            hasInternetConnection = false,
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun NoCoursesInfoPreview() {
    OpenEdXTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
        ) {
            NoCoursesInfo(modifier = Modifier.align(Alignment.Center))
            FindACourseButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                findACourseClick = {})
        }
    }
}
