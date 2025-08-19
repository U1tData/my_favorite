package com.example.shizukusmartdnsvpn.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.shizukusmartdnsvpn.R
import com.example.shizukusmartdnsvpn.vpn.SmartDnsVpnService
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val prepareVpn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            startService(Intent(this, SmartDnsVpnService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRequest = findViewById<Button>(R.id.btnRequest)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val status = findViewById<TextView>(R.id.txtStatus)
        val etUpstreams = findViewById<EditText>(R.id.etUpstreams)

        btnRequest.setOnClickListener {
            if (Shizuku.pingBinder()) {
                status.text = getString(R.string.status_shizuku_granted)
            } else {
                Shizuku.requestPermission(0)
            }
        }

        btnStart.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                prepareVpn.launch(intent)
            } else {
                startService(Intent(this, SmartDnsVpnService::class.java).apply {
                    putExtra("upstreams", etUpstreams.text.toString())
                })
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, SmartDnsVpnService::class.java))
        }
    }
}
