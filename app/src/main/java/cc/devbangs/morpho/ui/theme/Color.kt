package cc.devbangs.morpho.ui.theme

import androidx.compose.ui.graphics.Color

// Ink & paper — light-first, high-contrast, no color slop
val Paper       = Color(0xFFFFFFFF) // system bars + base surface
val PaperSunk   = Color(0xFFF4F5F7) // recessed fields / rails
val PaperLine   = Color(0xFFE7E9EE) // hairline dividers
val Ink         = Color(0xFF0B0D12) // primary text
val InkSoft     = Color(0xFF5A6472) // secondary text
val InkFaint    = Color(0xFF9AA3B2) // tertiary / placeholder

// The single accent: deep cobalt
val Cobalt      = Color(0xFF1A46E5)
val CobaltPress = Color(0xFF1638BE)
val CobaltWash  = Color(0xFFEAEEFF) // tinted chip / selected bg
val CobaltEdge  = Color(0xFFC8D3FF) // tinted hairline

// Section 34 asks for semantic tokens and section 19 names a green success
// state, but green only existed as a category accent. Same value, given the
// role it was already meant to have.
val Success     = Color(0xFF15803D)
val SuccessWash = Color(0xFFE8F5ED)

// Category accents (muted, derived from cobalt family — used sparingly as 1px marks)
val CatPdf       = Color(0xFF1A46E5)
val CatImage     = Color(0xFF0E7C86)
val CatConverter = Color(0xFF6A4BD6)
val CatVideo     = Color(0xFFC2410C)
val CatAudio     = Color(0xFF9333AA)
val CatText      = Color(0xFF0F766E)
val CatGenerator = Color(0xFFB45309)
val CatDeveloper = Color(0xFF334155)
val CatAi        = Color(0xFF1A46E5)
val CatPrivacy   = Color(0xFF15803D)
