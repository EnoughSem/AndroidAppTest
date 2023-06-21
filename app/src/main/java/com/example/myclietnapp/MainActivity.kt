package com.example.myclietnapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var buttonConnect: Button
    private lateinit var buttonPort1: Button
    private lateinit var buttonPort2: Button
    private lateinit var buttonSettings: Button
    private lateinit var textViewConnect: TextView
    private lateinit var textViewPort1: TextView
    private lateinit var textViewPort2: TextView
    private lateinit var textViewStatusInPort: TextView

    private val settingsFileName = "Settings"
    private val settingsIpAddress = "IpAddress"
    private val settingsPort = "PortAddress"

    private lateinit var ipAddress: String
    private var ipPort: Int = 30003

    private lateinit var settings: SharedPreferences

    private var connection: Socket? = null
    private var port1: Boolean = false
        set(value) {
            if (value) {
                CoroutineScope(Dispatchers.Default).launch { openPort(getConnection(), 1) }
            } else {
                CoroutineScope(Dispatchers.Default).launch { closePort(getConnection(), 1) }
            }
            field = value
        }
    private var port2: Boolean = false
        set(value) {
            if (value) {
                CoroutineScope(Dispatchers.Default).launch { openPort(getConnection(), 2) }
            } else {
                CoroutineScope(Dispatchers.Default).launch { closePort(getConnection(), 2) }
            }
            field = value
        }

    private var connectionEnabled = false

    override fun onPause() {
        super.onPause()
        connection!!.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)

        ipAddress = settings.getString(settingsIpAddress, "192.168.0.123")!!
        ipPort = settings.getInt(settingsPort, 30003)

        buttonConnect = findViewById(R.id.buttonConnect)
        buttonPort1 = findViewById(R.id.buttonPort1)
        buttonPort2 = findViewById(R.id.buttonPort2)
        buttonSettings = findViewById(R.id.buttonSettings)
        textViewConnect = findViewById(R.id.textViewConnectStatus)
        textViewPort1 = findViewById(R.id.textViewPort1Status)
        textViewPort2 = findViewById(R.id.textViewPort2Status)

        buttonConnect.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (buttonConnect.text == "Подключиться") {
                    connectionEnabled = true
                    if (authentication(getConnection())) {
                        withContext(Dispatchers.Main) {
                            textViewConnect.text = "Подключено"
                            buttonConnect.text = "Отключиться"
                        }

                        CoroutineScope(Dispatchers.Default).launch {
                            while (!checkOutPorts(getConnection())) {
                                delay(100)
                            }
                        }

                        CoroutineScope(Dispatchers.Default).launch {
                            while (true) {
                                checkInPorts(getConnection())
                                delay(100)
                            }
                        }
                    }
                } else if (buttonConnect.text == "Отключиться") {
                    connectionEnabled = false
                    withContext(Dispatchers.IO) {
                        getConnection().close()
                    }
                    withContext(Dispatchers.Main) {
                        textViewConnect.text = "Отключено"
                        buttonConnect.text = "Подключиться"
                    }
                }
            }
        }
        buttonPort1.setOnClickListener {
            port1 = textViewPort1.text == "Закрыт"
            if (connection != null && connection!!.isConnected)
                if (port1)
                    textViewPort1.text = "Открыт"
                else
                    textViewPort1.text = "Закрыт"
        }

        buttonPort2.setOnClickListener {
            port2 = textViewPort2.text == "Закрыт"
            if (connection != null && connection!!.isConnected)
                if (port2)
                    textViewPort2.text = "Открыт"
                else
                    textViewPort2.text = "Закрыт"
        }

        buttonSettings.setOnClickListener {
            settings()
        }
    }

    private suspend fun authentication(connect: Socket): Boolean = coroutineScope {
        var isAuth = false
        if (connect.isConnected && !connect.isClosed) {
            val byteArray = byteArrayOf(0x04, 0x02, 0x00, 0x00, 0x31, 0x32, 0x33, 0x34)
            send(connect, byteArray)
            val resultBytes = read(connect)
            if (resultBytes.size == 8) {
                if (resultBytes[0] == 0x06.toByte() && resultBytes[1] == 0x02.toByte() &&
                    resultBytes[2] == 0x00.toByte() && resultBytes[3] == 0x00.toByte()
                )
                    isAuth = true
                else if (resultBytes[0] == 0x06.toByte() && resultBytes[1] == 0x02.toByte() &&
                    resultBytes[2] == 0xff.toByte() && resultBytes[3] == 0x00.toByte()
                )
                    isAuth = true
                else if (resultBytes[0] == 0x06.toByte() && resultBytes[1] == 0x02.toByte() &&
                    resultBytes[2] == 0x00.toByte() && resultBytes[3] == 0xff.toByte()
                )
                    isAuth = true
            }
        }
        return@coroutineScope isAuth
    }

    private suspend fun send(connect: Socket, _bytes: ByteArray) {
        delay(25)

        if (!connect.isConnected) return
        if (connect.isClosed) return

        withContext(Dispatchers.IO) {
            connect.getOutputStream().write(_bytes)
            connect.getOutputStream().flush()
        }
    }

    private suspend fun read(connect: Socket): ByteArray {
        val buffer = ByteArray(8)
        delay(25)

        if (!connect.isConnected) return buffer
        if (connect.isClosed) return buffer

        withContext(Dispatchers.IO) {
            connect.getInputStream().read(buffer)
        }

        return buffer
    }

    private suspend fun openPort(connect: Socket, port: Int): Boolean {
        var isOpen = false
        val byteArray = if (port == 1)
            byteArrayOf(0x04, 0x0E, 0x0A, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0B, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)
        send(connect, byteArray)
        val resultBytes = read(connect)
        if (resultBytes[0] == 0x06.toByte() && resultBytes[1] == 0x0E.toByte() && resultBytes[3] == 0xFF.toByte())
            isOpen = true
        return isOpen
    }


    private suspend fun closePort(connect: Socket, port: Int): Boolean {
        var isClose = false
        val byteArray = if (port == 1)
            byteArrayOf(0x04, 0x0E, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0B, 0x00, 0x00, 0x00, 0x00, 0x00)
        send(connect, byteArray)
        val resultBytes = read(connect)
        if (resultBytes[0] == 0x06.toByte() && resultBytes[1] == 0x0E.toByte() && resultBytes[3] == 0x00.toByte())
            isClose = true
        return isClose
    }

    private fun getConnection(): Socket {
        var isAlive = false
        if (connection != null)
            if (connection!!.isConnected && !connection!!.isClosed) {
                isAlive = true
            }

        return if (isAlive) {
            connection as Socket
        } else {
            var isConnected = false
            while (connectionEnabled) {
                if (isConnected) break

                try {
                    connection = Socket(ipAddress, ipPort)
                    isConnected = true
                } catch (ex: Exception) {
                    Log.d("[SOCKET CONNECTION ERROR]", ex.message.toString())
                }
            }

            connection as Socket
        }
    }

    private suspend fun checkOutPorts(connect: Socket): Boolean {
        var resultMessage = false
        textViewPort1 = findViewById(R.id.textViewPort1Status)
        textViewPort2 = findViewById(R.id.textViewPort2Status)
        val byteArray = byteArrayOf(0x04, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        send(connect, byteArray)
        val result = read(connect)
        if (result.size == 8)
            if (result[0] == 0x06.toByte())
                if (result[1] == 0x12.toByte())
                    if (result[2] == 0x00.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Закрыт"
                            textViewPort2.text = "Закрыт"
                        }
                        resultMessage = true
                    } else if (result[2] == 0x01.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Открыт"
                            textViewPort2.text = "Закрыт"
                        }
                        resultMessage = true
                    } else if (result[2] == 0x02.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Закрыт"
                            textViewPort2.text = "Открыт"
                        }
                        resultMessage = true
                    } else {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Открыт"
                            textViewPort2.text = "Открыт"
                        }
                        resultMessage = true
                    }
        return resultMessage
    }

    private fun settings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private suspend fun checkInPorts(connect: Socket) {
        textViewStatusInPort = findViewById(R.id.textViewStatusInPort)
        val byteArray = byteArrayOf(0x04, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        send(connect, byteArray)
        val result = read(connect)
        if (result.size == 8)
            if (result[0] == 0x06.toByte())
                if (result[1] == 0x0A.toByte())
                    if (result[2] == 0x00.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewStatusInPort.text = "Замкнуты все контакты"
                        }
                    } else if (result[2] == 0x01.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewStatusInPort.text = "Второй и земля"
                        }
                    } else if (result[2] == 0x02.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewStatusInPort.text = "Первый и земля"
                        }
                    } else if (result[2] == 0x03.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewStatusInPort.text = "Не закнуто"
                        }
                    }
                    else {
                        withContext(Dispatchers.Main) {
                            textViewStatusInPort.text = "Ошибка"
                        }
                    }
    }
}