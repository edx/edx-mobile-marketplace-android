import android.content.Context

object CourseProgressNotificationManager {

    private const val PREF_NAME = "course_progress_notifications"

    fun getProgressMilestone(progress: Float): Int? {
        val percentage = (progress * 100).toInt()

        return when {
            percentage >= 100 -> 100
            percentage >= 75 -> 75
            percentage >= 50 -> 50
            percentage >= 25 -> 25
            percentage > 0 -> 0
            else -> null
        }
    }

    fun getProgressMessage(
        milestone: Int,
        courseName: String
    ): String {
        return when (milestone) {
            0 -> "📚 You've started $courseName. Keep learning!"
            25 -> "🎉 You've completed 25% of $courseName! Keep it up."
            50 -> "🚀 You're halfway through $courseName. Don't stop now!"
            75 -> "🔥 You've completed 75% of $courseName. You're almost there!"
            100 -> "✅ You've completed 100% of $courseName."
            else -> ""
        }
    }

    fun shouldNotify(
        context: Context,
        courseId: String,
        currentMilestone: Int
    ): Boolean {
        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

        val lastMilestone = prefs.getInt(courseId, -1)

        return currentMilestone > lastMilestone
    }

    fun saveMilestone(
        context: Context,
        courseId: String,
        milestone: Int
    ) {
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putInt(courseId, milestone)
            .apply()
    }
}