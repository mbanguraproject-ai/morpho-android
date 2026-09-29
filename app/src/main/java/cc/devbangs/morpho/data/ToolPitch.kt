package cc.devbangs.morpho.data

/**
 * What each tool is for, in the user's terms.
 *
 * Every tool screen used to open on the same three numbered dots - "Choose a
 * PDF", a verb, "Save" - derived from the tool's id. It was the same shape on
 * all 132 tools, it told nobody anything they did not already know from having
 * tapped the tool, and it took the most valuable strip of the screen to do it.
 *
 * This replaces it with a written pitch per tool: what it does, which part of
 * it you control, and what it costs you. [badges] are derived from the tool's
 * real flags rather than written by hand, so a badge cannot drift out of step
 * with what the tool actually does.
 */
object ToolPitch {

    /** One entry per tool in [ToolRegistry]; [of] falls back if one is missed. */
    private val copy: Map<String, String> = mapOf(
        "pdf-unlocker" to
            "Remove the password and the printing, copying and editing restrictions from a PDF you own. You get back an ordinary PDF that opens anywhere, with the pages untouched.",
        "pdf-signer" to
            "Sign a PDF by drawing your signature with your finger. Place it on any page, at the size and position you want, and export a signed document that holds up as a normal PDF.",
        "pdf-editor" to
            "Place your own text anywhere on a PDF page. The document's existing text is never rewritten, so nothing shifts or reflows — what you add sits on top, exactly where you put it.",
        "pdf-to-word" to
            "Turn a PDF into a .docx you can actually edit in Word, with the text flowing as paragraphs instead of frozen lines. Layout-heavy pages keep their structure where the source allows it.",
        "pdf-to-excel" to
            "Pull the tables out of a PDF into a real .xlsx, with rows and columns as cells rather than a wall of text. Best on documents whose tables have visible rules.",
        "excel-to-pdf" to
            "Turn a spreadsheet into a clean PDF that looks the same on every device. Useful when you need to send figures that nobody should be able to edit by accident.",
        "pdf-to-powerpoint" to
            "Convert a PDF deck back into editable slides, one slide per page, so you can change a title or a chart without rebuilding the whole presentation.",
        "ppt-to-pdf" to
            "Turn a slide deck into a PDF that keeps your fonts and layout wherever it is opened. The usual fix for a deck that renders wrong on someone else's machine.",
        "pdf-to-html" to
            "Convert a PDF into an HTML page you can put on a site or open in any browser, with the text selectable and searchable instead of locked in a document.",
        "word-to-pdf" to
            "Turn a Word document's text into a PDF, entirely on your phone. Nothing is uploaded, which matters when the document is a contract or a letter you would rather not send to a server.",
        "pdf-compressor" to
            "Shrink a PDF so it fits an upload limit or an email attachment. You choose how hard to compress and see the new size before you save, so you can stop at the point where the document still looks right.",
        "merge-pdf" to
            "Combine several PDFs into one document. Reorder the files before you merge so the finished PDF reads in the order you want. Free, unlimited, and no watermarks.",
        "pdf-splitter" to
            "Break one PDF into separate files — page by page, or at the points you choose. Useful for pulling a single signed page out of a long scan.",
        "pdf-to-jpg" to
            "Turn every page of a PDF into an image you can post, send or drop into a slide. Pick the quality you need before saving.",
        "jpg-to-pdf" to
            "Turn photos into a single PDF, in the order you choose. The usual way to hand in receipts, ID scans or homework as one tidy document.",
        "pdf-page-rotator" to
            "Fix pages that scanned in sideways or upside down. Rotate the whole document or just the pages that are wrong, and save it straight.",
        "image-compressor" to
            "Make an image smaller without making it look bad. You choose the quality level and see the resulting file size before you save, so you can hit an upload limit exactly instead of guessing.",
        "image-target-size" to
            "Name the size you are allowed - 200 KB, 1 MB, whatever the form says - and Morpho finds the highest quality that still fits underneath it. For exam boards, visa portals and job applications that reject anything over a limit.",
        "image-resizer" to
            "Resize an image to exact dimensions. Pick a preset or type your own width and height, choose whether to fit or fill, and see the real output size before you save — no surprise crops.",
        "background-remover" to
            "Cut the subject out of a photo and drop the background, leaving a transparent PNG you can put on any colour. Runs on your device, so the photo is never uploaded.",
        "image-cropper" to
            "Crop to a free shape or to a fixed ratio — square, 4:3, 16:9, or the sizes profile pictures actually want. You see the crop before you commit to it.",
        "jpg-to-png" to
            "Convert JPG to PNG when you need lossless pixels or a format that supports transparency. The original is left untouched.",
        "png-to-jpg" to
            "Convert PNG to JPG to cut file size sharply, with the quality level in your hands so you decide the trade.",
        "webp-to-png" to
            "Convert WebP images into PNG, for the apps and programs that still refuse to open WebP.",
        "svg-to-png" to
            "Turn a vector SVG into a PNG at the exact pixel size you need, so a logo can go somewhere that only takes bitmaps.",
        "heic-to-jpg" to
            "Convert iPhone HEIC photos into JPG so they open everywhere — Windows, older Android, web uploads, print shops.",
        "image-to-pdf" to
            "Turn one or many images into a single PDF document, in the order you choose.",
        "scan-to-pdf" to
            "Use your camera as a document scanner. Edges are found for you and the perspective is flattened, so a photo of a page on your desk comes out looking like a real scan, saved as a PDF.",
        "mp4-to-mp3" to
            "Pull the audio out of a video and save it as an MP3 — a lecture, an interview, a track from a clip you recorded.",
        "csv-to-json" to
            "Convert CSV rows into a JSON array, with the header row becoming the keys. For getting spreadsheet data into an API or an app.",
        "xml-to-json" to
            "Convert XML into the equivalent JSON, so you can work with a feed or a config in a format your code actually wants.",
        "yaml-to-json" to
            "Convert YAML into JSON — the quick way to check what a config file really says, or to feed it to something that only speaks JSON.",
        "docx-to-txt" to
            "Pull the plain text out of a Word file, with the formatting dropped. For when you want the words and nothing else.",
        "subtitle-converter" to
            "Convert subtitles between SRT and VTT, the two formats players and browsers disagree about.",
        "png-to-webp" to
            "Convert PNG to WebP to cut page weight without visible loss. The format most sites now want for images.",
        "jpg-to-webp" to
            "Convert JPG to WebP for smaller files at the same visual quality — usually the single biggest win for a slow page.",
        "json-to-csv" to
            "Flatten a JSON array into CSV rows you can open in Excel, Sheets or Numbers.",
        "video-to-gif" to
            "Turn a clip into an animated GIF that plays anywhere, in a chat or a doc, without needing a video player.",
        "video-compressor" to
            "Shrink a video so it fits a sharing limit or an upload cap, with the quality trade in your hands.",
        "video-trimmer" to
            "Cut a video down to the part that matters. Set the start and the end and save just that section — the rest of the file is left alone.",
        "silence-video" to
            "Strip the audio track off a video, leaving the picture exactly as it was. For when the background noise is the problem, not the footage.",
        "mp3-converter" to
            "Convert any audio file into MP3, the format that plays on everything from a car stereo to a browser.",
        "wav-converter" to
            "Convert audio into uncompressed WAV, for editing, mastering or anything that refuses a compressed file.",
        "audio-compressor" to
            "Shrink an audio file by re-encoding it to AAC. Smaller files, with the quality level yours to choose.",
        "volume-booster" to
            "Amplify a recording that came out too quiet, with clipping protection so the loud parts do not turn to distortion.",
        "text-to-speech" to
            "Have your device read text aloud in its own voice. Useful for proofreading a draft by ear, or listening to something instead of reading it.",
        "audio-trimmer" to
            "Cut an audio file down to the section you want and save just that. Nothing is re-recorded and nothing else is touched.",
        "audio-transcriber" to
            "Turn speech in a recording into text you can read, search and edit — an interview, a lecture, a meeting you recorded.",
        "voice-recorder" to
            "Record audio straight from your microphone and save it as a file you can keep, send or run through the other audio tools here.",
        "speech-to-text" to
            "Speak and watch it become text, on your device. For getting a thought down faster than you can type it.",
        "audio-joiner" to
            "Join several audio files into one continuous track, in the order you set.",
        "screenshot-to-text" to
            "Pull the text out of a screenshot so you can copy it instead of retyping it. The image never leaves your phone.",
        "image-to-text" to
            "Read the text out of any photo — a page, a sign, a whiteboard, a receipt — and get it back as text you can edit and copy.",
        "word-counter" to
            "Count words, characters, sentences and paragraphs as you type, so you can hit a limit exactly instead of hoping.",
        "text-formatter" to
            "Clean up text that arrived a mess — collapse runs of spaces, strip stray line breaks, tidy the indentation — and get something you can actually paste.",
        "remove-duplicate-lines" to
            "Strip repeated lines out of a list, keeping the first of each. For deduping emails, URLs or any list that got pasted twice.",
        "text-sorter" to
            "Sort lines alphabetically, in reverse, or by length. The fast fix for a list that needs to be in order.",
        "text-reverser" to
            "Reverse text by character, by word or by line — whichever level you need.",
        "markdown-editor" to
            "Write Markdown and watch it render live beside you, so you can see exactly what a README or a post will look like.",
        "keyword-density-checker" to
            "See which words a piece of writing actually leans on, and how often. For checking a page reads naturally rather than stuffed.",
        "regex-tester" to
            "Test a regular expression against real text and watch the matches light up as you type, rather than guessing and re-running your code.",
        "xml-formatter" to
            "Pretty-print XML with proper indentation, so a single-line document becomes something you can read and debug.",
        "html-minifier" to
            "Strip the whitespace and comments out of HTML to cut page weight, without changing what it renders.",
        "css-minifier" to
            "Minify CSS down to the smallest file that behaves identically. Less to download, same page.",
        "js-minifier" to
            "Minify JavaScript to cut its size, leaving the behaviour alone.",
        "sql-formatter" to
            "Format and indent a SQL query so a long one-liner becomes something you can read, review and hand to a colleague.",
        "code-beautifier" to
            "Indent and format a block of code that arrived flattened or badly wrapped, so you can read it before you run it.",
        "url-parser" to
            "Break a URL into its parts — scheme, host, path, query parameters, fragment — so you can see exactly what is being sent.",
        "cron-explainer" to
            "Turn a cron expression into plain English, so you know what it will actually do before you schedule it.",
        "markdown-to-html" to
            "Convert Markdown into clean HTML you can paste into a page, a CMS or an email.",
        "html-to-markdown" to
            "Convert HTML back into Markdown, for moving content into a README, a wiki or a notes app.",
        "qr-code-generator" to
            "Make a QR code for a link, some text, a phone number or Wi-Fi details — then save it as an image or share it straight out. No expiry, no tracking, no account, and the code keeps working forever.",
        "invoice-generator" to
            "Build a real invoice: your business and logo, saved clients, line items with tax and discounts, payments recorded against the balance, and a signature. Export a clean PDF with no watermark and no per-document charge.",
        "receipt-generator" to
            "Issue a proper payment receipt — who paid, for what, how much, and against which invoice — and export it as a PDF you can hand over or file.",
        "quotation-generator" to
            "Send a price quote or proforma that looks like it came from a real business, with your items, your terms and a validity date. Turn it into an invoice when it is accepted.",
        "resume-builder" to
            "Build a resume section by section and export a clean PDF that reads well on a screen and survives an applicant tracking system.",
        "password-generator" to
            "Generate strong passwords with the length and character mix you choose. Generated on your device, never sent anywhere, never stored.",
        "username-generator" to
            "Generate unique usernames you can actually use, for when every obvious one is taken.",
        "email-signature-generator" to
            "Build a tidy email signature with your name, role and contact details, ready to paste into your mail app.",
        "gradient-generator" to
            "Build a CSS linear gradient visually and copy the exact rule out, instead of guessing at colour stops.",
        "css-generator" to
            "Generate box-shadow and border-radius CSS by eye, and copy the rule when it looks right.",
        "cover-letter-generator" to
            "Draft a cover letter from a structured template, so you start from a working letter instead of an empty page.",
        "api-key-generator" to
            "Generate random, high-entropy API keys on your device. Nothing is transmitted and nothing is kept.",
        "hashtag-generator" to
            "Turn keywords into a set of usable hashtags, formatted and ready to paste under a post.",
        "palette-generator" to
            "Generate a full range of tints and shades from one base colour, with every code ready to copy.",
        "youtube-thumbnail-downloader" to
            "Grab the thumbnail image from a YouTube video in the highest resolution available.",
        "barcode-generator" to
            "Generate barcodes in the standard retail and logistics formats, then save or share them as images. For labelling stock, assets or anything that needs scanning.",
        "json-formatter" to
            "Format and validate JSON in one step — indented so you can read it, and checked so you find the broken comma instead of hunting for it.",
        "color-picker" to
            "Pick a colour and build a palette around it, with the codes ready to copy in the format you need.",
        "favicon-generator" to
            "Generate the favicon sizes a site actually asks for from a single image, instead of exporting each one by hand.",
        "text-diff-checker" to
            "Compare two pieces of text and see exactly what changed, line by line — what was added, what was removed.",
        "base64-encoder" to
            "Encode text to Base64 or decode it back. Both directions, on device, nothing sent anywhere.",
        "ai-text-rewriter" to
            "Rewrite a passage to be clearer, shorter or in a different tone, keeping what you meant. For a paragraph that says the right thing badly.",
        "grammar-checker" to
            "Find and fix grammar, spelling and punctuation mistakes in a piece of writing, with the corrections shown so you stay in control of them.",
        "essay-writer" to
            "Generate a structured first draft on a topic — an opening, an argument, a conclusion — that you then edit into your own.",
        "paragraph-generator" to
            "Generate a focused paragraph on a topic, for when you need a starting point rather than a blank page.",
        "uuid-generator" to
            "Generate v4 UUIDs, one or many at a time, ready to copy.",
        "jwt-decoder" to
            "Decode a JWT and read its header, payload and expiry. Decoding happens on your device, so a live token is never transmitted.",
        "url-encoder" to
            "Percent-encode a URL or decode one back, so a link with spaces or special characters survives being sent.",
        "hash-generator" to
            "Generate SHA hashes from text — for checking a value matches without storing the value itself.",
        "color-converter" to
            "Convert a colour between HEX, RGB, HSL and HSV, so you can hand the same colour to CSS, a design tool and an app.",
        "case-converter" to
            "Switch text between eleven case formats — sentence case, title case, camelCase, snake_case, kebab-case and the rest — without retyping a word.",
        "slug-generator" to
            "Turn a title into a clean URL slug: lowercase, hyphenated, punctuation stripped, ready to publish.",
        "lorem-ipsum-generator" to
            "Generate placeholder text by words, sentences or paragraphs, so you can fill a design without waiting on real copy.",
        "fake-data-generator" to
            "Generate realistic test data — names, emails, addresses, phone numbers — for seeding a database or filling a demo without using anyone's real details.",
        "character-counter" to
            "Count characters against the real limits of the places you post — so you know before you paste whether it fits.",
        "image-blur" to
            "Blur part of a photo to hide a face, a licence plate, an address or a bank card. You control how strong the blur is, and the pixels underneath are gone for good in the saved copy.",
        "watermark-image" to
            "Protect an image with your own text or your logo. You set the size, the position and how strong the mark is, so it is visible enough to matter without ruining the picture.",
        "image-rotator" to
            "Rotate a photo by any angle, not just ninety degrees, and straighten a picture that was taken at a tilt.",
        "exif-remover" to
            "Strip the GPS location, camera model, serial number and timestamps out of a photo before you post it. The image looks identical; what it quietly said about you is gone.",
        "gif-maker" to
            "Turn a set of images into an animated GIF. You choose the order and the speed, and get a file that plays anywhere without a video player.",
        "meme-generator" to
            "Put top and bottom text on an image in the classic meme style. Type, position, save, share.",
        "image-metadata-viewer" to
            "See everything a photo is carrying — GPS coordinates, camera and lens, the exact time it was taken, editing history. Read it before you share it.",
        "pdf-metadata-remover" to
            "Strip the author, the software, the creation and revision dates and other hidden fields out of a PDF, so the document says only what is printed on it.",
        "video-metadata-remover" to
            "Remove the location tags, device details and hidden timestamps stored inside a video file, leaving the footage untouched.",
        "batch-image-converter" to
            "Convert a whole batch of images at once instead of one at a time. Pick the output format and quality once and it applies to every file in the set.",
        "thumbnail-creator" to
            "Generate thumbnails at several sizes from one image, so you get the set a store listing, a blog or an app icon actually asks for without resizing by hand.",
        "sharpen-image" to
            "Bring back detail in a soft or slightly blurry photo. The strength is yours to set, and you can see the result before saving — sharpening too hard looks worse than not sharpening at all.",
        "pdf-page-numbering" to
            "Add page numbers to a PDF, positioned where you want them and starting from the number you choose. For documents that have to be cited or collated.",
        "pdf-watermark" to
            "Stamp text across every page of a PDF — DRAFT, CONFIDENTIAL, your own name. You control the text, and the mark goes on every page so a single leaked page still carries it.",
        "pdf-page-extractor" to
            "Pull out just the pages you need into a new PDF, and leave the original alone. Give it a range and you get a short document instead of a long one.",
        "pdf-text-extractor" to
            "Get the plain text out of a PDF so you can search it, quote it or paste it somewhere else. Works on PDFs that carry real text rather than scans.",
        "pdf-page-deleter" to
            "Delete the pages you do not want — a blank back page, a cover sheet, an internal note — and save the rest as a clean document.",
        "pdf-metadata-editor" to
            "Edit the title, author and subject stored inside a PDF. These are the fields that show up in a document's properties and in search results, and they are often wrong by default.",
        "pdf-image-extractor" to
            "Pull the embedded images out of a PDF as separate picture files, at the resolution they were stored, instead of screenshotting the page.",
        "pdf-ocr-scanner" to
            "Read the text off a scanned PDF, so a photographed or faxed document becomes text you can copy and search. Runs on your device — the scan never leaves your phone.",
        "html-to-pdf" to
            "Render an HTML page and export it as a PDF, so a receipt, a report or a saved page becomes a document you can file or send.",
        "pdf-header-footer" to
            "Add a repeating header and footer to every page — a case name, a company, a date. You write the text and it goes on the whole document.",
        "pdf-crop" to
            "Trim the margins off PDF pages. Useful for scans with a wide border, or a document you want to read comfortably on a phone screen.",
        "pdf-bates-numbering" to
            "Apply Bates numbering across a PDF with your own prefix and starting number — the sequential stamping used in legal discovery and document production.",
        "pdf-annotator" to
            "Mark up a PDF with a highlighter and a pen, the way you would on paper. Your marks are saved into the document, so whoever opens it next sees them.",
        "pdf-password-protector" to
            "Lock a PDF with a password, so it cannot be opened without one. Done on your device, which means the password is never sent anywhere.",
        "pdf-reorder-pages" to
            "Drag pages into the order you want and save the result. The fix for a scan that came out back to front, or a report whose sections belong the other way round.",
    )

    /** The pitch, falling back to the registry one-liner if an id is unknown. */
    fun of(t: Tool): String = copy[t.id] ?: t.short

    /** A short claim shown under the pitch. [icon] is a MorphoIcon glyph key. */
    data class Badge(val icon: String, val label: String)

    /**
     * Derived, never authored. "Private" is claimed only where the file
     * genuinely never leaves the device, and the cost badge reads straight off
     * [Tool.plus], so neither can promise something the tool does not do.
     */
    fun badges(t: Tool): List<Badge> = listOf(
        if (t.plus) Badge("crown", "Plus") else Badge("check", "Free"),
        Badge("check", "No signup"),
        if (t.offline) Badge("shield", "Private") else Badge("check", "No watermarks")
    )

    /**
     * The paid message, said plainly and before the tool is opened rather than
     * after the user has picked a file. Null for every free tool.
     */
    fun plusNote(t: Tool, isPlus: Boolean): String? = when {
        !t.plus -> null
        isPlus -> "Included in your Morpho Plus plan."
        t.offline ->
            "${t.name} runs entirely on your device - Morpho Plus is what " +
                "unlocks it. One subscription covers every Plus tool and " +
                "removes ads across the app."
        else ->
            "${t.name} runs on Morpho's conversion engine rather than on your " +
                "phone, so it needs a connection. Morpho Plus unlocks it and " +
                "every other server tool, and removes ads across the app."
    }
}
