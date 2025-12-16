package org.openedx.downloads.presentation.download

import org.openedx.core.domain.model.DownloadCoursePreview
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState

data class DownloadsUIState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val downloadCoursePreviews: List<DownloadCoursePreview> = emptyList(),
    val downloadModels: List<DownloadModel> = emptyList(),
    val courseDownloadState: Map<String, DownloadedState> = emptyMap(),
)
