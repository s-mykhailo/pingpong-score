package com.sm.smtts

import android.app.Application
import com.sm.smtts.data.AuthRepository
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.PendingRoomQueue
import com.sm.smtts.data.RoomRepository
import com.sm.smtts.data.SessionStore
import com.sm.smtts.data.ThemeStore

class SmttsApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var roomRepository: RoomRepository
        private set
    lateinit var matchRepository: MatchRepository
        private set
    lateinit var pendingQueue: PendingQueue
        private set
    lateinit var pendingRoomQueue: PendingRoomQueue
        private set
    lateinit var themeStore: ThemeStore
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        authRepository = AuthRepository(sessionStore)
        roomRepository = RoomRepository(this)
        matchRepository = MatchRepository()
        pendingQueue = PendingQueue(this)
        pendingRoomQueue = PendingRoomQueue(this)
        themeStore = ThemeStore(this)
    }
}
