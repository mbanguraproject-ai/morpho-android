package cc.devbangs.morpho.ui.tool.kit

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cc.devbangs.morpho.core.Space
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Returns true if this tool id has a native text/dev implementation here. */
fun hasTextDevTool(id: String): Boolean = id in setOf(
    "word-counter","character-counter","case-converter","slug-generator",
    "lorem-ipsum-generator","json-formatter","base64-encoder","url-encoder",
    "hash-generator","uuid-generator","jwt-decoder","color-converter"
)

@Composable
fun TextDevTool(id: String, accent: Color) {
    when (id) {
        "word-counter", "character-counter" -> CounterTool(accent)
        "case-converter" -> CaseTool(accent)
        "slug-generator" -> SlugTool(accent)
        "lorem-ipsum-generator" -> LoremTool(accent)
        "json-formatter" -> JsonTool(accent)
        "base64-encoder" -> Base64Tool(accent)
        "url-encoder" -> UrlTool(accent)
        "hash-generator" -> HashTool(accent)
        "uuid-generator" -> UuidTool(accent)
        "jwt-decoder" -> JwtTool(accent)
        "color-converter" -> ColorTool(accent)
    }
}

@Composable
private fun CounterTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val words = if (t.isBlank()) 0 else t.trim().split(Regex("\\s+")).size
    val chars = t.length
    val noSpaces = t.count { !it.isWhitespace() }
    val sentences = if (t.isBlank()) 0 else t.split(Regex("[.!?]+")).count { it.isNotBlank() }
    val lines = if (t.isEmpty()) 0 else t.lines().size
    val readMins = maxOf(1, Math.round(words / 200.0).toInt())
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("TEXT"); ToolInput(t, { t = it }, "Paste or type text here…", minLines = 6) }
        StatGrid(listOf(
            "Words" to "$words", "Characters" to "$chars",
            "No spaces" to "$noSpaces", "Sentences" to "$sentences",
            "Lines" to "$lines", "Read time" to "${readMins}m"
        ), accent)
    }
}

@Composable
private fun CaseTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    fun title(s: String) = s.split(" ").joinToString(" ") { w ->
        if (w.isEmpty()) w else w[0].uppercase() + w.drop(1).lowercase()
    }
    fun camel(s: String): String {
        val parts = s.trim().split(Regex("[\\s_-]+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        return parts[0].lowercase() + parts.drop(1).joinToString("") {
            it[0].uppercase() + it.drop(1).lowercase()
        }
    }
    val forms = listOf(
        "UPPERCASE" to t.uppercase(),
        "lowercase" to t.lowercase(),
        "Title Case" to title(t),
        "Sentence case" to t.lowercase().replaceFirstChar { it.uppercase() },
        "camelCase" to camel(t),
        "snake_case" to t.trim().lowercase().replace(Regex("[\\s-]+"), "_"),
        "kebab-case" to t.trim().lowercase().replace(Regex("[\\s_]+"), "-")
    )
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("TEXT"); ToolInput(t, { t = it }, "Type text to convert…", minLines = 3) }
        forms.forEach { (label, out) -> ToolResult(out, accent, mono = false, label = label) }
    }
}

@Composable
private fun SlugTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val slug = t.trim().lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("[\\s-]+"), "-").trim('-')
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("TITLE"); ToolInput(t, { t = it }, "My Awesome Blog Post!", minLines = 2) }
        ToolResult(slug, accent, label = "SLUG")
    }
}

@Composable
private fun LoremTool(accent: Color) {
    var count by remember { mutableStateOf(3) }
    val para = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
    val out = (1..count).joinToString("\n\n") { para }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column {
            FieldLabel("PARAGRAPHS: $count")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf(1,3,5,10).forEach { n ->
                    Box(Modifier.weight(1f)) {
                        ToolButton("$n", if (count==n) accent else accent.copy(alpha=0.4f)) { count = n }
                    }
                }
            }
        }
        ToolResult(out, accent, mono = false)
    }
}

@Composable
private fun JsonTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val result = remember(t) {
        if (t.isBlank()) "" else try {
            val trimmed = t.trim()
            if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2)
            else JSONObject(trimmed).toString(2)
        } catch (e: Exception) { "⚠ Invalid JSON: ${e.message}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("JSON"); ToolInput(t, { t = it }, "{\"key\":\"value\"}", minLines = 5, mono = true) }
        ToolResult(result, accent, label = "FORMATTED")
    }
}

@Composable
private fun Base64Tool(accent: Color) {
    var t by remember { mutableStateOf("") }
    var decode by remember { mutableStateOf(false) }
    val out = remember(t, decode) {
        if (t.isBlank()) "" else try {
            if (decode) String(Base64.decode(t, Base64.DEFAULT))
            else Base64.encodeToString(t.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { "⚠ Invalid input" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) { ToolButton("Encode", if (!decode) accent else accent.copy(alpha=0.4f)) { decode = false } }
            Box(Modifier.weight(1f)) { ToolButton("Decode", if (decode) accent else accent.copy(alpha=0.4f)) { decode = true } }
        }
        Column { FieldLabel(if (decode) "BASE64" else "TEXT"); ToolInput(t, { t = it }, "Enter text…", minLines = 4, mono = true) }
        ToolResult(out, accent)
    }
}

@Composable
private fun UrlTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    var decode by remember { mutableStateOf(false) }
    val out = remember(t, decode) {
        if (t.isBlank()) "" else try {
            if (decode) java.net.URLDecoder.decode(t, "UTF-8")
            else java.net.URLEncoder.encode(t, "UTF-8")
        } catch (e: Exception) { "⚠ Invalid input" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) { ToolButton("Encode", if (!decode) accent else accent.copy(alpha=0.4f)) { decode = false } }
            Box(Modifier.weight(1f)) { ToolButton("Decode", if (decode) accent else accent.copy(alpha=0.4f)) { decode = true } }
        }
        Column { FieldLabel("URL / TEXT"); ToolInput(t, { t = it }, "https://example.com/?q=a b", minLines = 3, mono = true) }
        ToolResult(out, accent)
    }
}

@Composable
private fun HashTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    fun hash(algo: String, s: String): String {
        if (s.isEmpty()) return ""
        val md = MessageDigest.getInstance(algo)
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("TEXT"); ToolInput(t, { t = it }, "Enter text to hash…", minLines = 3) }
        ToolResult(hash("MD5", t), accent, label = "MD5")
        ToolResult(hash("SHA-1", t), accent, label = "SHA-1")
        ToolResult(hash("SHA-256", t), accent, label = "SHA-256")
    }
}

@Composable
private fun UuidTool(accent: Color) {
    var count by remember { mutableStateOf(5) }
    var seed by remember { mutableStateOf(0) }
    val out = remember(count, seed) { (1..count).joinToString("\n") { UUID.randomUUID().toString() } }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf(1,5,10,25).forEach { n ->
                Box(Modifier.weight(1f)) { ToolButton("$n", if (count==n) accent else accent.copy(alpha=0.4f)) { count = n } }
            }
        }
        ToolButton("Regenerate", accent) { seed++ }
        ToolResult(out, accent, label = "UUIDs (v4)")
    }
}

@Composable
private fun JwtTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val decoded = remember(t) {
        if (t.isBlank()) "" else try {
            val parts = t.trim().split(".")
            if (parts.size < 2) "⚠ Not a valid JWT" else {
                val header = String(Base64.decode(parts[0], Base64.URL_SAFE))
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                "HEADER\n${JSONObject(header).toString(2)}\n\nPAYLOAD\n${JSONObject(payload).toString(2)}"
            }
        } catch (e: Exception) { "⚠ Could not decode: ${e.message}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("JWT TOKEN"); ToolInput(t, { t = it }, "eyJhbGci…", minLines = 4, mono = true) }
        ToolResult(decoded, accent, label = "DECODED")
    }
}

@Composable
private fun ColorTool(accent: Color) {
    var hex by remember { mutableStateOf("1A46E5") }
    val clean = hex.trim().removePrefix("#")
    val info = remember(clean) {
        if (!Regex("^[0-9a-fA-F]{6}$").matches(clean)) "⚠ Enter a 6-digit hex" else {
            val r = clean.substring(0,2).toInt(16)
            val g = clean.substring(2,4).toInt(16)
            val b = clean.substring(4,6).toInt(16)
            val (h,s,l) = rgbToHsl(r,g,b)
            "HEX   #${clean.uppercase()}\nRGB   rgb($r, $g, $b)\nHSL   hsl($h, $s%, $l%)"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("HEX"); ToolInput(hex, { hex = it }, "1A46E5", minLines = 1, mono = true) }
        ToolResult(info, accent, label = "CONVERSIONS")
    }
}

private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int,Int,Int> {
    val rf=r/255.0; val gf=g/255.0; val bf=b/255.0
    val max=maxOf(rf,gf,bf); val min=minOf(rf,gf,bf); val d=max-min
    var h=0.0
    if (d!=0.0) h = when(max){ rf->((gf-bf)/d)%6; gf->(bf-rf)/d+2; else->(rf-gf)/d+4 }*60
    if (h<0) h+=360
    val l=(max+min)/2
    val s=if(d==0.0)0.0 else d/(1-Math.abs(2*l-1))
    return Triple(Math.round(h).toInt(), Math.round(s*100).toInt(), Math.round(l*100).toInt())
}
