package com.example.shizukusmartdnsvpn.shizuku

import android.os.Build
import rikka.shizuku.Shizuku

object ShizukuRunner {
    fun canUse(): Boolean = Shizuku.pingBinder()

    fun exec(cmd: Array<String>): Int {
        if (!canUse()) return -1
        return try {
            val process = Shizuku.newProcess(cmd, null, "/")
            process.waitFor()
        } catch (e: Throwable) {
            -2
        }
    }
}
