package com.animex.tv

import android.app.Application
import com.animex.tv.core.session.UserSessionRepository

class AnimeXTVApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserSessionRepository.initialize(this)
    }
}
