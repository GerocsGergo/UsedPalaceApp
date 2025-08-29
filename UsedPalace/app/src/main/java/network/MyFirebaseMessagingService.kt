package network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.SaveFcmTokenRequest
import com.example.usedpalace.requests.SearchRequestID
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var apiService: ApiService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]

        if (!title.isNullOrEmpty() && !body.isNullOrEmpty()) {
            showNotification(title, body)
        }
    }


    private fun initialize() {
        RetrofitClient.init(this)
        apiService = RetrofitClient.apiService
    }


    private fun sendTokenToServer(token: String) {
        initialize()
        val userId = UserSession.getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                    // Most mentjük az FCM tokent a username-mel
                    val response = apiService.saveFcmToken(SaveFcmTokenRequest(userId, token))
                    if (response.success) {
                        withContext(Dispatchers.Main) {
                            ErrorHandler.logToLogcat("FCM", "Token saved successfully", ErrorHandler.LogLevel.DEBUG)
                        }
                    }
                 else {
                    withContext(Dispatchers.Main) {
                        ErrorHandler.logToLogcat("FCM", "Failed to fetch username", ErrorHandler.LogLevel.ERROR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.logToLogcat("FCM", "Failed to save token: ${e.message}")
                }
            }
        }
    }



    private fun showNotification(title: String?, message: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "channel_id_1"

        val channel =
            NotificationChannel(channelId, "Notification_Channel", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.baseline_eye_40)
            .build()

        notificationManager.notify(0, notification)
    }
}
