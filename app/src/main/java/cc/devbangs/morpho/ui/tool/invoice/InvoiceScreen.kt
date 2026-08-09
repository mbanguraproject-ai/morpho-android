package cc.devbangs.morpho.ui.tool.invoice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.ui.tool.kit.FieldLabel
import cc.devbangs.morpho.ui.tool.kit.savePdfToDownloads
import cc.devbangs.morpho.ui.tool.kit.sharePdf

@Composable
fun InvoiceTool(accent: Color) {
    val s = remember { InvoiceState() }
    var tab by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Tabs(tab, accent) { tab = it }
        when (tab) {
            0 -> DetailsTab(s, accent)
            1 -> StyleTab(s, accent)
            2 -> PreviewTab(s, accent)
        }
    }
}

@Composable
private fun Tabs(tab: Int, accent: Color, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk).padding(4.dp)) {
        listOf("Details","Style","Preview").forEachIndexed { i, label ->
            val sel = i == tab
            Box(
                Modifier.weight(1f).clip(Shape.chip)
                    .background(if (sel) accent else Color.Transparent)
                    .clickable { onTab(i) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (sel) Paper else InkSoft, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Field(label: String, v: MutableState<String>, hint: String,
                  minLines: Int = 1, number: Boolean = false) {
    Column {
        FieldLabel(label)
        Box(Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
            .border(1.dp, PaperLine, Shape.field).padding(13.dp)) {
            if (v.value.isEmpty()) Text(hint, color = InkFaint, fontSize = 15.sp)
            BasicTextField(
                value = v.value, onValueChange = { v.value = it },
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Cobalt), minLines = minLines,
                keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailsTab(s: InvoiceState, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        SectionTitle("YOUR BUSINESS")
        Field("BUSINESS NAME", s.bizName, "Acme Studio")
        Field("ADDRESS · PHONE · EMAIL", s.bizDetails, "12 King St, Freetown\n+232 …", minLines = 2)
        Field("TAX / VAT ID", s.bizTaxId, "TIN 100234567")

        SectionTitle("BILL TO")
        Field("CLIENT NAME", s.clientName, "Blue Co Ltd")
        Field("CLIENT ADDRESS", s.clientDetails, "45 Wilkinson Rd", minLines = 2)
        Field("PO / REFERENCE", s.poNumber, "BC-8842")

        SectionTitle("INVOICE DETAILS")
        Field("INVOICE NUMBER", s.invoiceNumber, "INV-2026-001")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.weight(1f)) { Field("ISSUE DATE", s.issueDate, "08 Aug 2026") }
            Box(Modifier.weight(1f)) { Field("DUE DATE", s.dueDate, "22 Aug 2026") }
        }

        SectionTitle("LINE ITEMS")
        s.items.forEachIndexed { i, item -> LineItemRow(s, i, item, accent) }
        AddItemButton(accent) { s.items.add(LineItem("", "1", "0")) }

        SectionTitle("TOTALS")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.weight(1f)) { Field("DISCOUNT %", s.discountRate, "0", number = true) }
            Box(Modifier.weight(1f)) { Field("TAX %", s.taxRate, "0", number = true) }
        }
        Field("TAX LABEL", s.taxLabel, "GST")

        SectionTitle("PAYMENT & NOTES")
        Field("PAYMENT INSTRUCTIONS", s.payment, "Orange Money +232 …\nBank …", minLines = 2)
        Field("NOTES", s.notes, "Thank you for your business.", minLines = 2)

        LiveTotal(s, accent)
    }
}

@Composable
private fun LineItemRow(s: InvoiceState, index: Int, item: LineItem, accent: Color) {
    Column(Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Item ${index + 1}", color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            if (s.items.size > 1)
                Box(Modifier.clip(Shape.pill).clickable { s.items.removeAt(index) }.padding(4.dp)) {
                    MorphoIcon("close", tint = InkFaint, size = 16.dp)
                }
        }
        InlineField(item.description, "Description")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { InlineField(item.qty, "Qty", number = true) }
            Box(Modifier.weight(1.4f)) { InlineField(item.rate, "Rate", number = true) }
            Box(Modifier.weight(1.4f), contentAlignment = Alignment.CenterEnd) {
                Text(s.money(item.amount), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InlineField(v: MutableState<String>, hint: String, number: Boolean = false) {
    Box(Modifier.fillMaxWidth().clip(Shape.chip).background(Paper).border(1.dp, PaperLine, Shape.chip)
        .padding(horizontal = 11.dp, vertical = 10.dp)) {
        if (v.value.isEmpty()) Text(hint, color = InkFaint, fontSize = 14.sp)
        BasicTextField(v.value, { v.value = it },
            textStyle = TextStyle(color = Ink, fontSize = 14.sp), cursorBrush = SolidColor(Cobalt),
            singleLine = true,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddItemButton(accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.tile).background(accent.copy(alpha = 0.08f))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon("plus", tint = accent, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text("Add item", color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LiveTotal(s: InvoiceState, accent: Color) {
    Row(Modifier.fillMaxWidth().clip(Shape.card).background(accent).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("TOTAL DUE", color = Paper.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        Text(s.money(s.total), color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StyleTab(s: InvoiceState, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        SectionTitle("TEMPLATE")
        Template.entries.forEach { t ->
            val sel = s.template.value == t
            Row(Modifier.fillMaxWidth().clip(Shape.tile)
                .background(if (sel) accent.copy(alpha = 0.10f) else PaperSunk)
                .border(if (sel) 2.dp else 1.dp, if (sel) accent else PaperLine, Shape.tile)
                .clickable { s.template.value = t }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(t.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (sel) MorphoIcon("check", tint = accent, size = 20.dp)
            }
        }
        SectionTitle("ACCENT COLOR")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ACCENTS.forEach { a ->
                val col = Color(a.argb)
                val sel = s.accent.value == a
                Box(Modifier.size(44.dp).clip(Shape.chip).background(col)
                    .border(if (sel) 3.dp else 0.dp, Paper, Shape.chip)
                    .clickable { s.accent.value = a }) {
                    if (sel) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MorphoIcon("check", tint = Paper, size = 20.dp)
                    }
                }
            }
        }
        SectionTitle("CURRENCY")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CURRENCIES.take(5).forEach { cur ->
                val sel = s.currency.value == cur
                Box(Modifier.weight(1f).clip(Shape.chip)
                    .background(if (sel) accent else PaperSunk)
                    .clickable { s.currency.value = cur }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(cur, color = if (sel) Paper else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CURRENCIES.drop(5).forEach { cur ->
                val sel = s.currency.value == cur
                Box(Modifier.weight(1f).clip(Shape.chip)
                    .background(if (sel) accent else PaperSunk)
                    .clickable { s.currency.value = cur }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text(cur, color = if (sel) Paper else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PreviewTab(s: InvoiceState, accent: Color) {
    val ctx = LocalContext.current
    val bmp = remember(
        s.template.value, s.accent.value, s.currency.value, s.total,
        s.bizName.value, s.clientName.value, s.items.size, s.invoiceNumber.value,
        s.issueDate.value, s.dueDate.value, s.taxRate.value, s.discountRate.value,
        s.payment.value, s.notes.value
    ) { renderInvoiceBitmap(s) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Box(Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
            .border(1.dp, PaperLine, Shape.card).padding(8.dp)) {
            Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent)
                    .clickable {
                        val name = s.invoiceNumber.value.ifBlank { "invoice" }
                        savePdfToDownloads(ctx, renderInvoicePdf(s), name)
                    }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Save PDF", color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                    .clickable {
                        val name = s.invoiceNumber.value.ifBlank { "invoice" }
                        sharePdf(ctx, renderInvoicePdf(s), name)
                    }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Share", color = accent, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp))
}
