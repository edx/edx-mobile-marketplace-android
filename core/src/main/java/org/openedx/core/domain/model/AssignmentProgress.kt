package org.openedx.core.domain.model

import kotlinx.parcelize.IgnoredOnParcel

data class AssignmentProgress(
    val assignmentType: String,
    val numPointsEarned: Float,
    val numPointsPossible: Float,
    val shortLabel: String
) {
    fun toPointString(separator: String = ""): String {
        return "${numPointsEarned.toInt()}$separator/$separator${numPointsPossible.toInt()}"
    }
    @IgnoredOnParcel
    val label = shortLabel
        .replace(" ", "")
        .replaceFirst(Regex("^(\\D+)(0*)(\\d+)$"), "$1$3")
}
