package com.arrazolapp.gpstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val prefs = context.getSharedPreferences("agent_config", Context.MODE_PRIVATE)
            val userId = prefs.getString("userId", "") ?: ""

            if (userId.isNotEmpty()) {
                // ── Si el tracking estaba activo antes de apagarse →
                //    reiniciarlo directamente en modo tracking, no en standby ──
                val wasTracking = prefs.getBoolean("trackingActive", false)
                val action = if (wasTracking) {
                    Log.d("BootReceiver", "Celular encendido — reiniciando tracking automáticamente")
                    TrackingService.ACTION_START
                } else {
                    Log.d("BootReceiver", "Celular encendido — iniciando en STANDBY")
                    TrackingService.ACTION_STANDBY
                }

                val serviceIntent = Intent(context, TrackingService::class.java).apply {
                    this.action = action
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
