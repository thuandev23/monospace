package com.monospace.app.core.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppBlockingState {
    private val _blockedPackage = MutableStateFlow<String?>(null)
    val blockedPackage: StateFlow<String?> = _blockedPackage.asStateFlow()

    var temporaryUnlockUntil: Long = 0L
        private set

    fun setBlockedPackage(pkg: String?) {
        _blockedPackage.value = pkg
    }

    fun grantTemporaryUnlock(durationMs: Long) {
        temporaryUnlockUntil = System.currentTimeMillis() + durationMs
        _blockedPackage.value = null
    }

    fun isTemporarilyUnlocked(): Boolean =
        System.currentTimeMillis() < temporaryUnlockUntil
}
