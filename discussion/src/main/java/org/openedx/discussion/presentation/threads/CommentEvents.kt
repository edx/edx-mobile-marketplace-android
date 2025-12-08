import kotlinx.coroutines.flow.MutableSharedFlow

object CommentEvents {
    val commentAdded = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1
    )
}