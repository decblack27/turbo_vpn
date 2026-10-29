package com.turbotunnel.mm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TunnelService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTunnel()
            return START_NOT_STICKY
        }
        startTunnel()
        return START_NOT_STICKY
    }

    private fun startTunnel() {
        startForegroundCompat()
        try {
            tun = Builder()
                .setSession(getString(R.string.app_name))
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setMtu(1500)
                .establish()

            if (tun == null) error("VPN interface establish failed")

            // TODO: 1) Xray core start (libv2ray)
            //       2) tun fd -> tun2socks -> 127.0.0.1:10808
            //       ဒီနှစ်ခု မပြီးမချင်း traffic မသွားပါ။

            _state.value = true
            Log.i(TAG, "Tunnel up")
        } catch (e: Exception) {
            Log.e(TAG, "Start failed", e)
            _state.value = false
            stopTunnel()
        }
    }

    private fun stopTunnel() {
        tun?.close()
        tun = null
        _state.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tun?.close()
        tun = null
        _state.value = false
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VPN status", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Connected")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    companion object {
        const val ACTION_STOP = "com.turbotunnel.mm.STOP"
        private const val TAG = "TunnelService"
        private const val CHANNEL_ID = "tunnel"
        private const val NOTIF_ID = 1

        private val _state = MutableStateFlow(false)
        val state: StateFlow<Boolean> = _state.asStateFlow()
    }
}
