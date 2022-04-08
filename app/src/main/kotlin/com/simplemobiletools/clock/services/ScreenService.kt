package com.simplemobiletools.clock.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.xdandroid.hellodaemon.AbsWorkService

class ScreenService : AbsWorkService() {

    override fun shouldStopService(intent: Intent?, flags: Int, startId: Int): Boolean {
        Log.e("ScreenService", "shouldStopService")
        return shouldStop
    }

    override fun isWorkRunning(intent: Intent?, flags: Int, startId: Int): Boolean {
        Log.e("ScreenService", "isWorkRunning")
        return isRunning
    }

    override fun startWork(intent: Intent?, flags: Int, startId: Int) {
        Log.e("ScreenService", "startWork")
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(receiver, filter)
        isRunning = true
    }

    override fun stopWork(intent: Intent?, flags: Int, startId: Int) {
        Log.e("ScreenService", "stopWork")
        shouldStop = true
        unregisterReceiver(receiver)
        cancelJobAlarmSub()
        isRunning = false
    }

    override fun onBind(intent: Intent?, alwaysNull: Void?): IBinder? {
        Log.e("ScreenService", "onBind")
        return null
    }

    override fun onServiceKilled(rootIntent: Intent?) {
        Log.e("ScreenService", "onServiceKilled")
    }

    companion object {
        private var shouldStop = false
        private var isRunning = false

        fun stop() {
            shouldStop = true
        }

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
}
