package org.openedx.discussion.presentation.responses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.RecaptchaManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.Logger
import org.openedx.discussion.R
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.BaseDiscussionViewModel
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionAnalyticsType
import org.openedx.discussion.system.notifier.DiscussionCommentDataChanged
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionResponseAdded
import org.openedx.core.R as CoreR

class DiscussionResponsesViewModel(
    val courseId: String,
    val threadId: String,
    val isPostingEnabled: Boolean,
    private var comment: DiscussionComment,
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    private val corePreferences: CorePreferences,
    analytics: DiscussionAnalytics,
) : BaseDiscussionViewModel(courseId, threadId, analytics) {

    private val logger = Logger(TAG)

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
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
//     var isLoading = false

    private suspend fun sendUpdatedComment() {
        notifier.send(DiscussionCommentDataChanged(comment))
    }

    init {
        loadCommentResponses()
        logResponseScreenEvent(threadId = threadId, responseId = comment.id)
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
        if (_isLoading.value != true && page != -1) {
            loadCommentsInternal()
        }
    }

    private fun loadCommentsInternal() {
        viewModelScope.launch {
            try {
               // _isLoading.postValue(true)
                val response = interactor.getCommentsResponses(comment.id, page)
                if (response.pagination.next.isNotEmpty()) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                comments.addAll(response.results.map {
                    it.copy(isAuthor = it.author == corePreferences.user?.username)
                })
                _uiState.value = DiscussionResponsesUIState.Success(comment, comments.toList())
            } catch (e: Exception) {
                handleException(e)
            } finally {
              //  _isLoading.postValue(false)
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
                handleException(e)
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
                handleException(e)
            }
        }
    }

    fun createComment(rawBody: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val reCaptchaToken = interactor.getRecaptchaToken(
                    courseId = courseId,
                    recaptchaAction = RecaptchaManager.RecaptchaActionComment
                )
                val response = interactor.createComment(
                    threadId = comment.threadId,
                    rawBody = rawBody,
                    parentId = comment.id,
                    captchaToken = reCaptchaToken,
                )
                response.isAuthor = response.author == corePreferences.user?.username

                comment = comment.copy(childCount = comment.childCount + 1)
                sendUpdatedComment()

                comments.add(0, response)
                _uiState.value =
                    DiscussionResponsesUIState.Success(comment, comments.toList())
                logCommentAddedEvent(
                    responseId = response.id,
                    commentId = comment.id,
                    author = response.author,
                )

                notifier.send(DiscussionResponseAdded())
            } catch (e: Exception) {
                handleException(e)
            }finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun handleException(e: Exception) {
        logger.e(throwable = e)
        if (e.isInternetError()) {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(CoreR.string.core_error_no_connection))
        } else {
            _uiMessage.value =
                UIMessage.SnackBarMessage(resourceManager.getString(R.string.discussion_something_went_wrong_error))
        }
    }

    companion object {
        private const val TAG = "DiscussionResponsesViewModel"
    }
}
