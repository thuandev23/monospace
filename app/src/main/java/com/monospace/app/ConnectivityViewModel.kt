package com.monospace.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monospace.app.core.common.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // assume online; flow sẽ flip false nếu mất mạng
        )
}
