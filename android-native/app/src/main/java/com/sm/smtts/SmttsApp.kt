package com.sm.smtts

import android.app.Application
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.SessionStore

class SmttsApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var roomRepository: RoomRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        authRepository = AuthRepository(sessionStore)
        roomRepository = RoomRepository()
    }
}
