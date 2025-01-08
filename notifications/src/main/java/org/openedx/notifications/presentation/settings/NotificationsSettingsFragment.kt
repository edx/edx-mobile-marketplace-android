package org.openedx.notifications.presentation.settings

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.Toolbar
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.WindowType
import org.openedx.core.ui.displayCutoutForLandscape
import org.openedx.core.ui.noRippleClickable
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.settingsHeaderBackground
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.notifications.R
import org.openedx.notifications.domain.model.NotificationsConfiguration

class NotificationsSettingsFragment : Fragment() {

    private val viewModel by viewModel<NotificationsSettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val notificationsConfiguration by viewModel.notificationsConfiguration.collectAsState()

                NotificationsSettingsScreen(
                    windowSize = windowSize,
                    notificationsConfiguration = notificationsConfiguration,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    discussionPreferenceChanged = {
                        viewModel.setDiscussionNotificationPreference(it)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NotificationsSettingsScreen(
    windowSize: WindowSize,
    notificationsConfiguration: NotificationsConfiguration,
    discussionPreferenceChanged: (Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        scaffoldState = scaffoldState
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 420.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            )
        }

        val topBarWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier
                        .fillMaxWidth()
                )
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .settingsHeaderBackground()
                    .statusBarsInset(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Toolbar(
                    modifier = topBarWidth
                        .displayCutoutForLandscape(),
                    label = stringResource(id = R.string.notification_push_notifications),
                    canShowBackBtn = true,
                    labelTint = MaterialTheme.appColors.settingsTitleContent,
                    iconTint = MaterialTheme.appColors.settingsTitleContent,
                    onBackClick = onBackClick
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.appShapes.screenBackgroundShape)
                        .background(MaterialTheme.appColors.background)
                        .displayCutoutForLandscape(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = contentWidth
                    ) {
                        Row(
                            Modifier
                                .testTag("btn_discussions_activity")
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .noRippleClickable {
                                    discussionPreferenceChanged(
                                        notificationsConfiguration.discussionsPushEnabled.not()
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    modifier = Modifier.testTag("txt_discussions_activity_label"),
                                    text = stringResource(id = R.string.notification_discussions_activity),
                                    color = MaterialTheme.appColors.textPrimary,
                                    style = MaterialTheme.appTypography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    modifier = Modifier.testTag("txt_discussions_activity_description"),
                                    text = stringResource(id = R.string.notification_discussions_activity_description),
                                    color = MaterialTheme.appColors.textSecondary,
                                    style = MaterialTheme.appTypography.labelMedium
                                )
                            }
                            Switch(
                                modifier = Modifier.testTag("sw_discussions_activity"),
                                checked = notificationsConfiguration.discussionsPushEnabled,
                                onCheckedChange = {
                                    discussionPreferenceChanged(
                                        notificationsConfiguration.discussionsPushEnabled.not()
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.appColors.primary,
                                    checkedTrackColor = MaterialTheme.appColors.primary,
                                    uncheckedThumbColor = MaterialTheme.appColors.cardViewBorder,
                                    uncheckedTrackColor = MaterialTheme.appColors.cardViewBorder,
                                )
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsSettingsScreenPreview() {
    OpenEdXTheme {
        NotificationsSettingsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            discussionPreferenceChanged = {},
            onBackClick = {},
            notificationsConfiguration = NotificationsConfiguration(
                discussionsPushEnabled = false,
            )
        )
    }
}
