package org.openedx.profile.presentation.profile.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.openedx.core.UIMessage
import org.openedx.core.ui.HandleUIMessage
import org.openedx.core.ui.OpenEdXOutlinePrimaryButton
import org.openedx.core.ui.SubscriptionBanner
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.windowSizeValue
import org.openedx.profile.R
import org.openedx.profile.presentation.profile.ProfileUIState
import org.openedx.profile.presentation.ui.ProfileInfoSection
import org.openedx.profile.presentation.ui.ProfileTopic
import org.openedx.profile.presentation.ui.mockAccount
import org.openedx.core.R as CoreR

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ProfileView(
    windowSize: WindowSize,
    uiState: ProfileUIState,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    isSubscriptionBannerVisible: Boolean,
    onAction: (ProfileViewAction) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { onAction(ProfileViewAction.SwipeRefresh) }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState
    ) { paddingValues ->

        val contentWidth = remember(windowSize) {
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                compact = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset()
                .displayCutoutForLandscape()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Toolbar(
                    label = stringResource(id = CoreR.string.core_profile),
                    canShowSettingsIcon = true,
                    onSettingsClick = onSettingsClick
                )

                Surface(color = MaterialTheme.appColors.background) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (uiState is ProfileUIState.Data) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(contentWidth)
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = if (isSubscriptionBannerVisible) 72.dp else 0.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                SubscriptionBanner(
                                    visible = isSubscriptionBannerVisible,
                                    onDismiss = { onAction(ProfileViewAction.DismissSubscriptionBanner) }
                                )
                                ProfileTopic(
                                    image = uiState.account.profileImage.imageUrlFull,
                                    title = uiState.account.name,
                                    subtitle = "@${uiState.account.username}"
                                )

                                ProfileInfoSection(uiState.account)

                                OpenEdXOutlinePrimaryButton(
                                    text = stringResource(R.string.profile_edit_profile),
                                    onClick = { onAction(ProfileViewAction.EditAccountClick) }
                                )
//                                SubscriptionBanner(
//                                    visible = isSubscriptionBannerVisible,
//                                    onDismiss = { onAction(ProfileViewAction.DismissSubscriptionBanner) }
//                                )
                            }
                        }

                        PullRefreshIndicator(
                            refreshing,
                            pullRefreshState,
                            Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }

//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopCenter)
//                    .padding(start = 24.dp, top = 80.dp, end = 24.dp)
//                    .zIndex(1f)
//            ) {
//                SubscriptionBanner(
//                    visible = isSubscriptionBannerVisible,
//                    onDismiss = { onAction(ProfileViewAction.DismissSubscriptionBanner) }
//                )
//            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            isSubscriptionBannerVisible = true,
            onAction = {},
            onSettingsClick = {},
        )
    }
}


@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfileScreenTabletPreview() {
    OpenEdXTheme {
        ProfileView(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = mockUiState,
            uiMessage = null,
            refreshing = false,
            isSubscriptionBannerVisible = true,
            onAction = {},
            onSettingsClick = {},
        )
    }
}

private val mockUiState = ProfileUIState.Data(
    account = mockAccount
)

internal interface ProfileViewAction {
    object EditAccountClick : ProfileViewAction
    object SwipeRefresh : ProfileViewAction
    object DismissSubscriptionBanner : ProfileViewAction
}
