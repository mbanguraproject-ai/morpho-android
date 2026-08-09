package cc.devbangs.morpho.data

// AUTO-GENERATED: all 82 Morpho tools.
object ToolRegistry {
    val all: List<Tool> = listOf(
        Tool("pdf-unlocker", "PDF Unlocker", "Remove PDF password and restrictions", ToolCategory.PDF, "pdf-unlocker", offline = false, popular = false),
        Tool("pdf-signer", "PDF Signer", "Add signatures to PDF documents", ToolCategory.PDF, "pdf-signer", offline = false, popular = false),
        Tool("pdf-editor", "PDF Editor", "Add text and annotations to PDFs", ToolCategory.PDF, "pdf-editor", offline = false, popular = false),
        Tool("pdf-to-word", "PDF to Word Converter", "Convert PDF files to editable Word documents", ToolCategory.PDF, "pdf-to-word", offline = false, popular = true),
        Tool("word-to-pdf", "Word to PDF Converter", "Convert Word documents to PDF format", ToolCategory.PDF, "word-to-pdf", offline = false, popular = true),
        Tool("pdf-compressor", "PDF Compressor", "Reduce PDF file size instantly", ToolCategory.PDF, "pdf-compressor", offline = false, popular = true),
        Tool("merge-pdf", "Merge PDF", "Combine multiple PDFs into one", ToolCategory.PDF, "merge-pdf", offline = true, popular = true),
        Tool("pdf-splitter", "PDF Splitter", "Split PDFs into separate pages", ToolCategory.PDF, "pdf-splitter", offline = true, popular = false),
        Tool("pdf-to-jpg", "PDF to JPG Converter", "Convert PDF pages to JPG images", ToolCategory.PDF, "pdf-to-jpg", offline = true, popular = false),
        Tool("jpg-to-pdf", "JPG to PDF Converter", "Convert JPG images to PDF", ToolCategory.PDF, "jpg-to-pdf", offline = true, popular = false),
        Tool("pdf-page-rotator", "PDF Page Rotator", "Rotate PDF pages to any angle", ToolCategory.PDF, "pdf-page-rotator", offline = true, popular = false),
        Tool("image-compressor", "Image Compressor", "Compress images without losing quality", ToolCategory.IMAGE, "image-compressor", offline = true, popular = true),
        Tool("image-resizer", "Image Resizer", "Resize images to exact dimensions", ToolCategory.IMAGE, "image-resizer", offline = true, popular = true),
        Tool("background-remover", "Background Remover", "Remove image backgrounds with AI", ToolCategory.IMAGE, "background-remover", offline = false, popular = true),
        Tool("image-cropper", "Image Cropper", "Crop images to any size or ratio", ToolCategory.IMAGE, "image-cropper", offline = true, popular = false),
        Tool("jpg-to-png", "JPG to PNG Converter", "Convert JPG images to PNG format", ToolCategory.CONVERTER, "jpg-to-png", offline = true, popular = false),
        Tool("png-to-jpg", "PNG to JPG Converter", "Convert PNG images to JPG format", ToolCategory.CONVERTER, "png-to-jpg", offline = true, popular = false),
        Tool("webp-to-png", "WebP to PNG Converter", "Convert WebP images to PNG", ToolCategory.CONVERTER, "webp-to-png", offline = true, popular = false),
        Tool("svg-to-png", "SVG to PNG Converter", "Convert SVG vectors to PNG images", ToolCategory.CONVERTER, "svg-to-png", offline = false, popular = false),
        Tool("heic-to-jpg", "HEIC to JPG Converter", "Convert iPhone HEIC photos to JPG", ToolCategory.CONVERTER, "heic-to-jpg", offline = true, popular = false),
        Tool("image-to-pdf", "Image to PDF Converter", "Convert images to PDF documents", ToolCategory.CONVERTER, "image-to-pdf", offline = true, popular = false),
        Tool("mp4-to-mp3", "MP4 to MP3 Converter", "Extract audio from video as MP3", ToolCategory.VIDEO, "mp4-to-mp3", offline = false, popular = false),
        Tool("csv-to-json", "CSV to JSON Converter", "Convert CSV data to JSON format", ToolCategory.CONVERTER, "csv-to-json", offline = true, popular = false),
        Tool("json-to-csv", "JSON to CSV Converter", "Convert JSON data to CSV format", ToolCategory.CONVERTER, "json-to-csv", offline = true, popular = false),
        Tool("video-to-gif", "Video to GIF Converter", "Convert video clips to animated GIFs", ToolCategory.VIDEO, "video-to-gif", offline = false, popular = false),
        Tool("video-compressor", "Video Compressor", "Compress videos to smaller file size", ToolCategory.VIDEO, "video-compressor", offline = false, popular = false),
        Tool("screen-recorder", "Screen Recorder", "Record your screen", ToolCategory.VIDEO, "screen-recorder", offline = false, popular = false),
        Tool("video-trimmer", "Video Trimmer", "Trim videos to exact length", ToolCategory.VIDEO, "video-trimmer", offline = false, popular = false),
        Tool("mp3-converter", "MP3 Converter", "Convert any audio file to MP3", ToolCategory.AUDIO, "mp3-converter", offline = false, popular = false),
        Tool("wav-converter", "WAV Converter", "Convert audio files to WAV format", ToolCategory.AUDIO, "wav-converter", offline = false, popular = false),
        Tool("audio-trimmer", "Audio Trimmer", "Trim and cut audio files", ToolCategory.AUDIO, "audio-trimmer", offline = false, popular = false),
        Tool("voice-recorder", "Voice Recorder", "Record audio from your microphone", ToolCategory.AUDIO, "voice-recorder", offline = false, popular = false),
        Tool("audio-joiner", "Audio Joiner", "Combine multiple audio files into one", ToolCategory.AUDIO, "audio-joiner", offline = false, popular = false),
        Tool("screenshot-to-text", "Screenshot to Text (OCR)", "Extract text from screenshots using OCR", ToolCategory.TEXT, "screenshot-to-text", offline = false, popular = false),
        Tool("image-to-text", "Image to Text Extractor", "Extract text from any image", ToolCategory.TEXT, "image-to-text", offline = false, popular = false),
        Tool("word-counter", "Word Counter", "Count words, characters, and more", ToolCategory.TEXT, "word-counter", offline = true, popular = false),
        Tool("qr-code-generator", "QR Code Generator", "Create custom QR codes instantly", ToolCategory.GENERATOR, "qr-code-generator", offline = true, popular = true),
        Tool("invoice-generator", "Invoice Generator", "Create professional invoices for free", ToolCategory.GENERATOR, "invoice-generator", offline = true, popular = false),
        Tool("resume-builder", "Resume Builder", "Build professional resumes in minutes", ToolCategory.GENERATOR, "resume-builder", offline = true, popular = false),
        Tool("password-generator", "Password Generator", "Generate strong, secure passwords", ToolCategory.GENERATOR, "password-generator", offline = true, popular = true),
        Tool("youtube-thumbnail-downloader", "YouTube Thumbnail Downloader", "Download YouTube thumbnails in HD", ToolCategory.GENERATOR, "youtube-thumbnail-downloader", offline = true, popular = false),
        Tool("barcode-generator", "Barcode Generator", "Generate barcodes in multiple formats", ToolCategory.GENERATOR, "barcode-generator", offline = true, popular = false),
        Tool("json-formatter", "JSON Formatter & Validator", "Format and validate JSON data", ToolCategory.DEVELOPER, "json-formatter", offline = true, popular = true),
        Tool("color-picker", "Color Picker & Palette Generator", "Pick colors and generate palettes", ToolCategory.DEVELOPER, "color-picker", offline = true, popular = false),
        Tool("favicon-generator", "Favicon Generator", "Generate website favicons from images", ToolCategory.DEVELOPER, "favicon-generator", offline = true, popular = false),
        Tool("text-diff-checker", "Text Diff Checker", "Compare two texts and find differences", ToolCategory.DEVELOPER, "text-diff-checker", offline = true, popular = false),
        Tool("base64-encoder", "Base64 Encoder/Decoder", "Encode and decode Base64 strings", ToolCategory.DEVELOPER, "base64-encoder", offline = true, popular = false),
        Tool("ai-text-rewriter", "AI Text Rewriter", "Rewrite text with AI intelligence", ToolCategory.AI, "ai-text-rewriter", offline = false, popular = false),
        Tool("ai-image-upscaler", "AI Image Upscaler", "Enhance image quality with AI", ToolCategory.AI, "ai-image-upscaler", offline = false, popular = false),
        Tool("grammar-checker", "Grammar Checker", "Fix grammar and spelling errors", ToolCategory.AI, "grammar-checker", offline = false, popular = false),
        Tool("essay-writer", "AI Essay Writer", "Generate essays on any topic with AI", ToolCategory.AI, "essay-writer", offline = false, popular = false),
        Tool("paragraph-generator", "AI Paragraph Generator", "Generate paragraphs on any topic with AI", ToolCategory.AI, "paragraph-generator", offline = false, popular = false),
        Tool("uuid-generator", "UUID Generator", "Generate v4 UUIDs in bulk", ToolCategory.DEVELOPER, "uuid-generator", offline = true, popular = false),
        Tool("jwt-decoder", "JWT Decoder", "Decode and inspect JWT tokens", ToolCategory.DEVELOPER, "jwt-decoder", offline = true, popular = false),
        Tool("url-encoder", "URL Encoder/Decoder", "Encode and decode URLs instantly", ToolCategory.DEVELOPER, "url-encoder", offline = true, popular = false),
        Tool("hash-generator", "Hash Generator", "Generate SHA hashes from text", ToolCategory.DEVELOPER, "hash-generator", offline = true, popular = false),
        Tool("color-converter", "Color Code Converter", "Convert between HEX, RGB, HSL, HSV", ToolCategory.DEVELOPER, "color-converter", offline = true, popular = false),
        Tool("case-converter", "Case Converter", "Convert text between 11 case formats", ToolCategory.TEXT, "case-converter", offline = true, popular = false),
        Tool("slug-generator", "Slug Generator", "Turn titles into clean URL slugs", ToolCategory.TEXT, "slug-generator", offline = true, popular = false),
        Tool("lorem-ipsum-generator", "Lorem Ipsum Generator", "Generate Lorem Ipsum placeholder text", ToolCategory.TEXT, "lorem-ipsum-generator", offline = true, popular = false),
        Tool("fake-data-generator", "Fake Data Generator", "Generate test data: names, emails, addresses", ToolCategory.GENERATOR, "fake-data-generator", offline = true, popular = false),
        Tool("character-counter", "Character Counter", "Count characters with live platform limits", ToolCategory.TEXT, "character-counter", offline = true, popular = false),
        Tool("image-blur", "Image Blur", "Blur photos or hide sensitive details", ToolCategory.IMAGE, "image-blur", offline = true, popular = false),
        Tool("watermark-image", "Watermark Image", "Add text watermarks to protect images", ToolCategory.IMAGE, "watermark-image", offline = true, popular = false),
        Tool("image-rotator", "Image Rotator", "Rotate photos by any angle", ToolCategory.IMAGE, "image-rotator", offline = true, popular = false),
        Tool("exif-remover", "EXIF Remover", "Strip GPS and metadata from photos", ToolCategory.IMAGE, "exif-remover", offline = true, popular = false),
        Tool("gif-maker", "GIF Maker", "Make animated GIFs from images", ToolCategory.IMAGE, "gif-maker", offline = false, popular = false),
        Tool("meme-generator", "Meme Generator", "Make memes with top and bottom text", ToolCategory.IMAGE, "meme-generator", offline = false, popular = false),
        Tool("image-metadata-viewer", "Image Metadata Viewer", "Inspect EXIF and hidden photo data", ToolCategory.IMAGE, "image-metadata-viewer", offline = true, popular = false),
        Tool("batch-image-converter", "Batch Image Converter", "Convert many images at once, any format", ToolCategory.IMAGE, "batch-image-converter", offline = true, popular = false),
        Tool("thumbnail-creator", "Thumbnail Creator", "Generate thumbnails in multiple sizes", ToolCategory.IMAGE, "thumbnail-creator", offline = true, popular = false),
        Tool("sharpen-image", "Sharpen Image", "Sharpen blurry photos with adjustable intensity", ToolCategory.IMAGE, "sharpen-image", offline = true, popular = false),
        Tool("pdf-page-numbering", "PDF Page Numbering", "Add page numbers to PDF documents", ToolCategory.PDF, "pdf-page-numbering", offline = true, popular = false),
        Tool("pdf-watermark", "PDF Watermark", "Add text watermarks across PDF pages", ToolCategory.PDF, "pdf-watermark", offline = true, popular = false),
        Tool("pdf-page-extractor", "PDF Page Extractor", "Extract specific pages into a new PDF", ToolCategory.PDF, "pdf-page-extractor", offline = true, popular = false),
        Tool("pdf-text-extractor", "PDF Text Extractor", "Extract plain text from any PDF", ToolCategory.PDF, "pdf-text-extractor", offline = false, popular = false),
        Tool("pdf-header-footer", "PDF Header & Footer", "Add headers and footers to PDF pages", ToolCategory.PDF, "pdf-header-footer", offline = false, popular = false),
        Tool("pdf-crop", "PDF Crop", "Trim margins off PDF pages", ToolCategory.PDF, "pdf-crop", offline = true, popular = false),
        Tool("pdf-bates-numbering", "PDF Bates Numbering", "Bates stamp PDFs for legal discovery", ToolCategory.PDF, "pdf-bates-numbering", offline = false, popular = false),
        Tool("pdf-annotator", "PDF Annotator", "Highlight, note, and draw on PDFs", ToolCategory.PDF, "pdf-annotator", offline = false, popular = false),
        Tool("pdf-password-protector", "PDF Password Protector", "Add password protection to PDFs", ToolCategory.PDF, "pdf-password-protector", offline = false, popular = false),
        Tool("pdf-reorder-pages", "PDF Reorder Pages", "Drag-and-drop to reorder PDF pages", ToolCategory.PDF, "pdf-reorder-pages", offline = true, popular = false),
    )

    val byCategory: Map<ToolCategory, List<Tool>> = all.groupBy { it.category }

    fun byId(id: String): Tool? = all.firstOrNull { it.id == id }

    val popular: List<Tool> = all.filter { it.popular }

    fun search(q: String): List<Tool> {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return emptyList()
        return all.filter {
            it.name.lowercase().contains(s) || it.short.lowercase().contains(s) || it.category.label.lowercase().contains(s)
        }
    }
}
