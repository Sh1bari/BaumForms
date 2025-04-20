package ru.noxly.baumforms.helper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SessionStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("session_state", Context.MODE_PRIVATE)

    fun setActiveSession(sessionId: Int) {
        prefs.edit().putInt("active_session_id", sessionId).apply()
    }

    fun clearActiveSession() {
        prefs.edit().remove("active_session_id").apply()
    }

    fun getActiveSession(): Int? {
        val id = prefs.getInt("active_session_id", -1)
        return if (id == -1) null else id
    }
}