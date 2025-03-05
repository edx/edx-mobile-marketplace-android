package org.openedx.discussion.presentation.threads

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.PushGlobalManager
import org.openedx.core.system.ResourceManager
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.ThreadsData
import org.openedx.discussion.presentation.BaseDiscussionViewModel
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import org.openedx.discussion.system.notifier.DiscussionCommentAdded
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionResponseAdded
import org.openedx.discussion.system.notifier.DiscussionThreadAdded
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.discussion.system.notifier.DiscussionThreadFollowed

class DiscussionThreadsViewModel(
    val courseId: String,
    val topicId: String,
    private val threadType: String,
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    private val pushGlobalManager: PushGlobalManager,
    analytics: DiscussionAnalytics,
) : BaseDiscussionViewModel(courseId, "", analytics) {

    private val _uiState =
        MutableLiveData<DiscussionThreadsUIState>(DiscussionThreadsUIState.Loading)
    val uiState: LiveData<DiscussionThreadsUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _showPrimer = MutableStateFlow(false)
    val showPrimer: StateFlow<Boolean> = _showPrimer.asStateFlow()

    private val threadsList = mutableListOf<org.openedx.discussion.domain.model.Thread>()
    private var nextPage = 1
    private var isLoading = false
    private var lastOrderBy = SortType.LAST_ACTIVITY_AT.queryParam
    private var lastFilterType = FilterType.ALL_POSTS.value

    private var isBlockAlreadyCompleted = false

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is DiscussionThreadAdded) {
                    if (lastOrderBy.isNotEmpty()) {
                        refreshThreads()
                    }
                } else if (it is DiscussionThreadDataChanged) {
                    val index = threadsList.indexOfFirst { thread ->
                        thread.id == it.thread.id
                    }
                    if (index >= 0) {
                        threadsList[index] = it.thread
                        _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
                    }
                }

                when (it) {
                    is DiscussionThreadAdded,
                    is DiscussionCommentAdded,
                    is DiscussionResponseAdded,
                    is DiscussionThreadFollowed -> {
                        _showPrimer.value = true
                    }
                }
            }
        }
    }

    init {
        loadThreads()
        logTopicScreenEvent(topicId)
    }

    private fun loadThreads() {
        viewModelScope.launch {
            try {
                val response = fetchThreads()
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    nextPage++
                } else {
                    _canLoadMore.value = false
                    nextPage = -1
                }
                threadsList.addAll(response.results)
                _uiState.value = DiscussionThreadsUIState.Threads(threadsList.toList())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                _isUpdating.value = false
                isLoading = false
            }
        }
    }

    private suspend fun fetchThreads(): ThreadsData {
        val filterValue = lastFilterType.takeUnless { it == FilterType.ALL_POSTS.value }

        return when (threadType) {
            DiscussionTopicsViewModel.ALL_POSTS -> {
                interactor.getAllThreads(courseId, lastOrderBy, filterValue, nextPage)
            }

            DiscussionTopicsViewModel.FOLLOWING_POSTS -> {
                interactor.getFollowingThreads(courseId, lastOrderBy, nextPage)
            }

            DiscussionTopicsViewModel.TOPIC -> {
                interactor.getThreads(courseId, topicId, lastOrderBy, filterValue, nextPage)
            }

            else -> throw IllegalArgumentException("Invalid thread type")
        }
    }

    fun fetchMore() {
        if (!isLoading && nextPage != -1) {
            isLoading = true
            loadThreads()
        }
    }

    fun refreshThreads() {
        _isUpdating.value = true
        threadsList.clear()
        nextPage = 1
        loadThreads()
    }

    fun sortThreads(orderBy: String) {
        if (lastOrderBy != orderBy) {
            lastOrderBy = orderBy
            threadsList.clear()
            nextPage = 1
            loadThreads()
        }
    }

    fun filterThreads(filter: String) {
        if (lastFilterType != filter) {
            lastFilterType = filter
            threadsList.clear()
            nextPage = 1
            loadThreads()
        }
    }

    fun markBlockCompleted(blockId: String) {
        if (isBlockAlreadyCompleted) return
        viewModelScope.launch {
            try {
                isBlockAlreadyCompleted = true
                interactor.markBlocksCompletion(courseId, listOf(blockId))
            } catch (e: Exception) {
                isBlockAlreadyCompleted = false
                e.printStackTrace()
            }
        }
    }

    fun showNotificationsPrimer(context: Context, fm: FragmentManager) {
        pushGlobalManager.showNotificationsPrimer(context, fm)
    }
}
