package com.turbotunnel.mm

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var btn: Button

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn()
        } else {
            showState(connecting = false, connected = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        btn = findViewById(R.id.connectBtn)

        btn.setOnClickListener {
            if (TunnelService.state.value) stopVpn() else requestVpn()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TunnelService.state.collect { connected ->
                    showState(connecting = false, connected = connected)
                }
            }
        }
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermission.launch(intent)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        showState(connecting = true, connected = false)
        startForegroundService(Intent(this, TunnelService::class.java))
    }

    private fun stopVpn() {
        startService(
            Intent(this, TunnelService::class.java).setAction(TunnelService.ACTION_STOP)
        )
    }

    private fun showState(connecting: Boolean, connected: Boolean) {
        when {
            connecting -> {
                status.text = "Connecting…"
                status.setTextColor(Color.parseColor("#9AA4B2"))
                btn.text = "…"
                btn.isEnabled = false
            }
            connected -> {
                status.text = "Connected"
                status.setTextColor(Color.parseColor("#4CAF50"))
                btn.text = "DISCONNECT"
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E88E5"))
                btn.isEnabled = true
            }
            else -> {
                status.text = "Disconnected"
                status.setTextColor(Color.parseColor("#9AA4B2"))
                btn.text = "CONNECT"
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2A2F3A"))
                btn.isEnabled = true
            }
        }
    }
}
