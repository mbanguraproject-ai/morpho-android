# Morpho — Files, Transformed

A privacy-first Android file-tools app by Dev_Bangs (cc.devbangs.morpho).
~130 genuinely-working tools across 10 categories. Kotlin + Jetpack Compose,
Material 3, light theme. minSdk 24 / target 36.

Free tools run 100% on-device. Server-backed conversions are Morpho Plus.

---

## Architecture

- **UI:** 100% Jetpack Compose, Material 3, single light theme.
- **Tool registry:** `data/ToolRegistry.kt` — every tool is a `Tool(id, name, desc, category, iconKey, offline, popular)`.
- **Categories:** `data/ToolCategory.kt` — 10 categories: PDF, IMAGE, CONVERTER, VIDEO, AUDIO, TEXT, GENERATOR, DEVELOPER, AI, PRIVACY.
- **Dispatch:** `ui/tool/ToolScreen.kt` → `ToolHost` routes a tool id to its composable via `hasXTool(id)` sets, plus special branches (invoice/receipt/quotation, pdf-signer, resume). Unmatched ids → honest `Placeholder`.
  - IMPORTANT: every `hasXTool` set MUST have a matching `hasXTool(tool.id) -> XTool(...)` line in ToolHost. A set that is imported but not called = tools silently fall through to the placeholder. (This bug hit hasEncoderTool once — fixed.)
- **Tool kits:** `ui/tool/kit/*.kt` — grouped composables (TextDevTools, GeneratorTools, ImageTools, PdfTools, PdfBoxTools, ConverterTools, ExtraTools, MediaTools, EncoderTools, OcrTools, LastTools). Shared helpers in ToolKit.kt. Note: `PickRow` is defined privately per-file (making it internal causes overload clashes).
- **Icons:** `ui/icon/ToolIcons.kt` maps iconKey → Phosphor Bold icon (each needs an explicit import). Missing map entry → falls back to category default.

## Categories (10)

PDF · Image · Convert · Video · Audio · Text · Generate · Developer · AI · **Privacy**

Privacy (green / shield): EXIF Remover (flagship), Image Metadata Viewer,
PDF Metadata Remover, Video Metadata Remover — all on-device. Selling point:
files never leave the device (unlike web tools that secretly upload).

## Tool types

- **On-device (offline=true):** the majority. PDF ops (merge/split/compress/crop/rotate/
  sign/OCR/metadata/image-extract), image tools, converters, text/dev tools, generators
  (incl. invoice/receipt/quotation via shared DocType tool), audio (wav/compress/volume/
  trim/join/record/TTS/STT), video (trim/mute/gif), scan-to-pdf, word-to-pdf, privacy tools.
- **Server (offline=false) = Morpho Plus:** pdf↔word/excel/powerpoint, ppt→pdf, pdf→html,
  svg→png, mp3-converter, video-compressor. All via CloudConvert through a Cloudflare Worker.
- **Coming soon (honest placeholders):** AI tools (need an LLM/image backend), pdf-editor.

## Server conversions (Morpho Plus)

Flow: App → Cloudflare Worker → CloudConvert → converted file → back to app.

- **Worker:** `morpho-convert-worker/src/index.js`, deployed at
  `https://morpho-convert.secretsafe-cc.workers.dev`. Orchestrates a CloudConvert
  job (import/upload → convert → export/url), polls, streams the file back.
  Supports `?from=&to=&name=` plus `&crf=&width=` for video compression.
- **CloudConvert key:** stored as CF secret `CLOUDCONVERT_KEY` (never in code/APK).
  Free tier = 25 conversion-minutes/day.
- **App client:** `ui/tool/kit/CloudConvert.kt` — `cloudConvert(ctx, uri, from, to, outName, extraParams)`
  via HttpURLConnection. Generic `ConvertTool` composable handles all conversions.

## Plus gating (3 layers — ALL required before shipping server tools)

1. **Client-side gate (UX):** ToolHost shows a "Morpho Plus feature" upsell card
   (with Upgrade button → PlusScreen) when `!tool.offline && !AdState.isPlus`.
   Free users see the upsell; Plus users get the tool.
2. **Layer 1 — shared secret:** app sends `X-Morpho-Key`; Worker rejects mismatches (401).
   Secret stored as CF secret `SHARED_SECRET`. (Not sufficient alone — extractable from APK.)
3. **Layer 2 — Play verification:** app sends `X-Morpho-Token` (purchase token) +
   `X-Morpho-Product`. Worker signs a JWT with the service account (`GOOGLE_SA` secret),
   gets a Google OAuth token, calls Play `purchases.subscriptionsv2` — only ACTIVE /
   IN_GRACE_PERIOD subscriptions pass, else 403. This is the real security.

Billing: `billing/BillingManager.kt` (object). Products `morpho_plus_monthly` ($2.99),
`morpho_plus_yearly` ($19.99). Exposes `AdState.isPlus`, retains `activeToken` /
`activeProductId` for the Worker.

Google Cloud setup done: project `morpho-plus-verify`, Play Android Developer API enabled,
service account `morpho-play-verify@morpho-plus-verify.iam.gserviceaccount.com` with
Play Console financial-data access. JSON key stored as Worker secret `GOOGLE_SA`.

## Build & release

- `./build.sh [debug|release|aab]`. Env if needed:
  `export JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.13-tem; export ANDROID_HOME=$HOME/android-sdk`
- Fast compile check: `./gradlew :app:compileDebugKotlin --no-daemon -Dorg.gradle.daemon=false 2>&1 | grep "^e:"`
- Outputs: APK `app/build/outputs/apk/release/app-arm64-v8a-release.apk`;
  AAB `app/build/outputs/bundle/release/app-release.aab`.
- **ProGuard:** minify ON. Keep rules cover PdfBox/Apache/BouncyCastle, ML Kit, ZXing,
  Phosphor, Billing, AdMob, UMP, Haze, coroutines, BillingManager, enum values/valueOf.
- Keystore: `../morpho-release.keystore` (alias `morpho`) — gitignored, IRREPLACEABLE, back it up.
- versionCode / versionName bumped in `app/build.gradle.kts` each release.

## Repos

- App backup: github.com/mbanguraproject-ai/morpho-android (push: `unset GITHUB_TOKEN` then `git push`).
- Worker: `/workspaces/morpho-convert-worker` (deploy: `export CLOUDFLARE_API_TOKEN=...` then `wrangler deploy`).

## OUTSTANDING before shipping server tools

- [ ] Confirm the Plus PASSING path (a real/license-tester active subscription converts end-to-end).
- [ ] Back up the release keystore.
- [ ] Rotate the Cloudflare API token (was exposed in terminal during setup).
- [ ] Register AdMob test device before tapping own ads.
