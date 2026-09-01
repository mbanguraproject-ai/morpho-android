package cc.devbangs.morpho.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateOf

/** A file Morpho produced, as recorded by MediaStore. */
data class MorphoFile(
    val uri: Uri,
    val name: String,
    val mime: String,
    val sizeBytes: Long,
    /** Epoch seconds, as MediaStore stores it. */
    val addedAt: Long
) {
    val kind: FileKind get() = FileKind.of(mime, name)
}

enum class FileKind(val label: String) {
    PDF("PDF"),
    IMAGE("Images"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Other");

    companion object {
        fun of(mime: String, name: String): FileKind {
            val m = mime.lowercase()
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                m == "application/pdf" || ext == "pdf" -> PDF
                m.startsWith("image/") -> IMAGE
                m.startsWith("video/") -> VIDEO
                m.startsWith("audio/") -> AUDIO
                else -> OTHER
            }
        }
    }
}

/**
 * The files Morpho has produced.
 *
 * Deliberately not a device-wide file browser. Listing the user's whole
 * storage would need MANAGE_EXTERNAL_STORAGE, which Play grants to file
 * managers and not to a tools app, and MediaStore does not index Word or
 * Excel documents at all - so a "245 files, 56 Word" view could not be built
 * honestly.
 *
 * What can be built, with no permission whatsoever, is everything Morpho
 * itself wrote: an app always has access to the media it created. Every save
 * path already targets Download/Morpho, Pictures/Morpho, Movies/Morpho or
 * Music/Morpho, so those four queries return real names, sizes and dates.
 *
 * Below Android 10 PDFs are written with a plain FileOutputStream rather than
 * through MediaStore, so they are not indexed and will not appear here.
 * Images, video and audio are inserted via MediaStore on every version.
 */
object FileStore {

    private val _files = mutableStateOf<List<MorphoFile>>(emptyList())
    private val _loading = mutableStateOf(false)
    private val _loaded = mutableStateOf(false)

    val files: List<MorphoFile> get() = _files.value
    val loading: Boolean get() = _loading.value
    val loaded: Boolean get() = _loaded.value

    val totalBytes: Long get() = _files.value.sumOf { it.sizeBytes }

    fun countOf(kind: FileKind): Int = _files.value.count { it.kind == kind }

    /** Blocking; call from Dispatchers.IO. */
    fun refresh(ctx: Context) {
        _loading.value = true
        val out = mutableListOf<MorphoFile>()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                out += query(ctx, MediaStore.Downloads.EXTERNAL_CONTENT_URI, "Download/Morpho%")
            }
            out += query(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/Morpho%")
            out += query(ctx, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Movies/Morpho%")
            out += query(ctx, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "Music/Morpho%")
        }
        _files.value = out.sortedByDescending { it.addedAt }.take(500)
        _loaded.value = true
        _loading.value = false
    }

    private fun query(ctx: Context, collection: Uri, relativePath: String): List<MorphoFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        )
        // RELATIVE_PATH only exists from Android 10. Below that the folder is
        // matched on the legacy absolute path column instead.
        val (selection, args) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" to arrayOf(relativePath)
            else
                @Suppress("DEPRECATION")
                "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("%/Morpho/%")

        val result = mutableListOf<MorphoFile>()
        ctx.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result += MorphoFile(
                    uri = android.content.ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol).orEmpty(),
                    mime = c.getString(mimeCol).orEmpty(),
                    sizeBytes = c.getLong(sizeCol),
                    addedAt = c.getLong(dateCol)
                )
            }
        }
        return result
    }
}
