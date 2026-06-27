package com.example.qr_paring_app

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class RobotSettingsActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference
    private var robotId = "12345" // ඔයාගේ රොබෝ අයිඩී එක

    private lateinit var tvVolumeValue: TextView
    private lateinit var seekBarVolume: SeekBar
    private lateinit var switchNightMode: SwitchMaterial

    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var btnReminderTime: Button
    private lateinit var etReminderMsg: TextInputEditText
    private lateinit var btnSaveReminder: Button

    // Default Values
    private var startH = 20
    private var startM = 0
    private var endH = 0
    private var endM = 0
    private var remindH = 18
    private var remindM = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_robot_settings)

        // Firebase RTDB සම්බන්ධ කිරීම
        dbRef = FirebaseDatabase.getInstance("https://smartedubuddy-default-rtdb.firebaseio.com/").getReference("Robots").child(robotId)

        tvVolumeValue = findViewById(R.id.tvVolumeValue)
        seekBarVolume = findViewById(R.id.seekBarVolume)
        switchNightMode = findViewById(R.id.switchNightMode)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        btnReminderTime = findViewById(R.id.btnReminderTime)
        etReminderMsg = findViewById(R.id.etReminderMsg)
        btnSaveReminder = findViewById(R.id.btnSaveReminder)

        // 🔊 Volume Controller Logic
        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVolumeValue.text = "$progress%"
                dbRef.child("Volume").setValue(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 🌙 Manual Night Mode Switch Logic
        switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            dbRef.child("ManualNightMode").setValue(isChecked)
        }

        // ⏰ 1. Night Mode Auto - Start Time Picker
        btnStartTime.setOnClickListener {
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                startH = selectedHour
                startM = selectedMinute
                btnStartTime.text = String.format("Start: %02d:%02d", selectedHour, selectedMinute)
            }, startH, startM, true).show()
        }

        // ⏰ 2. Night Mode Auto - End Time Picker
        btnEndTime.setOnClickListener {
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                endH = selectedHour
                endM = selectedMinute
                btnEndTime.text = String.format("End: %02d:%02d", selectedHour, selectedMinute)
            }, endH, endM, true).show()
        }

        // 🕒 3. Kids Smart Alarm Reminder - Time Picker
        btnReminderTime.setOnClickListener {
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                remindH = selectedHour
                remindM = selectedMinute
                btnReminderTime.text = String.format("Reminder: %02d:%02d", selectedHour, selectedMinute)
            }, remindH, remindM, true).show()
        }

        // 🚀 සේරම දත්ත (Schedule & Alarm) Firebase Cloud එකට සේව් කිරීම
        btnSaveReminder.setOnClickListener {
            val msg = etReminderMsg.text.toString().trim()
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please enter a reminder message!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // සකස් කරගත් වේලාවන් Map එකක් ලෙස Firebase එකට යැවීම
            val scheduleMap = mapOf(
                "autoNightStartHour" to startH,
                "autoNightStartMin" to startM,
                "autoNightEndHour" to endH,
                "autoNightEndMin" to endM,
                "alarmHour" to remindH,
                "alarmMin" to remindM,
                "alarmMessage" to msg,
                "isAlarmTriggered" to false
            )

            dbRef.child("ScheduleSettings").setValue(scheduleMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Successfully Synced with Robot! 🤖🚀", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to sync with Firebase!", Toast.LENGTH_SHORT).show()
                }
        }
    }
}