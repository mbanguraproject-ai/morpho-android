package cc.devbangs.morpho.ui.tool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.Tool
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.tool.kit.hasTextDevTool
import cc.devbangs.morpho.ui.tool.kit.TextDevTool
import cc.devbangs.morpho.ui.tool.kit.hasGeneratorTool
import cc.devbangs.morpho.ui.tool.kit.GeneratorTool
import cc.devbangs.morpho.ui.tool.kit.hasImageTool
import cc.devbangs.morpho.ui.tool.kit.ImageTool
import cc.devbangs.morpho.ui.tool.kit.hasPdfTool
import cc.devbangs.morpho.ui.tool.kit.PdfTool
import cc.devbangs.morpho.ui.tool.kit.hasConverterTool
import cc.devbangs.morpho.ui.tool.kit.ConverterTool
import cc.devbangs.morpho.ui.tool.kit.hasExtraTool
import cc.devbangs.morpho.ui.tool.kit.ExtraTool
import cc.devbangs.morpho.ui.tool.invoice.InvoiceTool
import cc.devbangs.morpho.ui.tool.resume.ResumeTool
import cc.devbangs.morpho.ui.theme.*

@Composable
fun ToolScreen(
    toolId: String,
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val tool = ToolRegistry.byId(toolId)
    val tint = tool?.category?.accent ?: Cobalt

    Column(Modifier.fillMaxSize().background(Paper)) {
        // top zone
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to tint.copy(alpha = 0.10f), 1f to Paper)
            )
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack)
                Spacer(Modifier.weight(1f))
            }
            if (tool != null) Row(
                Modifier.padding(start = Space.gutter, end = Space.gutter, top = 4.dp, bottom = Space.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).clip(Shape.card).background(tint),
                    contentAlignment = Alignment.Center
                ) { MorphoIcon(tool.iconKey, tint = Paper, size = 29.dp) }
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(tool.name, style = MaterialTheme.typography.headlineSmall, color = Ink)
                    StatusLabel(tool)
                }
            }
        }

        if (tool == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unknown tool", color = InkSoft)
            }
            return
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = Space.gutter, end = Space.gutter, top = Space.md,
                    bottom = contentPadding.calculateBottomPadding() + Space.xxl)
        ) {
            Text(tool.short, style = MaterialTheme.typography.bodyLarge, color = InkSoft)
            Spacer(Modifier.height(Space.xl))
            // Real tool UIs mount here via dispatch (added next).
            ToolHost(tool = tool)
        }
    }
}

@Composable
private fun StatusLabel(tool: Tool) {
    val (txt, c) = if (tool.offline) "Works offline" to tool.category.accent
                   else "Server tool · coming soon" to InkSoft
    Row(verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon(if (tool.offline) "check" else "clock", tint = c, size = 13.dp)
        Spacer(Modifier.width(5.dp))
        Text(txt, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Dispatch to the real per-tool UI, else a branded placeholder. */
@Composable
private fun ToolHost(tool: Tool) {
    when {
        hasTextDevTool(tool.id) -> TextDevTool(tool.id, tool.category.accent)
        hasGeneratorTool(tool.id) -> GeneratorTool(tool.id, tool.category.accent)
        hasImageTool(tool.id) -> ImageTool(tool.id, tool.category.accent)
        hasPdfTool(tool.id) -> PdfTool(tool.id, tool.category.accent)
        hasConverterTool(tool.id) -> ConverterTool(tool.id, tool.category.accent)
        hasExtraTool(tool.id) -> ExtraTool(tool.id, tool.category.accent)
        tool.id == "invoice-generator" -> InvoiceTool(tool.category.accent)
        tool.id == "resume-builder" -> ResumeTool(tool.category.accent)
        else -> Placeholder(tool)
    }
}

@Composable
private fun Placeholder(tool: Tool) {
    Box(
        Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk).padding(Space.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphoIcon(if (tool.offline) "settings" else "clock", tint = InkFaint, size = 30.dp)
            Spacer(Modifier.height(Space.md))
            Text(
                if (tool.offline) "This tool arrives in the next build."
                else "This tool needs a server engine\nand arrives in a later build.",
                color = InkSoft, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
