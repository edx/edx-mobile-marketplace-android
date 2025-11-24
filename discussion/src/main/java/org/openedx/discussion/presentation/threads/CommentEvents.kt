package org.openedx.discussion.presentation.threads

import kotlinx.coroutines.flow.MutableSharedFlow

object CommentEvents {
    val commentAdded = MutableSharedFlow<Unit>(
        replay = 1,                 // makes AllPosts catch the event when returning
        extraBufferCapacity = 1     // event won't be lost
    )
}