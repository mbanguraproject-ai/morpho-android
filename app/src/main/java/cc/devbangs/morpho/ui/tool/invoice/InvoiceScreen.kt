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
import android.graphics.Bitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import cc.devbangs.morpho.data.invoice.BusinessRecord
import cc.devbangs.morpho.data.invoice.CatalogItemRecord
import cc.devbangs.morpho.data.invoice.ClientRecord
import cc.devbangs.morpho.data.invoice.InvoiceRecord
import cc.devbangs.morpho.data.invoice.InvoiceRepo
import cc.devbangs.morpho.ui.tool.kit.ProcessingCard
import cc.devbangs.morpho.ui.tool.kit.ToolButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong
import cc.devbangs.morpho.ui.tool.kit.savePdfToDownloads
import cc.devbangs.morpho.ui.tool.kit.sharePdf

/**
 * Invoices, receipts and quotes.
 *
 * This was a single editor that opened blank every time and hardcoded its
 * number to INV-2026-001. Nothing was kept, so there was no reopening
 * yesterday's invoice, no sequence, and two invoices in a row carried the same
 * number. It is a list of saved documents now, with the editor sitting behind
 * it.
 */
@Composable
fun InvoiceTool(accent: Color, docType: DocType = DocType.INVOICE) {
    // null shows the list. 0 opens a new document, anything else opens that
    // record. The key forces a fresh editor per open, so state from the last
    // document cannot leak into the next one.
    var openId by remember { mutableStateOf<Long?>(null) }
    var openKey by remember { mutableStateOf(0) }

    if (openId == null) {
        InvoiceList(
            docType, accent,
            onNew = { openId = 0L; openKey++ },
            onOpen = { openId = it; openKey++ }
        )
    } else {
        InvoiceEditor(openId ?: 0L, openKey, docType, accent) { openId = null }
    }
}

/** Amount for a list row, from the stored total. */
private fun money(currency: String, v: Double): String {
    val cents = (v * 100).roundToLong()
    val whole = cents / 100
    val rest = (cents % 100).toInt()
    return currency + " " + "%,d".format(whole) + "." + "%02d".format(kotlin.math.abs(rest))
}

@Composable
private fun InvoiceList(
    docType: DocType,
    accent: Color,
    onNew: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val all by InvoiceRepo.observeAll(ctx).collectAsState(initial = emptyList())
    // Each type keeps its own list, matching the separate numbering.
    val rows = all.filter { it.docType == docType.name }
    // Deleting a document someone issued should take two taps, not one.
    var confirmId by remember { mutableStateOf(0L) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        ToolButton("New " + docType.title.lowercase(), accent, onClick = onNew)

        if (rows.isEmpty()) {
            Text(
                "Nothing saved yet. Anything you create here stays on this device.",
                color = InkFaint, fontSize = 13.sp
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Space.sm),
                modifier = Modifier.heightIn(max = 520.dp)
            ) {
                items(rows, key = { it.id }) { r ->
                    InvoiceRow(
                        r, accent,
                        confirming = confirmId == r.id,
                        onOpen = { onOpen(r.id) },
                        onAskDelete = { confirmId = if (confirmId == r.id) 0L else r.id },
                        onConfirmDelete = {
                            confirmId = 0L
                            scope.launch { InvoiceRepo.delete(ctx, r.id) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(
    r: InvoiceRecord,
    accent: Color,
    confirming: Boolean,
    onOpen: () -> Unit,
    onAskDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk).padding(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { onOpen() }) {
                Text(
                    r.number.ifBlank { "Untitled" },
                    color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    r.clientName.ifBlank { "No client yet" },
                    color = InkSoft, fontSize = 13.sp
                )
                if (r.issueDate.isNotBlank()) {
                    Text(r.issueDate, color = InkFaint, fontSize = 12.sp)
                }
            }
            Text(
                money(r.currency, r.total),
                color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(Space.sm))
            Box(
                Modifier.size(30.dp).clip(Shape.pill)
                    .background(if (confirming) accent else PaperLine.copy(alpha = 0.5f))
                    .clickable { onAskDelete() },
                contentAlignment = Alignment.Center
            ) {
                MorphoIcon("close", tint = if (confirming) Paper else InkSoft, size = 13.dp)
            }
        }
        if (confirming) {
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Delete this permanently?", color = InkSoft, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(Shape.pill).background(accent)
                        .clickable { onConfirmDelete() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) { Text("Delete", color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun InvoiceEditor(
    id: Long,
    key: Int,
    docType: DocType,
    accent: Color,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = remember(key) { InvoiceState().apply { this.docType.value = docType } }
    var ready by remember(key) { mutableStateOf(false) }
    var saving by remember(key) { mutableStateOf(false) }
    var savedAt by remember(key) { mutableStateOf(0L) }
    var tab by remember(key) { mutableStateOf(0) }

    LaunchedEffect(key) {
        if (id > 0L) {
            InvoiceRepo.load(ctx, id)?.let { s.loadFrom(it) }
        } else {
            // The number comes from the highest one already used for this
            // type, so it never repeats and never skips.
            val (seq, number) = InvoiceRepo.nextNumber(ctx, docType.name, docType.numberPrefix)
            s.seq = seq
            s.invoiceNumber.value = number
        }
        ready = true
    }

    if (!ready) {
        ProcessingCard("Opening", accent)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(Shape.pill).background(PaperSunk)
                    .clickable { onBack() }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("All " + docType.title.lowercase() + "s", color = InkSoft, fontSize = 13.sp) }
            Spacer(Modifier.weight(1f))
            if (savedAt > 0L && !saving) {
                Text("Saved", color = accent, fontSize = 12.sp)
                Spacer(Modifier.width(Space.sm))
            }
            Box(
                Modifier.clip(Shape.pill)
                    .background(if (saving) accent.copy(alpha = 0.4f) else accent)
                    .clickable(enabled = !saving) {
                        saving = true
                        scope.launch {
                            s.recordId = InvoiceRepo.save(ctx, s.toRecord(), s.itemRecords())
                            savedAt = System.currentTimeMillis()
                            saving = false
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    if (saving) "Saving" else "Save",
                    color = Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

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
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Collected once here rather than per line, so twenty items do not open
    // twenty collectors on the same query.
    val catalog by InvoiceRepo.observeCatalog(ctx).collectAsState(initial = emptyList())

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        SectionTitle("YOUR BUSINESS")
        SavedBusinessRow(s, accent)
        Field("BUSINESS NAME", s.bizName, "Acme Studio")
        Field("ADDRESS · PHONE · EMAIL", s.bizDetails, "12 King St, Freetown\n+232 …", minLines = 2)
        Field("TAX / VAT ID", s.bizTaxId, "TIN 100234567")

        SectionTitle("BILL TO")
        SavedClientRow(s, accent)
        Field("CLIENT NAME", s.clientName, "Blue Co Ltd")
        Field("CLIENT ADDRESS", s.clientDetails, "45 Wilkinson Rd", minLines = 2)
        Field("PO / REFERENCE", s.poNumber, "BC-8842")

        SectionTitle("INVOICE DETAILS")
        Field("INVOICE NUMBER", s.invoiceNumber, "INV00001")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.weight(1f)) { DateField("ISSUE DATE", s.issueDate, accent) }
            Box(Modifier.weight(1f)) { DateField("DUE DATE", s.dueDate, accent) }
        }

        SectionTitle("LINE ITEMS")
        if (catalog.isNotEmpty()) {
            SavedRow(
                labels = catalog.map { it.id to it.name.ifBlank { "Unnamed" } },
                accent = accent,
                canSave = false,
                onPick = { id ->
                    catalog.firstOrNull { it.id == id }?.let {
                        s.items.add(LineItem(it.name, "1", it.rate, it.taxRate))
                    }
                },
                onSave = {},
                onDelete = { id -> scope.launch { InvoiceRepo.deleteCatalogItem(ctx, id) } }
            )
        }
        s.items.forEachIndexed { i, item ->
            LineItemRow(s, i, item, accent) {
                scope.launch {
                    val existing = catalog.firstOrNull {
                        it.name.equals(item.description.value, true)
                    }
                    InvoiceRepo.saveCatalogItem(
                        ctx,
                        CatalogItemRecord(
                            id = existing?.id ?: 0L,
                            name = item.description.value,
                            rate = item.rate.value,
                            taxRate = item.taxRate.value
                        )
                    )
                }
            }
        }
        // A new line starts on the document's default rate, so the common case
        // of one rate throughout needs no per-line typing at all.
        AddItemButton(accent) { s.items.add(LineItem("", "1", "0", s.taxRate.value)) }

        SectionTitle("TOTALS")
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.weight(1f)) { Field("DISCOUNT %", s.discountRate, "0", number = true) }
            Box(Modifier.weight(1f)) { Field("DEFAULT TAX %", s.taxRate, "0", number = true) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
            Box(Modifier.weight(1f)) { Field("TAX LABEL", s.taxLabel, "GST") }
            Box(Modifier.weight(1f)) { Field("SHIPPING", s.shipping, "0", number = true) }
        }

        SectionTitle("PAYMENT & NOTES")
        Field("PAYMENT INSTRUCTIONS", s.payment, "Orange Money +232 …\nBank …", minLines = 2)
        Field("NOTES", s.notes, "Thank you for your business.", minLines = 2)

        LiveTotal(s, accent)
    }
}

@Composable
private fun LineItemRow(
    s: InvoiceState,
    index: Int,
    item: LineItem,
    accent: Color,
    onSaveToCatalogue: () -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Item ${index + 1}", color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            // Only offered once the line says something worth keeping, and
            // this is where people discover the catalogue exists at all.
            if (item.description.value.isNotBlank()) {
                Box(
                    Modifier.clip(Shape.pill).background(accent.copy(alpha = 0.12f))
                        .clickable { onSaveToCatalogue() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) { Text("Save to list", color = accent, fontSize = 11.sp) }
                Spacer(Modifier.width(6.dp))
            }
            if (s.items.size > 1)
                Box(Modifier.clip(Shape.pill).clickable { s.items.removeAt(index) }.padding(4.dp)) {
                    MorphoIcon("close", tint = InkFaint, size = 16.dp)
                }
        }
        InlineField(item.description, "Description")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { InlineField(item.qty, "Qty", number = true) }
            Box(Modifier.weight(1.4f)) { InlineField(item.rate, "Rate", number = true) }
            Box(Modifier.weight(1f)) { InlineField(item.taxRate, "Tax %", number = true) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val lineTax = item.amount * (item.taxRate.value.toDoubleOrNull() ?: 0.0) / 100.0
            Text(
                if (lineTax > 0.0) "incl. " + s.money(lineTax) + " tax" else "",
                color = InkFaint, fontSize = 11.sp, modifier = Modifier.weight(1f)
            )
            Text(s.money(item.amount), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
    Column(
        Modifier.fillMaxWidth().clip(Shape.card).background(accent).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // The parts are shown because a single figure gives the user no way to
        // tell a mistyped tax rate from a mistyped quantity.
        BreakdownRow("Subtotal", s.money(s.subtotal))
        if (s.discountAmt > 0.0) BreakdownRow("Discount", "\u2212 " + s.money(s.discountAmt))
        if (s.taxAmt > 0.0) {
            val label = s.taxLabel.value.ifBlank { "Tax" } +
                (s.uniformTaxRate?.let { " (" + it + "%)" } ?: "")
            BreakdownRow(label, s.money(s.taxAmt))
        }
        if (s.shippingAmt > 0.0) BreakdownRow("Shipping", s.money(s.shippingAmt))
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOTAL DUE", color = Paper.copy(alpha = 0.9f), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
            )
            Text(s.money(s.total), color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, color = Paper.copy(alpha = 0.72f), fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(value, color = Paper.copy(alpha = 0.9f), fontSize = 13.sp)
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

/**
 * A saved entry, and the control that saves the current one.
 *
 * Tapping an entry fills the fields below rather than binding to them. The
 * invoice keeps its own copy, so editing a client here later leaves documents
 * already issued exactly as they were sent.
 */
@Composable
private fun SavedRow(
    labels: List<Pair<Long, String>>,
    accent: Color,
    canSave: Boolean,
    onPick: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: (Long) -> Unit
) {
    var armed by remember { mutableStateOf(0L) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canSave) {
            Box(
                Modifier.clip(Shape.pill).background(accent)
                    .clickable { onSave() }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text("Save this", color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        labels.forEach { (id, label) ->
            val warn = armed == id
            Row(
                Modifier.clip(Shape.pill)
                    .background(if (warn) accent.copy(alpha = 0.22f) else PaperSunk)
                    .padding(start = 13.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = InkSoft, fontSize = 12.sp,
                    modifier = Modifier.clickable { armed = 0L; onPick(id) }
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(20.dp).clip(Shape.pill)
                        .background(if (warn) accent else PaperLine.copy(alpha = 0.6f))
                        .clickable {
                            if (warn) { armed = 0L; onDelete(id) } else armed = id
                        },
                    contentAlignment = Alignment.Center
                ) { MorphoIcon("close", tint = if (warn) Paper else InkSoft, size = 10.dp) }
            }
        }
        if (labels.isEmpty() && !canSave) {
            Text("Fill this in once and save it for next time.", color = InkFaint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SavedBusinessRow(s: InvoiceState, accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by InvoiceRepo.observeBusinesses(ctx).collectAsState(initial = emptyList())
    SavedRow(
        labels = saved.map { it.id to it.name.ifBlank { "Unnamed" } },
        accent = accent,
        canSave = s.bizName.value.isNotBlank(),
        onPick = { id ->
            saved.firstOrNull { it.id == id }?.let {
                s.bizName.value = it.name
                s.bizDetails.value = it.details
                s.bizTaxId.value = it.taxId
            }
        },
        onSave = {
            scope.launch {
                // Same name updates in place instead of stacking duplicates
                // every time an invoice is written.
                val existing = saved.firstOrNull { it.name.equals(s.bizName.value, true) }
                InvoiceRepo.saveBusiness(
                    ctx,
                    BusinessRecord(
                        id = existing?.id ?: 0L,
                        name = s.bizName.value,
                        details = s.bizDetails.value,
                        taxId = s.bizTaxId.value
                    )
                )
            }
        },
        onDelete = { id -> scope.launch { InvoiceRepo.deleteBusiness(ctx, id) } }
    )
}

@Composable
private fun SavedClientRow(s: InvoiceState, accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by InvoiceRepo.observeClients(ctx).collectAsState(initial = emptyList())
    SavedRow(
        labels = saved.map { it.id to it.name.ifBlank { "Unnamed" } },
        accent = accent,
        canSave = s.clientName.value.isNotBlank(),
        onPick = { id ->
            saved.firstOrNull { it.id == id }?.let {
                s.clientName.value = it.name
                s.clientDetails.value = it.details
                s.poNumber.value = it.reference
            }
        },
        onSave = {
            scope.launch {
                val existing = saved.firstOrNull { it.name.equals(s.clientName.value, true) }
                InvoiceRepo.saveClient(
                    ctx,
                    ClientRecord(
                        id = existing?.id ?: 0L,
                        name = s.clientName.value,
                        details = s.clientDetails.value,
                        reference = s.poNumber.value
                    )
                )
            }
        },
        onDelete = { id -> scope.launch { InvoiceRepo.deleteClient(ctx, id) } }
    )
}

/**
 * Everything the rendered page draws, as one string.
 *
 * The old refresh list watched item count and the total, which meant renaming
 * a line changed neither and the preview kept showing the old wording. Line
 * text is folded in here, so anything that appears on the page triggers a
 * redraw and nothing else does.
 */
private fun previewSignature(s: InvoiceState): String = listOf(
    s.template.value.name, s.accent.value.label, s.currency.value,
    s.invoiceNumber.value, s.issueDate.value, s.dueDate.value, s.validUntil.value,
    s.docType.value.name,
    s.bizName.value, s.bizDetails.value, s.bizTaxId.value,
    s.clientName.value, s.clientDetails.value, s.poNumber.value,
    s.taxLabel.value, s.taxRate.value, s.discountRate.value, s.shipping.value,
    s.payment.value, s.notes.value,
    s.items.joinToString("|") {
        it.description.value + ";" + it.qty.value + ";" + it.rate.value + ";" + it.taxRate.value
    }
).joinToString("~")

/**
 * Preview and export.
 *
 * Both halves of this ran on the main thread. The page bitmap was built inside
 * remember(), so a full-page render happened during composition on every
 * keystroke that moved the total. And Save PDF rendered the document and wrote
 * it in the click handler, which is the same freeze - and past about five
 * seconds, the same "isn't responding" dialog - that the rest of the app was
 * fixed for.
 *
 * The write path did not need changing: reportSave and sharePdf marshal their
 * own toasts, notification and ad counter to the main thread already, so they
 * are safe to call from IO exactly as they are.
 */
@Composable
private fun PreviewTab(s: InvoiceState, accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sig = previewSignature(s)

    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    // Debounced, so typing a client name does not render the page per letter.
    LaunchedEffect(sig) {
        delay(240)
        val rendered = withContext(Dispatchers.Default) {
            try { renderInvoiceBitmap(s) }
            catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
        }
        if (rendered != null) bmp = rendered
    }

    fun export(share: Boolean) {
        busy = true
        scope.launch {
            val name = s.invoiceNumber.value.ifBlank { "invoice" }
            val bytes = withContext(Dispatchers.Default) {
                try { renderInvoicePdf(s) }
                catch (e: Exception) { null } catch (e: OutOfMemoryError) { null }
            }
            if (bytes != null) {
                withContext(Dispatchers.IO) {
                    if (share) sharePdf(ctx, bytes, name) else savePdfToDownloads(ctx, bytes, name)
                }
            }
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        val page = bmp
        // The last good render stays up while the next one is built, so the
        // page does not blink on every edit.
        if (page == null) {
            ProcessingCard("Drawing your " + s.docType.value.title.lowercase(), accent)
        } else {
            Box(
                Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
                    .border(1.dp, PaperLine, Shape.card).padding(8.dp)
            ) {
                Image(
                    page.asImageBitmap(), null,
                    Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().clip(Shape.field)
                        .background(if (busy) accent.copy(alpha = 0.4f) else accent)
                        .clickable(enabled = !busy && page != null) { export(false) }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (busy) "Working\u2026" else "Save PDF",
                        color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().clip(Shape.field)
                        .background(accent.copy(alpha = if (busy) 0.04f else 0.10f))
                        .clickable(enabled = !busy && page != null) { export(true) }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
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
