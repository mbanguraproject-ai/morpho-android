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
    "password-generator","qr-code-generator","barcode-generator","fake-data-generator"
)

@Composable
fun GeneratorTool(id: String, accent: Color) {
    when (id) {
        "password-generator" -> PasswordTool(accent)
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
