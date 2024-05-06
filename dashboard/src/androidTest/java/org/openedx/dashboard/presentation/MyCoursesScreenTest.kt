package org.openedx.dashboard.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import org.junit.Rule
import org.junit.Test
import org.openedx.core.AppUpdateState
import org.openedx.core.domain.model.Certificate
import org.openedx.core.domain.model.CourseSharingUtmParameters
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrolledCourseData
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import java.util.Date

class MyCoursesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    //region mockEnrolledCourse
    private val mockCourseEnrolled = EnrolledCourse(
        auditAccessExpires = null,
        created = "created",
        certificate = Certificate(""),
        mode = "mode",
        isActive = true,
        course = EnrolledCourseData(
            id = "id",
            name = "name",
            number = "",
            org = "Org",
            start = Date(),
            startDisplay = "",
            startType = "",
            end = null,
            dynamicUpgradeDeadline = "",
            subscriptionId = "",
            coursewareAccess = CoursewareAccess(
                true,
                "",
                "",
                "",
                "",
                ""
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
    //endregion

    @Test
    fun dashboardScreenLoading() {
        composeTestRule.setContent {
            DashboardListView(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                apiHostUrl = "http://localhost:8000",
                state = DashboardUIState.Courses(
                    listOf(mockCourseEnrolled, mockCourseEnrolled),
                    false
                ),
                uiMessage = null,
                canLoadMore = false,
                refreshing = false,
                hasInternetConnection = true,
                onReloadClick = {},
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {},
                appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
                onSettingsClick = {},
                iapCallback = { _, _ -> },
                onGetHelp = {},
                iapState = IAPUIState.Clear,
            )
        }

        with(composeTestRule) {
            onNode(
                hasProgressBarRangeInfo(
                    ProgressBarRangeInfo(
                        current = 0f,
                        range = 0f..0f,
                        steps = 0
                    )
                )
            )
        }
    }

    @Test
    fun dashboardScreenLoaded() {
        composeTestRule.setContent {
            DashboardListView(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                apiHostUrl = "http://localhost:8000",
                state = DashboardUIState.Courses(
                    listOf(mockCourseEnrolled, mockCourseEnrolled),
                    false
                ),
                uiMessage = null,
                canLoadMore = false,
                refreshing = false,
                hasInternetConnection = true,
                onReloadClick = {},
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {},
                appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
                onSettingsClick = {},
                iapCallback = { _, _ -> },
                onGetHelp = {},
                iapState = IAPUIState.Clear,
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren()
                .assertAny(hasText(mockCourseEnrolled.course.name))
        }
    }

    @Test
    fun dashboardScreenRefreshing() {
        composeTestRule.setContent {
            DashboardListView(
                windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
                apiHostUrl = "http://localhost:8000",
                state = DashboardUIState.Courses(
                    listOf(mockCourseEnrolled, mockCourseEnrolled),
                    false
                ),
                uiMessage = null,
                canLoadMore = false,
                refreshing = true,
                hasInternetConnection = true,
                onReloadClick = {},
                onSwipeRefresh = {},
                paginationCallback = {},
                onItemClick = {},
                appUpgradeParameters = AppUpdateState.AppUpgradeParameters(),
                onSettingsClick = {},
                iapCallback = { _, _ -> },
                onGetHelp = {},
                iapState = IAPUIState.Clear,
            )
        }

        with(composeTestRule) {
            onNode(hasScrollAction()).onChildren()
                .assertAny(hasText(mockCourseEnrolled.course.name))
            onNode(
                hasScrollAction().and(
                    hasAnyChild(
                        hasProgressBarRangeInfo(
                            ProgressBarRangeInfo(
                                current = 0f,
                                range = 0f..0f,
                                steps = 0
                            )
                        )
                    )
                )
            )
        }
    }
}
