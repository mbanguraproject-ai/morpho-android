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
    val toPng = id == "jpg-to-png" || id == "webp-to-png" || id == "svg-to-png"
    val fmt = if (toPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
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
