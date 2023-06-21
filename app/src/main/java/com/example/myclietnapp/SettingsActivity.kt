package com.example.myclietnapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonSave: Button

    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private val settingsFileName = "Settings"
    private val settingsIpAddress = "IpAddress"
    private val settingsPort = "PortAddress"

    private lateinit var editTextIp: EditText
    private lateinit var editTextPort: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)
        buttonSave = findViewById(R.id.buttonSave)
        editTextIp = findViewById(R.id.editTextIp)
        editTextPort = findViewById(R.id.editTextPort)

        editTextIp.setText(settings.getString(settingsIpAddress, "192.168.0.123"))
        editTextPort.setText(settings.getInt(settingsPort, 30003).toString())

        buttonSave.setOnClickListener {
            recordSettings()
            backToMain()
        }
    }


    private fun recordSettings() {
        settings = getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)
        editor = settings.edit()

        editTextIp = findViewById(R.id.editTextIp)
        editTextPort = findViewById(R.id.editTextPort)

        val textIp = editTextIp.text.toString()
        val textPort = Integer.parseInt(editTextPort.text.toString())

        editor.putString(settingsIpAddress, textIp)
        editor.putInt(settingsPort, textPort)
        editor.apply()
    }

    private fun backToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}