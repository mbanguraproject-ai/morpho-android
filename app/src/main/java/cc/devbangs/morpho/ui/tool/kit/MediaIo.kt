package cc.devbangs.morpho.ui.tool.kit

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
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
/**
 * Outcome of a trim.
 *
 * This used to return null for every failure and the tool had no else branch,
 * so trimming an MP3 did nothing at all with no message. MediaMuxer writes an
 * MPEG-4 container, which cannot carry an MP3 track, so that case is now named
 * rather than swallowed.
 */
sealed interface TrimOutcome {
    data class Success(val file: File) : TrimOutcome
    data class Failure(val reason: String) : TrimOutcome
}

fun trimMedia(ctx: Context, uri: Uri, startMs: Long, endMs: Long, ext: String): TrimOutcome {
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
            // MPEG-4 cannot carry MP3, Vorbis or several other codecs, so a
            // straight remux is impossible. Re-encode instead of refusing.
            if (mime in REMUX_UNSUPPORTED) {
                runCatching { muxer.release() }
                runCatching { extractor.release() }
                return transcodeAudioSegment(ctx, uri, startMs, endMs)
            }
            if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                extractor.selectTrack(i)
                indexMap[i] = muxer.addTrack(fmt)
                if (fmt.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
                    maxBuffer = maxOf(maxBuffer, fmt.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }
        if (indexMap.isEmpty()) {
            runCatching { muxer.release() }
            return TrimOutcome.Failure("Morpho couldn't find an audio or video track in that file.")
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
        TrimOutcome.Success(out)
    } catch (e: Exception) {
        // A remux can fail for reasons the track scan cannot predict. Falling
        // back to a re-encode keeps the tool working rather than reporting a
        // format problem the user cannot act on.
        runCatching { extractor.release() }
        transcodeAudioSegment(ctx, uri, startMs, endMs)
    } finally {
        runCatching { extractor.release() }
    }
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
    val ok = try {
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
        val dst = ctx.contentResolver.insert(collection, values)
        if (dst == null) false
        else {
            val stream = ctx.contentResolver.openOutputStream(dst)
            if (stream == null) { ctx.contentResolver.delete(dst, null, null); false }
            else { stream.use { o -> file.inputStream().use { it.copyTo(o) } }; true }
        }
    } catch (e: Exception) {
        false
    }
    reportSave(ctx, ok, "File ready", "Your file was saved to your gallery.",
        "Saved to ${if (isVideo) "Movies" else "Music"}/Morpho")
    return ok
}

fun fmtTime(ms: Long): String {
    val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60)
}

/** Codecs an MPEG-4 container cannot hold, so they must be re-encoded. */
private val REMUX_UNSUPPORTED = setOf(
    "audio/mpeg",        // MP3
    "audio/vorbis",
    "audio/opus",
    "audio/x-ms-wma",
    "audio/flac",
    "audio/amr-wb",
    "audio/3gpp"
)

/**
 * Trim by decoding to PCM and re-encoding to AAC.
 *
 * The fast path in [trimMedia] copies compressed samples straight into a new
 * container, which is lossless and quick but only works when the container can
 * hold that codec. MP3 is the common case it cannot, and MP3 is what most
 * people have, so this path exists to make the tool work on anything the
 * device can decode.
 *
 * Two passes with the PCM on disk rather than one interleaved pipeline: the
 * intermediate for a few minutes of stereo audio is tens of megabytes, which
 * is fine on disk and not fine in memory.
 */
private fun transcodeAudioSegment(
    ctx: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long
): TrimOutcome {
    val startUs = startMs * 1000
    val endUs = endMs * 1000
    val pcmFile = File(ctx.cacheDir, "seg_${System.currentTimeMillis()}.pcm")
    var sampleRate = 44100
    var channels = 2

    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(ctx, uri, null)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                track = i; format = f; break
            }
        }
        if (track < 0 || format == null) {
            return TrimOutcome.Failure("Morpho couldn't find an audio track in that file.")
        }
        extractor.selectTrack(track)
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(format, null, null, 0)
        decoder.start()
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        java.io.FileOutputStream(pcmFile).use { out ->
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buf = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        val ts = extractor.sampleTime
                        if (size < 0 || ts > endUs) {
                            decoder.queueInputBuffer(
                                inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, ts, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = decoder.dequeueOutputBuffer(info, 10000)
                if (outIndex >= 0) {
                    // Seeking lands on a sync point before the mark, so the
                    // lead-in is decoded and then dropped here.
                    if (info.size > 0 && info.presentationTimeUs >= startUs) {
                        val buf = decoder.getOutputBuffer(outIndex)!!
                        val chunk = ByteArray(info.size)
                        buf.get(chunk)
                        buf.clear()
                        out.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        decoder.stop()
        decoder.release()
    } catch (e: Exception) {
        pcmFile.delete()
        return TrimOutcome.Failure("Morpho couldn't decode that audio.")
    } finally {
        runCatching { extractor.release() }
    }

    if (pcmFile.length() <= 0L) {
        pcmFile.delete()
        return TrimOutcome.Failure("That selection came out empty. Try a wider range.")
    }

    val out = File(ctx.cacheDir, "trim_${System.currentTimeMillis()}.m4a")
    try {
        val encFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, if (channels > 1) 192000 else 128000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxTrack = -1
        var muxing = false
        val info = MediaCodec.BufferInfo()
        val bytesPerFrame = channels * 2
        var totalRead = 0L
        var inputDone = false

        java.io.FileInputStream(pcmFile).use { input ->
            while (true) {
                if (!inputDone) {
                    val inIndex = encoder.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buf = encoder.getInputBuffer(inIndex)!!
                        buf.clear()
                        val temp = ByteArray(minOf(buf.capacity(), 32 * 1024))
                        val read = input.read(temp)
                        if (read <= 0) {
                            encoder.queueInputBuffer(
                                inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            buf.put(temp, 0, read)
                            // Timestamps come from how much audio has been fed,
                            // so the output starts at zero regardless of where
                            // in the source the selection began.
                            val ptsUs = totalRead / bytesPerFrame * 1_000_000L / sampleRate
                            encoder.queueInputBuffer(inIndex, 0, read, ptsUs, 0)
                            totalRead += read
                        }
                    }
                }
                val outIndex = encoder.dequeueOutputBuffer(info, 10000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxing = true
                    }
                    outIndex >= 0 -> {
                        val encoded = encoder.getOutputBuffer(outIndex)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            info.size = 0
                        }
                        if (info.size > 0 && muxing) {
                            encoded.position(info.offset)
                            encoded.limit(info.offset + info.size)
                            muxer.writeSampleData(muxTrack, encoded, info)
                        }
                        encoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        }
        encoder.stop(); encoder.release()
        if (muxing) { muxer.stop() }
        muxer.release()
        pcmFile.delete()
        return if (out.length() > 0L) TrimOutcome.Success(out)
        else TrimOutcome.Failure("Morpho couldn't write the trimmed audio.")
    } catch (e: Exception) {
        pcmFile.delete()
        out.delete()
        return TrimOutcome.Failure("Morpho couldn't re-encode that audio.")
    }
}
