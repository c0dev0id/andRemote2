package de.codevoid.andremote2

import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores one complete remote-control key mapping.
 *
 * The [keycodes] array has exactly [ACTION_COUNT] entries, one per control in this fixed order:
 *   0 = Joystick Up
 *   1 = Joystick Down
 *   2 = Joystick Left
 *   3 = Joystick Right
 *   4 = Button Top
 *   5 = Button Bottom
 *   6 = Lever Up
 *   7 = Lever Down
 */
data class RemoteKeyPreset(
    val name: String,
    val keycodes: IntArray
) {
    companion object {
        const val ACTION_COUNT = 8

        val ACTION_NAMES = listOf(
            "Joystick Up",
            "Joystick Down",
            "Joystick Left",
            "Joystick Right",
            "Button Top",
            "Button Bottom",
            "Lever Up",
            "Lever Down"
        )

        /**
         * Order in which the wizard asks the user to map controls.
         * Order: Up, Left, Down, Right, Button Top, Button Bottom, Lever Up, Lever Down
         */
        val WIZARD_ORDER = intArrayOf(0, 2, 1, 3, 4, 5, 6, 7)

        /** The built-in default preset. Never stored in JSON, never deletable. */
        val DMD_REMOTE_2 = RemoteKeyPreset(
            name = "DMD Remote 2",
            keycodes = intArrayOf(
                KeyEvent.KEYCODE_DPAD_UP,     // 0: Joystick Up
                KeyEvent.KEYCODE_DPAD_DOWN,   // 1: Joystick Down
                KeyEvent.KEYCODE_DPAD_LEFT,   // 2: Joystick Left
                KeyEvent.KEYCODE_DPAD_RIGHT,  // 3: Joystick Right
                KeyEvent.KEYCODE_ENTER,       // 4: Button Top
                KeyEvent.KEYCODE_ESCAPE,      // 5: Button Bottom
                KeyEvent.KEYCODE_F6,          // 6: Lever Up
                KeyEvent.KEYCODE_F7           // 7: Lever Down
            )
        )

        fun fromJson(json: JSONObject): RemoteKeyPreset {
            val name = json.getString("name")
            val arr = json.getJSONArray("keycodes")
            val keycodes = IntArray(ACTION_COUNT) { arr.getInt(it) }
            return RemoteKeyPreset(name, keycodes)
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        val arr = JSONArray()
        keycodes.forEach { arr.put(it) }
        put("keycodes", arr)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteKeyPreset) return false
        return name == other.name && keycodes.contentEquals(other.keycodes)
    }

    override fun hashCode(): Int = 31 * name.hashCode() + keycodes.contentHashCode()
}
