package org.openedx.core.system.notifier.app

import org.openedx.core.domain.model.EnrolledCourse

class EnrolledCourseEvent(val enrolledCourses: List<EnrolledCourse>,) : AppEvent
