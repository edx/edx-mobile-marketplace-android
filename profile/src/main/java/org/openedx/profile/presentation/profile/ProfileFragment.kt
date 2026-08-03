package org.openedx.profile.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.profile.presentation.profile.compose.ProfileView
import org.openedx.profile.presentation.profile.compose.ProfileViewAction

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)
                val lifecycleOwner = LocalLifecycleOwner.current
                val bannerVisibilityState = androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(false)
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            bannerVisibilityState.value = viewModel.isSubscriptionBannerVisible()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                ProfileView(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    isSubscriptionBannerVisible = bannerVisibilityState.value,
                    viewModel.subscriptionBannerUrl,
                    onSettingsClick = {
                        viewModel.profileRouter.navigateToSettings(requireActivity().supportFragmentManager)
                    },
                    onAction = { action ->
                        when (action) {
                            ProfileViewAction.EditAccountClick -> {
                                viewModel.profileEditClicked(
                                    requireParentFragment().parentFragmentManager
                                )
                            }
                            ProfileViewAction.SwipeRefresh -> {
                                viewModel.updateAccount()
                            }
                            ProfileViewAction.DismissSubscriptionBanner -> {
                                viewModel.dismissSubscriptionBanner()
                                bannerVisibilityState.value = false
                            }
                        }
                    }
                )
            }
        }
    }
}
