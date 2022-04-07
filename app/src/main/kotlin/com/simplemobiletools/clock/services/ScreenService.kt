package com.simplemobiletools.clock.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class ScreenService : Service() {
    companion object {
        private var screenOn = System.currentTimeMillis()
        private var screenOff = -1L
        private var screenDuration = 0L

        fun isWakeUp(): Boolean {
            if (screenOn > screenOff) {
                return true
            }
            val now = System.currentTimeMillis()
            if (now - screenOff > 1000 * 60 * 30) {
                return false
            }
            if (screenDuration < 1000 * 60 * 5) {
                return false
            }
            return true
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenOn = System.currentTimeMillis()
                Intent.ACTION_SCREEN_OFF -> {
                    screenOff = System.currentTimeMillis()
                    screenDuration += screenOff - screenOn
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheck()
    }

    private fun startCheck() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(receiver, filter)
    }

    private fun stopCheck() {
        unregisterReceiver(receiver)
    }
}
