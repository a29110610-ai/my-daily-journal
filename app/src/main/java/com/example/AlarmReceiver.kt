package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "journal_reminder_channel"
        const val EXTRA_ENTRY_ID = "extra_entry_id"
        const val EXTRA_ENTRY_TITLE = "extra_entry_title"
        const val EXTRA_ENTRY_CONTENT = "extra_entry_content"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getIntExtra(EXTRA_ENTRY_ID, 0)
        val entryTitle = intent.getStringExtra(EXTRA_ENTRY_TITLE) ?: "یادآوری روزنامه من"
        val entryContent = intent.getStringExtra(EXTRA_ENTRY_CONTENT) ?: "زمان ثبت یادداشت شما فرا رسیده است!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "یادآور روزنامه من"
            val channelDescription = "نمایش اعلان‌های مربوط به یادداشت‌های ثبت شده"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when user clicks on the notification - opens MainActivity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_entry_id", entryId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            entryId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard reliable system icon
            .setContentTitle(entryTitle)
            .setContentText(entryContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(entryContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))

        notificationManager.notify(entryId, notificationBuilder.build())
    }
}
