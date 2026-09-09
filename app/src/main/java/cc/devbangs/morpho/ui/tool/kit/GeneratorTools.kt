package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

fun hasGeneratorTool(id: String): Boolean = id in setOf(
    "password-generator","qr-code-generator","barcode-generator","fake-data-generator",
    "username-generator","email-signature-generator","gradient-generator","css-generator",
    "cover-letter-generator","api-key-generator","hashtag-generator","palette-generator"
)

@Composable
fun GeneratorTool(id: String, accent: Color) {
    when (id) {
        "password-generator" -> PasswordTool(accent)
        "username-generator" -> UsernameTool(accent)
        "email-signature-generator" -> EmailSigTool(accent)
        "gradient-generator" -> GradientTool(accent)
        "css-generator" -> CssGenTool(accent)
        "cover-letter-generator" -> CoverLetterTool(accent)
        "api-key-generator" -> ApiKeyTool(accent)
        "hashtag-generator" -> HashtagTool(accent)
        "palette-generator" -> PaletteGenTool(accent)
        "qr-code-generator" -> QrTool(accent)
        "barcode-generator" -> BarcodeTool(accent)
        "fake-data-generator" -> FakeDataTool(accent)
    }
}

// ---------- Password ----------
@Composable
private fun PasswordTool(accent: Color) {
    var length by remember { mutableStateOf(16) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var seed by remember { mutableStateOf(0) }

    val pw = remember(length, upper, lower, digits, symbols, seed) {
        val sets = buildString {
            if (upper) append("ABCDEFGHJKLMNPQRSTUVWXYZ")
            if (lower) append("abcdefghijkmnpqrstuvwxyz")
            if (digits) append("23456789")
            if (symbols) append("!@#\$%^&*-_=+?")
        }
        if (sets.isEmpty()) "" else (1..length).map { sets[Random.nextInt(sets.length)] }.joinToString("")
    }
    val strength = when {
        length >= 16 && listOf(upper,lower,digits,symbols).count { it } >= 3 -> "Strong" to accent
        length >= 12 -> "Good" to accent.copy(alpha = 0.7f)
        else -> "Weak" to InkSoft
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ToolResult(pw, accent, label = "PASSWORD")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Strength: ", color = InkSoft, fontSize = 13.sp)
            Text(strength.first, color = strength.second, fontSize = 13.sp)
        }
        Column {
            FieldLabel("LENGTH: $length")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf(8,12,16,24,32).forEach { n ->
                    Box(Modifier.weight(1f)) {
                        ToolButton("$n", if (length==n) accent else accent.copy(alpha=0.35f)) { length = n }
                    }
                }
            }
        }
        Toggle("Uppercase (A-Z)", upper, accent) { upper = it }
        Toggle("Lowercase (a-z)", lower, accent) { lower = it }
        Toggle("Digits (0-9)", digits, accent) { digits = it }
        Toggle("Symbols (!@#)", symbols, accent) { symbols = it }
        ToolButton("Regenerate", accent) { seed++ }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk)
            .clickable { onChange(!on) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(44.dp, 26.dp).clip(Shape.pill)
                .background(if (on) accent else PaperLine),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.padding(3.dp).size(20.dp).clip(Shape.pill).background(Paper))
        }
    }
}

// ---------- QR ----------
@Composable
private fun QrTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    var sizeKey by remember { mutableStateOf("1024") }
    var ecc by remember { mutableStateOf("M") }
    var quiet by remember { mutableStateOf("4") }
    var fmtKey by remember { mutableStateOf("PNG") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val fmt = compressFormatOf(fmtKey)

    // Encoding at print sizes is millions of pixels, so it is debounced and
    // run off the main thread. It used to happen inside remember(), during
    // composition, on the main thread.
    LaunchedEffect(t, sizeKey, ecc, quiet) {
        if (t.isBlank()) {
            bmp = null; failed = false; working = false
            return@LaunchedEffect
        }
        working = true
        delay(220)
        val r = withContext(Dispatchers.Default) {
            try {
                qrBitmap(t, sizeKey.toInt(), ecc, quiet.toInt())
            } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
        }
        bmp = r
        failed = r == null
        working = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("CONTENT"); ToolInput(t, { t = it }, "URL, text, Wi-Fi, anything…", minLines = 3) }
        Column { FieldLabel("SIZE PX"); ChipRow(listOf("512", "1024", "2048"), sizeKey, accent) { sizeKey = it } }
        Column {
            FieldLabel("ERROR CORRECTION " + eccHint(ecc))
            ChipRow(listOf("L", "M", "Q", "H"), ecc, accent) { ecc = it }
        }
        Column { FieldLabel("QUIET ZONE"); ChipRow(listOf("1", "2", "4"), quiet, accent) { quiet = it } }
        Column { FieldLabel("OUTPUT FORMAT"); ChipRow(listOf("PNG", "JPEG", "WEBP"), fmtKey, accent) { fmtKey = it } }

        if (working) ProcessingCard("Generating code", accent)
        bmp?.let { b ->
            if (!working) {
                Box(
                    Modifier.fillMaxWidth().clip(Shape.card).background(Paper).padding(Space.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Image(b.asImageBitmap(), null, Modifier.size(240.dp))
                }
                StatGrid(
                    listOf(
                        "Size" to (b.width.toString() + "\u00d7" + b.height),
                        "Correction" to eccLabel(ecc),
                        "Quiet zone" to (quiet + " modules"),
                        "Format" to fmtKey
                    ),
                    accent
                )
                BitmapResultActions(accent, "morpho_qr", fmt, 100) { b }
            }
        }
        if (failed && !working) Text(
            "⚠ Could not encode this content.", color = InkSoft, fontSize = 13.sp
        )
    }
}

// ---------- Barcode ----------
@Composable
private fun BarcodeTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    var sym by remember { mutableStateOf("CODE 128") }
    var widthKey by remember { mutableStateOf("1024") }
    var fmtKey by remember { mutableStateOf("PNG") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val fmt = compressFormatOf(fmtKey)

    LaunchedEffect(t, sym, widthKey) {
        if (t.isBlank()) {
            bmp = null; failed = false; working = false
            return@LaunchedEffect
        }
        working = true
        delay(220)
        val w = widthKey.toInt()
        val r = withContext(Dispatchers.Default) {
            try {
                barcodeBitmap(t, sym, w, (w * 0.36f).toInt())
            } catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
        }
        bmp = r
        failed = r == null
        working = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("CODE VALUE"); ToolInput(t, { t = it }, "Enter digits/text…", minLines = 1, mono = true) }
        Column {
            FieldLabel("SYMBOLOGY " + symHint(sym))
            ChipRow(listOf("CODE 128", "EAN-13", "UPC-A"), sym, accent) { sym = it }
            Spacer(Modifier.height(6.dp))
            ChipRow(listOf("CODE 39", "ITF"), sym, accent) { sym = it }
        }
        Column { FieldLabel("WIDTH PX"); ChipRow(listOf("512", "1024", "2048"), widthKey, accent) { widthKey = it } }
        Column { FieldLabel("OUTPUT FORMAT"); ChipRow(listOf("PNG", "JPEG", "WEBP"), fmtKey, accent) { fmtKey = it } }

        if (working) ProcessingCard("Generating code", accent)
        bmp?.let { b ->
            if (!working) {
                Box(
                    Modifier.fillMaxWidth().clip(Shape.card).background(Paper).padding(Space.lg),
                    contentAlignment = Alignment.Center
                ) { Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().height(120.dp)) }
                StatGrid(
                    listOf(
                        "Size" to (b.width.toString() + "\u00d7" + b.height),
                        "Symbology" to sym,
                        "Format" to fmtKey,
                        "Quiet zone" to "10 modules"
                    ),
                    accent
                )
                BitmapResultActions(accent, "morpho_barcode", fmt, 100) { b }
            }
        }
        if (failed && !working) Text(
            "⚠ " + symRule(sym), color = InkSoft, fontSize = 13.sp
        )
    }
}

// ---------- Fake Data ----------
@Composable
private fun FakeDataTool(accent: Color) {
    var seed by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(5) }
    var region by remember { mutableStateOf("Global") }
    var format by remember { mutableStateOf("Lines") }

    val out = remember(seed, count, region, format) {
        val people = (1..count).map { fakePerson(region) }
        when (format) {
            "CSV" -> "name,email,phone,city,company\n" +
                people.joinToString("\n") { p ->
                    listOf(p.name, p.email, p.phone, p.city, p.company)
                        .joinToString(",") { if (it.contains(',')) "\"$it\"" else it }
                }
            "JSON" -> people.joinToString(",\n", "[\n", "\n]") { p ->
                """  {"name": "${p.name}", "email": "${p.email}", """ +
                    """"phone": "${p.phone}", "city": "${p.city}", "company": "${p.company}"}"""
            }
            else -> people.joinToString("\n\n") {
                "Name     ${it.name}\nEmail    ${it.email}\n" +
                    "Phone    ${it.phone}\nCity     ${it.city}\nCompany  ${it.company}"
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        FieldLabel("REGION")
        ChipRow(listOf("Global", "Africa", "Europe", "Americas", "Asia"), region, accent) {
            region = it
        }
        FieldLabel("FORMAT")
        ChipRow(listOf("Lines", "CSV", "JSON"), format, accent) { format = it }
        FieldLabel("HOW MANY")
        ChipRow(listOf("1", "5", "10", "20"), "$count", accent) { count = it.toInt() }
        ToolButton("Regenerate", accent) { seed++ }
        ToolResult(out, accent, mono = format != "Lines", label = "RECORDS")
        Text(
            "Invented for testing. Any resemblance to a real person is coincidence, " +
                "and these are not safe to use as real contact details.",
            color = InkFaint, fontSize = 12.sp
        )
    }
}

/** Small labelled chips for a fixed set of choices. */
@androidx.compose.runtime.Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).clip(Shape.field)
                    .background(if (on) accent else PaperSunk)
                    .clickable { onSelect(o) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (on) Paper else InkSoft, fontSize = 13.sp)
            }
        }
    }
}

// ---- helpers ----
private const val CODE_BLACK = android.graphics.Color.BLACK
private const val CODE_WHITE = android.graphics.Color.WHITE

/**
 * Matrix to bitmap in one allocation.
 *
 * setPixel per module meant 409,600 calls at the old fixed 640px, and it ran
 * during composition on the main thread. At a print-usable 2048px that is 4.2
 * million calls. setPixels writes the row buffer in one go.
 *
 * ARGB_8888 rather than the old 16-bit config: a code is pure black and white,
 * and 16-bit cannot represent either exactly, so edges pick up a tint that
 * survives into the saved file.
 */
private fun matrixToBitmap(m: BitMatrix): Bitmap {
    val w = m.width
    val h = m.height
    val px = IntArray(w * h)
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) px[row + x] = if (m[x, y]) CODE_BLACK else CODE_WHITE
    }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}

/**
 * QR encode.
 *
 * Two fixes beyond the controls. The quiet zone was 1; the QR specification
 * calls for 4 modules of clear margin, and below that scanners struggle,
 * especially where the code sits on a dark background. And without a character
 * set hint ZXing falls back to ISO-8859-1, so any content outside Latin-1
 * encoded wrong.
 */
private fun qrBitmap(text: String, size: Int, ecc: String, quietZone: Int): Bitmap {
    val level = when (ecc) {
        "L" -> ErrorCorrectionLevel.L
        "Q" -> ErrorCorrectionLevel.Q
        "H" -> ErrorCorrectionLevel.H
        else -> ErrorCorrectionLevel.M
    }
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to level,
        EncodeHintType.MARGIN to quietZone,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    return matrixToBitmap(
        MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    )
}

/**
 * Barcode encode.
 *
 * CODE 128 alone does not cover the field: a shop or a warehouse needs EAN-13
 * and UPC-A for retail products and ITF for cartons. Each has its own length
 * and character rules, so an invalid value throws and the caller reports which
 * rule was broken rather than a bare failure.
 */
private fun barcodeBitmap(text: String, symbology: String, w: Int, h: Int): Bitmap {
    val f = when (symbology) {
        "EAN-13" -> BarcodeFormat.EAN_13
        "UPC-A" -> BarcodeFormat.UPC_A
        "CODE 39" -> BarcodeFormat.CODE_39
        "ITF" -> BarcodeFormat.ITF
        else -> BarcodeFormat.CODE_128
    }
    val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 10)
    return matrixToBitmap(MultiFormatWriter().encode(text, f, w, h, hints))
}

/** What each correction level buys, in the label. */
private fun eccHint(ecc: String): String = when (ecc) {
    "L" -> "(7% recoverable)"
    "Q" -> "(25% recoverable)"
    "H" -> "(30% recoverable)"
    else -> "(15% recoverable)"
}

private fun eccLabel(ecc: String): String = when (ecc) {
    "L" -> "L - 7%"
    "Q" -> "Q - 25%"
    "H" -> "H - 30%"
    else -> "M - 15%"
}

private fun symHint(sym: String): String = when (sym) {
    "EAN-13" -> "(13 digits)"
    "UPC-A" -> "(12 digits)"
    "ITF" -> "(even digit count)"
    "CODE 39" -> "(A-Z, 0-9, - . $ / + %)"
    else -> "(any text)"
}

/** Said back to the user when encoding fails, so the rule is discoverable. */
private fun symRule(sym: String): String = when (sym) {
    "EAN-13" -> "EAN-13 needs exactly 13 digits (or 12 and it adds the check digit)."
    "UPC-A" -> "UPC-A needs exactly 12 digits (or 11 and it adds the check digit)."
    "ITF" -> "ITF needs digits only, in an even count."
    "CODE 39" -> "CODE 39 takes A-Z, 0-9 and - . $ / + % only."
    else -> "Could not encode this value."
}

/**
 * Test data, by region.
 *
 * The first version drew every name from one small Sierra Leonean pool and gave
 * every record a +232 number, in an app that ships to 177 countries. It also
 * meant a two-name pool could produce the developer's own name against a
 * fabricated phone number, which is not a good look for a tool whose whole job
 * is to invent people.
 *
 * The pools are wide enough that any pairing is plainly generic, and the region
 * is the user's choice rather than an assumption.
 */
private class Person(
    val name: String, val email: String, val phone: String,
    val city: String, val company: String
)

private val NAMES: Map<String, Pair<List<String>, List<String>>> = mapOf(
    "Africa" to Pair(
        listOf("Aminata", "Kwame", "Chidi", "Fatoumata", "Ibrahim", "Ngozi",
            "Sekou", "Adaeze", "Mamadou", "Thandiwe", "Yaw", "Halima"),
        listOf("Kamara", "Okafor", "Diallo", "Mensah", "Traore", "Adeyemi",
            "Sesay", "Boateng", "Cisse", "Nwosu", "Dlamini", "Abebe")
    ),
    "Europe" to Pair(
        listOf("Sofia", "Lukas", "Emma", "Mateo", "Ines", "Jonas",
            "Elena", "Anders", "Chiara", "Piotr", "Maja", "Liam"),
        listOf("Muller", "Rossi", "Dubois", "Novak", "Andersson", "Garcia",
            "Kowalski", "Silva", "Jansen", "Murphy", "Virtanen", "Horvat")
    ),
    "Americas" to Pair(
        listOf("Olivia", "Diego", "Ava", "Santiago", "Emily", "Mateo",
            "Noah", "Camila", "Ethan", "Valentina", "Mia", "Lucas"),
        listOf("Smith", "Rodriguez", "Johnson", "Silva", "Brown", "Martinez",
            "Wilson", "Costa", "Davis", "Ramirez", "Taylor", "Reyes")
    ),
    "Asia" to Pair(
        listOf("Wei", "Priya", "Haruto", "Ji-woo", "Anjali", "Kenji",
            "Mei", "Arjun", "Sakura", "Min-jun", "Aarav", "Linh"),
        listOf("Chen", "Sharma", "Tanaka", "Kim", "Patel", "Suzuki",
            "Wang", "Singh", "Nakamura", "Park", "Nguyen", "Rahman")
    )
)

private val CITIES: Map<String, List<String>> = mapOf(
    "Africa" to listOf("Freetown", "Accra", "Lagos", "Nairobi", "Dakar", "Kigali"),
    "Europe" to listOf("Lisbon", "Krakow", "Dublin", "Turin", "Utrecht", "Malmo"),
    "Americas" to listOf("Portland", "Medellin", "Toronto", "Recife", "Austin", "Puebla"),
    "Asia" to listOf("Osaka", "Pune", "Da Nang", "Busan", "Chiang Mai", "Surabaya")
)

private val DIALS: Map<String, List<String>> = mapOf(
    "Africa" to listOf("+232", "+233", "+234", "+254"),
    "Europe" to listOf("+44", "+49", "+33", "+39"),
    "Americas" to listOf("+1", "+55", "+52"),
    "Asia" to listOf("+81", "+91", "+86", "+82")
)

private val COMPANIES = listOf(
    "Larkfield Group", "Blue Harbor", "Nimbus Labs", "Meridian Works",
    "Copperline", "Fernbank", "Alder & Vale", "Stonecrest", "Halcyon Supply"
)
private val domains = listOf("example.com", "example.org", "example.net")

private fun fakePerson(region: String): Person {
    val key = if (region == "Global") NAMES.keys.random() else region
    val (firsts, lasts) = NAMES[key] ?: NAMES.values.first()
    val f = firsts.random()
    val l = lasts.random()
    // example.com and friends are reserved for exactly this, so nothing
    // generated here can reach a real inbox.
    val email = "${f.lowercase()}.${l.lowercase()}${Random.nextInt(10, 99)}@${domains.random()}"
    val dial = (DIALS[key] ?: DIALS.values.first()).random()
    val phone = "$dial ${Random.nextInt(70, 99)} ${Random.nextInt(100, 999)} ${Random.nextInt(1000, 9999)}"
    return Person(
        name = "$f $l",
        email = email,
        phone = phone,
        city = (CITIES[key] ?: CITIES.values.first()).random(),
        company = COMPANIES.random()
    )
}

@androidx.compose.runtime.Composable
private fun UsernameTool(accent: Color) {
    var seed by remember { mutableStateOf(0) }
    val adj = listOf("swift","brave","cosmic","silent","lunar","neon","royal","wild","mystic","turbo","pixel","shadow","golden","frost","hyper")
    val noun = listOf("fox","wolf","raven","tiger","comet","ninja","phoenix","viper","falcon","panther","dragon","wizard","rider","hunter","ghost")
    val names = remember(seed) {
        (1..6).map { "${adj.random()}${noun.random().replaceFirstChar { c -> c.uppercase() }}${Random.nextInt(10,99)}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        names.forEach { ToolResult(it, accent, label = "USERNAME") }
        ToolButton("Generate more", accent) { seed++ }
    }
}

@androidx.compose.runtime.Composable
private fun EmailSigTool(accent: Color) {
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val sig = buildString {
        if (name.isNotBlank()) append("$name\n")
        if (title.isNotBlank()) append("$title")
        if (title.isNotBlank() && company.isNotBlank()) append(" | ")
        if (company.isNotBlank()) append(company)
        if (title.isNotBlank() || company.isNotBlank()) append("\n")
        if (email.isNotBlank()) append("$email\n")
        if (phone.isNotBlank()) append(phone)
    }.trim()
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("NAME"); ToolInput(name, { name = it }, "Jane Doe", minLines = 1) }
        Column { FieldLabel("TITLE"); ToolInput(title, { title = it }, "Product Manager", minLines = 1) }
        Column { FieldLabel("COMPANY"); ToolInput(company, { company = it }, "Acme Inc", minLines = 1) }
        Column { FieldLabel("EMAIL"); ToolInput(email, { email = it }, "jane@acme.com", minLines = 1) }
        Column { FieldLabel("PHONE"); ToolInput(phone, { phone = it }, "+1 555 0100", minLines = 1) }
        if (sig.isNotEmpty()) ToolResult(sig, accent, mono = false, label = "SIGNATURE")
    }
}

@androidx.compose.runtime.Composable
private fun GradientTool(accent: Color) {
    var c1 by remember { mutableStateOf("#1A46E5") }
    var c2 by remember { mutableStateOf("#3B2FC9") }
    var angle by remember { mutableStateOf(135) }
    val css = "background: linear-gradient(${angle}deg, $c1, $c2);"
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("COLOR 1 (hex)"); ToolInput(c1, { c1 = it }, "#1A46E5", minLines = 1, mono = true) }
        Column { FieldLabel("COLOR 2 (hex)"); ToolInput(c2, { c2 = it }, "#3B2FC9", minLines = 1, mono = true) }
        FieldLabel("ANGLE: ${angle}\u00b0")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(0,45,90,135,180).forEach { a -> Box(Modifier.weight(1f)) {
                ToolButton("$a\u00b0", if (angle==a) accent else accent.copy(alpha=0.35f)) { angle = a } } }
        }
        ToolResult(css, accent, mono = true, label = "CSS")
    }
}

@androidx.compose.runtime.Composable
private fun CssGenTool(accent: Color) {
    var radius by remember { mutableStateOf(12) }
    var shadowX by remember { mutableStateOf(0) }
    var shadowY by remember { mutableStateOf(4) }
    var blur by remember { mutableStateOf(12) }
    val css = "border-radius: ${radius}px;\nbox-shadow: ${shadowX}px ${shadowY}px ${blur}px rgba(0,0,0,0.15);"
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        FieldLabel("BORDER RADIUS: ${radius}px")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(0,8,12,20,999).forEach { r -> Box(Modifier.weight(1f)) {
                ToolButton(if (r==999) "full" else "$r", if (radius==r) accent else accent.copy(alpha=0.35f)) { radius = r } } }
        }
        FieldLabel("SHADOW BLUR: ${blur}px")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(0,6,12,24,40).forEach { bl -> Box(Modifier.weight(1f)) {
                ToolButton("$bl", if (blur==bl) accent else accent.copy(alpha=0.35f)) { blur = bl } } }
        }
        ToolResult(css, accent, mono = true, label = "CSS")
    }
}

@androidx.compose.runtime.Composable
/**
 * Cover letter.
 *
 * The first version had one hardcoded template with three blanks, so every
 * person who used it sent the same letter, padded with lines like "with my
 * background and passion for this field" that say nothing. A hiring manager
 * recognises that immediately.
 *
 * This asks for the things a letter actually needs - what you have done, what
 * you are good at, why this employer - and writes around them. It is still a
 * draft to edit rather than something to send unread, and it says so.
 */
private fun CoverLetterTool(accent: Color) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var years by remember { mutableStateOf("") }
    var strengths by remember { mutableStateOf("") }
    var why by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("Warm") }

    val ready = name.isNotBlank() && role.isNotBlank() && company.isNotBlank()
    val letter = remember(name, role, company, years, strengths, why, tone) {
        if (!ready) "" else buildCoverLetter(name, role, company, years, strengths, why, tone)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("YOUR NAME"); ToolInput(name, { name = it }, "Jane Doe", minLines = 1) }
        Column { FieldLabel("ROLE"); ToolInput(role, { role = it }, "Software Engineer", minLines = 1) }
        Column { FieldLabel("COMPANY"); ToolInput(company, { company = it }, "Acme Inc", minLines = 1) }
        Column {
            FieldLabel("YEARS IN THIS KIND OF WORK")
            ToolInput(years, { years = it }, "4", minLines = 1)
        }
        Column {
            FieldLabel("WHAT YOU ARE GOOD AT")
            ToolInput(strengths, { strengths = it },
                "Kotlin, shipping on small teams, working with designers", minLines = 2)
        }
        Column {
            FieldLabel("WHY THIS EMPLOYER")
            ToolInput(why, { why = it },
                "You build tools people rely on every day", minLines = 2)
        }
        FieldLabel("TONE")
        ChipRow(listOf("Warm", "Direct", "Formal"), tone, accent) { tone = it }

        if (letter.isNotEmpty()) {
            ToolResult(letter, accent, mono = false, label = "DRAFT")
            Text(
                "A starting point, not a finished letter. Replace anything that " +
                    "does not sound like you before you send it.",
                color = InkFaint, fontSize = 12.sp
            )
        } else {
            Text("Fill in your name, the role and the company to see a draft.",
                color = InkFaint, fontSize = 13.sp)
        }
    }
}

private fun buildCoverLetter(
    name: String, role: String, company: String,
    years: String, strengths: String, why: String, tone: String
): String {
    val n = name.trim(); val r = role.trim(); val c = company.trim()
    val skills = strengths.split(",", "\n")
        .map { it.trim() }.filter { it.isNotEmpty() }

    val opening = when (tone) {
        "Direct" -> "I am applying for the $r role at $c."
        "Formal" -> "I wish to be considered for the position of $r at $c."
        else -> "I would like to be considered for the $r role at $c."
    }

    val experience = years.trim().toIntOrNull()?.let { y ->
        val span = if (y == 1) "a year" else "$y years"
        when (tone) {
            "Direct" -> "I have spent $span doing this kind of work."
            "Formal" -> "I have $span of experience in this field."
            else -> "I have been doing this kind of work for $span."
        }
    }

    val strengthLine = when {
        skills.isEmpty() -> null
        skills.size == 1 -> "The part I am strongest on is ${skills[0]}."
        else -> {
            val list = skills.dropLast(1).joinToString(", ") + " and " + skills.last()
            when (tone) {
                "Formal" -> "My principal strengths are $list."
                else -> "Where I am strongest: $list."
            }
        }
    }

    val whyLine = why.trim().takeIf { it.isNotEmpty() }?.let {
        val body = it.trimEnd('.', ' ')
        when (tone) {
            "Direct" -> "I want to work at $c because $body."
            "Formal" -> "I am drawn to $c in particular because $body."
            else -> "What draws me to $c is that $body."
        }
    }

    val closing = when (tone) {
        "Direct" -> "I would be glad to talk it through. Thank you for your time."
        "Formal" -> "I would welcome the opportunity to discuss my application further. " +
            "Thank you for your consideration."
        else -> "I would love the chance to talk about it. Thank you for reading."
    }

    val body = listOfNotNull(
        listOfNotNull(opening, experience).joinToString(" "),
        listOfNotNull(strengthLine, whyLine).joinToString(" ").takeIf { it.isNotBlank() },
        closing
    ).joinToString("\n\n")

    val signOff = if (tone == "Formal") "Yours sincerely," else "Best regards,"
    return "Dear Hiring Manager,\n\n$body\n\n$signOff\n$n"
}

@androidx.compose.runtime.Composable
private fun ApiKeyTool(accent: Color) {
    var seed by remember { mutableStateOf(0) }
    var prefix by remember { mutableStateOf("sk") }
    val key = remember(seed, prefix) {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        "${prefix}_" + (1..40).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("PREFIX"); ToolInput(prefix, { prefix = it }, "sk", minLines = 1, mono = true) }
        ToolResult(key, accent, mono = true, label = "API KEY")
        ToolButton("Generate new", accent) { seed++ }
    }
}

@androidx.compose.runtime.Composable
private fun HashtagTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val tags = remember(t) {
        val words = Regex("[\\p{L}]+").findAll(t).map { it.value.lowercase() }.filter { it.length > 2 }.distinct().take(20).toList()
        if (words.isEmpty()) "" else words.joinToString(" ") { "#$it" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("TOPIC / KEYWORDS"); ToolInput(t, { t = it }, "morning coffee sunrise beach", minLines = 2) }
        if (tags.isNotEmpty()) ToolResult(tags, accent, mono = false, label = "HASHTAGS")
    }
}

@androidx.compose.runtime.Composable
private fun PaletteGenTool(accent: Color) {
    var base by remember { mutableStateOf("#1A46E5") }
    val palette = remember(base) {
        val hex = base.trim().removePrefix("#")
        if (hex.length != 6) "" else try {
            val r = hex.substring(0,2).toInt(16); val g = hex.substring(2,4).toInt(16); val b = hex.substring(4,6).toInt(16)
            fun shade(f: Double): String {
                val nr = (r*f).toInt().coerceIn(0,255); val ng=(g*f).toInt().coerceIn(0,255); val nb=(b*f).toInt().coerceIn(0,255)
                return "#%02X%02X%02X".format(nr,ng,nb)
            }
            listOf(0.5,0.75,1.0,1.25,1.5).joinToString("\n") { shade(it) }
        } catch (e: Exception) { "\u26a0 Invalid hex." }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("BASE COLOR (hex)"); ToolInput(base, { base = it }, "#1A46E5", minLines = 1, mono = true) }
        if (palette.isNotEmpty()) ToolResult(palette, accent, mono = true, label = "PALETTE (dark \u2192 light)")
    }
}
