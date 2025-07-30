package org.openedx.core.extension

fun Boolean?.isTrue(): Boolean = this == true

fun Boolean?.isFalse(): Boolean = this == false

fun Boolean?.orTrue(): Boolean = this != false //same as this ?: true
