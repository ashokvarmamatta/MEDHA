package com.ashes.dev.works.ai.neural.brain.medha.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.ashes.dev.works.ai.neural.brain.medha.MainActivity
import com.ashes.dev.works.ai.neural.brain.medha.R

/**
 * Foreground service that keeps Medha's offline model alive in the background.
 * Holds a wake lock so the CPU stays active for inference even when the screen is off.
 */
class MedhaService : Service() {

    companion object {
        private const val CHANNEL_ID = "medha_service"
        private const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TAG = "Medha::OfflineModel"
        // 10-minute safety cap — far more than a normal generation, but bounded so a
        // missed "inference off" can never pin the CPU awake for an hour.
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val ACTION_INFERENCE_ON = "medha.INFERENCE_ON"
        private const val ACTION_INFERENCE_OFF = "medha.INFERENCE_OFF"

        fun start(context: Context) = send(context, null)

        /** Acquire the wake lock for the duration of an active offline generation. */
        fun inferenceOn(context: Context) = send(context, ACTION_INFERENCE_ON)

        /** Release the wake lock when generation finishes (keeps the model resident, CPU free). */
        fun inferenceOff(context: Context) = send(context, ACTION_INFERENCE_OFF)

        private fun send(context: Context, action: String?) {
            val intent = Intent(context, MedhaService::class.java).apply { if (action != null) this.action = action }
            // minSdk is 31, so startForegroundService is always the right call.
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MedhaService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // The model staying resident does NOT need a wake lock — only the CPU work of an
        // active generation does. Acquire only between inference-on / inference-off.
        when (intent?.action) {
            ACTION_INFERENCE_ON -> acquireWakeLock()
            ACTION_INFERENCE_OFF -> releaseWakeLock()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }
        wakeLock?.let { if (!it.isHeld) it.acquire(WAKE_LOCK_TIMEOUT_MS) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medha AI Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the offline AI model running in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Medha AI")
            .setContentText("Offline model is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
