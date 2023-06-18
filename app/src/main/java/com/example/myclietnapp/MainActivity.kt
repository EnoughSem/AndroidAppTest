package com.example.myclietnapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonConnect: Button = findViewById(R.id.buttonConnect)
        val buttonPort1: Button = findViewById(R.id.buttonPort1)
        val buttonPort2: Button = findViewById(R.id.buttonPort2)
        val textViewConnect: TextView = findViewById(R.id.textViewConnectStatus)
        val textViewPort1: TextView = findViewById(R.id.textViewPort1Status)
        val textViewPort2: TextView = findViewById(R.id.textViewPort2Status)

        buttonConnect.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                authentication(getConnection())
            }
        }
        buttonPort1.setOnClickListener {
            if ((textViewPort1.text == "Состояние") or (textViewPort1.text == "Закрыт" )) {
                CoroutineScope(Dispatchers.IO).launch {
                    openPort(getConnection(), 1)
                }
                textViewPort1.text = "Открыт"
            }
            else{
                CoroutineScope(Dispatchers.IO).launch {
                    closePort(getConnection(), 1)
                }
                textViewPort1.text = "Закрыт"
            }
        }

        buttonPort2.setOnClickListener{
            if ((textViewPort2.text == "Состояние") or (textViewPort2.text == "Закрыт" )) {
                CoroutineScope(Dispatchers.IO).launch {
                    openPort(getConnection(), 2)
                }
                textViewPort2.text = "Открыт"
            }
            else{
                CoroutineScope(Dispatchers.IO).launch {
                    closePort(getConnection(), 2)
                }
                textViewPort2.text = "Закрыт"
            }
        }
    }

    private suspend fun authentication (connect: Socket) {
        if (connect.isConnected) {
            val textView: TextView = findViewById(R.id.textViewConnectStatus)
            val byteArray = byteArrayOf(0x04, 0x02, 0x00, 0x00, 0x31, 0x32, 0x33, 0x34)
            withContext(Dispatchers.IO) { send(connect, byteArray) }
            withContext(Dispatchers.IO) {
                connect.close()
            }
        }
    }

    private suspend fun send(connect: Socket, _bytes: ByteArray){
        withContext(Dispatchers.IO) {
            connect.getOutputStream().write(_bytes)
            connect.getOutputStream().flush()
            delay(400)
        }
    }

    private suspend fun read(connect: Socket): ByteArray{
        return withContext(Dispatchers.IO) {
            connect.getInputStream().readBytes()}
    }

    private suspend fun openPort(connect: Socket, port: Int){
        val byteArray = if (port== 1)
            byteArrayOf(0x04, 0x0E, 0x0a, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0b, 0xFF.toByte(), 0x00, 0x00, 0x00, 0x00)
        withContext(Dispatchers.IO) { send(connect, byteArray) }
        withContext(Dispatchers.IO) { connect.close() }
    }

    private suspend fun closePort(connect: Socket, port: Int){
        val byteArray = if (port== 1)
            byteArrayOf(0x04, 0x0E, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x00)
        else
            byteArrayOf(0x04, 0x0E, 0x0b, 0x00, 0x00, 0x00, 0x00, 0x00)
        withContext(Dispatchers.IO) { send(connect, byteArray) }
        withContext(Dispatchers.IO) { connect.close() }
    }

    private fun getConnection(): Socket{
        return Socket("192.168.0.123", 30003)
    }
}