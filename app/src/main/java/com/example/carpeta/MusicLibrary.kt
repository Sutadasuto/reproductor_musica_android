package com.example.carpeta

data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val folderPath: String,
    val folderName: String,
    val filePath: String,
)

data class MusicPlaylist(val id: String, val name: String, val tracks: List<Track>)

object MusicLibrary {
    const val ALL_MUSIC_ID = "__all_music__"
    const val ALL_MUSIC_NAME = "Toda la música"

    fun playlists(tracks: List<Track>): List<MusicPlaylist> {
        val ordered = tracks.sortedBy { it.title.lowercase() }
        val all = MusicPlaylist(ALL_MUSIC_ID, ALL_MUSIC_NAME, ordered)
        val folders = ordered.groupBy { it.folderPath }
            .map { (path, songs) -> MusicPlaylist(path, songs.first().folderName, songs) }
            .sortedBy { it.name.lowercase() }
        return listOf(all) + folders
    }

    fun search(tracks: List<Track>, query: String): List<Track> {
        val term = query.trim()
        return if (term.isEmpty()) tracks else tracks.filter { it.title.contains(term, ignoreCase = true) }
    }

    fun titleFromFilename(filename: String): String =
        if (filename.endsWith(".mp3", ignoreCase = true)) filename.dropLast(4) else filename
}
