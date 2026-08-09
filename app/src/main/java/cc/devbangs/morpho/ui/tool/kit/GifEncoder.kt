package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Minimal GIF89a animated encoder (public-domain lineage: Broockman/Gustavson NeuQuant).
 * No external dependency. Good enough for short clips / image sequences.
 */
class GifEncoder {
    private var width = 0; private var height = 0
    private var out: OutputStream? = null
    private var started = false
    private var firstFrame = true
    private var delay = 0
    private var repeat = 0
    private var pixels: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorTab: ByteArray? = null
    private val usedEntry = BooleanArray(256)
    private var colorDepth = 0
    private var palSize = 7
    private var dispose = -1
    private var sample = 10

    fun setDelay(ms: Int) { delay = Math.round(ms / 10f) }
    fun setRepeat(iter: Int) { if (iter >= 0) repeat = iter }
    fun start(os: OutputStream): Boolean {
        out = os; started = true
        return try { writeString("GIF89a"); true } catch (e: Exception) { false }
    }
    fun addFrame(bm: Bitmap): Boolean {
        if (!started) return false
        return try {
            if (firstFrame) { width = bm.width; height = bm.height }
            getImagePixels(bm); analyzePixels()
            if (firstFrame) { writeLSD(); writePalette(); if (repeat >= 0) writeNetscapeExt() }
            writeGraphicCtrlExt(); writeImageDesc(); if (!firstFrame) writePalette(); writePixels()
            firstFrame = false; true
        } catch (e: Exception) { false }
    }
    fun finish(): Boolean {
        if (!started) return false
        return try { out?.write(0x3b); out?.flush(); true } catch (e: Exception) { false } finally { started = false }
    }
    private fun getImagePixels(bm: Bitmap) {
        val w = bm.width; val h = bm.height
        val scaled = if (w != width || h != height) Bitmap.createScaledBitmap(bm, width, height, true) else bm
        pixels = ByteArray(width * height * 3)
        val px = IntArray(width * height)
        scaled.getPixels(px, 0, width, 0, 0, width, height)
        var i = 0
        for (p in px) {
            pixels!![i++] = (p and 0xFF).toByte()
            pixels!![i++] = (p shr 8 and 0xFF).toByte()
            pixels!![i++] = (p shr 16 and 0xFF).toByte()
        }
    }
    private fun analyzePixels() {
        val len = pixels!!.size; val nPix = len / 3
        indexedPixels = ByteArray(nPix)
        val nq = NeuQuant(pixels!!, len, sample)
        colorTab = nq.process()
        var k = 0
        for (j in 0 until nPix) {
            val index = nq.map(pixels!![k++].toInt() and 0xff, pixels!![k++].toInt() and 0xff, pixels!![k++].toInt() and 0xff)
            usedEntry[index] = true
            indexedPixels!![j] = index.toByte()
        }
        pixels = null; colorDepth = 8; palSize = 7
    }
    private fun writeGraphicCtrlExt() {
        out?.write(0x21); out?.write(0xf9); out?.write(4)
        val disp = if (dispose >= 0) dispose and 7 shl 2 else 0
        out?.write(0 or disp or 0); out?.write(delay and 0xFF); out?.write(delay shr 8 and 0xFF); out?.write(0); out?.write(0)
    }
    private fun writeImageDesc() {
        out?.write(0x2c); writeShort(0); writeShort(0); writeShort(width); writeShort(height)
        if (firstFrame) out?.write(0) else out?.write(0x80 or palSize)
    }
    private fun writeLSD() {
        writeShort(width); writeShort(height)
        out?.write(0xf0 or palSize); out?.write(0); out?.write(0)
    }
    private fun writeNetscapeExt() {
        out?.write(0x21); out?.write(0xff); out?.write(11); writeString("NETSCAPE2.0")
        out?.write(3); out?.write(1); writeShort(repeat); out?.write(0)
    }
    private fun writePalette() {
        out?.write(colorTab!!, 0, colorTab!!.size)
        val n = 3 * 256 - colorTab!!.size
        for (i in 0 until n) out?.write(0)
    }
    private fun writePixels() { LZWEncoder(width, height, indexedPixels!!, colorDepth).encode(out!!) }
    private fun writeShort(v: Int) { out?.write(v and 0xFF); out?.write(v shr 8 and 0xFF) }
    private fun writeString(s: String) { for (c in s) out?.write(c.code) }
}
