package de.codevoid.andremote2

import android.os.Handler
import android.os.Looper
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object KeyEventLog {

    private const val MAX_ENTRIES = 200
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val entries = ArrayDeque<String>()
    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var onNewEntry: (() -> Unit)? = null

    fun log(source: String, message: String) {
        val timestamp = LocalTime.now().format(timeFormatter)
        val entry = "$timestamp [$source] $message"
        synchronized(lock) {
            entries.addLast(entry)
            if (entries.size > MAX_ENTRIES) {
                entries.removeFirst()
            }
        }
        mainHandler.post { onNewEntry?.invoke() }
    }

    fun setOnNewEntryListener(listener: (() -> Unit)?) {
        onNewEntry = listener
    }

    fun getEntries(): List<String> {
        synchronized(lock) {
            return entries.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
        }
        mainHandler.post { onNewEntry?.invoke() }
    }
}
