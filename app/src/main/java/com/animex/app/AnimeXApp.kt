package com.animex.app

import android.app.Application
import com.animex.app.core.session.UserSessionRepository

class AnimeXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSessionRepository.initialize(this)
    }
}
