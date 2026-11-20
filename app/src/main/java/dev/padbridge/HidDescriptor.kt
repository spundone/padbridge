package dev.padbridge

/**
 * One composite HID report descriptor registered with the Android HID Device profile.
 *
 * Hosts cache this at pairing time. Any change here means users must unpair and re-pair,
 * so bump [VERSION] whenever the bytes change and keep it frozen otherwise.
 *
 *  Report 1  Keyboard  (input 8 bytes: mods, reserved, 6 keys; output 1 byte: LEDs)
 *  Report 2  Mouse     (input 7 bytes: buttons, X16, Y16, wheel, AC pan;
 *                       feature 1 byte: wheel + pan resolution multipliers)
 *  Report 3  Consumer  (input 2 bytes: one 16-bit consumer usage)
 *  Report 4  Gamepad   (input 9 bytes: 16 buttons, hat, X Y Z Rz, brake, accelerator)
 */
object HidDescriptor {
    /**
     * v1: keyboard, mouse, consumer, gamepad. v2 adds the Windows Precision Touchpad collections, v3 the N-key
     * rollover keyboard, v4 both; +4 with the drawing tablet (pen digitizer and absolute mouse).
     */
    val VERSION get() = 1 + (if (Settings.nativeTouchpad) 1 else 0) + (if (Settings.nkro) 2 else 0) +
        (if (Settings.tablet) 4 else 0)

    const val ID_KEYBOARD = 1
    const val ID_MOUSE = 2
    const val ID_CONSUMER = 3
    const val ID_GAMEPAD = 4

    const val KEYBOARD_LEN = 8
    const val MOUSE_LEN = 7
    const val CONSUMER_LEN = 2
    const val GAMEPAD_LEN = 9

    /** Drawing tablet (opt-in): a pen digitizer and an absolute mouse, both 0..[TABLET_MAX] over the whole screen. */
    const val ID_PEN = 0x0C
    const val ID_ABS = 0x0D
    const val PEN_LEN = 7
    const val ABS_LEN = 5
    const val TABLET_MAX = 32767
    const val PEN_PRESSURE_MAX = 4095

    /**
     * Pen: tip, barrel button, eraser, in range (hover), then X, Y and tip pressure. Windows Ink and Linux treat it as
     * a pen tablet (absolute position, hover, pressure). Absolute mouse: three buttons and X, Y; macOS (and anything
     * that ignores pens) moves its pointer straight to the spot.
     */
    val TABLET: ByteArray = b(
        0x05, 0x0D,        // Usage Page (Digitizers)
        0x09, 0x02,        // Usage (Pen)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_PEN,      //   Report ID
        0x09, 0x20,        //   Usage (Stylus)
        0xA1, 0x00,        //   Collection (Physical)
        0x09, 0x42,        //     Tip Switch
        0x09, 0x44,        //     Barrel Switch
        0x09, 0x45,        //     Eraser
        0x09, 0x32,        //     In Range
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x04,
        0x81, 0x02,        //     Input (Data, Var, Abs)
        0x95, 0x04, 0x81, 0x03, // 4 bits padding
        0x05, 0x01,        //     Usage Page (Generic Desktop)
        0x55, 0x0E,        //     Unit Exponent (-2)
        0x65, 0x13,        //     Unit (inch)
        0x35, 0x00,        //     Physical Min 0
        0x46, 0xE8, 0x03,  //     Physical Max 10.00 in
        0x09, 0x30,        //     Usage (X)
        0x15, 0x00, 0x26, 0xFF, 0x7F, 0x75, 0x10, 0x95, 0x01,
        0x81, 0x02,
        0x46, 0x71, 0x02,  //     Physical Max 6.25 in
        0x09, 0x31,        //     Usage (Y)
        0x81, 0x02,
        0x65, 0x00, 0x55, 0x00, 0x45, 0x00, // units off
        0x05, 0x0D,        //     Usage Page (Digitizers)
        0x09, 0x30,        //     Tip Pressure
        0x15, 0x00, 0x26, 0xFF, 0x0F, 0x75, 0x10, 0x95, 0x01,
        0x81, 0x02,
        0xC0,              //   End Collection
        0xC0,              // End Collection

        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x02,        // Usage (Mouse)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_ABS,      //   Report ID
        0x09, 0x01,        //   Usage (Pointer)
        0xA1, 0x00,        //   Collection (Physical)
        0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x03,
        0x81, 0x02,        //     Buttons 1-3
        0x95, 0x05, 0x81, 0x03,
        0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x15, 0x00, 0x26, 0xFF, 0x7F, 0x75, 0x10, 0x95, 0x02,
        0x81, 0x02,        //     X, Y (absolute)
        0xC0,
        0xC0,
    )

    /** N-key rollover keyboard (opt-in): modifiers + one bit per key from [NKRO_FIRST] to [NKRO_LAST]. */
    const val ID_NKRO = 0x0B
    const val NKRO_FIRST = 0x04
    const val NKRO_LAST = 0x7B
    const val NKRO_LEN = 1 + (NKRO_LAST - NKRO_FIRST + 1) / 8

    /** A second keyboard that reports every held key as a bit, so any number of keys can be down at once. */
    val NKRO: ByteArray = b(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x06,        // Usage (Keyboard)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_NKRO,     //   Report ID
        0x05, 0x07,        //   Usage Page (Key Codes)
        0x19, 0xE0,        //   Usage Min (Left Control)
        0x29, 0xE7,        //   Usage Max (Right GUI)
        0x15, 0x00,        //   Logical Min 0
        0x25, 0x01,        //   Logical Max 1
        0x75, 0x01,        //   Report Size 1
        0x95, 0x08,        //   Report Count 8
        0x81, 0x02,        //   Input (Data, Var, Abs)    modifiers
        0x19, NKRO_FIRST,  //   Usage Min (a)
        0x29, NKRO_LAST,   //   Usage Max
        0x95, NKRO_LAST - NKRO_FIRST + 1, // Report Count 120
        0x81, 0x02,        //   Input (Data, Var, Abs)    one bit per key
        0xC0,              // End Collection
    )

    /** Physical max of the resolution multiplier: each wheel unit is 1/8 notch once the host enables it. */
    const val HIRES_MULTIPLIER = 8

    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    val BYTES: ByteArray = b(
        // ---------- Keyboard ----------
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x06,        // Usage (Keyboard)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_KEYBOARD, //   Report ID
        0x05, 0x07,        //   Usage Page (Key Codes)
        0x19, 0xE0,        //   Usage Min (Left Control)
        0x29, 0xE7,        //   Usage Max (Right GUI)
        0x15, 0x00,        //   Logical Min 0
        0x25, 0x01,        //   Logical Max 1
        0x75, 0x01,        //   Report Size 1
        0x95, 0x08,        //   Report Count 8
        0x81, 0x02,        //   Input (Data, Var, Abs)    modifiers
        0x95, 0x01,        //   Report Count 1
        0x75, 0x08,        //   Report Size 8
        0x81, 0x01,        //   Input (Const)             reserved
        0x95, 0x05,        //   Report Count 5
        0x75, 0x01,        //   Report Size 1
        0x05, 0x08,        //   Usage Page (LEDs)
        0x19, 0x01,        //   Usage Min (Num Lock)
        0x29, 0x05,        //   Usage Max (Kana)
        0x91, 0x02,        //   Output (Data, Var, Abs)   LEDs
        0x95, 0x01,        //   Report Count 1
        0x75, 0x03,        //   Report Size 3
        0x91, 0x01,        //   Output (Const)            LED padding
        0x95, 0x06,        //   Report Count 6
        0x75, 0x08,        //   Report Size 8
        0x15, 0x00,        //   Logical Min 0
        0x26, 0xFF, 0x00,  //   Logical Max 255
        0x05, 0x07,        //   Usage Page (Key Codes)
        0x19, 0x00,        //   Usage Min 0
        0x2A, 0xFF, 0x00,  //   Usage Max 255
        0x81, 0x00,        //   Input (Data, Array)       key array
        0xC0,              // End Collection

        // ---------- Mouse ----------
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x02,        // Usage (Mouse)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_MOUSE,    //   Report ID
        0x09, 0x01,        //   Usage (Pointer)
        0xA1, 0x00,        //   Collection (Physical)
        0x05, 0x09,        //     Usage Page (Buttons)
        0x19, 0x01,        //     Usage Min 1
        0x29, 0x05,        //     Usage Max 5
        0x15, 0x00,        //     Logical Min 0
        0x25, 0x01,        //     Logical Max 1
        0x95, 0x05,        //     Report Count 5
        0x75, 0x01,        //     Report Size 1
        0x81, 0x02,        //     Input (Data, Var, Abs)
        0x95, 0x01,        //     Report Count 1
        0x75, 0x03,        //     Report Size 3
        0x81, 0x01,        //     Input (Const)
        0x05, 0x01,        //     Usage Page (Generic Desktop)
        0x09, 0x30,        //     Usage (X)
        0x09, 0x31,        //     Usage (Y)
        0x16, 0x01, 0x80,  //     Logical Min -32767
        0x26, 0xFF, 0x7F,  //     Logical Max 32767
        0x75, 0x10,        //     Report Size 16
        0x95, 0x02,        //     Report Count 2
        0x81, 0x06,        //     Input (Data, Var, Rel)
        // Vertical wheel with resolution multiplier
        0xA1, 0x02,        //     Collection (Logical)
        0x09, 0x48,        //       Usage (Resolution Multiplier)
        0x15, 0x00,        //       Logical Min 0
        0x25, 0x01,        //       Logical Max 1
        0x35, 0x01,        //       Physical Min 1
        0x45, HIRES_MULTIPLIER, // Physical Max 8
        0x75, 0x02,        //       Report Size 2
        0x95, 0x01,        //       Report Count 1
        0xB1, 0x02,        //       Feature (Data, Var, Abs)
        0x35, 0x00,        //       Physical Min 0
        0x45, 0x00,        //       Physical Max 0
        0x09, 0x38,        //       Usage (Wheel)
        0x15, 0x81,        //       Logical Min -127
        0x25, 0x7F,        //       Logical Max 127
        0x75, 0x08,        //       Report Size 8
        0x95, 0x01,        //       Report Count 1
        0x81, 0x06,        //       Input (Data, Var, Rel)
        0xC0,              //     End Collection
        // Horizontal pan with resolution multiplier
        0xA1, 0x02,        //     Collection (Logical)
        0x09, 0x48,        //       Usage (Resolution Multiplier)
        0x15, 0x00,        //       Logical Min 0
        0x25, 0x01,        //       Logical Max 1
        0x35, 0x01,        //       Physical Min 1
        0x45, HIRES_MULTIPLIER, // Physical Max 8
        0x75, 0x02,        //       Report Size 2
        0x95, 0x01,        //       Report Count 1
        0xB1, 0x02,        //       Feature (Data, Var, Abs)
        0x35, 0x00,        //       Physical Min 0
        0x45, 0x00,        //       Physical Max 0
        0x05, 0x0C,        //       Usage Page (Consumer)
        0x0A, 0x38, 0x02,  //       Usage (AC Pan)
        0x15, 0x81,        //       Logical Min -127
        0x25, 0x7F,        //       Logical Max 127
        0x75, 0x08,        //       Report Size 8
        0x95, 0x01,        //       Report Count 1
        0x81, 0x06,        //       Input (Data, Var, Rel)
        0xC0,              //     End Collection
        0x75, 0x04,        //     Report Size 4
        0x95, 0x01,        //     Report Count 1
        0xB1, 0x01,        //     Feature (Const)  pad feature byte
        0xC0,              //   End Collection
        0xC0,              // End Collection

        // ---------- Consumer control ----------
        0x05, 0x0C,        // Usage Page (Consumer)
        0x09, 0x01,        // Usage (Consumer Control)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_CONSUMER, //   Report ID
        0x15, 0x00,        //   Logical Min 0
        0x26, 0xFF, 0x03,  //   Logical Max 1023
        0x19, 0x00,        //   Usage Min 0
        0x2A, 0xFF, 0x03,  //   Usage Max 1023
        0x75, 0x10,        //   Report Size 16
        0x95, 0x01,        //   Report Count 1
        0x81, 0x00,        //   Input (Data, Array)
        0xC0,              // End Collection

        // ---------- Gamepad ----------
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x05,        // Usage (Gamepad)
        0xA1, 0x01,        // Collection (Application)
        0x85, ID_GAMEPAD,  //   Report ID
        0x05, 0x09,        //   Usage Page (Buttons)
        0x19, 0x01,        //   Usage Min 1
        0x29, 0x10,        //   Usage Max 16
        0x15, 0x00,        //   Logical Min 0
        0x25, 0x01,        //   Logical Max 1
        0x75, 0x01,        //   Report Size 1
        0x95, 0x10,        //   Report Count 16
        0x81, 0x02,        //   Input (Data, Var, Abs)
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x39,        //   Usage (Hat Switch)
        0x15, 0x00,        //   Logical Min 0
        0x25, 0x07,        //   Logical Max 7
        0x35, 0x00,        //   Physical Min 0
        0x46, 0x3B, 0x01,  //   Physical Max 315
        0x65, 0x14,        //   Unit (Degrees)
        0x75, 0x04,        //   Report Size 4
        0x95, 0x01,        //   Report Count 1
        0x81, 0x42,        //   Input (Data, Var, Abs, Null State)
        0x65, 0x00,        //   Unit (None)
        0x45, 0x00,        //   Physical Max 0
        0x75, 0x04,        //   Report Size 4
        0x95, 0x01,        //   Report Count 1
        0x81, 0x01,        //   Input (Const)
        0x09, 0x30,        //   Usage (X)    left stick
        0x09, 0x31,        //   Usage (Y)
        0x09, 0x32,        //   Usage (Z)    right stick
        0x09, 0x35,        //   Usage (Rz)
        0x15, 0x00,        //   Logical Min 0
        0x26, 0xFF, 0x00,  //   Logical Max 255
        0x75, 0x08,        //   Report Size 8
        0x95, 0x04,        //   Report Count 4
        0x81, 0x02,        //   Input (Data, Var, Abs)
        0x05, 0x02,        //   Usage Page (Simulation Controls)
        0x09, 0xC5,        //   Usage (Brake)        left trigger
        0x09, 0xC4,        //   Usage (Accelerator)  right trigger
        0x15, 0x00,        //   Logical Min 0
        0x26, 0xFF, 0x00,  //   Logical Max 255
        0x75, 0x08,        //   Report Size 8
        0x95, 0x02,        //   Report Count 2
        0x81, 0x02,        //   Input (Data, Var, Abs)
        0xC0,              // End Collection
    )

    // ---------------------------------------------------------------- Precision Touchpad (opt-in)
    //
    // Windows Precision Touchpad / Linux hid-multitouch collection, per Microsoft's PTP spec:
    // https://learn.microsoft.com/windows-hardware/design/component-guidelines/touchpad-windows-precision-touchpad-collection
    // The device starts in mouse mode (report 2). Only when the host writes Input Mode = 3 (report 8)
    // does the phone switch to sending raw contacts (report 5) and let the host do the gestures.

    const val ID_PTP_INPUT = 5
    const val ID_PTP_CAPS = 6
    const val ID_PTP_BLOB = 7
    const val ID_PTP_MODE = 8
    const val ID_PTP_SWITCH = 9
    const val ID_PTP_LATENCY = 0x0A
    const val PTP_FINGERS = 5
    const val PTP_LEN = PTP_FINGERS * 6 + 4 // 5 contacts + scan time + count + button
    const val PTP_MAX = 4095

    /** Microsoft's published default certification blob ("PTPHQA"), required to exist (signature optional on Win10+). */
    val PTPHQA: ByteArray = b(
        0xfc, 0x28, 0xfe, 0x84, 0x40, 0xcb, 0x9a, 0x87, 0x0d, 0xbe, 0x57, 0x3c, 0xb6, 0x70, 0x09, 0x88,
        0x07, 0x97, 0x2d, 0x2b, 0xe3, 0x38, 0x34, 0xb6, 0x6c, 0xed, 0xb0, 0xf7, 0xe5, 0x9c, 0xf6, 0xc2,
        0x2e, 0x84, 0x1b, 0xe8, 0xb4, 0x51, 0x78, 0x43, 0x1f, 0x28, 0x4b, 0x7c, 0x2d, 0x53, 0xaf, 0xfc,
        0x47, 0x70, 0x1b, 0x59, 0x6f, 0x74, 0x43, 0xc4, 0xf3, 0x47, 0x18, 0x53, 0x1a, 0xa2, 0xa1, 0x71,
        0xc7, 0x95, 0x0e, 0x31, 0x55, 0x21, 0xd3, 0xb5, 0x1e, 0xe9, 0x0c, 0xba, 0xec, 0xb8, 0x89, 0x19,
        0x3e, 0xb3, 0xaf, 0x75, 0x81, 0x9d, 0x53, 0xb9, 0x41, 0x57, 0xf4, 0x6d, 0x39, 0x25, 0x29, 0x7c,
        0x87, 0xd9, 0xb4, 0x98, 0x45, 0x7d, 0xa7, 0x26, 0x9c, 0x65, 0x3b, 0x85, 0x68, 0x89, 0xd7, 0x3b,
        0xbd, 0xff, 0x14, 0x67, 0xf2, 0x2b, 0xf0, 0x2a, 0x41, 0x54, 0xf0, 0xfd, 0x2c, 0x66, 0x7c, 0xf8,
        0xc0, 0x8f, 0x33, 0x13, 0x03, 0xf1, 0xd3, 0xc1, 0x0b, 0x89, 0xd9, 0x1b, 0x62, 0xcd, 0x51, 0xb7,
        0x80, 0xb8, 0xaf, 0x3a, 0x10, 0xc1, 0x8a, 0x5b, 0xe8, 0x8a, 0x56, 0xf0, 0x8c, 0xaa, 0xfa, 0x35,
        0xe9, 0x42, 0xc4, 0xd8, 0x55, 0xc3, 0x38, 0xcc, 0x2b, 0x53, 0x5c, 0x69, 0x52, 0xd5, 0xc8, 0x73,
        0x02, 0x38, 0x7c, 0x73, 0xb6, 0x41, 0xe7, 0xff, 0x05, 0xd8, 0x2b, 0x79, 0x9a, 0xe2, 0x34, 0x60,
        0x8f, 0xa3, 0x32, 0x1f, 0x09, 0x78, 0x62, 0xbc, 0x80, 0xe3, 0x0f, 0xbd, 0x65, 0x20, 0x08, 0x13,
        0xc1, 0xe2, 0xee, 0x53, 0x2d, 0x86, 0x7e, 0xa7, 0x5a, 0xc5, 0xd3, 0x7d, 0x98, 0xbe, 0x31, 0x48,
        0x1f, 0xfb, 0xda, 0xaf, 0xa2, 0xa8, 0x6a, 0x89, 0xd6, 0xbf, 0xf2, 0xd3, 0x32, 0x2a, 0x9a, 0xe4,
        0xcf, 0x17, 0xb7, 0xb8, 0xf4, 0xe1, 0x33, 0x08, 0x24, 0x8b, 0xc4, 0x43, 0xa5, 0xe5, 0x24, 0xc2,
    )

    /** PTP collections; [physW]/[physH] are the pad's physical size in 0.01 inch. */
    fun ptp(physW: Int, physH: Int): ByteArray {
        val finger = b(
            0x05, 0x0D, 0x09, 0x22, 0xA1, 0x02,             // Digitizer / Finger / Logical
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x02,  //  0..1, 1 bit x2
            0x55, 0x00, 0x65, 0x00, 0x35, 0x00, 0x45, 0x00,  //  reset unit / exponent / physical
            0x09, 0x47, 0x09, 0x42, 0x81, 0x02,              //  Confidence, Tip Switch
            0x95, 0x06, 0x81, 0x03,                          //  6-bit pad
            0x25, 0x0F, 0x75, 0x08, 0x95, 0x01, 0x09, 0x51, 0x81, 0x02, // Contact ID (8 bits)
            0x05, 0x01, 0x26, 0xFF, 0x0F, 0x75, 0x10,        //  GD, logical max 4095, 16 bits
            0x55, 0x0E, 0x65, 0x13,                          //  exponent -2, unit inch
            0x46, physW and 0xFF, (physW shr 8) and 0xFF, 0x09, 0x30, 0x81, 0x02, // X
            0x46, physH and 0xFF, (physH shr 8) and 0xFF, 0x09, 0x31, 0x81, 0x02, // Y
            0xC0,
        )
        val head = b(0x05, 0x0D, 0x09, 0x05, 0xA1, 0x01, 0x85, ID_PTP_INPUT) // Touch Pad / Application
        val tail = b(
            0x05, 0x0D, 0x55, 0x0C, 0x66, 0x01, 0x10,        // exponent -4, unit seconds
            0x47, 0xFF, 0xFF, 0x00, 0x00, 0x27, 0xFF, 0xFF, 0x00, 0x00,
            0x75, 0x10, 0x95, 0x01, 0x09, 0x56, 0x81, 0x02,  // Scan Time (100 us units)
            0x55, 0x00, 0x65, 0x00, 0x45, 0x00, 0x25, 0x7F, 0x75, 0x08, 0x09, 0x54, 0x81, 0x02, // Contact Count
            0x05, 0x09, 0x09, 0x01, 0x25, 0x01, 0x75, 0x01, 0x95, 0x01, 0x81, 0x02,  // Button 1
            0x95, 0x07, 0x81, 0x03,                          // pad
            0x05, 0x0D, 0x85, ID_PTP_CAPS, 0x09, 0x55, 0x09, 0x59, 0x75, 0x04, 0x95, 0x02, 0x25, 0x0F, 0xB1, 0x02, // max contacts / pad type
            0x85, ID_PTP_LATENCY, 0x09, 0x60, 0x75, 0x01, 0x95, 0x01, 0x25, 0x01, 0xB1, 0x02, 0x95, 0x07, 0xB1, 0x03, // latency
            0x06, 0x00, 0xFF, 0x85, ID_PTP_BLOB, 0x09, 0xC5, 0x15, 0x00, 0x26, 0xFF, 0x00,
            0x75, 0x08, 0x96, 0x00, 0x01, 0xB1, 0x02,        // certification blob, 256 bytes
            0xC0,
            0x05, 0x0D, 0x09, 0x0E, 0xA1, 0x01,              // Configuration / Application
            0x85, ID_PTP_MODE, 0x09, 0x22, 0xA1, 0x02, 0x09, 0x52, 0x15, 0x00, 0x25, 0x0A, 0x75, 0x08, 0x95, 0x01, 0xB1, 0x02, 0xC0, // Input Mode
            0x09, 0x22, 0xA1, 0x00, 0x85, ID_PTP_SWITCH, 0x09, 0x57, 0x09, 0x58, 0x75, 0x01, 0x95, 0x02, 0x25, 0x01, 0xB1, 0x02,
            0x95, 0x06, 0xB1, 0x03, 0xC0,                    // Surface / Button switch
            0xC0,
        )
        var out = head
        repeat(PTP_FINGERS) { out += finger }
        return out + tail
    }

    /** The descriptor to register: base, plus N-key rollover and PTP when they're on. */
    fun current(physW: Int, physH: Int): ByteArray =
        BYTES + (if (Settings.nkro) NKRO else ByteArray(0)) + (if (Settings.nativeTouchpad) ptp(physW, physH) else ByteArray(0)) +
            (if (Settings.tablet) TABLET else ByteArray(0))
}
