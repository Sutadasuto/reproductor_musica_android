package com.example.carpeta

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicLibraryTest {
    private val tracks = listOf(
        Track("1", "content://1", "Moonlight", "Music/Classical", "Classical", "/music/Moonlight.mp3"),
        Track("2", "content://2", "Blue Train", "Music/Jazz", "Jazz", "/music/Blue Train.mp3"),
        Track("3", "content://3", "Nocturne", "Music/Classical", "Classical", "/music/Nocturne.mp3"),
    )

    @Test
    fun playlistsCombineAllMusicAndGroupByFolderPath() {
        val playlists = MusicLibrary.playlists(tracks)
        assertEquals(MusicLibrary.ALL_MUSIC_ID, playlists.first().id)
        assertEquals(3, playlists.first().tracks.size)
        assertEquals(2, playlists.first { it.name == "Classical" }.tracks.size)
        assertEquals(setOf("Classical", "Jazz"), playlists.drop(1).map { it.name }.toSet())
    }

    @Test
    fun searchMatchesFilenameTitlesIgnoringCase() {
        assertEquals(listOf("Moonlight"), MusicLibrary.search(tracks, "moon").map { it.title })
        assertEquals(3, MusicLibrary.search(tracks, " ").size)
    }

    @Test
    fun titleDropsMp3SuffixRegardlessOfCase() {
        assertEquals("Night Drive", MusicLibrary.titleFromFilename("Night Drive.MP3"))
    }
}