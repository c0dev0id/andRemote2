package de.codevoid.keytester

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)

        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            tvLog.text = ""
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            KeyEvent.ACTION_MULTIPLE -> "ACTION_MULTIPLE"
            else -> event.action.toString()
        }
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] $action keyCode=${event.keyCode} flags=0x${event.flags.toString(16)} repeat=${event.repeatCount}"
        tvLog.append(entry + "\n")
        scrollLog.post { scrollLog.fullScroll(ScrollView.FOCUS_DOWN) }
        return super.dispatchKeyEvent(event)
    }
}
