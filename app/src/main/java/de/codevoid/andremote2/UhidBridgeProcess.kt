package de.codevoid.andremote2

/**
 * Standalone bridge process launched via app_process with shell privileges.
 * Opens /dev/uhid, creates a virtual gamepad device, and relays 4-byte
 * packets received from stdin as UHID_INPUT2 HID reports.
 *
 * Packet format (4 bytes): [X: int8] [Y: int8] [Z: int8] [buttons: uint8]
 *   - X/Y: joystick axes, -127..127
 *   - Z:   lever axis, -127..127
 *   - buttons: bit 0 = button top, bit 1 = button bottom
 *
 * Invoked by UhidBridge via:
 *   app_process -Djava.class.path=<apk> /system/bin de.codevoid.andremote2.UhidBridgeProcess
 */
object UhidBridgeProcess {

    // HID Report Descriptor: gamepad with X/Y/Z axes (signed 8-bit) and 4 buttons
    // Report = 4 bytes: [X] [Y] [Z] [buttons(4 bits) + padding(4 bits)]
    private val HID_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01,           // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05,           // Usage (Game Pad)
        0xA1.toByte(), 0x01,           // Collection (Application)
        0x09.toByte(), 0x01,           //   Usage (Pointer)
        0xA1.toByte(), 0x00,           //   Collection (Physical)
        0x09.toByte(), 0x30,           //     Usage (X)
        0x09.toByte(), 0x31,           //     Usage (Y)
        0x09.toByte(), 0x32,           //     Usage (Z)
        0x15.toByte(), 0x81.toByte(),  //     Logical Minimum (-127)
        0x25.toByte(), 0x7F,           //     Logical Maximum (127)
        0x75.toByte(), 0x08,           //     Report Size (8 bits)
        0x95.toByte(), 0x03,           //     Report Count (3)
        0x81.toByte(), 0x02,           //     Input (Data, Variable, Absolute)
        0xC0.toByte(),                 //   End Collection
        0x05.toByte(), 0x09,           //   Usage Page (Button)
        0x19.toByte(), 0x01,           //   Usage Minimum (Button 1)
        0x29.toByte(), 0x04,           //   Usage Maximum (Button 4)
        0x15.toByte(), 0x00,           //   Logical Minimum (0)
        0x25.toByte(), 0x01,           //   Logical Maximum (1)
        0x75.toByte(), 0x01,           //   Report Size (1 bit)
        0x95.toByte(), 0x04,           //   Report Count (4)
        0x81.toByte(), 0x02,           //   Input (Data, Variable, Absolute)
        0x95.toByte(), 0x04,           //   Report Count (4) — padding
        0x81.toByte(), 0x03,           //   Input (Constant) — 4-bit padding
        0xC0.toByte()                  // End Collection
    )

    private const val UHID_DESTROY = 1
    private const val UHID_CREATE2 = 11
    private const val UHID_INPUT2 = 12

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val uhid = java.io.FileOutputStream("/dev/uhid")
            Runtime.getRuntime().addShutdownHook(Thread {
                try { writeDestroy(uhid) } catch (_: Exception) {}
                try { uhid.close() } catch (_: Exception) {}
            })
            writeCreate2(uhid)
            val stdin = System.`in`
            val pkt = ByteArray(4)
            while (true) {
                var off = 0
                while (off < 4) {
                    val n = stdin.read(pkt, off, 4 - off)
                    if (n < 0) return
                    off += n
                }
                writeInput2(uhid, pkt[0], pkt[1], pkt[2], pkt[3])
            }
        } catch (e: Exception) {
            System.err.println("UhidBridgeProcess: ${e.message}")
        }
    }

    private fun writeInt32LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value         and 0xFF).toByte()
        buf[offset + 1] = (value ushr  8 and 0xFF).toByte()
        buf[offset + 2] = (value ushr 16 and 0xFF).toByte()
        buf[offset + 3] = (value ushr 24 and 0xFF).toByte()
    }

    private fun writeInt16LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value        and 0xFF).toByte()
        buf[offset + 1] = (value ushr 8 and 0xFF).toByte()
    }

    /**
     * Writes a UHID_CREATE2 event to /dev/uhid (4376 bytes).
     *
     * uhid_event layout for create2:
     *   [0..3]    type (uint32 LE) = 11
     *   [4..131]  name[128]
     *   [132..195] phys[64]
     *   [196..259] uniq[64]
     *   [260..261] rd_size (uint16 LE)
     *   [262..263] bus (uint16 LE)
     *   [264..267] vendor (uint32 LE)
     *   [268..271] product (uint32 LE)
     *   [272..275] version (uint32 LE)
     *   [276..279] country (uint32 LE)
     *   [280..4375] rd_data[4096]
     */
    private fun writeCreate2(out: java.io.OutputStream) {
        val buf = ByteArray(4376)
        writeInt32LE(buf, 0, UHID_CREATE2)
        val name = "andRemote2 Gamepad".toByteArray(Charsets.UTF_8)
        System.arraycopy(name, 0, buf, 4, name.size)
        writeInt16LE(buf, 260, HID_DESCRIPTOR.size)
        writeInt16LE(buf, 262, 0x06) // BUS_VIRTUAL
        writeInt32LE(buf, 264, 0x0001) // vendor
        writeInt32LE(buf, 268, 0x0001) // product
        System.arraycopy(HID_DESCRIPTOR, 0, buf, 280, HID_DESCRIPTOR.size)
        out.write(buf)
        out.flush()
    }

    /**
     * Writes a UHID_INPUT2 event to /dev/uhid (10 bytes minimum for a 4-byte report).
     *
     * uhid_event layout for input2:
     *   [0..3] type (uint32 LE) = 12
     *   [4..5] size (uint16 LE) = 4
     *   [6..9] report data: [x, y, z, buttons]
     */
    private fun writeInput2(out: java.io.OutputStream, x: Byte, y: Byte, z: Byte, buttons: Byte) {
        val buf = ByteArray(10)
        writeInt32LE(buf, 0, UHID_INPUT2)
        writeInt16LE(buf, 4, 4)
        buf[6] = x
        buf[7] = y
        buf[8] = z
        buf[9] = buttons
        out.write(buf)
        out.flush()
    }

    /** Writes a UHID_DESTROY event to /dev/uhid. */
    private fun writeDestroy(out: java.io.OutputStream) {
        val buf = ByteArray(4)
        writeInt32LE(buf, 0, UHID_DESTROY)
        out.write(buf)
        out.flush()
    }
}
