package cc.devbangs.morpho.ui.tool.kit

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.nio.ByteBuffer

/** Duration of a media Uri in microseconds. */
fun mediaDurationUs(ctx: Context, uri: Uri): Long {
    return try {
        MediaMetadataRetriever().use { r ->
            r.setDataSource(ctx, uri)
            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000
        }
    } catch (e: Exception) { 0L }
}

/**
 * Lossless trim [startMs, endMs] of a media Uri to a cache file via MediaMuxer.
 * Copies compressed samples directly — no re-encode, so it's fast and lossless.
 * Returns the output File, or null.
 */
fun trimMedia(ctx: Context, uri: Uri, startMs: Long, endMs: Long, ext: String): File? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(ctx, uri, null)
        val out = File(ctx.cacheDir, "trim_${System.currentTimeMillis()}.$ext")
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val indexMap = HashMap<Int, Int>()
        var maxBuffer = 1 shl 20
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                indexMap[i] = muxer.addTrack(fmt)
                if (fmt.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
                    maxBuffer = maxOf(maxBuffer, fmt.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }
        muxer.start()
        val buffer = ByteBuffer.allocate(maxBuffer)
        val info = android.media.MediaCodec.BufferInfo()
        val startUs = startMs * 1000; val endUs = endMs * 1000

        for (srcTrack in indexMap.keys) {
            extractor.unselectTrack(srcTrack)
        }
        for ((srcTrack, dstTrack) in indexMap) {
            extractor.selectTrack(srcTrack)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val ts = extractor.sampleTime
                if (ts > endUs) break
                if (ts >= startUs) {
                    info.presentationTimeUs = ts - startUs
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrack, buffer, info)
                }
                extractor.advance()
            }
            extractor.unselectTrack(srcTrack)
        }
        muxer.stop(); muxer.release()
        out
    } catch (e: Exception) { null } finally { extractor.release() }
}



/**
 * Remove the audio track from a video (mute it). Copies only the video track
 * via MediaMuxer — no re-encode, fast and lossless. Returns the output File, or null.
 */
fun muteVideo(ctx: Context, uri: Uri): File? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(ctx, uri, null)
        val out = File(ctx.cacheDir, "muted_${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var srcVideo = -1; var dstVideo = -1; var maxBuffer = 1 shl 20
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/")) {
                srcVideo = i
                dstVideo = muxer.addTrack(fmt)
                if (fmt.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
                    maxBuffer = maxOf(maxBuffer, fmt.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }
        if (srcVideo < 0) return null
        muxer.start()
        val buffer = ByteBuffer.allocate(maxBuffer)
        val info = android.media.MediaCodec.BufferInfo()
        extractor.selectTrack(srcVideo)
        while (true) {
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(dstVideo, buffer, info)
            extractor.advance()
        }
        muxer.stop(); muxer.release()
        out
    } catch (e: Exception) { null } finally { extractor.release() }
}



/**
 * Remux a video into a fresh MP4 container to drop container-level metadata
 * (location, tags). Copies video + audio streams without re-encoding.
 */
fun stripVideoMetadata(ctx: Context, uri: Uri): File? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(ctx, uri, null)
        val out = File(ctx.cacheDir, "clean_${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val indexMap = HashMap<Int, Int>()
        var maxBuffer = 1 shl 20
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                indexMap[i] = muxer.addTrack(fmt)
                if (fmt.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
                    maxBuffer = maxOf(maxBuffer, fmt.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }
        // deliberately do NOT call muxer.setLocation() or setOrientationHint from source metadata
        muxer.start()
        val buffer = ByteBuffer.allocate(maxBuffer)
        val info = android.media.MediaCodec.BufferInfo()
        for (srcTrack in indexMap.keys) extractor.unselectTrack(srcTrack)
        for ((srcTrack, dstTrack) in indexMap) {
            extractor.selectTrack(srcTrack)
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, info)
                extractor.advance()
            }
            extractor.unselectTrack(srcTrack)
        }
        muxer.stop(); muxer.release()
        out
    } catch (e: Exception) { null } finally { extractor.release() }
}

/** Save a cache media file to the gallery (Movies or Music). */
fun saveMediaToGallery(ctx: Context, file: File, displayName: String, isVideo: Boolean): Boolean {
    cc.devbangs.morpho.ads.AdState.markUsed()
    cc.devbangs.morpho.notify.Notifier.notifyDone(ctx, "File ready", "Your file was saved to your gallery.")
    return try {
        val mime = if (isVideo) "video/mp4" else "audio/mp4"
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                         else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (isVideo) "Movies/Morpho" else "Music/Morpho"))
        }
        val dst = ctx.contentResolver.insert(collection, values) ?: return false
        ctx.contentResolver.openOutputStream(dst)?.use { o -> file.inputStream().use { it.copyTo(o) } }
        Toast.makeText(ctx, "Saved to ${if (isVideo) "Movies" else "Music"}/Morpho", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) { Toast.makeText(ctx, "Save failed", Toast.LENGTH_SHORT).show(); false }
}

fun fmtTime(ms: Long): String {
    val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60)
}
