package com.example.edkura.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.edkura.DashboardActivity
import com.example.edkura.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "edkura_notifications"
        private const val CHANNEL_NAME = "EdKura Notifications"
        private const val CHANNEL_DESC = "Notifications for new messages or requests."
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Log new token for debugging:
        android.util.Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Log the received message
        android.util.Log.d("FCM", "Message received: ${remoteMessage.data}")

        // If you want to force a notification to display even when the app is in foreground:
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "EdKura Notification"
        val message = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: "You have a new notification."
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Create channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = CHANNEL_DESC
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists!
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Use NotificationManagerCompat to show the notification
        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
