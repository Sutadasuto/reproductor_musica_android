package com.example.carpeta

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import java.io.File

class MediaStoreScanner(private val resolver: ContentResolver) {
    fun scan(): List<Track> {
        val audioUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATA,
        )
        val tracks = mutableListOf<Track>()

        resolver.query(audioUri, projection, null, null, "${MediaStore.Audio.Media.DISPLAY_NAME} ASC")
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val filename = cursor.getString(nameColumn) ?: continue
                    if (!filename.endsWith(".mp3", ignoreCase = true)) continue

                    val relativePath = cursor.getString(pathColumn)
                    val fullPath = if (dataColumn >= 0) cursor.getString(dataColumn).orEmpty() else ""
                    val folderPath: String
                    val folderName: String

                    if (!relativePath.isNullOrBlank()) {
                        val trimmed = relativePath.trimEnd('/')
                        folderPath = trimmed.ifBlank { "Música" }
                        folderName = trimmed.substringAfterLast('/').ifBlank { "Música" }
                    } else if (fullPath.isNotBlank()) {
                        val parent = File(fullPath).parentFile
                        if (parent != null) {
                            folderPath = parent.absolutePath
                            folderName = parent.name.ifBlank { "Música" }
                        } else {
                            folderPath = "Música"
                            folderName = "Música"
                        }
                    } else {
                        folderPath = "Música"
                        folderName = "Música"
                    }

                    val id = cursor.getLong(idColumn).toString()
                    tracks += Track(
                        id = id,
                        uri = Uri.withAppendedPath(audioUri, id).toString(),
                        title = MusicLibrary.titleFromFilename(filename),
                        folderPath = folderPath,
                        folderName = folderName,
                        filePath = fullPath,
                    )
                }
            }
        return tracks
    }
}
