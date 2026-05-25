package org.openedx.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import org.openedx.core.system.ResourceManager

open class BaseViewModel : ViewModel(), DefaultLifecycleObserver