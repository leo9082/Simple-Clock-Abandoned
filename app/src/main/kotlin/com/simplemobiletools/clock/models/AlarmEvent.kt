package com.simplemobiletools.clock.models

import com.simplemobiletools.clock.helpers.INVALID_TIMER_ID

sealed class AlarmEvent(open val alarmId: Int) {
    data class Delete(override val alarmId: Int) : AlarmEvent(alarmId)
    data class Update(override val alarmId: Int) : AlarmEvent(alarmId)
}
