package com.abmax.ipcdaemon

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var tvSpeed: TextView
    private lateinit var tvRpm: TextView
    private lateinit var tvFuel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvThroughput: TextView

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSpeed       = findViewById(R.id.tvSpeed)
        tvRpm         = findViewById(R.id.tvRpm)
        tvFuel        = findViewById(R.id.tvFuel)
        tvStatus      = findViewById(R.id.tvStatus)
        tvThroughput  = findViewById(R.id.tvThroughput)

        connectToDaemon()
    }

    private fun connectToDaemon() {
        thread {
            while (true) {
                try {
                    mainHandler.post { tvStatus.text = "Connecting to daemon..." }
                    val socket = Socket("127.0.0.1", 9000)
                    mainHandler.post { tvStatus.text = "Connected! Receiving telemetry..." }

                    val stream: InputStream  = socket.getInputStream()
                    val out:    OutputStream = socket.getOutputStream()

                    // VehicleData: 3 floats (4 bytes each) + int64 (8 bytes) + int32 (4 bytes) = 24 bytes
                    val MSG_SIZE = 24
                    val buffer = ByteArray(MSG_SIZE)

                    var msgCount = 0
                    var windowStart = System.currentTimeMillis()

                    while (true) {
                        // Read full message
                        var bytesRead = 0
                        while (bytesRead < MSG_SIZE) {
                            val r = stream.read(buffer, bytesRead, MSG_SIZE - bytesRead)
                            if (r == -1) throw Exception("Daemon disconnected")
                            bytesRead += r
                        }

                        val recvTime = System.currentTimeMillis()

                        val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                        val speed = bb.float
                        val rpm   = bb.float
                        val fuel  = bb.float
                        bb.long   // skip timestamp
                        val seq   = bb.int

                        // Echo back: seq (4 bytes) + recvTime (8 bytes) = 12 bytes
                        val ack = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                        ack.putInt(seq)
                        ack.putLong(recvTime)
                        out.write(ack.array())
                        out.flush()

                        // Throughput: count messages per second
                        msgCount++
                        val now = System.currentTimeMillis()
                        if (now - windowStart >= 1000) {
                            val throughput = msgCount
                            msgCount = 0
                            windowStart = now
                            mainHandler.post {
                                tvThroughput.text = "Throughput: $throughput msg/sec"
                            }
                        }

                        mainHandler.post {
                            tvSpeed.text = "Speed: %.1f km/h".format(speed)
                            tvRpm.text   = "RPM:   %.0f".format(rpm)
                            tvFuel.text  = "Fuel:  %.1f%%".format(fuel)
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post { tvStatus.text = "Retrying... (${e.message})" }
                    Thread.sleep(2000)
                }
            }
        }
    }
}