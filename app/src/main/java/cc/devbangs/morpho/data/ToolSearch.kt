package cc.devbangs.morpho.data

/**
 * Blueprint section 10 - search understands names, formats, tasks and plain
 * language, not just substrings of a tool name.
 *
 * Tool ids are already semantic ("jpg-to-pdf", "pdf-compressor"), so the
 * searchable vocabulary is derived from id, name, description, category and
 * any explicit keywords rather than hand-authored per tool.
 *
 * Query words are expanded through a synonym table so "make pdf smaller"
 * reaches the compressor and "turn a photo into a PDF" reaches image-to-pdf.
 */
object ToolSearch {

    /** Section 10: group results instead of dumping identical-looking cards. */
    enum class Group(val label: String) {
        CONVERT("Convert"),
        OPTIMIZE("Optimize"),
        EDIT("Edit"),
        ORGANIZE("Organize"),
        CREATE("Create"),
        OTHER("More")
    }

    private val STOP = setOf(
        "i", "a", "an", "the", "to", "into", "from", "my", "me", "need", "want",
        "how", "do", "can", "this", "that", "is", "it", "of", "for", "and", "or",
        "please", "some", "make", "turn", "get", "put", "with", "on", "in", "as",
        "be", "have", "would", "like", "am", "im", "help", "one", "file",
        "files", "app", "tool", "tools", "too", "very", "really", "just",
        "these", "those", "them", "all", "any", "there", "here"
    )

    /** Word the user is likely to type -> words that appear in tool metadata. */
    private val SYNONYMS: Map<String, List<String>> = mapOf(
        // People search for "pdf editor" meaning several different things.
        // Offer what Morpho actually has rather than nothing.
        "editor" to listOf("pdf-editor", "pdf-annotator", "pdf-signer"),
        "annotate" to listOf("pdf-annotator", "pdf-editor"),
        "highlight" to listOf("pdf-annotator"),
        "markup" to listOf("pdf-annotator", "pdf-editor"),
        "transcribe" to listOf("audio-transcriber"),
        "transcription" to listOf("audio-transcriber"),
        "speech" to listOf("audio-transcriber"),
        "dictation" to listOf("audio-transcriber"),
        "subtitles" to listOf("audio-transcriber", "subtitle-converter"),
        "smaller" to listOf("compress"), "shrink" to listOf("compress"),
        "reduce" to listOf("compress"), "compressed" to listOf("compress"),
        "size" to listOf("compress", "resize"), "lighter" to listOf("compress"),
        "big" to listOf("compress"), "large" to listOf("compress"),
        "huge" to listOf("compress"), "heavy" to listOf("compress"),
        "massive" to listOf("compress"), "fat" to listOf("compress"),
        "photo" to listOf("image"), "photos" to listOf("image"),
        "picture" to listOf("image"), "pictures" to listOf("image"),
        "pic" to listOf("image"), "pics" to listOf("image"), "img" to listOf("image"),
        "doc" to listOf("word", "document"), "docs" to listOf("word", "document"),
        "docx" to listOf("word"), "document" to listOf("word", "pdf"),
        "slides" to listOf("powerpoint", "ppt"), "slide" to listOf("powerpoint", "ppt"),
        "presentation" to listOf("powerpoint", "ppt"),
        "spreadsheet" to listOf("excel"), "sheet" to listOf("excel"),
        "xlsx" to listOf("excel"), "xls" to listOf("excel"),
        "combine" to listOf("merge"), "join" to listOf("merge"),
        "together" to listOf("merge"), "single" to listOf("merge"),
        "separate" to listOf("split"), "divide" to listOf("split"),
        "cut" to listOf("split", "trim"),
        "lock" to listOf("password", "protect"), "unlock" to listOf("password"),
        "encrypt" to listOf("password", "protect"),
        "sound" to listOf("audio"), "song" to listOf("audio"), "music" to listOf("audio"),
        "movie" to listOf("video"), "clip" to listOf("video"), "film" to listOf("video"),
        "scale" to listOf("resize"), "dimensions" to listOf("resize"),
        "bigger" to listOf("resize"), "wider" to listOf("resize"),
        "bg" to listOf("background"), "transparent" to listOf("background"),
        "signature" to listOf("sign"), "autograph" to listOf("sign"),
        "flip" to listOf("rotate"), "sideways" to listOf("rotate"),
        "words" to listOf("text"), "wording" to listOf("text"),
        "read" to listOf("ocr", "text"), "scanned" to listOf("ocr", "scan"),
        "copy" to listOf("extract"), "pull" to listOf("extract"),
        "password" to listOf("password"), "secure" to listOf("password", "protect"),
        "metadata" to listOf("metadata", "exif"), "hidden" to listOf("metadata", "exif"),
        "jpeg" to listOf("jpg"), "markdown" to listOf("md"),
        "watermark" to listOf("watermark"), "stamp" to listOf("watermark")
    )

    /** Everything this tool can be found by. */
    private val vocabulary: Map<String, Set<String>> =
        ToolRegistry.all.associate { t ->
            t.id to buildSet {
                addAll(t.id.split("-"))
                addAll(words(t.name))
                addAll(words(t.short))
                addAll(words(t.category.label))
                addAll(t.keywords.map { it.lowercase() })
            }.filter { it.isNotBlank() }.toSet()
        }

    private fun words(s: String): List<String> =
        s.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

    private fun expand(token: String): List<String> =
        listOf(token) + SYNONYMS[token].orEmpty()

    fun search(q: String): List<Tool> {
        val raw = words(q)
        if (raw.isEmpty()) return emptyList()
        val full = q.trim().lowercase()

        val meaningful = raw.filterNot { it in STOP }.ifEmpty { raw }
        val terms = meaningful.map { expand(it) }

        val scored = ToolRegistry.all.mapNotNull { t ->
            val vocab = vocabulary[t.id].orEmpty()
            val name = t.name.lowercase()

            // Every meaningful term must be reachable, or the tool is not a match.
            val matchedAll = terms.all { alts ->
                alts.any { alt -> vocab.any { v -> v == alt || v.startsWith(alt) } }
            }
            if (!matchedAll) return@mapNotNull null

            var score = 0
            if (name == full) score += 1000
            if (name.startsWith(full)) score += 400
            if (name.contains(full)) score += 200

            terms.forEach { alts ->
                alts.forEach { alt ->
                    if (t.id.split("-").any { it == alt }) score += 14
                    if (words(t.name).any { it == alt }) score += 12
                    if (words(t.name).any { it.startsWith(alt) }) score += 4
                    if (words(t.short).any { it == alt }) score += 6
                    if (t.keywords.any { it.lowercase() == alt }) score += 8
                    if (words(t.category.label).any { it == alt }) score += 3
                }
            }
            if (t.popular) score += 3
            if (t.offline) score += 1
            t to score
        }

        if (scored.isNotEmpty())
            return scored.sortedByDescending { it.second }.map { it.first }

        // Nothing satisfied every term - fall back to anything that matches one.
        return ToolRegistry.all.filter { t ->
            val vocab = vocabulary[t.id].orEmpty()
            terms.any { alts -> alts.any { alt -> vocab.any { v -> v.startsWith(alt) } } }
        }.sortedByDescending { if (it.popular) 1 else 0 }
    }

    // ---- file-first matching -------------------------------------------------

    private val IMAGE_FORMATS =
        setOf("image", "jpg", "jpeg", "png", "webp", "heic", "bmp", "gif", "svg", "ico")
    private val VIDEO_FORMATS = setOf("video", "mp4", "mov", "mkv", "avi", "webm")
    private val AUDIO_FORMATS = setOf("audio", "mp3", "wav", "m4a", "aac", "ogg", "flac")
    private val TEXT_FORMATS = setOf("text", "txt", "md", "markdown", "csv", "json", "xml", "html")

    /** Collapse a format token to the family it belongs to, so png reaches a jpg tool. */
    private fun family(token: String): String = when (token) {
        in IMAGE_FORMATS -> "image"
        in VIDEO_FORMATS -> "video"
        in AUDIO_FORMATS -> "audio"
        in TEXT_FORMATS -> "text"
        "doc", "docx", "word" -> "word"
        "xls", "xlsx", "excel" -> "excel"
        "ppt", "pptx", "powerpoint" -> "powerpoint"
        else -> token
    }

    /** Input is captured or typed here, so an existing file is not the entry point. */
    private val NO_FILE_INPUT = setOf("voice-recorder", "scan-to-pdf")

    /**
     * What a tool takes in.
     *
     * An id of the form "x-to-y" states its own input, which is more reliable
     * than guessing from the category - word-to-pdf sits in the PDF category
     * but does not accept a PDF.
     */
    private fun inputFamilies(t: Tool): Set<String> {
        if (t.id in NO_FILE_INPUT) return emptySet()

        val marker = t.id.indexOf("-to-")
        if (marker > 0) return setOf(family(t.id.substring(0, marker)))

        // The id names its subject more precisely than the category does:
        // every Privacy tool sits in one category but each takes a different
        // kind of file.
        val prefix = t.id.substringBefore('-')
        when (prefix) {
            "pdf" -> return setOf("pdf")
            "image", "exif", "jpg", "png" -> return setOf("image")
            "video" -> return setOf("video")
            "audio" -> return setOf("audio")
            "text" -> return setOf("text")
        }
        return when (t.category) {
            ToolCategory.PDF -> setOf("pdf")
            ToolCategory.IMAGE -> setOf("image")
            ToolCategory.VIDEO -> setOf("video")
            ToolCategory.AUDIO -> setOf("audio")
            ToolCategory.TEXT -> setOf("text")
            // Generators and developer tools take typed input, not a file.
            else -> emptySet()
        }
    }

    /** Format families implied by a picked file's mime type and name. */
    private fun fileFamilies(mime: String, fileName: String): Set<String> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val m = mime.lowercase()
        return when {
            m == "application/pdf" || ext == "pdf" -> setOf("pdf")
            m.startsWith("image/") -> setOf("image")
            m.startsWith("video/") -> setOf("video")
            m.startsWith("audio/") -> setOf("audio")
            m.startsWith("text/") -> setOf("text")
            "wordprocessing" in m || "msword" in m -> setOf("word")
            "spreadsheet" in m || "ms-excel" in m -> setOf("excel")
            "presentation" in m || "ms-powerpoint" in m -> setOf("powerpoint")
            ext.isNotBlank() -> setOf(family(ext))
            else -> emptySet()
        }
    }

    /**
     * Tools that pick up a file handed over by the master sheet or the viewer,
     * rather than asking for it again.
     *
     * Most tools still ask, which is fine - but the ones that do not should come
     * first, because "pick a file, tap a tool, start work" is the whole point of
     * arriving from a file. Kept as a list rather than inferred because it
     * depends on each tool having wired the hand-off, which is not visible from
     * the registry.
     */
    private val ACCEPTS_HANDOFF = setOf(
        // single-PDF family
        "pdf-to-jpg", "pdf-page-rotator", "pdf-watermark",
        "pdf-splitter", "pdf-page-extractor", "pdf-page-numbering",
        // PDFBox family
        "pdf-password-protector", "pdf-compressor",
        // markup and signing
        "pdf-editor", "pdf-annotator", "pdf-signer",
        // page-level editing
        "pdf-crop", "pdf-reorder-pages",
        // image family
        "image-compressor", "image-resizer", "image-cropper", "image-rotator",
        "image-blur", "sharpen-image", "watermark-image", "exif-remover",
        "image-metadata-viewer", "batch-image-converter", "thumbnail-creator",
        "background-remover", "favicon-generator",
        // TrimTool is one screen shared by these three; the other media tools
        // have their own composables and have not been wired.
        "video-trimmer", "audio-trimmer", "mp4-to-mp3", "audio-transcriber",
        // audio and gif encoders
        "wav-converter", "audio-compressor", "volume-booster",
        "gif-maker", "video-to-gif",
        // more PDFBox tools
        "pdf-unlocker", "pdf-image-extractor",
        // image format converters share one screen
        "png-to-webp", "jpg-to-webp", "jpg-to-png", "png-to-jpg",
        "webp-to-png", "heic-to-jpg",
        // multi-file tools seed their list with the handed file
        "merge-pdf"
    )

    /**
     * Blueprint sections 14 and 17 - the file is the job description.
     *
     * Given what the user picked, the tools that can actually act on it,
     * grouped the same way search results are so the list stays readable.
     */
    fun forFile(mime: String, fileName: String): List<Pair<Group, List<Tool>>> {
        val families = fileFamilies(mime, fileName)
        if (families.isEmpty()) return emptyList()
        val hits = ToolRegistry.all
            .filter { inputFamilies(it).any { f -> f in families } }
            .sortedByDescending {
                (if (it.id in ACCEPTS_HANDOFF) 8 else 0) +
                    (if (it.popular) 2 else 0) + (if (it.offline) 1 else 0)
            }
        if (hits.isEmpty()) return emptyList()
        val buckets = hits.groupBy { group(it) }
        return Group.entries.mapNotNull { g ->
            buckets[g]?.takeIf { it.isNotEmpty() }?.let { g to it }
        }
    }

    /**
     * The three steps this specific tool actually involves.
     *
     * Every tool screen used to show the same "Choose, Adjust, Save" strip,
     * which says nothing on a QR generator and is wrong on a text tool. Both
     * halves are already known: [inputFamilies] says what goes in, and [group]
     * says what the tool does to it.
     */
    fun steps(t: Tool): Triple<String, String, String> {
        // File-matching is happy to treat a Text tool as accepting a .txt, but
        // for steps that is wrong: Word Counter takes typed text in a field, not
        // a picked file. Only trust a file input when the id gives evidence for
        // one, or the category is inherently file-based.
        val idEvidence = "-to-" in t.id ||
            t.id.substringBefore('-') in setOf("pdf", "image", "exif", "video", "audio")
        val fileCategory = t.category in setOf(
            ToolCategory.PDF, ToolCategory.IMAGE, ToolCategory.VIDEO,
            ToolCategory.AUDIO, ToolCategory.CONVERTER, ToolCategory.PRIVACY
        )
        val input = if (idEvidence || fileCategory) inputFamilies(t).firstOrNull() else null

        val verb = when (group(t)) {
            Group.CONVERT -> "Convert"
            Group.OPTIMIZE -> "Compress"
            Group.EDIT -> "Edit"
            Group.ORGANIZE -> "Arrange"
            Group.CREATE -> "Generate"
            Group.OTHER -> "Process"
        }
        if (input == null) {
            // Nothing is picked and nothing is written to storage.
            return Triple("Type", verb, "Copy")
        }
        val noun = when (input) {
            "pdf" -> "a PDF"
            "image" -> "an image"
            "video" -> "a video"
            "audio" -> "audio"
            "text" -> "a file"
            "word" -> "a Word file"
            "excel" -> "a spreadsheet"
            "powerpoint" -> "a deck"
            else -> "a file"
        }
        return Triple("Choose $noun", verb, "Save")
    }

    /** Which intent bucket a tool belongs to. Derived from id and name. */
    fun group(t: Tool): Group {
        val id = t.id
        val n = t.name.lowercase()
        return when {
            "compress" in id || "optimiz" in id || "reduce" in n || "booster" in id -> Group.OPTIMIZE
            "-to-" in id || "converter" in n || "convert" in n ||
                "extractor" in id -> Group.CONVERT
            listOf("merge", "split", "rotat", "page", "reorder", "organiz")
                .any { it in id } -> Group.ORGANIZE
            listOf("edit", "annotat", "sign", "crop", "resiz", "watermark", "redact",
                "remov", "trim", "blur", "filter", "unlock", "protect")
                .any { it in id } -> Group.EDIT
            t.category == ToolCategory.GENERATOR || "generator" in id || "maker" in id ||
                "creator" in id -> Group.CREATE
            else -> Group.OTHER
        }
    }

    /**
     * Blueprint section 11 - organise a category's depth into sections.
     *
     * Returns an empty list when the category should stay a flat grid: below
     * [SECTION_MIN] tools it already reads fine, and fewer than three sections
     * means the headers add structure without adding clarity.
     *
     * Popular leads when there are at least two popular tools. Sections of one
     * fall into Advanced rather than standing as a header over a single card.
     */
    fun curate(tools: List<Tool>): List<Pair<String, List<Tool>>> {
        if (tools.size < SECTION_MIN) return emptyList()

        val popular = tools.filter { it.popular }
        val leadWithPopular = popular.size >= 2
        val rest = if (leadWithPopular) tools.filterNot { it.popular } else tools
        val buckets = rest.groupBy { group(it) }

        val out = mutableListOf<Pair<String, List<Tool>>>()
        val leftovers = mutableListOf<Tool>()
        if (leadWithPopular) out += "Popular" to popular

        Group.entries.forEach { g ->
            val bucket = buckets[g].orEmpty()
            when {
                bucket.isEmpty() -> Unit
                g == Group.OTHER || bucket.size < 2 -> leftovers += bucket
                else -> out += g.label to bucket
            }
        }
        if (leftovers.isNotEmpty()) out += "Advanced" to leftovers

        return if (out.size >= 3) out else emptyList()
    }

    /** Below this a category reads fine as one grid. */
    private const val SECTION_MIN = 12

    /** Results split into intent groups, in a stable display order. */
    fun grouped(q: String): List<Pair<Group, List<Tool>>> {
        val hits = search(q)
        if (hits.isEmpty()) return emptyList()
        val buckets = hits.groupBy { group(it) }
        return Group.entries.mapNotNull { g ->
            buckets[g]?.takeIf { it.isNotEmpty() }?.let { g to it }
        }
    }
}
