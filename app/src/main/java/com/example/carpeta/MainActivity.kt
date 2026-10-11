package com.example.carpeta

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var isAppMinimized = false
    private val handler = Handler(Looper.getMainLooper())

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val p = player ?: return
            when (intent?.action) {
                ACTION_PREVIOUS -> {
                    if (p.mediaItemCount > 0) {
                        p.seekToPreviousMediaItem()
                        p.prepare()
                        p.play()
                    }
                    if (isAppMinimized) {
                        handler.postDelayed({ showNotification() }, 150)
                    }
                }
                ACTION_PLAY_PAUSE -> {
                    if (p.isPlaying) {
                        p.pause()
                    } else {
                        p.play()
                    }
                    if (isAppMinimized) {
                        handler.postDelayed({ showNotification() }, 150)
                    }
                }
                ACTION_NEXT -> {
                    if (p.mediaItemCount > 0) {
                        p.seekToNextMediaItem()
                        p.prepare()
                        p.play()
                    }
                    if (isAppMinimized) {
                        handler.postDelayed({ showNotification() }, 150)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
        }
        ContextCompat.registerReceiver(
            this,
            mediaReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }

        setContent {
            MusicaTheme {
                player?.let { MusicPlayerScreen(it) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isAppMinimized = false
        hideNotification()
    }

    override fun onStop() {
        super.onStop()
        isAppMinimized = true
        showNotification()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(mediaReceiver)
        } catch (_: Exception) {}
        hideNotification()
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de reproducción",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Controles de reproducción mientras la app está minimizada"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val p = player ?: return
        val currentMediaItem = p.currentMediaItem ?: return
        val title = currentMediaItem.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Música"
        val isPlaying = p.isPlaying || p.playWhenReady

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val prevIntent = PendingIntent.getBroadcast(
            this, 1, Intent(ACTION_PREVIOUS).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            this, 2, Intent(ACTION_PLAY_PAUSE).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val nextIntent = PendingIntent.getBroadcast(
            this, 3, Intent(ACTION_NEXT).setPackage(packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pausar" else "Reproducir"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("Música")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", nextIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2),
            )

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun hideNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "music_playback_channel"
        const val ACTION_PREVIOUS = "com.example.carpeta.ACTION_PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.carpeta.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.carpeta.ACTION_NEXT"
    }
}

@Composable
private fun MusicaTheme(content: @Composable () -> Unit) {
    val primaryRed = Color(0xFFE53935)
    val accentRed = Color(0xFFFF5252)
    val darkBackground = Color(0xFF121212)
    val darkSurface = Color(0xFF1E1E1E)
    val darkSurfaceVariant = Color(0xFF2A2A2A)
    val textPrimary = Color(0xFFEEEEEE)
    val textSecondary = Color(0xFFA0A0A0)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = primaryRed,
            onPrimary = Color.White,
            secondary = accentRed,
            background = darkBackground,
            onBackground = textPrimary,
            surface = darkSurface,
            onSurface = textPrimary,
            surfaceVariant = darkSurfaceVariant,
            onSurfaceVariant = textSecondary,
        ),
        content = content,
    )
}

@Composable
private fun MusicPlayerScreen(player: ExoPlayer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current

    var library by remember { mutableStateOf(emptyList<Track>()) }
    var selectedId by remember { mutableStateOf(MusicLibrary.ALL_MUSIC_ID) }
    var search by remember { mutableStateOf("") }
    var permissionGranted by remember { mutableStateOf(hasPermissions(context)) }
    var refreshing by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(player.isPlaying) }
    var currentId by remember { mutableStateOf(player.currentMediaItem?.mediaId) }
    var position by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeatOne by remember { mutableStateOf(player.repeatMode == Player.REPEAT_MODE_ONE) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }

    val playlists = remember(library) { MusicLibrary.playlists(library) }
    val selectedBase = playlists.firstOrNull { it.id == selectedId } ?: playlists.first()

    val activeTracks = selectedBase.tracks
    val visibleTracks = remember(activeTracks, search) { MusicLibrary.search(activeTracks, search) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
    }

    fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        }
        permissionLauncher.launch(permissions)
    }

    fun scan(keepCurrentTrack: Boolean) {
        if (!hasPermissions(context)) {
            permissionGranted = false
            requestPermissions()
            return
        }
        scope.launch {
            refreshing = true
            val wasPlaying = player.isPlaying
            val oldId = player.currentMediaItem?.mediaId
            val oldPosition = player.currentPosition.coerceAtLeast(0L)
            val scanned = withContext(Dispatchers.IO) {
                MediaStoreScanner(context.contentResolver).scan()
            }
            val updatedPlaylists = MusicLibrary.playlists(scanned)
            val active = updatedPlaylists.firstOrNull { it.id == selectedId } ?: updatedPlaylists.first()
            library = scanned
            selectedId = active.id

            val index = if (keepCurrentTrack && oldId != null) active.tracks.indexOfFirst { it.id == oldId } else -1
            val queue = active.tracks.map(::toMediaItem)
            if (queue.isNotEmpty()) {
                player.stop()
                player.shuffleModeEnabled = shuffle
                player.setMediaItems(queue, index.takeIf { it >= 0 } ?: 0, if (index >= 0) oldPosition else 0L)
                player.prepare()
            } else {
                player.clearMediaItems()
            }
            if (keepCurrentTrack && wasPlaying && index >= 0) player.play() else player.pause()
            refreshing = false
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) scan(keepCurrentTrack = false) else requestPermissions()
    }

    DisposableEffect(lifecycleOwner, player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { currentId = mediaItem?.mediaId }
            override fun onRepeatModeChanged(repeatMode: Int) { repeatOne = repeatMode == Player.REPEAT_MODE_ONE }
        }
        player.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playing = player.isPlaying
                currentId = player.currentMediaItem?.mediaId
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            player.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(playing, currentId) {
        while (isActive) {
            duration = player.duration.takeIf { it > 0 }?.toFloat() ?: 0f
            position = player.currentPosition.coerceAtLeast(0L).toFloat()
            delay(if (playing) 500L else 1000L)
        }
    }

    if (trackForDetails != null) {
        val track = trackForDetails!!
        AlertDialog(
            onDismissRequest = { trackForDetails = null },
            title = { Text("Detalles de la canción", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Título:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(track.title, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Carpeta:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(track.folderName, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Ruta absoluta:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(
                        track.filePath.ifBlank { "No disponible" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { trackForDetails = null }) {
                    Text("Cerrar")
                }
            },
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val topBar = @Composable {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaylistPicker(playlists, selectedBase) { playlist ->
                    selectedId = playlist.id
                    search = ""
                    shuffle = false
                    player.shuffleModeEnabled = false
                    player.stop()
                    val queue = playlist.tracks.map(::toMediaItem)
                    player.setMediaItems(queue)
                    if (queue.isNotEmpty()) player.prepare()
                    player.pause()
                }
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "MÚSICA",
                    Modifier.weight(1f).padding(start = 8.dp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = { scan(keepCurrentTrack = true) }, enabled = permissionGranted && !refreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar música", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        val searchBar = @Composable {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar en ${selectedBase.name}") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
            )
        }

        val nowPlaying = @Composable {
            NowPlayingBar(
                title = activeTracks.firstOrNull { it.id == currentId }?.title ?: "Ninguna canción en reproducción",
                playing = playing,
                position = position,
                duration = duration,
                shuffle = shuffle,
                repeatOne = repeatOne,
                enabled = activeTracks.isNotEmpty(),
                onPlayPause = {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.currentMediaItem == null && activeTracks.isNotEmpty()) {
                            val queue = activeTracks.map(::toMediaItem)
                            player.setMediaItems(queue)
                            player.prepare()
                        }
                        player.play()
                    }
                },
                onPrevious = {
                    if (player.mediaItemCount > 0) {
                        player.seekToPreviousMediaItem()
                        player.prepare()
                        player.play()
                    }
                },
                onNext = {
                    if (player.mediaItemCount > 0) {
                        player.seekToNextMediaItem()
                        player.prepare()
                        player.play()
                    }
                },
                onShuffle = {
                    shuffle = !shuffle
                    player.shuffleModeEnabled = shuffle
                },
                onRepeat = {
                    player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                        Player.REPEAT_MODE_ALL
                    } else {
                        Player.REPEAT_MODE_ONE
                    }
                },
                onSeek = { player.seekTo(it.toLong()) },
            )
        }

        val songsList = @Composable { modifier: Modifier ->
            Column(modifier) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedBase.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        val countText = if (visibleTracks.size == 1) "1 canción" else "${visibleTracks.size} canciones"
                        Text(countText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (refreshing) Text("Actualizando…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }

                when {
                    !permissionGranted -> EmptyState(
                        modifier = Modifier.weight(1f),
                        title = "Acceso a tu música",
                        message = "Permite el acceso al almacenamiento/audio para organizar tus archivos MP3.",
                        action = "Conceder permiso",
                        onAction = ::requestPermissions,
                    )
                    library.isEmpty() && !refreshing -> EmptyState(
                        modifier = Modifier.weight(1f),
                        title = "Sin canciones MP3",
                        message = "No se encontraron archivos MP3 en la memoria. Agrega canciones y presiona actualizar.",
                        action = "Actualizar",
                        onAction = { scan(false) },
                    )
                    visibleTracks.isEmpty() -> EmptyState(
                        modifier = Modifier.weight(1f),
                        title = "Sin resultados",
                        message = "No hay canciones con ese nombre en esta lista.",
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        items(visibleTracks, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                playing = track.id == currentId && playing,
                                onClick = {
                                    player.stop()
                                    val queue = activeTracks.map(::toMediaItem)
                                    val index = activeTracks.indexOfFirst { it.id == track.id }
                                    player.setMediaItems(queue, index.coerceAtLeast(0), 0L)
                                    player.prepare()
                                    player.play()
                                },
                                onLongClick = {
                                    trackForDetails = track
                                },
                            )
                        }
                    }
                }
            }
        }

        if (isLandscape) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                topBar()
                Row(
                    Modifier.fillMaxSize().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        Modifier.weight(0.42f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        searchBar()
                        Spacer(Modifier.height(12.dp))
                        nowPlaying()
                    }
                    songsList(Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                topBar()
                searchBar()
                Spacer(Modifier.height(10.dp))
                nowPlaying()
                Spacer(Modifier.height(6.dp))
                songsList(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PlaylistPicker(playlists: List<MusicPlaylist>, selected: MusicPlaylist, onSelect: (MusicPlaylist) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Seleccionar lista de reproducción", tint = MaterialTheme.colorScheme.onBackground)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            playlists.forEach { playlist ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(playlist.name, fontWeight = if (playlist.id == selected.id) FontWeight.Bold else FontWeight.Normal)
                            val playlistCountText = if (playlist.tracks.size == 1) "1 canción" else "${playlist.tracks.size} canciones"
                            Text(playlistCountText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { expanded = false; onSelect(playlist) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(track: Track, playing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (playing) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (playing) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            track.title,
            Modifier.weight(1f).padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier, title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(message, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (action != null && onAction != null) {
            Button(onClick = onAction, Modifier.padding(top = 16.dp)) { Text(action) }
        }
    }
}

@Composable
private fun NowPlayingBar(
    title: String,
    playing: Boolean,
    position: Float,
    duration: Float,
    shuffle: Boolean,
    repeatOne: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    val currentStr = formatTime(position)
    val durationStr = formatTime(duration)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$currentStr / $durationStr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Slider(
            value = position.coerceIn(0f, duration.coerceAtLeast(1f)),
            onValueChange = onSeek,
            enabled = duration > 0,
            valueRange = 0f..duration.coerceAtLeast(1f),
            modifier = Modifier.height(28.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onShuffle, enabled = enabled) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Modo aleatorio",
                    tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious, enabled = enabled) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Canción anterior",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onPlayPause,
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = onNext, enabled = enabled) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Canción siguiente",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onRepeat, enabled = enabled) {
                Icon(
                    Icons.Default.RepeatOne,
                    contentDescription = "Repetir canción actual",
                    tint = if (repeatOne) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatTime(millisFloat: Float): String {
    val totalSeconds = (millisFloat / 1000f).toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun toMediaItem(track: Track): MediaItem = MediaItem.Builder()
    .setMediaId(track.id)
    .setUri(track.uri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).build())
    .build()

    private fun hasPermissions(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
