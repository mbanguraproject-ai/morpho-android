#!/usr/bin/env python3
"""In-app Terms, new privacy URL, version bump. Anchored on content, so it
applies whether or not the scanner/passport patch is in the tree."""
import io, os, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
SRC = os.path.join(ROOT, "app/src/main/java/cc/devbangs/morpho")
ADS = ["ads/AdState.kt", "ads/BannerAd.kt", "ads/ConsentManager.kt",
       "ads/InterstitialManager.kt", "ads/NativeAdCard.kt",
       "ui/tool/kit/PdfIo.kt", "review/ReviewPrompt.kt"]
_p = {}

def sub(rel, old, new, count=1):
    p = os.path.join(SRC, rel)
    if p not in _p:
        _p[p] = io.open(p, encoding="utf-8").read()
    # Check the result first: an insert-after anchor still matches once the
    # insert has happened, so counting the anchor alone would duplicate it.
    if new in _p[p]:
        print("  already applied: %s" % rel)
        return
    n = _p[p].count(old)
    assert n == count, "ANCHOR %s: expected %d got %d for %r" % (rel, count, n, old[:70])
    _p[p] = _p[p].replace(old, new)

# ---- 0. the screen itself (new file) --------------------------------------
TERMS_KT = r"""package cc.devbangs.morpho.ui.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.BuildConfig
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.theme.*

/**
 * Terms of Use, carried in the app rather than linked out.
 *
 * The old row opened a GitHub Pages URL that was never published, so the one
 * screen a reviewer or a user is most likely to open led to a 404. Terms that
 * live in the binary cannot rot, work with no connection - which is how most
 * of this app is used - and ship and version with the build they describe.
 */

private const val EFFECTIVE = "3 October 2026"
private const val PUBLISHER = "Mohameds Engineering and Build Studio (MEBS)"
private const val CONTACT = "support@mebs.app"

private class Clause(val heading: String, val body: List<String>)

private val TERMS: List<Clause> = listOf(
    Clause(
        "1. These terms",
        listOf(
            "Morpho is a file and document toolkit for Android, published by $PUBLISHER " +
                "(\"we\", \"us\"). By installing or using Morpho you agree to these terms. " +
                "If you do not agree, do not use the app.",
            "We may update these terms when the app changes. The effective date above " +
                "tells you which version you are reading, and an updated version ships " +
                "with the app update that it covers. Continuing to use Morpho after an " +
                "update means you accept the revised terms."
        )
    ),
    Clause(
        "2. Your licence to use Morpho",
        listOf(
            "We grant you a personal, non-exclusive, non-transferable, revocable licence " +
                "to install and use Morpho on devices you own or control, for your own " +
                "purposes, personal or commercial.",
            "You may not copy, sell, rent, sublicense or redistribute the app; reverse " +
                "engineer, decompile or disassemble it except where that right cannot " +
                "lawfully be excluded; remove or obscure any notice in it; or use it to " +
                "build a competing product."
        )
    ),
    Clause(
        "3. Who may use it",
        listOf(
            "You must be old enough to form a binding contract where you live, and at " +
                "least 13. If you are under the age of majority in your country, you may " +
                "use Morpho only with the involvement of a parent or guardian, who accepts " +
                "these terms on your behalf.",
            "You are responsible for complying with the laws that apply to you when you " +
                "use the app."
        )
    ),
    Clause(
        "4. Your files stay yours",
        listOf(
            "You keep every right you already had in the files you open, create or save " +
                "with Morpho. We claim no ownership of them and no licence to use them.",
            "Most tools in Morpho run entirely on your device. Those files are never sent " +
                "anywhere by us. A small number of tools are marked \"Needs internet\" on " +
                "their own screen, and those send the file you choose to our conversion " +
                "service so it can be processed and returned. We use those files only to " +
                "perform the conversion you asked for.",
            "You are responsible for having the right to use the files you process, and " +
                "for the content of anything you create with the app - including invoices, " +
                "receipts, quotations, resumes and anything you stamp, sign or redact.",
            "Morpho writes its output to your device's storage. Keep your own backups: " +
                "uninstalling the app, clearing its data, or a device fault can remove " +
                "files and saved records, and we cannot recover them for you."
        )
    ),
    Clause(
        "5. How you may not use it",
        listOf(
            "Do not use Morpho to break the law, infringe anyone's rights, or process " +
                "material you have no right to process.",
            "Do not use it to create or alter documents in order to deceive - including " +
                "forging identity documents, falsifying invoices, receipts or records, or " +
                "misrepresenting a document as issued or signed by someone who did not " +
                "issue or sign it.",
            "Do not use it to remove security, watermarks, passwords or restrictions from " +
                "material that is not yours.",
            "Do not interfere with the app's advertising or subscription mechanisms, or " +
                "attempt to access paid features without paying for them."
        )
    ),
    Clause(
        "6. Morpho Plus",
        listOf(
            "Morpho Plus is an optional subscription that unlocks the tools marked Plus " +
                "and removes advertising. Prices are shown in the app before you buy, in " +
                "your local currency where Google Play supports it.",
            "Subscriptions are sold and billed by Google Play, not by us. They renew " +
                "automatically for the same period until you cancel, and the renewal is " +
                "charged to your Google Play account within 24 hours of the period ending.",
            "Cancel at any time in your Google Play subscription settings. Cancelling " +
                "stops the next renewal; you keep Plus until the end of the period you " +
                "have already paid for.",
            "Refunds are handled by Google under Google Play's refund policy. We cannot " +
                "issue refunds for a Google Play purchase ourselves, though we will help " +
                "where we can.",
            "We may change what Plus includes or what it costs. A price change takes " +
                "effect on your next renewal, and Google will notify you and ask you to " +
                "accept it where the law requires."
        )
    ),
    Clause(
        "7. Advertising",
        listOf(
            "Morpho is free to use and shows advertising to users who are not subscribed " +
                "to Plus. Ads are served by Google AdMob and may be personalised depending " +
                "on the choice you were given and the settings on your device.",
            "If you are in the European Economic Area or the United Kingdom, you were " +
                "shown a consent form the first time you opened the app. You can reopen it " +
                "at any time from Settings, under \"Manage privacy choices\", and change " +
                "your answer in either direction.",
            "We do not control the content of individual ads. Report anything that looks " +
                "wrong to us and we will pass it on."
        )
    ),
    Clause(
        "8. Services we build on",
        listOf(
            "Morpho uses Google Play services for advertising, for in-app purchases, for " +
                "Play ratings, and for the on-device machine-learning features behind " +
                "document scanning, background removal and text recognition. Your use of " +
                "those components is also subject to Google's own terms.",
            "Some on-device models download through Google Play services the first time " +
                "you use the feature that needs them. That download needs a connection; " +
                "everything after it runs offline."
        )
    ),
    Clause(
        "9. Our intellectual property",
        listOf(
            "Morpho, its name, logo, interface, icons and code are owned by us or our " +
                "licensors and are protected by copyright and other laws. Nothing in these " +
                "terms transfers any of that to you beyond the licence in clause 2."
        )
    ),
    Clause(
        "10. No warranty",
        listOf(
            "Morpho is provided \"as is\" and \"as available\". To the fullest extent the " +
                "law allows, we disclaim all warranties, express or implied, including " +
                "merchantability, fitness for a particular purpose, accuracy and " +
                "non-infringement.",
            "We do not warrant that the app will be uninterrupted or error-free, that " +
                "every conversion will be perfect, or that output will be accepted by any " +
                "third party that you submit it to.",
            "Morpho is a tool, not professional advice. Documents it helps you produce - " +
                "invoices, receipts, quotations, signed or stamped PDFs, Bates-numbered " +
                "sets, redactions - are your responsibility to check. Take legal, tax or " +
                "financial advice where it matters. In particular, satisfy yourself that a " +
                "redaction or a metadata removal has removed what you needed removed " +
                "before you share the file.",
            "Nothing here excludes any right you have as a consumer that cannot lawfully " +
                "be excluded."
        )
    ),
    Clause(
        "11. Limits on our liability",
        listOf(
            "To the fullest extent the law allows, we are not liable for indirect, " +
                "incidental, special or consequential loss, or for lost profits, lost " +
                "business, lost data or loss of goodwill, arising from your use of Morpho.",
            "Where liability cannot be excluded, our total liability to you for all claims " +
                "is limited to the greater of the amount you paid us for Morpho in the " +
                "twelve months before the claim, or ten United States dollars.",
            "These limits do not apply to liability for death or personal injury caused by " +
                "negligence, for fraud, or to any other liability that cannot lawfully be " +
                "limited."
        )
    ),
    Clause(
        "12. Ending the licence",
        listOf(
            "You can end this licence at any time by uninstalling Morpho.",
            "We may suspend or end your licence if you breach these terms, or if we stop " +
                "offering the app. If we end it without your being at fault while you hold " +
                "an active Plus subscription, you may be entitled to a pro-rata refund " +
                "through Google Play.",
            "Clauses 4, 9, 10, 11 and 13 survive the end of this licence."
        )
    ),
    Clause(
        "13. Governing law",
        listOf(
            "These terms are governed by the laws of Sierra Leone, and the courts of " +
                "Sierra Leone have non-exclusive jurisdiction over any dispute. If you are " +
                "a consumer elsewhere, you keep the benefit of any mandatory protections " +
                "of the country you live in, and may bring proceedings there."
        )
    ),
    Clause(
        "14. Contact",
        listOf(
            "Questions, complaints or reports about these terms or the app: $CONTACT.",
            "How Morpho handles data is covered separately in the Privacy Policy, linked " +
                "from Settings."
        )
    )
)

@Composable
fun TermsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(0f to Color(0xFFF0F3FF), 1f to Paper)
            )
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack, contentDescription = "Back")
                Spacer(Modifier.width(4.dp))
                Text("Terms of Use", style = MaterialTheme.typography.headlineSmall, color = Ink)
            }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(
                    start = Space.gutter, end = Space.gutter, top = Space.md,
                    bottom = contentPadding.calculateBottomPadding() + Space.xxl
                )
        ) {
            Column(
                Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk).padding(Space.lg)
            ) {
                Text("Effective $EFFECTIVE", color = Ink, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
                Text("Applies to Morpho v${BuildConfig.VERSION_NAME} and later",
                    color = InkSoft, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(Space.lg))

            TERMS.forEach { clause ->
                Text(
                    clause.heading,
                    color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Space.lg, bottom = Space.sm)
                )
                clause.body.forEach { para ->
                    Text(
                        para,
                        color = InkSoft, fontSize = 14.sp, lineHeight = 21.sp,
                        modifier = Modifier.padding(bottom = Space.sm)
                    )
                }
            }

            Spacer(Modifier.height(Space.xl))
            Text(
                "© 2026 $PUBLISHER",
                color = InkFaint, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
"""

_terms_path = os.path.join(SRC, "ui/settings/TermsScreen.kt")
_already_patched = os.path.exists(_terms_path)
os.makedirs(os.path.dirname(_terms_path), exist_ok=True)
io.open(_terms_path, "w", encoding="utf-8").write(TERMS_KT)
print("wrote ui/settings/TermsScreen.kt (%d chars)" % len(TERMS_KT))

# ---- 1. a route for the screen --------------------------------------------
sub("core/Nav.kt", '    data object Stats : Dest("stats")',
    '    data object Stats : Dest("stats")\n    data object Terms : Dest("terms")')

# ---- 2. links: new privacy URL, no dead terms URL --------------------------
sub("ui/settings/SettingsScreen.kt",
    '// Morpho links — swap GitHub Pages URLs before launch\n'
    'private const val PRIVACY_URL = "https://mbanguraproject-ai.github.io/privacy/"\n'
    'private const val TERMS_URL = "https://mbanguraproject-ai.github.io/terms/"',
    '// Terms are carried in the app - see TermsScreen - because the published\n'
    '// URL they used to point at was never put up and returned a 404.\n'
    'private const val PRIVACY_URL = "https://mebs.app/privacy/morpho"')

sub("ui/settings/SettingsScreen.kt",
    '                SettingRow("file-text", "Terms of Use", null) { openUrl(ctx, TERMS_URL) }',
    '                SettingRow("file-text", "Terms of Use", null) { onOpenTerms() }')

sub("ui/settings/SettingsScreen.kt",
    'fun SettingsScreen(\n'
    '    onBack: () -> Unit,\n'
    '    onOpenPlus: () -> Unit,\n'
    '    onOpenStats: () -> Unit,\n'
    '    contentPadding: PaddingValues\n'
    ') {',
    'fun SettingsScreen(\n'
    '    onBack: () -> Unit,\n'
    '    onOpenPlus: () -> Unit,\n'
    '    onOpenStats: () -> Unit,\n'
    '    onOpenTerms: () -> Unit,\n'
    '    contentPadding: PaddingValues\n'
    ') {')

# ---- 3. nav host -----------------------------------------------------------
sub("ui/MorphoApp.kt",
    '                SettingsScreen(\n'
    '                    onBack = { nav.popBackStack() },\n'
    '                    onOpenPlus = { nav.navigate(Dest.Plus.route) },\n'
    '                    onOpenStats = { nav.navigate(Dest.Stats.route) },\n'
    '                    contentPadding = bottomBarPadding(false)\n'
    '                )\n'
    '            }',
    '                SettingsScreen(\n'
    '                    onBack = { nav.popBackStack() },\n'
    '                    onOpenPlus = { nav.navigate(Dest.Plus.route) },\n'
    '                    onOpenStats = { nav.navigate(Dest.Stats.route) },\n'
    '                    onOpenTerms = { nav.navigate(Dest.Terms.route) },\n'
    '                    contentPadding = bottomBarPadding(false)\n'
    '                )\n'
    '            }\n'
    '            composable(Dest.Terms.route) {\n'
    '                TermsScreen(\n'
    '                    onBack = { nav.popBackStack() },\n'
    '                    contentPadding = bottomBarPadding(false)\n'
    '                )\n'
    '            }')

sub("ui/MorphoApp.kt",
    'import cc.devbangs.morpho.ui.settings.SettingsScreen\n',
    'import cc.devbangs.morpho.ui.settings.SettingsScreen\n'
    'import cc.devbangs.morpho.ui.settings.TermsScreen\n')

for rel in ADS:
    assert os.path.join(SRC, rel) not in _p, "refusing to touch " + rel
for p, s in _p.items():
    for a in ("@Composable", "@androidx.compose.runtime.Composable"):
        assert (a + "\n" + a) not in s, "orphan annotation in " + p
    io.open(p, "w", encoding="utf-8").write(s)

# ---- 4. version bump, read-then-increment so the base does not matter -------
g = os.path.join(ROOT, "app/build.gradle.kts")
t = io.open(g, encoding="utf-8").read()
code = int(re.search(r"versionCode = (\d+)", t).group(1))
name = re.search(r'versionName = "([0-9.]+)"', t).group(1)
# Bump only once: if the Terms screen was already in place before this run,
# the version that goes with it has already been set.
if _already_patched:
    print("version left at %d (%s) - nothing new to bump for" % (code, name))
    raise SystemExit(0)
major, minor, patch = (name.split(".") + ["0", "0"])[:3]
new_name = "%s.%s.%d" % (major, minor, int(patch) + 1)
t = re.sub(r"versionCode = \d+", "versionCode = %d" % (code + 1), t, count=1)
t = re.sub(r'versionName = "[0-9.]+"', 'versionName = "%s"' % new_name, t, count=1)
assert 'storePassword' in t and 'keyPassword' in t, "signing config disturbed"
io.open(g, "w", encoding="utf-8").write(t)

print("patched %d source file(s)" % len(_p))
print("version %d (%s) -> %d (%s)" % (code, name, code + 1, new_name))
