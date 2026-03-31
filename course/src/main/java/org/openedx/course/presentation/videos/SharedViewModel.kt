package org.openedx.course.presentation.videos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    val buttonVisibility = MutableLiveData<Boolean>(true)
    val navigationBarVisibility = MutableLiveData<Boolean>(true)


}
