package cc.devbangs.morpho.ui.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.billing.BillingManager
import androidx.compose.ui.platform.LocalContext
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

private val PlusBlue = Color(0xFF1A46E5)
private val PlusViolet = Color(0xFF6A4BD6)
private val Green = Color(0xFF16A34A)

@Composable
fun PlusScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    var yearly by remember { mutableStateOf(true) }
    val ctx = LocalContext.current
    val activity = remember(ctx) {
        var c: android.content.Context? = ctx
        while (c is android.content.ContextWrapper && c !is android.app.Activity) { c = c.baseContext }
        c as? android.app.Activity
    }
    val livePriceMonthly = BillingManager.monthlyPrice.value ?: "$2.99"
    val livePriceYearly = BillingManager.yearlyPrice.value ?: "$19.99"

    Column(Modifier.fillMaxSize().background(Paper)) {
        // top bar
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp).padding(horizontal = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButtonMorpho("chevron-left", onBack)
            Spacer(Modifier.width(4.dp))
            Text("Morpho Plus", style = MaterialTheme.typography.headlineSmall, color = Ink)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            // hero
            Column(
                Modifier.padding(horizontal = Space.gutter).fillMaxWidth().clip(Shape.card)
                    .background(Brush.linearGradient(listOf(PlusBlue, PlusViolet)))
                    .padding(Space.xl)
            ) {
                Box(
                    Modifier.size(46.dp).clip(Shape.chip).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { MorphoIcon("crown", tint = Color.White, size = 25.dp) }
                Spacer(Modifier.height(14.dp))
                Text("Unlock everything.\nRemove all ads.", color = Color.White,
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                Spacer(Modifier.height(8.dp))
                Text("14 premium cloud tools plus an ad-free experience.",
                    color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }

            // FREE tier (first, per spec)
            Column(
                Modifier.padding(horizontal = Space.gutter).fillMaxWidth().clip(Shape.card)
                    .border(1.5.dp, PaperLine, Shape.card).padding(Space.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Morpho Free", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.clip(Shape.pill).background(PaperSunk).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Current plan", color = InkSoft, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                FreeLine("67 tools, offline, forever")
                FreeLine("PDF, image, audio, video & more")
                Row(Modifier.padding(top = 4.dp)) {
                    Text("•  ", color = InkFaint, fontSize = 13.sp)
                    Text("Includes ads", color = InkFaint, fontSize = 13.sp)
                }
            }

            // PLUS tier
            Column(
                Modifier.padding(horizontal = Space.gutter).fillMaxWidth().clip(Shape.card)
                    .border(2.dp, PlusBlue, Shape.card).padding(Space.lg)
            ) {
                Text("Morpho Plus", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                PlusLine("No ads, anywhere")
                PlusLine("AI writing suite — rewrite, essay, grammar, paragraphs")
                PlusLine("Background remover")
                PlusLine("PDF ↔ Word, PDF editor, signer, annotator")
                PlusLine("Image upscaler · video compressor · SVG→PNG")
            }

            // pricing toggle
            Row(
                Modifier.padding(horizontal = Space.gutter).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                PriceCard("MONTHLY", livePriceMonthly, "/month", null, !yearly, Modifier.weight(1f)) { yearly = false }
                PriceCard("YEARLY", livePriceYearly, "/year", "SAVE 44%", yearly, Modifier.weight(1f)) { yearly = true }
            }

            // CTA
            Box(
                Modifier.padding(horizontal = Space.gutter, vertical = Space.sm).fillMaxWidth()
                    .clip(Shape.card).background(PlusBlue)
                    .clickable { activity?.let { BillingManager.purchase(it, yearly) } }
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (yearly) "Start Plus · $livePriceYearly/year" else "Start Plus · $livePriceMonthly/month",
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text("Cancel anytime  ·  Restore purchase", color = InkFaint, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp), 
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun FreeLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon("check", tint = InkSoft, size = 15.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = InkSoft, fontSize = 13.sp)
    }
}

@Composable
private fun PlusLine(text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        MorphoIcon("check", tint = PlusBlue, size = 15.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Ink, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun PriceCard(label: String, price: String, per: String, badge: String?,
                      selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(Shape.card)
                .background(if (selected) Color(0xFFF7F9FF) else Paper)
                .border(if (selected) 2.dp else 1.5.dp, if (selected) PlusBlue else PaperLine, Shape.card)
                .clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(price, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(per, color = InkFaint, fontSize = 11.sp)
        }
        if (badge != null) Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-8).dp).clip(Shape.pill)
                .background(Green).padding(horizontal = 9.dp, vertical = 2.dp)
        ) { Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
    }
}
