package com.example.musicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


enum class Actions {
    PLAY_PAUSE, NEXT, PREV
}

class MusicPlayerService : Service() {

    val binder by lazy {
        MusicPlayerBinder()
    }

    private var mediaPlayer = MediaPlayer()

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
        fun setMusicList(songs: List<Track>) {
            musicList.update { songs }
        }
        fun getCurrentTrack() = currentTrack
        fun getIsPlaying() = isPlaying
        fun getMaxDuration() = maxDuration
        fun getCurrentDuration() = currentDuration
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private var musicList = MutableStateFlow(emptyList<Track>())
    private var currentTrack = MutableStateFlow(Track("", "", R.raw.one, R.drawable.music))
    private val scope = CoroutineScope(Dispatchers.Default)
    private var maxDuration = MutableStateFlow(0f)
    private var currentDuration = MutableStateFlow(0f)
    private var isPlaying = MutableStateFlow(false)
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Actions.PREV.name -> {
                    prev()
                }
                Actions.PLAY_PAUSE.name -> {
                    playPause()
                }
                Actions.NEXT.name -> {
                    next()
                }
                else -> {
                    currentTrack.update { songs[0] }
                    playTrack(currentTrack.value)
                }
            }
        }
        return START_STICKY
    }
    fun playPause(){
        if(mediaPlayer.isPlaying){
            mediaPlayer.pause()
        }else {
            mediaPlayer.start()
        }
        sendNotification(currentTrack.value)
    }

    fun next(){
        val currIdx = songs.indexOf(currentTrack.value)
        val nextIdx = (currIdx + 1) % songs.size
        currentTrack.update { songs.get(nextIdx) }
        playTrack(currentTrack.value)
    }

    fun prev(){
        val currIdx = songs.indexOf(currentTrack.value)
        val prevIdx = if (currIdx == 0) songs.size - 1 else currIdx - 1
        currentTrack.update { songs.get(prevIdx) }
        playTrack(currentTrack.value)
    }

    private fun updateDuration(){
        job = scope.launch {
            if (!mediaPlayer.isPlaying)
                return@launch
            maxDuration.update { mediaPlayer.duration.toFloat() }
            while(true){
                currentDuration.update { mediaPlayer.currentPosition.toFloat() }
                delay(1000)
            }
        }
    }

    private fun playTrack(track: Track) {
        job?.cancel()
        mediaPlayer.reset()
        mediaPlayer = MediaPlayer.create(this, track.resId)
        mediaPlayer.start()
        sendNotification(track)
        updateDuration()
    }

    @SuppressLint("RestrictedApi")
    @OptIn(UnstableApi::class)
    private fun sendNotification(track: Track) {
        isPlaying.update { mediaPlayer.isPlaying }
        val mediaSession = MediaSessionCompat(this, "music_player")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.name)
            .setContentText(track.desc)
            .addAction(R.drawable.ic_prev, Actions.PREV.name, createPrevPendingIntent())
            .addAction(
                if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                Actions.PLAY_PAUSE.name,
                createPlayPausePendingIntent()
            )
            .addAction(R.drawable.ic_next, Actions.NEXT.name, createNextPendingIntent())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(resources, track.imageId))
            .setOngoing(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // check for notification permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startForeground(1, notification)
            }
        } else {
            startForeground(1, notification)
        }
    }

    private fun createPrevPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            action = Actions.PREV.name
        }
        // creates a pendingIntent that will start a service, like calling context.startService()
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPlayPausePendingIntent(): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            action = Actions.PLAY_PAUSE.name
        }
        // creates a pendingIntent that will start a service, like calling context.startService()
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNextPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).apply {
            action = Actions.NEXT.name
        }
        // creates a pendingIntent that will start a service, like calling context.startService()
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}