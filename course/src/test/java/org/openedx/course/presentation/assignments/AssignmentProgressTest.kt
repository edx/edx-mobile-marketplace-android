package org.openedx.course.presentation.assignments

import org.junit.Assert
import org.junit.Test
import org.openedx.core.domain.model.AssignmentProgress

class AssignmentProgressTest {

    @Test
    fun `toPointString uses earned and possible values`() {
        val progress = AssignmentProgress(
            assignmentType = "Homework",
            numPointsEarned = 1.9f,
            numPointsPossible = 3.1f,
            shortLabel = "HW01",
        )

        Assert.assertEquals("1/3", progress.toPointString())
        Assert.assertEquals("1 / 3", progress.toPointString(" "))
    }
}