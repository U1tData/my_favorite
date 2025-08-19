package com.example.shizukusmartdnsvpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.shizukusmartdnsvpn.R
import com.example.shizukusmartdnsvpn.ui.MainActivity
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class SmartDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())

        val upstreams = intent?.getStringExtra("upstreams")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("1.1.1.1", "8.8.8.8")

        // Build VPN interface
        val builder = Builder()
            .setSession("ShizukuSmartDNS")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()

        // Start SmartDNS process via Shizuku if possible, fallback to app process
        executor.execute {
            startSmartDns(upstreams)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
        executor.shutdownNow()
    }

    private fun buildNotification(): Notification {
        val channelId = "smartdns_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
            channel.description = getString(R.string.notif_channel_desc)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.status_running))
            .setContentIntent(pi)
            .build()
    }

    private fun startSmartDns(upstreams: List<String>) {
        // For demo: write a minimal SmartDNS config to app files and start a local DNS server on 127.0.0.1:5354
        val conf = buildString {
            appendLine("bind [::]:5354")
            upstreams.forEach { u ->
                appendLine("server $u")
            }
        }
        val confFile = File(filesDir, "smartdns.conf")
        confFile.writeText(conf)

        // Normally you'd bundle SmartDNS binary for relevant ABIs and exec with Shizuku to gain CAP_NET_ADMIN to bind to low ports or set protect.
        // Here we simulate a long-running local process. In production, replace with real binary execution.
        if (Shizuku.pingBinder()) {
            // Example: Shizuku exec placeholder
            // Shizuku.newProcess(arrayOf("/data/local/tmp/smartdns", "-c", confFile.absolutePath), null, null)
        }
    }
}
