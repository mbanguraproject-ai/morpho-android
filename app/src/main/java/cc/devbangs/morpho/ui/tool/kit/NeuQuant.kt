package cc.devbangs.morpho.ui.tool.kit

/** NeuQuant Neural-Net image quantization (Anthony Dekker, 1994 — public domain). */
class NeuQuant(private val thepicture: ByteArray, private val lengthcount: Int, sample: Int) {
    private val netsize = 256
    private val prime1 = 499; private val prime2 = 491; private val prime3 = 487; private val prime4 = 503
    private val maxnetpos = netsize - 1
    private val netbiasshift = 4
    private val ncycles = 100
    private val intbiasshift = 16; private val intbias = 1 shl intbiasshift
    private val gammashift = 10
    private val betashift = 10; private val beta = intbias shr betashift; private val betagamma = intbias shl (gammashift - betashift)
    private val initrad = netsize shr 3; private val radiusbiasshift = 6; private val radiusbias = 1 shl radiusbiasshift
    private val initradius = initrad * radiusbias; private val radiusdec = 30
    private val alphabiasshift = 10; private val initalpha = 1 shl alphabiasshift
    private var alphadec = 0
    private val radbiasshift = 8; private val radbias = 1 shl radbiasshift
    private val alpharadbshift = alphabiasshift + radbiasshift; private val alpharadbias = 1 shl alpharadbshift
    private val minpicturebytes = 3 * sample
    private val samplefac = sample
    private val network = Array(netsize) { IntArray(4) }
    private val netindex = IntArray(256)
    private val bias = IntArray(netsize)
    private val freq = IntArray(netsize)
    private val radpower = IntArray(initrad)

    init {
        for (i in 0 until netsize) {
            val p = network[i]
            p[0] = (i shl (netbiasshift + 8)) / netsize
            p[1] = p[0]; p[2] = p[0]
            freq[i] = intbias / netsize; bias[i] = 0
        }
    }

    fun process(): ByteArray { learn(); unbiasnet(); inxbuild(); return colorMap() }

    private fun colorMap(): ByteArray {
        val map = ByteArray(3 * netsize); val index = IntArray(netsize)
        for (i in 0 until netsize) index[network[i][3]] = i
        var k = 0
        for (i in 0 until netsize) {
            val j = index[i]
            map[k++] = network[j][0].toByte(); map[k++] = network[j][1].toByte(); map[k++] = network[j][2].toByte()
        }
        return map
    }
    private fun unbiasnet() { for (i in 0 until netsize) { for (j in 0..2) network[i][j] = network[i][j] shr netbiasshift; network[i][3] = i } }
    private fun inxbuild() {
        var previouscol = 0; var startpos = 0
        for (i in 0 until netsize) {
            val p = network[i]; var smallpos = i; var smallval = p[1]
            for (j in i + 1 until netsize) { val q = network[j]; if (q[1] < smallval) { smallpos = j; smallval = q[1] } }
            val q = network[smallpos]
            if (i != smallpos) {
                var j = q[0]; q[0] = p[0]; p[0] = j; j = q[1]; q[1] = p[1]; p[1] = j
                j = q[2]; q[2] = p[2]; p[2] = j; j = q[3]; q[3] = p[3]; p[3] = j
            }
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) shr 1
                for (j in previouscol + 1 until smallval) netindex[j] = i
                previouscol = smallval; startpos = i
            }
        }
        netindex[previouscol] = (startpos + maxnetpos) shr 1
        for (j in previouscol + 1 until 256) netindex[j] = maxnetpos
    }
    private fun learn() {
        if (lengthcount < minpicturebytes) return
        val alphadecFactor = 30 + (samplefac - 1) / 3; alphadec = alphadecFactor
        val p = thepicture; var pix = 0; val lim = lengthcount
        val samplepixels = lengthcount / (3 * samplefac)
        var delta = samplepixels / ncycles; var alpha = initalpha; var radius = initradius
        var rad = radius shr radiusbiasshift
        if (rad <= 1) rad = 0
        for (i in 0 until rad) radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad))
        val step = when {
            lengthcount < minpicturebytes -> 3
            lengthcount % prime1 != 0 -> 3 * prime1
            lengthcount % prime2 != 0 -> 3 * prime2
            lengthcount % prime3 != 0 -> 3 * prime3
            else -> 3 * prime4
        }
        var i = 0
        if (delta == 0) delta = 1
        while (i < samplepixels) {
            val b = (p[pix].toInt() and 0xff) shl netbiasshift
            val g = (p[pix + 1].toInt() and 0xff) shl netbiasshift
            val r = (p[pix + 2].toInt() and 0xff) shl netbiasshift
            val j = contest(b, g, r)
            altersingle(alpha, j, b, g, r)
            if (rad != 0) alterneigh(rad, j, b, g, r)
            pix += step; if (pix >= lim) pix -= lengthcount
            i++
            if (i % delta == 0) {
                alpha -= alpha / alphadec; radius -= radius / radiusdec; rad = radius shr radiusbiasshift
                if (rad <= 1) rad = 0
                for (k in 0 until rad) radpower[k] = alpha * (((rad * rad - k * k) * radbias) / (rad * rad))
            }
        }
    }
    fun map(b: Int, g: Int, r: Int): Int {
        var bestd = 1000; var best = -1; var i = netindex[g]; var j = i - 1
        while (i < netsize || j >= 0) {
            if (i < netsize) {
                val p = network[i]; var dist = p[1] - g
                if (dist >= bestd) i = netsize else {
                    i++; if (dist < 0) dist = -dist
                    var a = p[0] - b; if (a < 0) a = -a; dist += a
                    if (dist < bestd) { a = p[2] - r; if (a < 0) a = -a; dist += a; if (dist < bestd) { bestd = dist; best = p[3] } }
                }
            }
            if (j >= 0) {
                val p = network[j]; var dist = g - p[1]
                if (dist >= bestd) j = -1 else {
                    j--; if (dist < 0) dist = -dist
                    var a = p[0] - b; if (a < 0) a = -a; dist += a
                    if (dist < bestd) { a = p[2] - r; if (a < 0) a = -a; dist += a; if (dist < bestd) { bestd = dist; best = p[3] } }
                }
            }
        }
        return best
    }
    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestd = Int.MAX_VALUE; var bestbiasd = bestd; var bestpos = -1; var bestbiaspos = bestpos
        for (i in 0 until netsize) {
            val n = network[i]; var dist = n[0] - b; if (dist < 0) dist = -dist
            var a = n[1] - g; if (a < 0) a = -a; dist += a
            a = n[2] - r; if (a < 0) a = -a; dist += a
            if (dist < bestd) { bestd = dist; bestpos = i }
            val biasdist = dist - (bias[i] shr (intbiasshift - netbiasshift))
            if (biasdist < bestbiasd) { bestbiasd = biasdist; bestbiaspos = i }
            val betafreq = freq[i] shr betashift; freq[i] -= betafreq; bias[i] += betafreq shl gammashift
        }
        freq[bestpos] += beta; bias[bestpos] -= betagamma
        return bestbiaspos
    }
    private fun altersingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= alpha * (n[0] - b) / initalpha
        n[1] -= alpha * (n[1] - g) / initalpha
        n[2] -= alpha * (n[2] - r) / initalpha
    }
    private fun alterneigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
        var lo = i - rad; if (lo < -1) lo = -1
        var hi = i + rad; if (hi > netsize) hi = netsize
        var j = i + 1; var k = i - 1; var m = 1
        while (j < hi || k > lo) {
            val a = radpower[m++]
            if (j < hi) { val p = network[j++]; p[0] -= a * (p[0] - b) / alpharadbias; p[1] -= a * (p[1] - g) / alpharadbias; p[2] -= a * (p[2] - r) / alpharadbias }
            if (k > lo) { val p = network[k--]; p[0] -= a * (p[0] - b) / alpharadbias; p[1] -= a * (p[1] - g) / alpharadbias; p[2] -= a * (p[2] - r) / alpharadbias }
        }
    }
}
