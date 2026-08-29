package cc.devbangs.morpho.ui.tool.kit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

fun hasConverterTool(id: String): Boolean = id in setOf(
    "csv-to-json","json-to-csv","text-diff-checker","color-picker",
    "xml-to-json","yaml-to-json","docx-to-txt","subtitle-converter","png-to-webp","jpg-to-webp",
    "jpg-to-png","png-to-jpg","webp-to-png","heic-to-jpg","youtube-thumbnail-downloader"
)

@Composable
fun ConverterTool(id: String, accent: Color) {
    when (id) {
        "csv-to-json" -> CsvToJson(accent)
        "json-to-csv" -> JsonToCsv(accent)
        "text-diff-checker" -> DiffTool(accent)
        "color-picker" -> PaletteTool(accent)
        "youtube-thumbnail-downloader" -> YtThumb(accent)
        "xml-to-json" -> XmlToJsonTool(accent)
        "yaml-to-json" -> YamlToJsonTool(accent)
        "docx-to-txt" -> DocxToTxtTool(accent)
        "subtitle-converter" -> SubtitleTool(accent)
        else -> ImageConvert(id, accent)
    }
}

@Composable
private fun CsvToJson(accent: Color) {
    var t by remember { mutableStateOf("") }
    val out = remember(t) {
        if (t.isBlank()) "" else try {
            val lines = t.trim().lines().filter { it.isNotBlank() }
            val headers = lines.first().split(",").map { it.trim() }
            val arr = JSONArray()
            lines.drop(1).forEach { row ->
                val cells = row.split(","); val o = JSONObject()
                headers.forEachIndexed { i, h -> o.put(h, cells.getOrElse(i){""}.trim()) }
                arr.put(o)
            }
            arr.toString(2)
        } catch (e: Exception) { "⚠ Could not parse CSV" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("CSV"); ToolInput(t, { t = it }, "name,age\nAda,36", minLines = 5, mono = true) }
        ToolResult(out, accent, label = "JSON")
    }
}

@Composable
private fun JsonToCsv(accent: Color) {
    var t by remember { mutableStateOf("") }
    val out = remember(t) {
        if (t.isBlank()) "" else try {
            val arr = JSONArray(t.trim())
            if (arr.length() == 0) "" else {
                val keys = arr.getJSONObject(0).keys().asSequence().toList()
                val sb = StringBuilder(keys.joinToString(",")).append("\n")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    sb.append(keys.joinToString(",") { o.optString(it) }).append("\n")
                }
                sb.toString().trim()
            }
        } catch (e: Exception) { "⚠ Expecting a JSON array of objects" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("JSON ARRAY"); ToolInput(t, { t = it }, "[{\"name\":\"Ada\"}]", minLines = 5, mono = true) }
        ToolResult(out, accent, label = "CSV")
    }
}

@Composable
private fun DiffTool(accent: Color) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val out = remember(a, b) {
        if (a.isBlank() && b.isBlank()) "" else {
            val la = a.lines(); val lb = b.lines()
            val max = maxOf(la.size, lb.size)
            buildString {
                for (i in 0 until max) {
                    val x = la.getOrNull(i); val y = lb.getOrNull(i)
                    when {
                        x == y -> append("  ${x ?: ""}\n")
                        x == null -> append("+ $y\n")
                        y == null -> append("- $x\n")
                        else -> { append("- $x\n"); append("+ $y\n") }
                    }
                }
            }.trim()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("ORIGINAL"); ToolInput(a, { a = it }, "First text…", minLines = 3, mono = true) }
        Column { FieldLabel("CHANGED"); ToolInput(b, { b = it }, "Second text…", minLines = 3, mono = true) }
        ToolResult(out, accent, label = "DIFF")
    }
}

@Composable
private fun PaletteTool(accent: Color) {
    var hex by remember { mutableStateOf("1A46E5") }
    val clean = hex.trim().removePrefix("#")
    val valid = Regex("^[0-9a-fA-F]{6}$").matches(clean)
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("BASE HEX"); ToolInput(hex, { hex = it }, "1A46E5", minLines = 1, mono = true) }
        if (valid) {
            val base = clean.toLong(16)
            val r = (base shr 16 and 0xFF).toInt(); val g = (base shr 8 and 0xFF).toInt(); val b = (base and 0xFF).toInt()
            val shades = listOf(0.85f, 0.6f, 0.35f, 0f, -0.25f, -0.45f)
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                shades.forEach { f ->
                    val (rr,gg,bb) = if (f >= 0) Triple(
                        (r + (255-r)*f).toInt(), (g + (255-g)*f).toInt(), (b + (255-b)*f).toInt())
                    else Triple((r*(1+f)).toInt(), (g*(1+f)).toInt(), (b*(1+f)).toInt())
                    val h = "#%02X%02X%02X".format(rr,gg,bb)
                    Row(Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(Shape.chip).background(Color(0xFF000000 or (rr.toLong() shl 16) or (gg.toLong() shl 8) or bb.toLong())))
                        Spacer(Modifier.width(Space.md))
                        Text(h, color = Ink, fontSize = 15.sp)
                    }
                }
            }
        } else Text("Enter a 6-digit hex.", color = InkSoft, fontSize = 13.sp)
    }
}

@Composable
private fun YtThumb(accent: Color) {
    var url by remember { mutableStateOf("") }
    val id = remember(url) {
        Regex("(?:v=|youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
            ?: if (url.trim().length == 11) url.trim() else ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("YOUTUBE URL OR ID"); ToolInput(url, { url = it }, "https://youtu.be/…", minLines = 2) }
        if (id.isNotEmpty()) {
            val links = "Max     https://img.youtube.com/vi/$id/maxresdefault.jpg\nHQ      https://img.youtube.com/vi/$id/hqdefault.jpg\nMedium  https://img.youtube.com/vi/$id/mqdefault.jpg"
            ToolResult(links, accent, label = "THUMBNAIL URLS")
        }
    }
}

@Composable
private fun ImageConvert(id: String, accent: Color) {
    val ctx = LocalContext.current
    var src by remember { mutableStateOf<Bitmap?>(null) }
    val toWebp = id == "png-to-webp" || id == "jpg-to-webp"
    val toPng = id == "jpg-to-png" || id == "webp-to-png" || id == "svg-to-png"
    val fmt = when {
        toWebp -> Bitmap.CompressFormat.WEBP
        toPng -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.JPEG
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { u -> if (u != null) src = decodeBitmap(ctx, u) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ImagePickPreview(
            bitmap = src,
            accent = accent,
            onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onClear = { src = null }
        )
        src?.let { bmp ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(Modifier.weight(1f)) { ToolButton("Save", accent) {
                    saveToGallery(ctx, bmp, "morpho_${System.currentTimeMillis()}", fmt, 95) } }
                Box(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha=0.10f))
                        .clickable { shareBitmap(ctx, bmp, "morpho_${System.currentTimeMillis()}", fmt, 95) }
                        .padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                        Text("Share", color = accent, fontSize = 15.sp) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun XmlToJsonTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val out = remember(t) {
        if (t.isBlank()) "" else try {
            // minimal XML -> JSON for simple element trees
            fun parse(xml: String): String {
                val tagRe = Regex("<([\\w:-]+)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
                val matches = tagRe.findAll(xml).toList()
                if (matches.isEmpty()) return "\"" + xml.trim().replace("\"","\\\"") + "\""
                return "{" + matches.joinToString(",") { m ->
                    "\"${m.groupValues[1]}\": ${parse(m.groupValues[2].trim())}"
                } + "}"
            }
            val body = t.trim().replace(Regex("<\\?xml.*?\\?>"), "").trim()
            org.json.JSONObject(parse(body)).toString(2)
        } catch (e: Exception) { "\u26a0 Could not convert (simple XML only)." }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("XML"); ToolInput(t, { t = it }, "<user><name>Jane</name></user>", minLines = 5, mono = true) }
        if (out.isNotEmpty()) ToolResult(out, accent, mono = true, label = "JSON")
    }
}

@androidx.compose.runtime.Composable
private fun YamlToJsonTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    val out = remember(t) {
        if (t.isBlank()) "" else try {
            // simple flat YAML: key: value, and "- item" lists
            val obj = org.json.JSONObject()
            var listKey: String? = null
            val list = org.json.JSONArray()
            fun flush() { if (listKey != null) { obj.put(listKey, list); listKey = null } }
            t.lines().forEach { raw ->
                val line = raw.trimEnd()
                if (line.isBlank()) return@forEach
                val trimmed = line.trim()
                if (trimmed.startsWith("- ")) { list.put(trimmed.drop(2).trim().trim('"')) }
                else if (line.contains(":")) {
                    val k = line.substringBefore(":").trim()
                    val v = line.substringAfter(":").trim().trim('"')
                    if (v.isEmpty()) { listKey = k } else { obj.put(k, v) }
                }
            }
            flush()
            obj.toString(2)
        } catch (e: Exception) { "\u26a0 Could not convert (simple YAML only)." }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Column { FieldLabel("YAML (simple)"); ToolInput(t, { t = it }, "name: Jane\nrole: dev", minLines = 5, mono = true) }
        if (out.isNotEmpty()) ToolResult(out, accent, mono = true, label = "JSON")
    }
}

@androidx.compose.runtime.Composable
private fun DocxToTxtTool(accent: Color) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) {
            picked = true
            text = try {
                val bytes = ctx.contentResolver.openInputStream(u)?.readBytes() ?: ByteArray(0)
                val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes))
                var doc = ""
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") { doc = zis.readBytes().toString(Charsets.UTF_8); break }
                    entry = zis.nextEntry
                }
                zis.close()
                doc.replace(Regex("</w:p>"), "\n").replace(Regex("<[^>]+>"), "").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").trim()
                    .ifBlank { "No text found." }
            } catch (e: Exception) { "\u26a0 Could not read this .docx file." }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        PickRow(if (!picked) "Choose a .docx file" else "File loaded \u2713", accent) {
            picker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        if (text.isNotEmpty()) ToolResult(text, accent, mono = false, label = "EXTRACTED TEXT")
    }
}

@androidx.compose.runtime.Composable
private fun SubtitleTool(accent: Color) {
    var t by remember { mutableStateOf("") }
    var toVtt by remember { mutableStateOf(true) }
    val out = remember(t, toVtt) {
        if (t.isBlank()) "" else if (toVtt) {
            // SRT -> VTT: add header, commas to dots in timestamps
            "WEBVTT\n\n" + t.replace(Regex("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})"), "$1.$2")
                .replace(Regex("(?m)^\\d+\\s*$"), "").replace(Regex("\\n{3,}"), "\n\n").trim()
        } else {
            // VTT -> SRT: strip header, dots to commas, number cues
            var i = 0
            t.replace("WEBVTT", "").replace(Regex("(\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})"), "$1,$2")
                .trim().split(Regex("\\n{2,}")).filter { it.isNotBlank() }
                .joinToString("\n\n") { "${++i}\n$it" }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) { ToolButton("SRT \u2192 VTT", if (toVtt) accent else accent.copy(alpha=0.35f)) { toVtt = true } }
            Box(Modifier.weight(1f)) { ToolButton("VTT \u2192 SRT", if (!toVtt) accent else accent.copy(alpha=0.35f)) { toVtt = false } }
        }
        Column { FieldLabel("SUBTITLE TEXT"); ToolInput(t, { t = it }, "Paste .srt or .vtt content…", minLines = 5, mono = true) }
        if (out.isNotEmpty()) ToolResult(out, accent, mono = true, label = if (toVtt) "VTT" else "SRT")
    }
}

@androidx.compose.runtime.Composable
private fun PickRow(label: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent.copy(alpha = 0.07f))
            .border(1.5.dp, accent.copy(alpha = 0.22f), Shape.card)
            .clickable(onClick = onClick).padding(Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon("file-add", tint = accent, size = 22.dp)
        Spacer(Modifier.width(Space.md))
        Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
