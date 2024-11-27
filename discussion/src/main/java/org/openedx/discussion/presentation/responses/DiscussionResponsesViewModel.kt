package org.openedx.discussion.presentation.responses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.BaseDiscussionViewModel
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionAnalyticsType
import org.openedx.discussion.system.notifier.DiscussionCommentDataChanged
import org.openedx.discussion.system.notifier.DiscussionNotifier

class DiscussionResponsesViewModel(
    val courseId: String,
    val threadId: String,
    private var comment: DiscussionComment,
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    private val analytics: DiscussionAnalytics,
) : BaseDiscussionViewModel(courseId, threadId, analytics) {

    private val _uiState = MutableLiveData<DiscussionResponsesUIState>()
    val uiState: LiveData<DiscussionResponsesUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    var isThreadClosed: Boolean = false

    private val comments = mutableListOf<DiscussionComment>()
    private var page = 1
    private var isLoading = false

    private suspend fun sendUpdatedComment() {
        notifier.send(DiscussionCommentDataChanged(comment))
    }

    init {
        loadCommentResponses()
    }

    private fun loadCommentResponses() {
        _uiState.value = DiscussionResponsesUIState.Loading
        loadCommentsInternal()
    }

    fun updateCommentResponses() {
        _isUpdating.value = true
        page = 1
        comments.clear()
        loadCommentsInternal()
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            loadCommentsInternal()
        }
    }

    private fun loadCommentsInternal() {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = interactor.getCommentsResponses(comment.id, page)
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                comments.addAll(response.results)
                _uiState.value = DiscussionResponsesUIState.Success(comment, comments.toList())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                isLoading = false
                _isUpdating.value = false
            }
        }
    }

    fun setCommentUpvoted(commentId: String, vote: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setCommentVoted(commentId, vote)
                val index = comments.indexOfFirst {
                    it.id == response.id
                }
                if (index != -1) {
                    comments[index] =
                        comments[index].copy(voted = response.voted, voteCount = response.voteCount)
                    logLikeToggleEvent(
                        responseId = response.id,
                        commentId = comment.id,
                        discussionType = DiscussionAnalyticsType.COMMENT.value,
                        likePost = vote,
                        author = comment.author,
                    )
                } else {
                    comment = comment.copy(voted = response.voted, voteCount = response.voteCount)
                    sendUpdatedComment()
                    logLikeToggleEvent(
                        responseId = response.id,
                        discussionType = DiscussionAnalyticsType.RESPONSE.value,
                        likePost = vote,
                        author = response.author,
                    )
                }
                _uiState.value = DiscussionResponsesUIState.Success(comment, comments.toList())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun setCommentReported(commentId: String, vote: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setCommentFlagged(commentId, vote)
                val index = comments.indexOfFirst {
                    it.id == response.id
                }
                if (index != -1) {
                    comments[index] = comments[index].copy(abuseFlagged = response.abuseFlagged)
                    logReportToggleEvent(
                        responseId = response.id,
                        commentId = comment.id,
                        discussionType = DiscussionAnalyticsType.COMMENT.value,
                        reportPost = vote,
                        author = comment.author,
                    )
                } else {
                    comment = comment.copy(abuseFlagged = response.abuseFlagged)
                    sendUpdatedComment()
                    logReportToggleEvent(
                        responseId = response.id,
                        discussionType = DiscussionAnalyticsType.RESPONSE.value,
                        reportPost = vote,
                        author = response.author,
                    )
                }
                _uiState.value = DiscussionResponsesUIState.Success(comment, comments.toList())
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun createComment(rawBody: String) {
        viewModelScope.launch {
            try {
                val response = interactor.createComment(comment.threadId, rawBody, comment.id)
                comment = comment.copy(childCount = comment.childCount + 1)
                sendUpdatedComment()
                if (page == -1) {
                    comments.add(response)
                } else {
                    _uiMessage.value =
                        UIMessage.ToastMessage(resourceManager.getString(org.openedx.discussion.R.string.discussion_comment_added))
                }
                _uiState.value =
                    DiscussionResponsesUIState.Success(comment, comments.toList())
                logCommentAddedEvent(
                    responseId = response.id,
                    commentId = comment.id,
                    author = response.author,
                )
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

}