package com.example.shizukusmartdnsvpn.shizuku

import rikka.shizuku.Shizuku

object ShizukuRunner {
    fun canUse(): Boolean = Shizuku.pingBinder()

    fun exec(cmd: Array<String>): Int {
        if (!canUse()) return -1
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, cmd, null, "/") as Process
            process.waitFor()
        } catch (e: Throwable) {
            -2
        }
    }
}
