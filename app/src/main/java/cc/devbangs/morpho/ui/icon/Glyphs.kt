package cc.devbangs.morpho.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Custom glyph atlas. Every mark is drawn on a 24x24 grid.
 * Dispatch by tool id; unknown ids fall back to a category mark.
 */
internal fun DrawScope.drawGlyph(key: String, c: Color, u: Float, s: Stroke) {
    when (key) {
        // ---------- CATEGORY MARKS ----------
        "cat-pdf" -> { gRect(6f,3f,12f,18f,c,u,s,2.5f); gLine(9f,9f,15f,9f,c,u,s); gLine(9f,12f,15f,12f,c,u,s); gLine(9f,15f,13f,15f,c,u,s) }
        "cat-image" -> { gRect(3f,5f,18f,14f,c,u,s,2.5f); gCircle(8.5f,10f,1.6f,c,u,s); gPath(c,u,s){ m(4f,17f,u); l(9f,12f,u); l(13f,15f,u); l(16f,11f,u); l(20f,17f,u) } }
        "cat-converter" -> { gPath(c,u,s){ m(4f,8f,u); l(16f,8f,u) }; gPath(c,u,s){ m(13f,5f,u); l(16f,8f,u); l(13f,11f,u) }; gPath(c,u,s){ m(20f,16f,u); l(8f,16f,u) }; gPath(c,u,s){ m(11f,13f,u); l(8f,16f,u); l(11f,19f,u) } }
        "cat-video" -> { gRect(3f,5f,14f,14f,c,u,s,2.5f); gPath(c,u,s){ m(17f,9f,u); l(21f,6.5f,u); l(21f,17.5f,u); l(17f,15f,u); close() } }
        "cat-audio" -> { gPath(c,u,s){ m(5f,10f,u); l(5f,14f,u) }; gPath(c,u,s){ m(9f,7f,u); l(9f,17f,u) }; gPath(c,u,s){ m(13f,4f,u); l(13f,20f,u) }; gPath(c,u,s){ m(17f,8f,u); l(17f,16f,u) }; gPath(c,u,s){ m(21f,10.5f,u); l(21f,13.5f,u) } }
        "cat-text" -> { gLine(5f,7f,19f,7f,c,u,s); gLine(5f,12f,19f,12f,c,u,s); gLine(5f,17f,13f,17f,c,u,s) }
        "cat-generator" -> { gPath(c,u,s){ m(12f,3f,u); l(13.8f,9.2f,u); l(20f,10f,u); l(15.2f,13.8f,u); l(17f,20f,u); l(12f,16.3f,u); l(7f,20f,u); l(8.8f,13.8f,u); l(4f,10f,u); l(10.2f,9.2f,u); close() } }
        "cat-developer" -> { gPath(c,u,s){ m(8f,8f,u); l(4f,12f,u); l(8f,16f,u) }; gPath(c,u,s){ m(16f,8f,u); l(20f,12f,u); l(16f,16f,u) }; gLine(13.5f,6f,10.5f,18f,c,u,s) }
        "cat-ai" -> { gCircle(12f,12f,4.5f,c,u,s); gDot(12f,12f,1.1f,c,u); gLine(12f,3f,12f,6.5f,c,u,s); gLine(12f,17.5f,12f,21f,c,u,s); gLine(3f,12f,6.5f,12f,c,u,s); gLine(17.5f,12f,21f,12f,c,u,s) }

        // ---------- UI CHROME ----------
        "chevron-left" -> { gPath(c,u,s){ m(15f,5f,u); l(8f,12f,u); l(15f,19f,u) } }
        "chevron-right" -> { gPath(c,u,s){ m(9f,5f,u); l(16f,12f,u); l(9f,19f,u) } }
        "close" -> { gLine(6f,6f,18f,18f,c,u,s); gLine(18f,6f,6f,18f,c,u,s) }
        "check" -> { gPath(c,u,s){ m(5f,13f,u); l(10f,18f,u); l(19f,7f,u) } }
        "copy" -> { gRect(8f,8f,12f,12f,c,u,s,2.5f); gPath(c,u,s){ m(16f,8f,u); l(16f,5f,u); l(4f,5f,u); l(4f,17f,u); l(8f,17f,u) } }
        "download" -> { gLine(12f,4f,12f,15f,c,u,s); gPath(c,u,s){ m(7f,11f,u); l(12f,16f,u); l(17f,11f,u) }; gPath(c,u,s){ m(5f,19f,u); l(19f,19f,u) } }
        "upload" -> { gLine(12f,20f,12f,9f,c,u,s); gPath(c,u,s){ m(7f,13f,u); l(12f,8f,u); l(17f,13f,u) }; gPath(c,u,s){ m(5f,5f,u); l(19f,5f,u) } }
        "share" -> { gCircle(6f,12f,2.4f,c,u,s); gCircle(18f,6f,2.4f,c,u,s); gCircle(18f,18f,2.4f,c,u,s); gLine(8f,11f,16f,7f,c,u,s); gLine(8f,13f,16f,17f,c,u,s) }
        "plus" -> { gLine(12f,5f,12f,19f,c,u,s); gLine(5f,12f,19f,12f,c,u,s) }
        "minus" -> { gLine(5f,12f,19f,12f,c,u,s) }
        "refresh" -> { gPath(c,u,s){ m(19f,8f,u); l(19f,4f,u) }; gPath(c,u,s){ m(19f,8f,u); l(15f,8f,u) }; gCircle(12f,12f,7f,c,u,s) }
        "image-add" -> { gRect(3f,5f,14f,14f,c,u,s,2.5f); gCircle(7.5f,9.5f,1.4f,c,u,s); gPath(c,u,s){ m(4f,16f,u); l(8f,12f,u); l(12f,15f,u) }; gLine(18f,6f,18f,12f,c,u,s); gLine(15f,9f,21f,9f,c,u,s) }
        "file-add" -> { gPath(c,u,s){ m(6f,3f,u); l(14f,3f,u); l(18f,7f,u); l(18f,21f,u); l(6f,21f,u); close() }; gLine(12f,11f,12f,17f,c,u,s); gLine(9f,14f,15f,14f,c,u,s) }
        "settings" -> { gCircle(12f,12f,3f,c,u,s); gCircle(12f,12f,7.5f,c,u,s) }
        "clock" -> { gCircle(12f,12f,8f,c,u,s); gPath(c,u,s){ m(12f,8f,u); l(12f,12f,u); l(15f,14f,u) } }
        "info" -> { gCircle(12f,12f,8f,c,u,s); gDot(12f,8.5f,0.9f,c,u); gLine(12f,11.5f,12f,16f,c,u,s) }

        // ---------- BOTTOM-BAR TABS ----------
        "tab-home" -> { gPath(c,u,s){ m(4f,11f,u); l(12f,4f,u); l(20f,11f,u) }; gPath(c,u,s){ m(6f,10f,u); l(6f,20f,u); l(18f,20f,u); l(18f,10f,u) }; gPath(c,u,s){ m(10f,20f,u); l(10f,14f,u); l(14f,14f,u); l(14f,20f,u) } }
        "tab-grid" -> { gRect(4f,4f,7f,7f,c,u,s,2f); gRect(13f,4f,7f,7f,c,u,s,2f); gRect(4f,13f,7f,7f,c,u,s,2f); gRect(13f,13f,7f,7f,c,u,s,2f) }
        "tab-search" -> { gCircle(11f,11f,6f,c,u,s); gLine(15.5f,15.5f,20f,20f,c,u,s) }

        // ---------- PDF ----------
        "merge-pdf" -> { gRect(4f,6f,10f,13f,c,u,s); gRect(10f,4f,10f,13f,c,u,s) }
        "pdf-splitter" -> { gRect(6f,3f,12f,18f,c,u,s); gLine(12f,3f,12f,21f,c,u,s) }
        "pdf-to-jpg" -> { gRect(4f,4f,9f,16f,c,u,s); gRect(12f,8f,8f,12f,c,u,s,2f); gCircle(15f,12f,1.2f,c,u,s) }
        "jpg-to-pdf" -> { gRect(4f,8f,8f,12f,c,u,s,2f); gRect(12f,4f,8f,16f,c,u,s); gLine(14f,9f,18f,9f,c,u,s) }
        "pdf-page-rotator","pdf-reorder-pages" -> { gRect(6f,5f,12f,14f,c,u,s); gPath(c,u,s){ m(9f,3.5f,u); l(6.5f,5.5f,u); l(9f,7.5f,u) }; gPath(c,u,s){ m(15f,20.5f,u); l(17.5f,18.5f,u); l(15f,16.5f,u) } }
        "pdf-page-numbering","pdf-bates-numbering" -> { gRect(6f,3f,12f,18f,c,u,s); gLine(9f,8f,15f,8f,c,u,s); gLine(9f,11f,15f,11f,c,u,s); gDot(9f,17f,1f,c,u); gLine(11f,17f,15f,17f,c,u,s) }
        "pdf-watermark","pdf-header-footer" -> { gRect(6f,3f,12f,18f,c,u,s); gLine(8.5f,15f,15.5f,9f,c,u,s); gLine(9f,10f,14f,10f,c,u,s) }
        "pdf-page-extractor" -> { gRect(5f,3f,10f,15f,c,u,s); gRect(11f,8f,8f,12f,c,u,s); gLine(15f,11f,15f,17f,c,u,s); gLine(12f,14f,18f,14f,c,u,s) }
        "pdf-text-extractor" -> { gRect(6f,3f,12f,18f,c,u,s); gLine(9f,9f,15f,9f,c,u,s); gLine(9f,12f,15f,12f,c,u,s); gLine(9f,15f,12f,15f,c,u,s) }
        "pdf-crop" -> { gLine(8f,4f,8f,18f,c,u,s); gLine(4f,8f,18f,8f,c,u,s); gLine(16f,6f,16f,20f,c,u,s); gLine(6f,16f,20f,16f,c,u,s) }
        "pdf-unlocker","pdf-password-protector" -> { gRect(6f,11f,12f,9f,c,u,s,2f); gPath(c,u,s){ m(8.5f,11f,u); l(8.5f,8f,u); }; gPath(c,u,s){ m(8.5f,8f,u); l(15.5f,8f,u); l(15.5f,11f,u) }; gDot(12f,15.5f,1.1f,c,u) }
        "pdf-signer" -> { gRect(5f,3f,14f,18f,c,u,s); gPath(c,u,s){ m(8f,15f,u); l(10f,13f,u); l(11.5f,16f,u); l(14f,11f,u); l(16f,15f,u) } }
        "pdf-editor","pdf-annotator" -> { gRect(5f,3f,11f,18f,c,u,s); gPath(c,u,s){ m(14f,9f,u); l(20f,15f,u); l(17f,18f,u); l(11f,12f,u); close() } }
        "pdf-compressor" -> { gRect(6f,3f,12f,18f,c,u,s); gPath(c,u,s){ m(9f,10f,u); l(12f,13f,u); l(15f,10f,u) }; gPath(c,u,s){ m(9f,16f,u); l(12f,13f,u); l(15f,16f,u) } }
        "pdf-to-word" -> { gRect(4f,4f,9f,16f,c,u,s); gPath(c,u,s){ m(13f,10f,u); l(14.5f,17f,u); l(16f,12f,u); l(17.5f,17f,u); l(19f,10f,u) } }
        "word-to-pdf" -> { gPath(c,u,s){ m(4f,7f,u); l(5.5f,14f,u); l(7f,9f,u); l(8.5f,14f,u); l(10f,7f,u) }; gRect(12f,4f,8f,16f,c,u,s) }

        // ---------- IMAGE ----------
        "image-compressor" -> { gRect(4f,4f,16f,16f,c,u,s,2.5f); gPath(c,u,s){ m(9f,9f,u); l(12f,12f,u); l(9f,15f,u) }; gPath(c,u,s){ m(15f,9f,u); l(12f,12f,u); l(15f,15f,u) } }
        "image-resizer","thumbnail-creator" -> { gRect(4f,4f,16f,16f,c,u,s,2f); gPath(c,u,s){ m(8f,8f,u); l(8f,11f,u) }; gPath(c,u,s){ m(8f,8f,u); l(11f,8f,u) }; gPath(c,u,s){ m(16f,16f,u); l(16f,13f,u) }; gPath(c,u,s){ m(16f,16f,u); l(13f,16f,u) } }
        "image-cropper" -> { gLine(8f,3f,8f,17f,c,u,s); gLine(3f,8f,17f,8f,c,u,s); gLine(16f,7f,16f,21f,c,u,s); gLine(7f,16f,21f,16f,c,u,s) }
        "image-rotator" -> { gRect(6f,6f,12f,12f,c,u,s,2f); gPath(c,u,s){ m(6f,4f,u); l(4f,6f,u); l(6f,8f,u) }; gPath(c,u,s){ m(4f,6f,u); l(11f,6f,u) } }
        "image-blur" -> { gCircle(12f,12f,8f,c,u,s); gDot(9f,10f,0.8f,c,u); gDot(14f,9f,0.8f,c,u); gDot(11f,14f,0.8f,c,u); gDot(15f,14f,0.8f,c,u); gDot(12f,11.5f,0.8f,c,u) }
        "sharpen-image" -> { gPath(c,u,s){ m(12f,3f,u); l(15f,12f,u); l(12f,21f,u); l(9f,12f,u); close() }; gLine(6f,12f,18f,12f,c,u,s) }
        "watermark-image" -> { gRect(4f,5f,16f,14f,c,u,s,2f); gPath(c,u,s){ m(8f,15f,u); l(11f,9f,u); l(14f,15f,u) }; gLine(9f,13f,13f,13f,c,u,s) }
        "exif-remover","image-metadata-viewer" -> { gRect(4f,4f,16f,16f,c,u,s,2f); gCircle(12f,12f,5f,c,u,s); gLine(9f,15f,15f,9f,c,u,s) }
        "batch-image-converter" -> { gRect(4f,7f,12f,12f,c,u,s,2f); gRect(8f,3f,12f,12f,c,u,s,2f) }
        "background-remover" -> { gCircle(11f,9f,4f,c,u,s); gPath(c,u,s){ m(5f,20f,u); l(5f,17f,u); l(17f,17f,u); l(17f,20f,u) }; gLine(15f,4f,21f,10f,c,u,s); gLine(21f,4f,15f,10f,c,u,s) }
        "gif-maker" -> { gRect(4f,5f,16f,14f,c,u,s,2f); gPath(c,u,s){ m(11f,9f,u); l(11f,15f,u); l(15f,12f,u); close() } }
        "meme-generator" -> { gRect(4f,4f,16f,16f,c,u,s,3f); gLine(7f,7f,17f,7f,c,u,s); gLine(7f,17f,17f,17f,c,u,s); gDot(9f,12f,0.9f,c,u); gDot(15f,12f,0.9f,c,u) }

        // ---------- CONVERTER ----------
        "jpg-to-png","png-to-jpg","webp-to-png","svg-to-png","heic-to-jpg" -> { gRect(3f,7f,8f,10f,c,u,s,2f); gRect(13f,7f,8f,10f,c,u,s,2f); gPath(c,u,s){ m(11.5f,10f,u); l(12.5f,12f,u); l(11.5f,14f,u) } }
        "image-to-pdf" -> { gRect(3f,7f,8f,10f,c,u,s,2f); gRect(13f,5f,8f,14f,c,u,s); gLine(15f,10f,19f,10f,c,u,s) }
        "csv-to-json" -> { gRect(3f,5f,8f,14f,c,u,s,1.5f); gLine(3f,10f,11f,10f,c,u,s); gLine(3f,14f,11f,14f,c,u,s); gPath(c,u,s){ m(17f,6f,u); l(19f,6f,u); l(19f,18f,u); l(17f,18f,u) }; gPath(c,u,s){ m(15f,6f,u); l(13f,6f,u); l(13f,18f,u); l(15f,18f,u) } }
        "json-to-csv" -> { gPath(c,u,s){ m(9f,6f,u); l(11f,6f,u); l(11f,18f,u); l(9f,18f,u) }; gPath(c,u,s){ m(7f,6f,u); l(5f,6f,u); l(5f,18f,u); l(7f,18f,u) }; gRect(14f,5f,7f,14f,c,u,s,1.5f); gLine(14f,10f,21f,10f,c,u,s) }

        // ---------- VIDEO ----------
        "mp4-to-mp3" -> { gRect(3f,6f,10f,12f,c,u,s,2f); gPath(c,u,s){ m(6f,9f,u); l(6f,15f,u); l(10f,12f,u); close() }; gPath(c,u,s){ m(16f,17f,u); l(16f,10f,u); l(20f,9f,u); l(20f,16f,u) }; gCircle(15f,17f,1.4f,c,u,s); gCircle(19f,16f,1.4f,c,u,s) }
        "video-to-gif" -> { gRect(3f,6f,10f,12f,c,u,s,2f); gPath(c,u,s){ m(6f,9f,u); l(6f,15f,u); l(9.5f,12f,u); close() }; gLine(15f,9f,20f,9f,c,u,s); gLine(15f,12f,20f,12f,c,u,s); gLine(15f,15f,18f,15f,c,u,s) }
        "video-compressor" -> { gRect(3f,6f,18f,12f,c,u,s,2f); gPath(c,u,s){ m(9f,9f,u); l(12f,12f,u); l(9f,15f,u) }; gPath(c,u,s){ m(15f,9f,u); l(12f,12f,u); l(15f,15f,u) } }
        "screen-recorder" -> { gRect(3f,5f,18f,12f,c,u,s,2f); gDot(12f,11f,2f,c,u); gLine(9f,20f,15f,20f,c,u,s); gLine(12f,17f,12f,20f,c,u,s) }
        "video-trimmer" -> { gCircle(7f,7f,2.4f,c,u,s); gCircle(7f,17f,2.4f,c,u,s); gLine(9f,8f,20f,15f,c,u,s); gLine(9f,16f,20f,9f,c,u,s) }

        // ---------- AUDIO ----------
        "mp3-converter","wav-converter" -> { gPath(c,u,s){ m(4f,10f,u); l(4f,14f,u) }; gPath(c,u,s){ m(8f,7f,u); l(8f,17f,u) }; gPath(c,u,s){ m(12f,5f,u); l(12f,19f,u) }; gPath(c,u,s){ m(16f,8f,u); l(16f,16f,u) }; gPath(c,u,s){ m(20f,10f,u); l(20f,14f,u) } }
        "audio-trimmer" -> { gCircle(7f,7f,2.2f,c,u,s); gCircle(7f,17f,2.2f,c,u,s); gLine(9f,8f,20f,15f,c,u,s); gLine(9f,16f,20f,9f,c,u,s) }
        "voice-recorder" -> { gRect(9f,3f,6f,11f,c,u,s,3f); gPath(c,u,s){ m(6f,11f,u); l(6f,13f,u) }; gPath(c,u,s){ m(18f,11f,u); l(18f,13f,u) }; gPath(c,u,s){ m(6f,13f,u); l(7f,15f,u) }; gLine(12f,18f,12f,21f,c,u,s); gLine(9f,21f,15f,21f,c,u,s); gCircle(12f,13f,6f,c,u,s) }
        "audio-joiner" -> { gCircle(7f,12f,3f,c,u,s); gCircle(17f,12f,3f,c,u,s); gLine(10f,12f,14f,12f,c,u,s) }

        // ---------- TEXT ----------
        "word-counter","character-counter" -> { gLine(5f,7f,19f,7f,c,u,s); gLine(5f,12f,15f,12f,c,u,s); gLine(5f,17f,19f,17f,c,u,s); gDot(18f,12f,1.2f,c,u) }
        "case-converter" -> { gPath(c,u,s){ m(4f,17f,u); l(7.5f,7f,u); l(11f,17f,u) }; gLine(5.2f,13.5f,9.8f,13.5f,c,u,s); gPath(c,u,s){ m(14f,17f,u); l(16.5f,10f,u); l(19f,17f,u) }; gLine(14.8f,14.8f,18.2f,14.8f,c,u,s) }
        "slug-generator" -> { gPath(c,u,s){ m(10f,14f,u); l(8f,16f,u); l(6f,16f,u); l(4f,14f,u); l(4f,12f,u); l(6f,10f,u); l(8f,10f,u); l(9f,11f,u) }; gPath(c,u,s){ m(14f,10f,u); l(16f,8f,u); l(18f,8f,u); l(20f,10f,u); l(20f,12f,u); l(18f,14f,u); l(16f,14f,u); l(15f,13f,u) }; gLine(9f,15f,15f,9f,c,u,s) }
        "lorem-ipsum-generator" -> { gLine(5f,6f,19f,6f,c,u,s); gLine(5f,10f,17f,10f,c,u,s); gLine(5f,14f,19f,14f,c,u,s); gLine(5f,18f,13f,18f,c,u,s) }
        "screenshot-to-text","image-to-text" -> { gPath(c,u,s){ m(4f,7f,u); l(4f,5f,u); l(6f,5f,u) }; gPath(c,u,s){ m(18f,5f,u); l(20f,5f,u); l(20f,7f,u) }; gPath(c,u,s){ m(20f,17f,u); l(20f,19f,u); l(18f,19f,u) }; gPath(c,u,s){ m(6f,19f,u); l(4f,19f,u); l(4f,17f,u) }; gLine(8f,10f,16f,10f,c,u,s); gLine(8f,14f,13f,14f,c,u,s) }

        // ---------- GENERATOR ----------
        "qr-code-generator" -> { gRect(4f,4f,6f,6f,c,u,s,1f); gRect(14f,4f,6f,6f,c,u,s,1f); gRect(4f,14f,6f,6f,c,u,s,1f); gDot(16f,16f,1f,c,u); gDot(19f,19f,1f,c,u); gDot(16f,19f,1f,c,u); gDot(19f,16f,1f,c,u) }
        "barcode-generator" -> { gLine(4f,6f,4f,18f,c,u,s); gLine(7f,6f,7f,18f,c,u,s); gLine(9f,6f,9f,18f,c,u,s); gLine(12f,6f,12f,18f,c,u,s); gLine(15f,6f,15f,18f,c,u,s); gLine(17f,6f,17f,18f,c,u,s); gLine(20f,6f,20f,18f,c,u,s) }
        "password-generator" -> { gRect(4f,10f,16f,10f,c,u,s,2f); gPath(c,u,s){ m(7f,10f,u); l(7f,7f,u) }; gPath(c,u,s){ m(7f,7f,u); l(15f,7f,u); l(15f,10f,u) }; gDot(9f,15f,1f,c,u); gDot(12f,15f,1f,c,u); gDot(15f,15f,1f,c,u) }
        "invoice-generator" -> { gPath(c,u,s){ m(6f,3f,u); l(18f,3f,u); l(18f,21f,u); l(15f,19f,u); l(12f,21f,u); l(9f,19f,u); l(6f,21f,u); close() }; gLine(9f,8f,15f,8f,c,u,s); gLine(9f,12f,15f,12f,c,u,s) }
        "resume-builder" -> { gRect(5f,3f,14f,18f,c,u,s,2f); gCircle(12f,9f,2.2f,c,u,s); gPath(c,u,s){ m(8f,16f,u); l(9f,13.5f,u); l(15f,13.5f,u); l(16f,16f,u) } }
        "youtube-thumbnail-downloader" -> { gRect(3f,6f,18f,12f,c,u,s,3f); gPath(c,u,s){ m(10f,9f,u); l(10f,15f,u); l(15f,12f,u); close() } }
        "fake-data-generator" -> { gPath(c,u,s){ m(5f,6f,u); l(5f,18f,u) }; gPath(c,u,s){ m(19f,6f,u); l(19f,18f,u) }; drawGlyphDb(c,u,s) }

        // ---------- DEVELOPER ----------
        "json-formatter" -> { gPath(c,u,s){ m(9f,4f,u); l(7f,4f,u); l(7f,11f,u); l(5f,12f,u); l(7f,13f,u); l(7f,20f,u); l(9f,20f,u) }; gPath(c,u,s){ m(15f,4f,u); l(17f,4f,u); l(17f,11f,u); l(19f,12f,u); l(17f,13f,u); l(17f,20f,u); l(15f,20f,u) } }
        "base64-encoder" -> { gLine(6f,4f,6f,20f,c,u,s); gLine(18f,4f,18f,20f,c,u,s); gLine(6f,4f,9f,4f,c,u,s); gLine(6f,20f,9f,20f,c,u,s); gLine(15f,4f,18f,4f,c,u,s); gLine(15f,20f,18f,20f,c,u,s); gLine(9f,12f,15f,12f,c,u,s) }
        "uuid-generator" -> { gRect(4f,9f,16f,6f,c,u,s,3f); gDot(8f,12f,1f,c,u); gDot(12f,12f,1f,c,u); gDot(16f,12f,1f,c,u) }
        "jwt-decoder" -> { gCircle(8f,12f,4f,c,u,s); gLine(12f,12f,20f,12f,c,u,s); gLine(16f,12f,16f,16f,c,u,s); gLine(20f,12f,20f,15f,c,u,s); gDot(8f,12f,1f,c,u) }
        "url-encoder" -> { gPath(c,u,s){ m(10f,14f,u); l(7f,17f,u); l(4f,14f,u); l(7f,11f,u); l(9f,13f,u) }; gPath(c,u,s){ m(14f,10f,u); l(17f,7f,u); l(20f,10f,u); l(17f,13f,u); l(15f,11f,u) }; gLine(9f,15f,15f,9f,c,u,s) }
        "hash-generator" -> { gLine(9f,4f,7f,20f,c,u,s); gLine(17f,4f,15f,20f,c,u,s); gLine(4f,9f,20f,9f,c,u,s); gLine(4f,15f,20f,15f,c,u,s) }
        "color-converter","color-picker" -> { gCircle(12f,12f,8f,c,u,s); gPath(c,u,s){ m(12f,4f,u); l(12f,12f,u); l(19f,15f,u) }; gDot(12f,12f,1.2f,c,u) }
        "favicon-generator" -> { gCircle(12f,12f,8f,c,u,s); gPath(c,u,s){ m(4f,12f,u); l(20f,12f,u) }; gPath(c,u,s){ m(12f,4f,u); l(12f,20f,u) }; gPath(c,u,s){ m(6.5f,6.5f,u); l(17.5f,17.5f,u) } }
        "text-diff-checker" -> { gRect(3f,5f,7f,14f,c,u,s,1.5f); gRect(14f,5f,7f,14f,c,u,s,1.5f); gLine(5f,9f,8f,9f,c,u,s); gLine(16f,9f,19f,9f,c,u,s); gLine(5f,13f,8f,13f,c,u,s); gDot(11.5f,12f,1f,c,u) }

        // ---------- AI ----------
        "ai-text-rewriter","essay-writer","paragraph-generator","grammar-checker" -> { gLine(5f,8f,15f,8f,c,u,s); gLine(5f,13f,12f,13f,c,u,s); gPath(c,u,s){ m(16f,11f,u); l(18f,9f,u); l(20f,11f,u); l(14f,17f,u); l(12f,17f,u); l(12f,15f,u); close() } }
        "ai-image-upscaler" -> { gRect(4f,4f,16f,16f,c,u,s,2f); gPath(c,u,s){ m(9f,15f,u); l(11f,12f,u); l(13f,14f,u); l(16f,9f,u) }; gPath(c,u,s){ m(17f,4f,u); l(17.6f,6.4f,u); l(20f,7f,u); l(17.6f,7.6f,u); l(17f,10f,u); l(16.4f,7.6f,u); l(14f,7f,u); l(16.4f,6.4f,u); close() } }

        else -> when {
            key.startsWith("pdf") || key.contains("pdf") -> drawGlyph("cat-pdf", c, u, s)
            else -> drawGlyph("cat-generator", c, u, s)
        }
    }
}

private fun DrawScope.drawGlyphDb(c: Color, u: Float, s: Stroke) {
    gPath(c,u,s){ m(8f,8f,u); l(16f,8f,u) }
    gCircle(12f,8f,4f,c,u,s)
    gLine(8f,8f,8f,16f,c,u,s)
    gLine(16f,8f,16f,16f,c,u,s)
    gPath(c,u,s){ m(8f,16f,u); l(16f,16f,u) }
    gCircle(12f,16f,4f,c,u,s)
}
