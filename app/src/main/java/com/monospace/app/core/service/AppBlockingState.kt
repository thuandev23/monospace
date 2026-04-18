package com.monospace.app.core.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppBlockingState {
    private val _blockedPackage = MutableStateFlow<String?>(null)
    val blockedPackage: StateFlow<String?> = _blockedPackage.asStateFlow()

    private var temporaryUnlockUntil: Long = 0L

    fun setBlockedPackage(pkg: String?) {
        if (_blockedPackage.value != pkg) {
            android.util.Log.d("BLOCK_DEBUG", "AppBlockingState: setting blockedPackage to $pkg")
            _blockedPackage.value = pkg
        }
    }

    fun grantTemporaryUnlock(durationMs: Long) {
        android.util.Log.d(
            "BLOCK_DEBUG",
            "AppBlockingState: grantTemporaryUnlock for $durationMs ms"
        )
        temporaryUnlockUntil = System.currentTimeMillis() + durationMs
        setBlockedPackage(null)
    }

    fun isTemporarilyUnlocked(): Boolean {
        val now = System.currentTimeMillis()
        if (temporaryUnlockUntil <= 0L) return false
        val unlocked = now < temporaryUnlockUntil
        if (!unlocked && temporaryUnlockUntil > 0L) {
            android.util.Log.d(
                "BLOCK_DEBUG",
                "AppBlockingState: Temporary unlock EXPIRED -> blocking resumed"
            )
            temporaryUnlockUntil = 0L
        } else {
            val remaining = (temporaryUnlockUntil - now) / 1000
        }
        return unlocked
    }

    /**
     * Gọi khi dừng Focus Mode hoặc kết thúc Session để xóa trạng thái mở khóa tạm thời.
     */
    fun reset() {
        android.util.Log.d("BLOCK_DEBUG", "AppBlockingState: Resetting states")
        temporaryUnlockUntil = 0L
        _blockedPackage.value = null
    }
}
