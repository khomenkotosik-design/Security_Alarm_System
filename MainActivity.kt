package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var vibrator: Vibrator
    private var ringtone: Ringtone? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvSensorData: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnToggle: Button
    private lateinit var sbSensitivity: SeekBar

    private var isMonitoring = false
    private var threshold = 2.5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvSensorData = findViewById(R.id.tvSensorData)
        tvLog = findViewById(R.id.tvLog)
        btnToggle = findViewById(R.id.btnToggle)
        sbSensitivity = findViewById(R.id.sbSensitivity)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)

        sbSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = (progress / 10.0) + 0.5
                tvSensorData.text = "Поріг чутливості: ${"%.1f".format(threshold)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnToggle.setOnClickListener {
            if (isMonitoring) {
                showPinDialog() // Замість простого вимкнення — запит PIN
            } else {
                startMonitoring()
            }
        }
    }

    private fun showPinDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Деактивація охорони")
        builder.setMessage("Введіть PIN-код (за замовчуванням 1234):")

        // Створюємо поле вводу для чисел
        val input = EditText(this)
        input.inputType = EditorInfo.TYPE_CLASS_NUMBER
        input.hint = "PIN-код"
        builder.setView(input)

        builder.setPositiveButton("ВИМКНУТИ") { _, _ ->
            val enteredPin = input.text.toString()
            if (enteredPin == "1234") {
                stopMonitoring()
                addToLog("Охорону знято (вірний PIN)")
            } else {
                addToLog("СПРОБА ЗЛАМУ! Невірний PIN: $enteredPin")
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }

        builder.setNegativeButton("Скасувати", null)
        builder.show()
    }

    private fun startMonitoring() {
        isMonitoring = true
        btnToggle.text = "ВИМКНУТИ ОХОРОНУ"
        tvStatus.text = "ОХОРОНА АКТИВНА"
        tvStatus.setTextColor(Color.GREEN)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        addToLog("Систему активовано")
    }

    private fun stopMonitoring() {
        isMonitoring = false
        btnToggle.text = "УВІМКНУТИ ОХОРОНУ"
        tvStatus.text = "ОХОРОНА ВИМКНЕНА"
        tvStatus.setTextColor(Color.GRAY)
        sensorManager.unregisterListener(this)
        ringtone?.stop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - 9.8

            if (acceleration > 0.1 || acceleration < -0.1) {
                tvSensorData.text = "Сила руху: ${"%.2f".format(acceleration)} (Поріг: ${"%.1f".format(threshold)})"
            }

            if (acceleration > threshold || acceleration < -threshold) {
                triggerAlarm(acceleration)
            }
        }
    }

    private fun triggerAlarm(force: Double) {
        tvStatus.text = "ТРИВОГА! РУХ ВИЯВЛЕНО!"
        tvStatus.setTextColor(Color.RED)

        if (ringtone?.isPlaying == false) {
            ringtone?.play()
        }

        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        addToLog("ТРИВОГА! Сила удару: ${"%.2f".format(force)}")
    }

    private fun addToLog(message: String) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$currentTime] $message\n"

        if (tvLog.text == "Лог порожній...") {
            tvLog.text = entry
        } else {
            tvLog.append(entry)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}