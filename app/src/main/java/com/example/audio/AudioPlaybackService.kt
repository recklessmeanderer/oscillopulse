package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AudioPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "oscillopulse_audio_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_TOGGLE = "com.example.action.TOGGLE"
        const val ACTION_STOP = "com.example.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var audioEngine: AudioEngine
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: android.media.AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioEngine.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioEngine.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Dim synthesis volume temporarily during notification chimes / GPS alerts
                audioEngine.duckTarget = 0.25f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Restore volume smoothly
                audioEngine.duckTarget = 1.0f
            }
        }
    }

    private var isReceiverRegistered = false
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // If headphones or Bluetooth disconnect, safely pause audio synthesis
                audioEngine.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
        setupMediaSession()
        registerBecomingNoisyReceiver()

        // Observe playback state changes to update the sticky notification, MediaSession, and AudioFocus
        serviceScope.launch {
            audioEngine.isPlaying.collectLatest { isPlaying ->
                if (isPlaying) {
                    requestAudioFocus()
                } else {
                    audioEngine.duckTarget = 1.0f
                    abandonAudioFocus()
                }
                updatePlaybackState(isPlaying)
                updateNotification(isPlaying)
            }
        }
    }

    private fun requestAudioFocus() {
        val am = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager).also { audioManager = it } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification(audioEngine.isPlaying.value)
                startServiceAsForeground(notification)
            }
            ACTION_PLAY -> {
                audioEngine.start()
                updateNotification(true)
            }
            ACTION_PAUSE -> {
                audioEngine.stop()
                updateNotification(false)
            }
            ACTION_TOGGLE -> {
                audioEngine.togglePlayback()
            }
            ACTION_STOP -> {
                audioEngine.stop()
                stopForegroundService()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    private fun startServiceAsForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Synthesis",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing synthesis playback controls and status"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "OscilloPulseMediaSession").apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    audioEngine.start()
                }

                override fun onPause() {
                    audioEngine.stop()
                }

                override fun onStop() {
                    audioEngine.stop()
                    stopForegroundService()
                }

                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }

                    if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                audioEngine.start()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                audioEngine.stop()
                                return true
                            }
                            KeyEvent.KEYCODE_HEADSETHOOK,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                audioEngine.togglePlayback()
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                audioEngine.stop()
                                stopForegroundService()
                                return true
                            }
                        }
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })

            isActive = true
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_STOP

        val playbackSpeed = if (isPlaying) 1.0f else 0.0f
        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, playbackSpeed)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notification = buildNotification(isPlaying)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Play/Pause Toggle
        val toggleAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val toggleIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
        val toggleTitle = if (isPlaying) "Pause" else "Play"

        val toggleIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Stop
        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = audioEngine.getSummaryDescription()
        val statusPrefix = if (isPlaying) "Playing" else "Paused"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wave)
            .setContentTitle("OscilloPulse Synthesizer ($statusPrefix)")
            .setContentText(summary)
            .setContentIntent(appPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(toggleIcon, toggleTitle, togglePendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun registerBecomingNoisyReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(becomingNoisyReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterBecomingNoisyReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterBecomingNoisyReceiver()
        abandonAudioFocus()
        mediaSession?.let {
            it.isActive = false
            it.release()
        }
        mediaSession = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
