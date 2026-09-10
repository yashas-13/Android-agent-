package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID_BROADCAST = "channel_broadcast_status"
    private const val CHANNEL_NAME_BROADCAST = "WhatsApp Broadcast Alerts"

    const val CHANNEL_ID_OVERLAY = "channel_agent_overlay"
    private const val CHANNEL_NAME_OVERLAY = "AI Phone Agent Floating HUD"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val broadcastChannel = NotificationChannel(
                CHANNEL_ID_BROADCAST,
                CHANNEL_NAME_BROADCAST,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for completed or failed multi-group broadcasts"
            }

            val overlayChannel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                CHANNEL_NAME_OVERLAY,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent indicator when AI Agent Floating HUD is active"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(broadcastChannel)
            manager.createNotificationChannel(overlayChannel)
        }
    }

    fun showBroadcastSuccess(context: Context, groupCount: Int, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BROADCAST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ Broadcast Complete: $title")
            .setContentText("Successfully shared message to all $groupCount WhatsApp groups.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    fun showBroadcastFailure(context: Context, title: String, failedCount: Int, reason: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BROADCAST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Broadcast Alert: $title")
            .setContentText("$failedCount group(s) failed: $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Failed to deliver to $failedCount group(s).\nReason: $reason\nTap to inspect and report."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
