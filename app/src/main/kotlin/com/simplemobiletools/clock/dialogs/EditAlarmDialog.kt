package com.simplemobiletools.clock.dialogs

import android.app.TimePickerDialog
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.RingtoneManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.clock.R
import com.simplemobiletools.clock.activities.SimpleActivity
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.helpers.PICK_AUDIO_FILE_INTENT_ID
import com.simplemobiletools.clock.helpers.TODAY_BIT
import com.simplemobiletools.clock.helpers.TOMORROW_BIT
import com.simplemobiletools.clock.helpers.getCurrentDayMinutes
import com.simplemobiletools.clock.models.Alarm
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.SelectAlarmSoundDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.AlarmSound
import kotlinx.android.synthetic.main.dialog_edit_alarm.view.*

class EditAlarmDialog(val activity: SimpleActivity, val alarm: Alarm, val callback: (alarmId: Int) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_edit_alarm, null)
    private val textColor = activity.config.textColor

    init {
        restoreLastAlarm()
        updateAlarmTime()

        view.apply {
            edit_alarm_time.setOnClickListener {
                TimePickerDialog(
                    context,
                    context.getDialogTheme(),
                    timeSetListener,
                    alarm.timeInMinutes / 60,
                    alarm.timeInMinutes % 60,
                    context.config.use24HourFormat
                ).show()
            }

            edit_alarm_sound.colorCompoundDrawable(textColor)
            edit_alarm_sound.text = alarm.soundTitle
            edit_alarm_sound.setOnClickListener {
                SelectAlarmSoundDialog(activity, alarm.soundUri, AudioManager.STREAM_ALARM, PICK_AUDIO_FILE_INTENT_ID, RingtoneManager.TYPE_ALARM, true,
                    onAlarmPicked = {
                        if (it != null) {
                            updateSelectedAlarmSound(it)
                        }
                    }, onAlarmSoundDeleted = {
                        if (alarm.soundUri == it.uri) {
                            val defaultAlarm = context.getDefaultAlarmSound(RingtoneManager.TYPE_ALARM)
                            updateSelectedAlarmSound(defaultAlarm)
                        }
                        activity.checkAlarmsWithDeletedSoundUri(it.uri)
                    })
            }

            label_alarm_vibrate.colorCompoundDrawable(textColor)
            edit_alarm_vibrate.isChecked = alarm.vibrate
            edit_alarm_vibrate_holder.setOnClickListener {
                edit_alarm_vibrate.toggle()
                alarm.vibrate = edit_alarm_vibrate.isChecked
            }

            label_alarm_get_up.colorCompoundDrawable(textColor)
            edit_alarm_get_up.isChecked = alarm.wake
            edit_alarm_get_up_holder.setOnClickListener {
                edit_alarm_get_up.toggle()
                alarm.wake = edit_alarm_get_up.isChecked
            }

            edit_alarm_label_image.applyColorFilter(textColor)
            edit_alarm_label.setText(alarm.label)

            val dayLetters = activity.resources.getStringArray(R.array.week_day_letters).toList() as ArrayList<String>
            val dayIndexes = arrayListOf(0, 1, 2, 3, 4, 5, 6)
            if (activity.config.isSundayFirst) {
                dayIndexes.moveLastItemToFront()
            }

            dayIndexes.forEach {
                val pow = Math.pow(2.0, it.toDouble()).toInt()
                val day = activity.layoutInflater.inflate(R.layout.alarm_day, edit_alarm_days_holder, false) as TextView
                day.text = dayLetters[it]

                val isDayChecked = alarm.days > 0 && alarm.days and pow != 0
                day.background = getProperDayDrawable(isDayChecked)

                day.setTextColor(if (isDayChecked) context.config.backgroundColor else textColor)
                day.setOnClickListener {
                    if (alarm.days < 0) {
                        alarm.days = 0
                    }

                    val selectDay = alarm.days and pow == 0
                    if (selectDay) {
                        alarm.days = alarm.days.addBit(pow)
                    } else {
                        alarm.days = alarm.days.removeBit(pow)
                    }
                    day.background = getProperDayDrawable(selectDay)
                    day.setTextColor(if (selectDay) context.config.backgroundColor else textColor)
                    checkDaylessAlarm()
                }

                edit_alarm_days_holder.addView(day)
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (!activity.config.wasAlarmWarningShown) {
                            ConfirmationDialog(activity, messageId = R.string.alarm_warning, positive = R.string.ok, negative = 0) {
                                activity.config.wasAlarmWarningShown = true
                                it.performClick()
                            }

                            return@setOnClickListener
                        }

                        if (alarm.days <= 0) {
                            alarm.days = if (alarm.timeInMinutes > getCurrentDayMinutes()) {
                                TODAY_BIT
                            } else {
                                TOMORROW_BIT
                            }
                        }

                        alarm.label = view.edit_alarm_label.value
                        alarm.isEnabled = true

                        var alarmId = alarm.id
                        if (alarm.id == 0) {
                            alarmId = activity.dbHelper.insertAlarm(alarm)
                            if (alarmId == -1) {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        } else {
                            if (!activity.dbHelper.updateAlarm(alarm)) {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        }

                        activity.config.alarmLastConfig = alarm
                        callback(alarmId)
                        dismiss()
                    }
                }
            }
    }

    private fun restoreLastAlarm() {
        if (alarm.id == 0) {
            activity.config.alarmLastConfig?.let { lastConfig ->
                alarm.label = lastConfig.label
                alarm.days = lastConfig.days
                alarm.soundTitle = lastConfig.soundTitle
                alarm.soundUri = lastConfig.soundUri
                alarm.timeInMinutes = lastConfig.timeInMinutes
                alarm.vibrate = lastConfig.vibrate
            }
        }
    }

    private val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        alarm.timeInMinutes = hourOfDay * 60 + minute
        updateAlarmTime()
    }

    private fun updateAlarmTime() {
        view.edit_alarm_time.text = activity.getFormattedTime(alarm.timeInMinutes * 60, false, true)
        checkDaylessAlarm()
    }

    private fun checkDaylessAlarm() {
        if (alarm.days <= 0) {
            val textId = if (alarm.timeInMinutes > getCurrentDayMinutes()) {
                R.string.today
            } else {
                R.string.tomorrow
            }

            view.edit_alarm_dayless_label.text = "(${activity.getString(textId)})"
        }
        view.edit_alarm_dayless_label.beVisibleIf(alarm.days <= 0)
    }

    private fun getProperDayDrawable(selected: Boolean): Drawable {
        val drawableId = if (selected) R.drawable.circle_background_filled else R.drawable.circle_background_stroke
        val drawable = activity.resources.getDrawable(drawableId)
        drawable.applyColorFilter(textColor)
        return drawable
    }

    fun updateSelectedAlarmSound(alarmSound: AlarmSound) {
        alarm.soundTitle = alarmSound.title
        alarm.soundUri = alarmSound.uri
        view.edit_alarm_sound.text = alarmSound.title
    }
}
