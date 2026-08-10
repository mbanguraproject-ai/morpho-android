# Morpho — Project README & Handoff

**Morpho** is a native Android file-tools app: dozens of on-device tools for PDF, image, audio, video, and text, with premium UI, workflow chaining, AdMob monetization, and Play Billing subscriptions. Built in Jetpack Compose.

- **Package / applicationId:** `cc.devbangs.morpho`
- **minSdk:** 24 · **targetSdk / compileSdk:** 36 · **versionCode:** 1 · **versionName:** 1.0.0
- **Repo path:** `/workspaces/Morpho-File/android-app`
- **Contact email:** secretsafe.cc@gmail.com
- **GitHub account for legal pages:** mbanguraproject-ai

---

## 1. WHAT'S BUILT (complete)

### Tools — 67 working, on-device
Nine categories (PDF, Image, Convert, Video, Audio, Text, Generator, Developer, AI). Every offline tool genuinely works; cloud tools honestly show "server tool · coming soon." Highlights: PDF merge/split/compress/watermark/protect/OCR, image compress/resize/convert, video/audio trim, GIF maker, on-device OCR (ML Kit), QR generator (ZXing), invoice + resume PDF builders.

### Premium UI
- Slide+fade screen transitions across all navigation
- Home cards stagger-in on load
- Haze frosted-glass top bar + floating bottom nav (blur on Android 12+, tint fallback below)
- System nav bar contrast enforced (gesture pill stays visible, no collision with content)
- Animated "wings" mark on home (flaps once on entry)

### Brand identity
- Morpho Wings adaptive launcher icon (cobalt→violet gradient + butterfly wings)
- Animated splash screen (wings unfold, via androidx core-splashscreen)
- 512×512 Play icon + 1024×500 feature graphic (in /outputs)

### Workflow chaining (signature feature)
After a PDF tool produces output, "WHAT'S NEXT" cards suggest connected tools (merge→compress→protect, etc.). Tapping hands the file forward (via `WorkflowBus`, carries bytes directly) — no re-picking. Only genuinely-useful next steps are suggested; endpoints (protect, unlock, to-jpg, rotate) correctly show none. 8 wired PDF paths.

### Monetization — AdMob (real IDs wired)
- Native "You might like" ad card at bottom of Tools list (Morpho-styled, "Ad" labeled)
- Interstitial every 3rd *real* tool completion (only counts actual saves), 90s cap, fires on tool-exit → never interrupts the workflow chain
- UMP consent flow (GDPR/CCPA) — no ads load without consent
- All ads gated: Plus subscribers see zero ads
- **Real AdMob IDs are live in the code** (App ID `ca-app-pub-9121922395304175~3190267514`, native `/8812315198`, interstitial `/6203443667`)

### Monetization — Play Billing v9
- `BillingManager` connects, queries products, launches purchase, acknowledges, restores on launch
- On purchase → `AdState.isPlus = true` → ads stop everywhere
- Plus page shows live localized prices (falls back to $2.99/$19.99 until products exist)
- Product IDs the app queries: **`morpho_plus_monthly`**, **`morpho_plus_yearly`** (must match Play Console exactly)

### Other
- Completion notifications ("file ready") — permission-gated toggle in Settings
- Free/Plus plan pill on home
- Settings wired: Rate→Play, Privacy/Terms→GitHub Pages, About
- Legal pages written (privacy.html, terms.html in /outputs)
- ProGuard keep-rules for ALL reflection libs (PDFBox, ML Kit, ZXing, Phosphor, Haze, splashscreen, **Billing, AdMob, UMP**)

---

## 2. BUILD COMMANDS

Always export env first:
```bash
export JAVA_HOME="/usr/local/sdkman/candidates/java/17.0.13-tem"
export ANDROID_HOME="$HOME/android-sdk"
```

**Debug (for quick compile checks — do NOT download, it's 67MB unsplit):**
```bash
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.daemon=false
```

**Release APK (signed, split, ~25MB — for on-device testing):**
```bash
./gradlew --stop 2>/dev/null; pkill -9 -f java 2>/dev/null; sleep 6; sync
./gradlew :app:assembleRelease --no-daemon -Dorg.gradle.daemon=false
# output: app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

**Release AAB (for Play upload):**
```bash
./gradlew --stop 2>/dev/null; pkill -9 -f java 2>/dev/null; sleep 6; sync
./gradlew :app:bundleRelease --no-daemon -Dorg.gradle.daemon=false
# output: app/build/outputs/bundle/release/app-release.aab
```

**⚠️ Codespace memory:** R8 builds often OOM on the first attempt ("Gradle daemon disappeared"). This is normal — just re-run; the second attempt lands once memory settles. Config uses -Xmx3072m, R8 lite mode, capped workers.

---

## 3. SIGNING — CRITICAL

- Keystore: `morpho-release.keystore` (repo root, **gitignored**)
- Alias: `morpho` · store/key password: `morpho2026`
- Backup: `morpho-release.keystore.BACKUP` at `/workspaces/Morpho-File/`
- **⚠️ THIS FILE IS IRREPLACEABLE. If lost, you can NEVER update the app on Play again. Back it up to Drive + password manager NOW.**

---

## 4. WHAT REMAINS (to actually launch + earn)

### Before submitting to Play
1. **Host legal pages** — put privacy.html + terms.html on GitHub Pages at `mbanguraproject-ai.github.io/morpho/` (must be live before submit; Console checks the URL)
2. **Take screenshots** — 2-8 phone screenshots (home, a tool page, workflow "What's Next" cards, Plus page). Must be real device captures.
3. **Upload AAB** to Play Console, fill listing (use morpho-play-listing.md — has title, descriptions, data-safety answers, content rating)

### Subscriptions (in Play Console → Monetize)
Create two products with EXACT IDs (in progress):
- `morpho_plus_monthly` — base plan "monthly", auto-renewing, $2.99/mo, 7-day grace + 53-day hold
- `morpho_plus_yearly` — base plan "yearly", auto-renewing, $19.99/yr
Then test a real purchase on internal testing track with a license-tester account.

### AdMob (post-launch)
- Link the app in AdMob once it's live on Play
- **Register your test device** (Settings → Test devices in AdMob, or paste device ID into `MorphoApplication.kt`'s empty `testDevices` list) so YOU see test ads and can tap safely. Until then, DO NOT tap ads on your own device (invalid-traffic ban risk).
- Set up a UMP privacy message in AdMob for the consent form to display

### Data safety form
Declare: Device IDs = collected + shared (AdMob, advertising). Everything else = not collected (files processed on-device). **When cloud Plus tools ship later, UPDATE this to disclose file upload.**

### Future features (deferred, post-launch updates)
- Dark mode (deferred — would require refactoring all hardcoded color tokens to theme-aware; big job)
- Extend workflow chain to image family (resize→compress→convert)
- Wire header-footer / bates tools into the chain (currently excluded to stay honest)
- The 14 cloud Plus tools + backend proxy (AI writing, background remover, PDF↔Word, etc.)

---

## 5. KEY FILES MAP

| Area | Path |
|---|---|
| App entry / SDK init | `MainActivity.kt`, `MorphoApplication.kt` |
| Nav + shell | `ui/MorphoApp.kt`, `core/Nav.kt` |
| Home | `ui/home/HomeScreen.kt` |
| Tools dispatch | `ui/tool/ToolScreen.kt` (ToolHost) |
| Tool implementations | `ui/tool/kit/*.kt` (grouped by type) |
| Workflow chaining | `workflow/Workflow.kt` (bus + graph), `ui/tool/kit/NextStepCard.kt` |
| Ads | `ads/AdState.kt` (policy), `ads/InterstitialManager.kt`, `ads/NativeAdCard.kt`, `ads/ConsentManager.kt` |
| Billing | `billing/BillingManager.kt` |
| Notifications | `notify/Notifier.kt`, `notify/Prefs.kt` |
| Plus page | `ui/plus/PlusScreen.kt` |
| Settings | `ui/settings/SettingsScreen.kt` |
| Theme / colors | `ui/theme/Theme.kt`, `ui/theme/Color.kt` |
| ProGuard | `app/proguard-rules.pro` |

---

## 6. IDs & CONSTANTS TO KNOW

- **AdMob App ID** (manifest): `ca-app-pub-9121922395304175~3190267514`
- **AdMob native unit** (AdState.kt): `ca-app-pub-9121922395304175/8812315198`
- **AdMob interstitial unit** (AdState.kt): `ca-app-pub-9121922395304175/6203443667`
- **Subscription product IDs** (BillingManager.kt): `morpho_plus_monthly`, `morpho_plus_yearly`
- **Privacy URL** (SettingsScreen.kt): `https://mbanguraproject-ai.github.io/morpho/privacy.html`
- **Terms URL** (SettingsScreen.kt): `https://mbanguraproject-ai.github.io/morpho/terms.html`

---

*Status: feature-complete, signed AAB built (morpho-release-v1.aab), ready for Play Console submission once legal pages are hosted and screenshots taken.*
