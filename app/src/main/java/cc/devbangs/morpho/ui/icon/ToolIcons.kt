package cc.devbangs.morpho.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowClockwise
import com.adamglin.phosphoricons.bold.ArrowRight
import com.adamglin.phosphoricons.bold.ArrowsClockwise
import com.adamglin.phosphoricons.bold.CircleNotch
import com.adamglin.phosphoricons.bold.ArrowsDownUp
import com.adamglin.phosphoricons.bold.ArrowsIn
import com.adamglin.phosphoricons.bold.Article
import com.adamglin.phosphoricons.bold.Barcode
import com.adamglin.phosphoricons.bold.BracketsCurly
import com.adamglin.phosphoricons.bold.Brain
import com.adamglin.phosphoricons.bold.CaretLeft
import com.adamglin.phosphoricons.bold.CaretRight
import com.adamglin.phosphoricons.bold.Check
import com.adamglin.phosphoricons.bold.Checks
import com.adamglin.phosphoricons.bold.Clock
import com.adamglin.phosphoricons.bold.Code
import com.adamglin.phosphoricons.bold.CodeSimple
import com.adamglin.phosphoricons.bold.Copy
import com.adamglin.phosphoricons.bold.Crown
import com.adamglin.phosphoricons.bold.Moon
import com.adamglin.phosphoricons.bold.Bell
import com.adamglin.phosphoricons.bold.Shield
import com.adamglin.phosphoricons.bold.FileText
import com.adamglin.phosphoricons.bold.Star
import com.adamglin.phosphoricons.bold.Crop
import com.adamglin.phosphoricons.bold.Database
import com.adamglin.phosphoricons.bold.DotsThreeVertical
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.Drop
import com.adamglin.phosphoricons.bold.Eraser
import com.adamglin.phosphoricons.bold.Eyedropper
import com.adamglin.phosphoricons.bold.Faders
import com.adamglin.phosphoricons.bold.FileArrowDown
import com.adamglin.phosphoricons.bold.FileAudio
import com.adamglin.phosphoricons.bold.FileCsv
import com.adamglin.phosphoricons.bold.FileDoc
import com.adamglin.phosphoricons.bold.FileImage
import com.adamglin.phosphoricons.bold.FilePdf
import com.adamglin.phosphoricons.bold.FilePlus
import com.adamglin.phosphoricons.bold.FileSvg
import com.adamglin.phosphoricons.bold.FileVideo
import com.adamglin.phosphoricons.bold.Fingerprint
import com.adamglin.phosphoricons.bold.FrameCorners
import com.adamglin.phosphoricons.bold.Gif
import com.adamglin.phosphoricons.bold.GitDiff
import com.adamglin.phosphoricons.bold.Hash
import com.adamglin.phosphoricons.bold.Highlighter
import com.adamglin.phosphoricons.bold.House
import com.adamglin.phosphoricons.bold.IdentificationCard
import com.adamglin.phosphoricons.bold.Image
import com.adamglin.phosphoricons.bold.ImageSquare
import com.adamglin.phosphoricons.bold.Images
import com.adamglin.phosphoricons.bold.Info
import com.adamglin.phosphoricons.bold.Key
import com.adamglin.phosphoricons.bold.Link
import com.adamglin.phosphoricons.bold.ListNumbers
import com.adamglin.phosphoricons.bold.Lock
import com.adamglin.phosphoricons.bold.LockOpen
import com.adamglin.phosphoricons.bold.MagnifyingGlass
import com.adamglin.phosphoricons.bold.Microphone
import com.adamglin.phosphoricons.bold.Minus
import com.adamglin.phosphoricons.bold.Monitor
import com.adamglin.phosphoricons.bold.MusicNotes
import com.adamglin.phosphoricons.bold.Palette
import com.adamglin.phosphoricons.bold.Paragraph
import com.adamglin.phosphoricons.bold.Password
import com.adamglin.phosphoricons.bold.PencilSimple
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.bold.QrCode
import com.adamglin.phosphoricons.bold.Receipt
import com.adamglin.phosphoricons.bold.Resize
import com.adamglin.phosphoricons.bold.Rows
import com.adamglin.phosphoricons.bold.Scan
import com.adamglin.phosphoricons.bold.Scissors
import com.adamglin.phosphoricons.bold.SelectionInverse
import com.adamglin.phosphoricons.bold.ShareNetwork
import com.adamglin.phosphoricons.bold.Signature
import com.adamglin.phosphoricons.bold.Smiley
import com.adamglin.phosphoricons.bold.Sparkle
import com.adamglin.phosphoricons.bold.SquaresFour
import com.adamglin.phosphoricons.bold.StackSimple
import com.adamglin.phosphoricons.bold.Star
import com.adamglin.phosphoricons.bold.Sun
import com.adamglin.phosphoricons.bold.TextAa
import com.adamglin.phosphoricons.bold.TextT
import com.adamglin.phosphoricons.bold.UploadSimple
import com.adamglin.phosphoricons.bold.VideoCamera
import com.adamglin.phosphoricons.bold.Waveform
import com.adamglin.phosphoricons.bold.X
import com.adamglin.phosphoricons.bold.YoutubeLogo

object ToolIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "cat-pdf" to PhosphorIcons.Bold.FilePdf,
        "cat-image" to PhosphorIcons.Bold.Image,
        "cat-converter" to PhosphorIcons.Bold.ArrowsClockwise,
        "cat-video" to PhosphorIcons.Bold.VideoCamera,
        "cat-audio" to PhosphorIcons.Bold.MusicNotes,
        "cat-text" to PhosphorIcons.Bold.TextT,
        "cat-generator" to PhosphorIcons.Bold.Sparkle,
        "cat-developer" to PhosphorIcons.Bold.Code,
        "cat-ai" to PhosphorIcons.Bold.Brain,
        "tab-home" to PhosphorIcons.Bold.House,
        "tab-grid" to PhosphorIcons.Bold.SquaresFour,
        "tab-search" to PhosphorIcons.Bold.MagnifyingGlass,
        "chevron-left" to PhosphorIcons.Bold.CaretLeft,
        "chevron-right" to PhosphorIcons.Bold.CaretRight,
        "close" to PhosphorIcons.Bold.X,
        "check" to PhosphorIcons.Bold.Check,
        "copy" to PhosphorIcons.Bold.Copy,
        "download" to PhosphorIcons.Bold.DownloadSimple,
        "upload" to PhosphorIcons.Bold.UploadSimple,
        "share" to PhosphorIcons.Bold.ShareNetwork,
        "plus" to PhosphorIcons.Bold.Plus,
        "minus" to PhosphorIcons.Bold.Minus,
        "refresh" to PhosphorIcons.Bold.ArrowsClockwise,
        "spinner" to PhosphorIcons.Bold.CircleNotch,
        "settings" to PhosphorIcons.Bold.Faders,
        "clock" to PhosphorIcons.Bold.Clock,
        "info" to PhosphorIcons.Bold.Info,
        "crown" to PhosphorIcons.Bold.Crown,
        "moon" to PhosphorIcons.Bold.Moon,
        "bell" to PhosphorIcons.Bold.Bell,
        "shield" to PhosphorIcons.Bold.Shield,
        "file-text" to PhosphorIcons.Bold.FileText,
        "star" to PhosphorIcons.Bold.Star,
        "file-add" to PhosphorIcons.Bold.FilePlus,
        "image-add" to PhosphorIcons.Bold.ImageSquare,
        "dots" to PhosphorIcons.Bold.DotsThreeVertical,
        "arrow-right" to PhosphorIcons.Bold.ArrowRight,
        "pdf-unlocker" to PhosphorIcons.Bold.LockOpen,
        "pdf-signer" to PhosphorIcons.Bold.Signature,
        "pdf-editor" to PhosphorIcons.Bold.PencilSimple,
        "pdf-to-word" to PhosphorIcons.Bold.FileDoc,
        "word-to-pdf" to PhosphorIcons.Bold.FilePdf,
        "pdf-compressor" to PhosphorIcons.Bold.ArrowsIn,
        "merge-pdf" to PhosphorIcons.Bold.StackSimple,
        "pdf-splitter" to PhosphorIcons.Bold.Scissors,
        "pdf-to-jpg" to PhosphorIcons.Bold.FileImage,
        "jpg-to-pdf" to PhosphorIcons.Bold.FilePdf,
        "pdf-page-rotator" to PhosphorIcons.Bold.ArrowClockwise,
        "pdf-page-numbering" to PhosphorIcons.Bold.ListNumbers,
        "pdf-watermark" to PhosphorIcons.Bold.Drop,
        "pdf-page-extractor" to PhosphorIcons.Bold.FileArrowDown,
        "pdf-text-extractor" to PhosphorIcons.Bold.TextAa,
        "pdf-header-footer" to PhosphorIcons.Bold.Rows,
        "pdf-crop" to PhosphorIcons.Bold.Crop,
        "pdf-bates-numbering" to PhosphorIcons.Bold.Hash,
        "pdf-annotator" to PhosphorIcons.Bold.Highlighter,
        "pdf-password-protector" to PhosphorIcons.Bold.Lock,
        "pdf-reorder-pages" to PhosphorIcons.Bold.ArrowsDownUp,
        "image-compressor" to PhosphorIcons.Bold.ArrowsIn,
        "image-resizer" to PhosphorIcons.Bold.Resize,
        "background-remover" to PhosphorIcons.Bold.SelectionInverse,
        "image-cropper" to PhosphorIcons.Bold.Crop,
        "image-blur" to PhosphorIcons.Bold.Drop,
        "sharpen-image" to PhosphorIcons.Bold.Sun,
        "watermark-image" to PhosphorIcons.Bold.Drop,
        "image-rotator" to PhosphorIcons.Bold.ArrowClockwise,
        "exif-remover" to PhosphorIcons.Bold.Eraser,
        "image-metadata-viewer" to PhosphorIcons.Bold.Info,
        "batch-image-converter" to PhosphorIcons.Bold.Images,
        "thumbnail-creator" to PhosphorIcons.Bold.FrameCorners,
        "gif-maker" to PhosphorIcons.Bold.Gif,
        "meme-generator" to PhosphorIcons.Bold.Smiley,
        "jpg-to-png" to PhosphorIcons.Bold.FileImage,
        "png-to-jpg" to PhosphorIcons.Bold.FileImage,
        "webp-to-png" to PhosphorIcons.Bold.FileImage,
        "svg-to-png" to PhosphorIcons.Bold.FileSvg,
        "heic-to-jpg" to PhosphorIcons.Bold.FileImage,
        "image-to-pdf" to PhosphorIcons.Bold.FilePdf,
        "csv-to-json" to PhosphorIcons.Bold.FileCsv,
        "json-to-csv" to PhosphorIcons.Bold.BracketsCurly,
        "mp4-to-mp3" to PhosphorIcons.Bold.FileVideo,
        "video-to-gif" to PhosphorIcons.Bold.Gif,
        "video-compressor" to PhosphorIcons.Bold.ArrowsIn,
        "video-trimmer" to PhosphorIcons.Bold.Scissors,
        "mp3-converter" to PhosphorIcons.Bold.FileAudio,
        "wav-converter" to PhosphorIcons.Bold.Waveform,
        "audio-trimmer" to PhosphorIcons.Bold.Scissors,
        "voice-recorder" to PhosphorIcons.Bold.Microphone,
        "audio-joiner" to PhosphorIcons.Bold.Link,
        "screenshot-to-text" to PhosphorIcons.Bold.Scan,
        "image-to-text" to PhosphorIcons.Bold.TextT,
        "word-counter" to PhosphorIcons.Bold.TextAa,
        "case-converter" to PhosphorIcons.Bold.TextAa,
        "slug-generator" to PhosphorIcons.Bold.Link,
        "lorem-ipsum-generator" to PhosphorIcons.Bold.Paragraph,
        "character-counter" to PhosphorIcons.Bold.TextT,
        "qr-code-generator" to PhosphorIcons.Bold.QrCode,
        "invoice-generator" to PhosphorIcons.Bold.Receipt,
        "resume-builder" to PhosphorIcons.Bold.IdentificationCard,
        "password-generator" to PhosphorIcons.Bold.Password,
        "youtube-thumbnail-downloader" to PhosphorIcons.Bold.YoutubeLogo,
        "barcode-generator" to PhosphorIcons.Bold.Barcode,
        "fake-data-generator" to PhosphorIcons.Bold.Database,
        "json-formatter" to PhosphorIcons.Bold.BracketsCurly,
        "color-picker" to PhosphorIcons.Bold.Palette,
        "favicon-generator" to PhosphorIcons.Bold.Star,
        "text-diff-checker" to PhosphorIcons.Bold.GitDiff,
        "base64-encoder" to PhosphorIcons.Bold.CodeSimple,
        "uuid-generator" to PhosphorIcons.Bold.Fingerprint,
        "jwt-decoder" to PhosphorIcons.Bold.Key,
        "url-encoder" to PhosphorIcons.Bold.Link,
        "hash-generator" to PhosphorIcons.Bold.Hash,
        "color-converter" to PhosphorIcons.Bold.Eyedropper,
        "ai-text-rewriter" to PhosphorIcons.Bold.Sparkle,
        "ai-image-upscaler" to PhosphorIcons.Bold.Sparkle,
        "grammar-checker" to PhosphorIcons.Bold.Checks,
        "essay-writer" to PhosphorIcons.Bold.Article,
        "paragraph-generator" to PhosphorIcons.Bold.Paragraph,
    )

    val fallback: ImageVector = PhosphorIcons.Bold.Sparkle
    fun of(key: String): ImageVector = map[key] ?: fallback
}
