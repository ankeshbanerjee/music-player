package com.example.musicplayer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentCompositionErrors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.ui.theme.MusicPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var musicPlayerService: MusicPlayerService? = null
    private var isBound: Boolean = false
    private val currentTrack = MutableStateFlow(Track("", "", R.raw.one, R.drawable.music))
    private val isPlaying = MutableStateFlow(false)
    private val maxDuration = MutableStateFlow(0f)
    private val currentDuration = MutableStateFlow(0f)
    private var connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicPlayerService = (service as MusicPlayerService.MusicPlayerBinder).getService()
            service.setMusicList(songs)
            CoroutineScope(Dispatchers.Main).launch {
                service.getCurrentTrack().collectLatest {
                    currentTrack.value = it
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                service.getIsPlaying().collectLatest {
                    isPlaying.value = it
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                service.getMaxDuration().collectLatest {
                    maxDuration.value = it
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                service.getCurrentDuration().collectLatest {
                    currentDuration.value = it
                }
            }
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicPlayerService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                val curTrack = currentTrack.collectAsState()
                val isPlayingNow = isPlaying.collectAsState()
                val maxDur = maxDuration.collectAsState()
                val curDur = currentDuration.collectAsState()
                fun prev() {
                    musicPlayerService?.prev()
                }
                fun next() {
                    musicPlayerService?.next()
                }
                fun playPause() {
                    musicPlayerService?.playPause()
                }
                fun startForegroundService() {
                    val intent = Intent(this, MusicPlayerService::class.java)
                    startService(intent)
                    bindService(intent, connection, BIND_AUTO_CREATE)
                }
                fun stopForegroundService() {
                    val intent = Intent(this, MusicPlayerService::class.java)
                    stopService(intent)
                    unbindService(connection)
                }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
                    }
                }
                MusicPlayerContent(
                    currentTrack = curTrack.value,
                    maxDuration = maxDur.value,
                    currentDuration = curDur.value,
                    isPlaying = isPlayingNow.value,
                    prev = ::prev,
                    next = ::next,
                    playPause = ::playPause,
                    startForegroundService = ::startForegroundService,
                    stopForegroundService = ::stopForegroundService
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerContent(
    currentTrack: Track,
    maxDuration: Float,
    currentDuration: Float,
    isPlaying: Boolean,
    prev: () -> Unit,
    next: () -> Unit,
    playPause: () -> Unit,
    startForegroundService: () -> Unit,
    stopForegroundService: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        TopAppBar(title = { Text("Music Player") },
            actions = {
                IconButton(onClick = {
                    startForegroundService()
                }) {
                    Icon(Icons.Filled.PlayArrow, "play")
                }
                IconButton(onClick = {
                    stopForegroundService()
                }) {
                    Icon(Icons.Default.Close, "close")
                }
            })
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painterResource(id = currentTrack.imageId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(currentTrack.name, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 24.sp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(currentTrack.desc, style = TextStyle(fontWeight = FontWeight.Normal, fontSize = 18.sp))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text((currentDuration / 1000).toString())
            Slider(
                value = currentDuration,
                onValueChange = {},
                valueRange = 0f..maxDuration,
                modifier = Modifier.weight(1f)
            )
            Text((maxDuration / 1000).toString())
        }
        Row {
            IconButton(onClick = {
                prev()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_prev),
                    contentDescription = "prev"
                )
            }
            IconButton(onClick = {
                playPause()
            }) {
                Icon(
                    painter = if (isPlaying) painterResource(id = R.drawable.ic_pause) else painterResource(
                        id = R.drawable.ic_play
                    ), contentDescription = "play_pause"
                )
            }
            IconButton(onClick = {
                next()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_next),
                    contentDescription = "next"
                )
            }
        }

    }

}

@Preview
@Composable
fun MusicPlayerContentPreview() {
    MusicPlayerContent(
        Track("First Song", "First Song Description", R.raw.one, R.drawable.one),
        10000f,
        1000f,
        true,
        {},
        {},
        {},
        {},
        {}
    )
}
