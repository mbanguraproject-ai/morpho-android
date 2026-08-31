package cc.devbangs.morpho.ui.settings

import androidx.compose.foundation.background
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import cc.devbangs.morpho.BuildConfig
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.notify.Prefs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*


// Morpho links — swap GitHub Pages URLs before launch
private const val PRIVACY_URL = "https://mbanguraproject-ai.github.io/privacy/"
private const val TERMS_URL = "https://mbanguraproject-ai.github.io/terms/"
private const val PACKAGE = "cc.devbangs.morpho"

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
private fun openPlayRating(ctx: android.content.Context) {
    // Try the Play Store app first, fall back to browser
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE"))
    if (market.resolveActivity(ctx.packageManager) != null) {
        runCatching { ctx.startActivity(market) }.onFailure { openUrl(ctx, web.dataString ?: "") }
    } else runCatching { ctx.startActivity(web) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPlus: () -> Unit,
    contentPadding: PaddingValues
) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(Paper)) {
        // top bar
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to Color(0xFFF0F3FF), 1f to Paper))
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
                Spacer(Modifier.width(4.dp))
                Text("Settings", style = MaterialTheme.typography.headlineSmall, color = Ink)
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = Space.gutter, end = Space.gutter, top = Space.md,
                    bottom = contentPadding.calculateBottomPadding() + Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.lg)
        ) {
            PlusBanner(onOpenPlus)

            SettingsGroup("GENERAL") {
                NotificationsToggle(ctx)
            }
            SettingsGroup("ABOUT") {
                SettingRow("info", "About Morpho", "v${BuildConfig.VERSION_NAME}") { openUrl(ctx, "https://play.google.com/store/apps/details?id=$PACKAGE") }
                SettingRow("shield", "Privacy Policy", null) { openUrl(ctx, PRIVACY_URL) }
                SettingRow("file-text", "Terms of Use", null) { openUrl(ctx, TERMS_URL) }
                SettingRow("star", "Rate Morpho", null) { openPlayRating(ctx) }
            }
            Text("Morpho v${BuildConfig.VERSION_NAME}  ·  ${ToolRegistry.all.size} tools",
                color = InkFaint, fontSize = 12.sp,
                modifier = Modifier.padding(top = Space.md).align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun PlusBanner(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Shape.card)
            .background(Brush.linearGradient(listOf(Color(0xFF1A46E5), Color(0xFF6A4BD6))))
            .clickable(onClick = onClick).padding(Space.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(Shape.chip).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center) {
                MorphoIcon("crown", tint = Color.White, size = 19.dp)
            }
            Spacer(Modifier.width(11.dp))
            Text("Morpho Plus", color = Color.White, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Remove ads · unlock AI, background removal, Word conversion & more.",
            color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.clip(Shape.pill).background(Color.White).padding(horizontal = 18.dp, vertical = 9.dp)) {
            Text("Upgrade", color = Color(0xFF1A46E5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Column(
            Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk),
            content = content
        )
    }
}

@Composable
private fun NotificationsToggle(ctx: android.content.Context) {
    var enabled by remember { mutableStateOf(Prefs.notificationsEnabled(ctx)) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Only turn on if permission granted
        enabled = granted
        Prefs.setNotifications(ctx, granted)
    }
    SettingToggle("bell", "Notifications", "Get notified when a file is ready", enabled) { wantOn ->
        if (wantOn) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && !cc.devbangs.morpho.notify.Notifier.hasPermission(ctx)) {
                permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enabled = true; Prefs.setNotifications(ctx, true)
            }
        } else {
            enabled = false; Prefs.setNotifications(ctx, false)
        }
    }
}

@Composable
private fun SettingToggle(icon: String, label: String, sub: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon(icon, tint = InkSoft, size = 20.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 15.sp)
            Text(sub, color = InkFaint, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Paper, checkedTrackColor = Cobalt,
                uncheckedThumbColor = Paper, uncheckedTrackColor = PaperLine
            )
        )
    }
}

@Composable
private fun SettingRow(icon: String, label: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphoIcon(icon, tint = InkSoft, size = 20.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) { Text(value, color = InkFaint, fontSize = 14.sp); Spacer(Modifier.width(8.dp)) }
        MorphoIcon("chevron-right", tint = InkFaint, size = 16.dp)
    }
}
