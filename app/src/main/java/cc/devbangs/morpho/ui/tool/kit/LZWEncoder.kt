package cc.devbangs.morpho.ui.tool.kit

import java.io.OutputStream

/** GIF LZW encoder (public domain, based on Jef Poskanzer's Acme GifEncoder). */
class LZWEncoder(private val imgW: Int, private val imgH: Int, private val pixAry: ByteArray, colorDepth: Int) {
    private val EOF = -1
    private val initCodeSize = Math.max(2, colorDepth)
    private var remaining = 0; private var curPixel = 0
    private val BITS = 12; private val HSIZE = 5003
    private var n_bits = 0; private var maxbits = BITS; private var maxcode = 0; private val maxmaxcode = 1 shl BITS
    private val htab = IntArray(HSIZE); private val codetab = IntArray(HSIZE)
    private val hsize = HSIZE
    private var free_ent = 0; private var clear_flg = false
    private var g_init_bits = 0; private var ClearCode = 0; private var EOFCode = 0
    private var cur_accum = 0; private var cur_bits = 0
    private val masks = intArrayOf(0x0000,0x0001,0x0003,0x0007,0x000F,0x001F,0x003F,0x007F,0x00FF,0x01FF,0x03FF,0x07FF,0x0FFF,0x1FFF,0x3FFF,0x7FFF,0xFFFF)
    private var a_count = 0; private val accum = ByteArray(256)

    fun encode(os: OutputStream) {
        os.write(initCodeSize); remaining = imgW * imgH; curPixel = 0
        compress(initCodeSize + 1, os); os.write(0)
    }
    private fun MAXCODE(nBits: Int) = (1 shl nBits) - 1
    private fun char_out(c: Byte, os: OutputStream) { accum[a_count++] = c; if (a_count >= 254) flush_char(os) }
    private fun cl_block(os: OutputStream) { cl_hash(hsize); free_ent = ClearCode + 2; clear_flg = true; output(ClearCode, os) }
    private fun cl_hash(hsize: Int) { for (i in 0 until hsize) htab[i] = -1 }
    private fun compress(init_bits: Int, os: OutputStream) {
        var fcode: Int; var c: Int; var i: Int; var disp: Int; val hshift: Int
        var ent: Int
        g_init_bits = init_bits; clear_flg = false; n_bits = g_init_bits; maxcode = MAXCODE(n_bits)
        ClearCode = 1 shl (init_bits - 1); EOFCode = ClearCode + 1; free_ent = ClearCode + 2
        a_count = 0; ent = nextPixel()
        var hsh = 0; var fc = hsize
        while (fc < 65536) { hsh++; fc *= 2 }
        hshift = 8 - hsh; cl_hash(hsize); output(ClearCode, os)
        outer@ while (nextPixel().also { c = it } != EOF) {
            fcode = (c shl maxbits) + ent; i = (c shl hshift) xor ent
            if (htab[i] == fcode) { ent = codetab[i]; continue }
            else if (htab[i] >= 0) {
                disp = hsize - i; if (i == 0) disp = 1
                do { i -= disp; if (i < 0) i += hsize
                    if (htab[i] == fcode) { ent = codetab[i]; continue@outer }
                } while (htab[i] >= 0)
            }
            output(ent, os); ent = c
            if (free_ent < maxmaxcode) { codetab[i] = free_ent++; htab[i] = fcode } else cl_block(os)
        }
        output(ent, os); output(EOFCode, os)
    }
    private fun flush_char(os: OutputStream) { if (a_count > 0) { os.write(a_count); os.write(accum, 0, a_count); a_count = 0 } }
    private fun nextPixel(): Int {
        if (remaining == 0) return EOF
        remaining--; val pix = pixAry[curPixel++]; return pix.toInt() and 0xff
    }
    private fun output(code: Int, os: OutputStream) {
        cur_accum = cur_accum and masks[cur_bits]
        cur_accum = if (cur_bits > 0) cur_accum or (code shl cur_bits) else code
        cur_bits += n_bits
        while (cur_bits >= 8) { char_out((cur_accum and 0xff).toByte(), os); cur_accum = cur_accum shr 8; cur_bits -= 8 }
        if (free_ent > maxcode || clear_flg) {
            if (clear_flg) { n_bits = g_init_bits; maxcode = MAXCODE(n_bits); clear_flg = false }
            else { n_bits++; maxcode = if (n_bits == maxbits) maxmaxcode else MAXCODE(n_bits) }
        }
        if (code == EOFCode) {
            while (cur_bits > 0) { char_out((cur_accum and 0xff).toByte(), os); cur_accum = cur_accum shr 8; cur_bits -= 8 }
            flush_char(os)
        }
    }
}
