package org.openedx.downloads.presentation.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.ui.theme.OpenEdXTheme

class DownloadsFragment : Fragment() {

    private val viewModel by viewModel<DownloadsViewModel>()

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
                val uiState by viewModel.uiState.collectAsState()
                val uiMessage by viewModel.uiMessage.collectAsState(null)
                DownloadsScreen(
                    uiState = uiState,
                    uiMessage = uiMessage,
                    apiHostUrl = viewModel.apiHostUrl,
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onAction = { action ->
                        when (action) {
                            DownloadsViewActions.OpenSettings -> {
                                viewModel.onSettingsClick(requireActivity().supportFragmentManager)
                            }

                            DownloadsViewActions.SwipeRefresh -> {
                                viewModel.refreshData()
                            }

                            is DownloadsViewActions.OpenCourse -> {
                                viewModel.navigateToCourseOutline(
                                    fm = requireActivity().supportFragmentManager,
                                    courseId = action.courseId
                                )
                            }

                            is DownloadsViewActions.DownloadCourse -> {
                                viewModel.downloadCourse(
                                    requireActivity().supportFragmentManager,
                                    action.courseId
                                )
                            }

                            is DownloadsViewActions.CancelDownloading -> {
                                viewModel.cancelDownloading(action.courseId)
                            }

                            is DownloadsViewActions.RemoveDownloads -> {
                                viewModel.removeDownloads(
                                    requireActivity().supportFragmentManager,
                                    action.courseId
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
