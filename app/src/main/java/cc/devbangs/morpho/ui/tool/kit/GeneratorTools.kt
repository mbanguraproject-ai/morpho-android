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
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
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
    val bmp = remember(t) { if (t.isBlank()) null else qrBitmap(t, 640) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("CONTENT"); ToolInput(t, { t = it }, "URL, text, Wi-Fi, anything…", minLines = 3) }
        if (bmp != null) Box(
            Modifier.fillMaxWidth().clip(Shape.card).background(Paper).padding(Space.xl),
            contentAlignment = Alignment.Center
        ) {
            Image(bmp.asImageBitmap(), null, Modifier.size(240.dp))
        }
    }
}

// ---------- Barcode ----------
@Composable
private fun BarcodeTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val bmp = remember(t) {
        if (t.isBlank()) null else try { barcodeBitmap(t, 720, 260) } catch (e: Exception) { null }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("CODE VALUE"); ToolInput(t, { t = it }, "Enter digits/text…", minLines = 1, mono = true) }
        if (bmp != null) Box(
            Modifier.fillMaxWidth().clip(Shape.card).background(Paper).padding(Space.lg),
            contentAlignment = Alignment.Center
        ) { Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().height(120.dp)) }
        else if (t.isNotBlank()) Text("⚠ Could not encode this value.", color = InkSoft, fontSize = 13.sp)
    }
}

// ---------- Fake Data ----------
@Composable
private fun FakeDataTool(accent: Color) {
    var seed by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(5) }
    val out = remember(seed, count) { (1..count).joinToString("\n\n") { fakePerson() } }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(1,5,10,20).forEach { n ->
                Box(Modifier.weight(1f)) { ToolButton("$n", if (count==n) accent else accent.copy(alpha=0.35f)) { count = n } }
            }
        }
        ToolButton("Regenerate", accent) { seed++ }
        ToolResult(out, accent, mono = false, label = "RECORDS")
    }
}

// ---- helpers ----
private fun qrBitmap(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1)
    val m = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

private fun barcodeBitmap(text: String, w: Int, h: Int): Bitmap {
    val m = MultiFormatWriter().encode(text, BarcodeFormat.CODE_128, w, h)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    for (x in 0 until w) for (y in 0 until h)
        bmp.setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}

private val firsts = listOf("Aminata","Mohamed","Fatmata","Ibrahim","Isatu","Abu","Mariama","Sorie","Kadiatu","Alhaji")
private val lasts = listOf("Kamara","Bangura","Sesay","Koroma","Turay","Conteh","Jalloh","Mansaray","Fofanah","Bah")
private val domains = listOf("gmail.com","outlook.com","yahoo.com","proton.me")
private fun fakePerson(): String {
    val f = firsts.random(); val l = lasts.random()
    val email = "${f.lowercase()}.${l.lowercase()}${Random.nextInt(10,99)}@${domains.random()}"
    val phone = "+232 ${Random.nextInt(70,99)} ${Random.nextInt(100,999)} ${Random.nextInt(100,999)}"
    return "Name   $f $l\nEmail  $email\nPhone  $phone"
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
private fun CoverLetterTool(accent: Color) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    val letter = if (name.isBlank() || role.isBlank() || company.isBlank()) "" else
        "Dear Hiring Manager,\n\nI am writing to express my strong interest in the $role position at $company. With my background and passion for this field, I am confident I would be a valuable addition to your team.\n\nThroughout my career I have developed the skills needed to excel in this role, and I am excited about the opportunity to contribute to $company's continued success.\n\nThank you for considering my application. I look forward to discussing how I can contribute.\n\nSincerely,\n$name"
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("YOUR NAME"); ToolInput(name, { name = it }, "Jane Doe", minLines = 1) }
        Column { FieldLabel("ROLE"); ToolInput(role, { role = it }, "Software Engineer", minLines = 1) }
        Column { FieldLabel("COMPANY"); ToolInput(company, { company = it }, "Acme Inc", minLines = 1) }
        if (letter.isNotEmpty()) ToolResult(letter, accent, mono = false, label = "COVER LETTER")
    }
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
