package org.openedx.dashboard.presentation

interface DashboardAnalytics {
    fun logEvent(event: String, params: Map<String, Any?>)
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
    fun dashboardCourseClickedEvent(courseId: String, courseName: String)
}

enum class DashboardAnalyticsEvent(val eventName: String, val biValue: String) {
    LEARN_MY_COURSES(
        "Learn:My Courses",
        "edx.bi.app.learn.my_courses"
    ),
    LEARN_MY_PROGRAMS(
        "Learn:My Programs",
        "edx.bi.app.learn.my_programs"
    ),
    PRIMARY_COURSE_CARD_CLICKED(
        "Learn:Primary Course Card Clicked",
        "edx.bi.app.learn.primary_course.clicked"
    ),
    SECONDARY_COURSE_CARD_CLICKED(
        "Learn:Secondary Course Card Clicked",
        "edx.bi.app.learn.secondary_course.clicked"
    ),
    VIEW_ALL_COURSES_CLICKED(
        "Learn:View All Courses Clicked",
        "edx.bi.app.learn.view_all_courses.clicked"
    ),
    VIEW_ALL_CARD_CLICKED(
        "Learn:View All Card Clicked",
        "edx.bi.app.learn.view_all_card.clicked"
    ),
    MY_COURSES_ALL_COURSES_VIEWED(
        "MyCourses:All Courses Viewed",
        "edx.bi.app.my_courses.all_courses.viewed"
    ),
    MY_COURSES_FILTER_CLICKED(
        "MyCourses:Filter Clicked",
        "edx.bi.app.my_courses.filter.clicked"
    ),
    COURSE_CARD_CLICKED(
        "MyCourses:Course Card Clicked",
        "edx.bi.app.my_courses.course_card.clicked"
    ),
}

enum class DashboardAnalyticsKey(val key: String) {
    NAME("name"),
    CATEGORY("category"),
    COURSE_ID("course_id"),
    ACTION("action"),
    FILTER("filter"),
    BLOCK_ID("block_id"),
    LEARN("learn"),
    MY_COURSES("my_courses"),
}

enum class PrimaryCourseCardAction(val action: String) {
    CARD("card"),
    PAST_ASSIGNMENT("past_assignment"),
    UPCOMING_ASSIGNMENT("upcoming_assignment"),
    RESUME_COURSE("resume_course"),
    UPGRADE_VALUE_PROP("upgrade_value_prop"),
}
