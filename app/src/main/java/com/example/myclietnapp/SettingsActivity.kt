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

    private val SETTINGS_FILE_NAME = "Settings"
    private val SETTINGS_IP = "IpAdress"
    private val SETTINGS_PORT = "PortAdress"

    private lateinit var editTextIp: EditText
    private lateinit var editTextPort: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)
        buttonSave = findViewById(R.id.buttonSave)
        editTextIp = findViewById(R.id.editTextIp)
        editTextPort = findViewById(R.id.editTextPort)

        editTextIp.setText(settings.getString(SETTINGS_IP, "192.168.0.123"))
        editTextPort.setText(settings.getInt(SETTINGS_PORT, 30003).toString())

        buttonSave.setOnClickListener {
            recordSettings()
            backToMain()
        }


    }


    private fun recordSettings() {
        settings = getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE)
        editor = settings.edit()

        editTextIp = findViewById(R.id.editTextIp)
        editTextPort = findViewById(R.id.editTextPort)

        val textIp = editTextIp.getText().toString()
        val textPort = Integer.parseInt(editTextPort.getText().toString())

        editor.putString(SETTINGS_IP, textIp)
        editor.putInt(SETTINGS_PORT, textPort)
        editor.apply()
    }

    private fun backToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}