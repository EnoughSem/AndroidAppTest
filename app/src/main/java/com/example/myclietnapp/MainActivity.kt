package com.example.myclietnapp

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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private lateinit var buttonConnect: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonConnect = findViewById(R.id.buttonConnect)
        val buttonPort1: Button = findViewById(R.id.buttonPort1)
        val buttonPort2: Button = findViewById(R.id.buttonPort2)
        val textViewConnect: TextView = findViewById(R.id.textViewConnectStatus)
        val textViewPort1: TextView = findViewById(R.id.textViewPort1Status)
        val textViewPort2: TextView = findViewById(R.id.textViewPort2Status)

        buttonConnect.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (buttonConnect.text == "Подключиться") {
                    if (authentication(getConnection())) {
                        withContext(Dispatchers.Main) {
                            textViewConnect.text = "Подключено"
                            buttonConnect.text = "Отключиться"
                        }

                        CoroutineScope(Dispatchers.Default).launch {
                            while (true) {
                                checkOutPorts(getConnection())
                                delay(100)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        textViewConnect.text = "Отключено"
                        buttonConnect.text = "Подключиться"
                    }
                    withContext(Dispatchers.IO) {
                        connection!!.close()
                    }
                }
            }
        }
        buttonPort1.setOnClickListener {
            port1 = textViewPort1.text == "Закрыт"
        }

        buttonPort2.setOnClickListener {
            port2 = textViewPort2.text == "Закрыт"
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
        withContext(Dispatchers.IO) {
            delay(25)
            connect.getOutputStream().write(_bytes)
            connect.getOutputStream().flush()
        }
    }

    private suspend fun read(connect: Socket): ByteArray {
        val buffer = ByteArray(8)
        withContext(Dispatchers.IO) {
            delay(25)
            connect.getInputStream().read(buffer)
        }
        return buffer
    }

    private suspend fun openPort(connect: Socket, port: Int) {
        val byteArray = if (port == 1)
            byteArrayOf(0x04, 0x0E, 0x0A, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0B, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)

        withContext(Dispatchers.Default) {
            send(connect, byteArray)
            read(connect)
        }
    }


    private suspend fun closePort(connect: Socket, port: Int) {
        val byteArray = if (port == 1)
            byteArrayOf(0x04, 0x0E, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0B, 0x00, 0x00, 0x00, 0x00, 0x00)

        withContext(Dispatchers.Default) {
            send(connect, byteArray)
            read(connect)
        }
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
            while (!isConnected) {
                try {
                    connection = Socket("192.168.0.160", 30003)
                    isConnected = true
                } catch (ex: Exception) {
                    Log.d("[SOCKET CONNECTION ERROR]", ex.message.toString())
                }
            }

            connection as Socket
        }
    }

    private suspend fun checkOutPorts(connect: Socket) {
        val textViewPort1: TextView = findViewById(R.id.textViewPort1Status)
        val textViewPort2: TextView = findViewById(R.id.textViewPort2Status)
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
                    } else if (result[2] == 0x01.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Открыт"
                            textViewPort2.text = "Закрыт"
                        }
                    } else if (result[2] == 0x02.toByte()) {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Закрыт"
                            textViewPort2.text = "Открыт"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            textViewPort1.text = "Открыт"
                            textViewPort2.text = "Открыт"
                        }
                    }
    }
}