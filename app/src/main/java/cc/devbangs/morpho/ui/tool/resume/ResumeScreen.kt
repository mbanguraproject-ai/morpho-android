package cc.devbangs.morpho.ui.tool.resume

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.ui.tool.kit.FieldLabel
import cc.devbangs.morpho.ui.tool.kit.savePdfToDownloads
import cc.devbangs.morpho.ui.tool.kit.sharePdf

@Composable
fun ResumeTool(accent: Color) {
    val s = remember { ResumeState() }
    var tab by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Tabs(tab, accent) { tab = it }
        when (tab) {
            0 -> DetailsTab(s, accent)
            1 -> StyleTab(s, accent)
            2 -> PreviewTab(s, accent)
        }
    }
}

@Composable
private fun Tabs(tab: Int, accent: Color, onTab: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk).padding(4.dp)) {
        listOf("Details","Style","Preview").forEachIndexed { i, label ->
            val sel = i == tab
            Box(Modifier.weight(1f).clip(Shape.chip)
                .background(if (sel) accent else Color.Transparent)
                .clickable { onTab(i) }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center) {
                Text(label, color = if (sel) Paper else InkSoft, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Field(label: String, v: MutableState<String>, hint: String, minLines: Int = 1) {
    Column {
        FieldLabel(label)
        Box(Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
            .border(1.dp, PaperLine, Shape.field).padding(13.dp)) {
            if (v.value.isEmpty()) Text(hint, color = InkFaint, fontSize = 15.sp)
            BasicTextField(v.value, { v.value = it },
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(Cobalt), minLines = minLines,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = InkFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun DetailsTab(s: ResumeState, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        SectionTitle("CONTACT")
        Field("FULL NAME", s.name, "Mohamed Bangura")
        Field("TITLE", s.title, "Android Developer")
        Field("CONTACT LINE", s.contact, "email · phone · Freetown · linkedin.com/in/…", minLines = 2)

        SectionTitle("PROFESSIONAL SUMMARY")
        Field("SUMMARY", s.summary, "2–3 sentences on your strengths and focus…", minLines = 3)

        SectionTitle("WORK EXPERIENCE")
        s.experience.forEachIndexed { i, e -> ExperienceCard(s, i, e) }
        AddButton("Add experience", accent) { s.experience.add(ExperienceEntry()) }

        SectionTitle("EDUCATION")
        s.education.forEachIndexed { i, e -> EducationCard(s, i, e) }
        AddButton("Add education", accent) { s.education.add(EducationEntry()) }

        SectionTitle("SKILLS")
        Field("SKILLS (comma-separated)", s.skills, "Kotlin, Jetpack Compose, Git, REST APIs…", minLines = 2)
    }
}

@Composable
private fun ExperienceCard(s: ResumeState, i: Int, e: ExperienceEntry) {
    Column(Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RowHeader("Experience ${i+1}", s.experience.size > 1) { s.experience.removeAt(i) }
        Inline(e.role, "Role / job title")
        Inline(e.company, "Company")
        Inline(e.dates, "Jan 2023 – Present")
        Inline(e.bullets, "One achievement per line", minLines = 3)
    }
}

@Composable
private fun EducationCard(s: ResumeState, i: Int, e: EducationEntry) {
    Column(Modifier.fillMaxWidth().clip(Shape.tile).background(PaperSunk).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RowHeader("Education ${i+1}", s.education.size > 1) { s.education.removeAt(i) }
        Inline(e.school, "School / university")
        Inline(e.detail, "Degree · field")
        Inline(e.dates, "2019 – 2023")
    }
}

@Composable
private fun RowHeader(label: String, canRemove: Boolean, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        if (canRemove) Box(Modifier.clip(Shape.pill).clickable(onClick = onRemove).padding(4.dp)) {
            MorphoIcon("close", tint = InkFaint, size = 16.dp)
        }
    }
}

@Composable
private fun Inline(v: MutableState<String>, hint: String, minLines: Int = 1) {
    Box(Modifier.fillMaxWidth().clip(Shape.chip).background(Paper).border(1.dp, PaperLine, Shape.chip)
        .padding(horizontal = 11.dp, vertical = 10.dp)) {
        if (v.value.isEmpty()) Text(hint, color = InkFaint, fontSize = 14.sp)
        BasicTextField(v.value, { v.value = it },
            textStyle = TextStyle(color = Ink, fontSize = 14.sp), cursorBrush = SolidColor(Cobalt),
            minLines = minLines, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddButton(label: String, accent: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(Shape.tile).background(accent.copy(alpha = 0.08f))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        MorphoIcon("plus", tint = accent, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StyleTab(s: ResumeState, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        SectionTitle("LAYOUT")
        ResumeLayout.entries.forEach { l ->
            val sel = s.layout.value == l
            Row(Modifier.fillMaxWidth().clip(Shape.tile)
                .background(if (sel) accent.copy(alpha = 0.10f) else PaperSunk)
                .border(if (sel) 2.dp else 1.dp, if (sel) accent else PaperLine, Shape.tile)
                .clickable { s.layout.value = l }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(l.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (sel) MorphoIcon("check", tint = accent, size = 20.dp)
            }
        }
        SectionTitle("ACCENT (ATS-safe)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RESUME_ACCENTS.forEach { a ->
                val sel = s.accent.value == a
                Box(Modifier.size(44.dp).clip(Shape.chip).background(Color(a.argb))
                    .border(if (sel) 3.dp else 0.dp, Paper, Shape.chip)
                    .clickable { s.accent.value = a }) {
                    if (sel) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MorphoIcon("check", tint = Paper, size = 20.dp)
                    }
                }
            }
        }
        Text("All layouts are single-column with standard headings — safe for applicant tracking systems.",
            color = InkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun PreviewTab(s: ResumeState, accent: Color) {
    val ctx = LocalContext.current
    val bmp = remember(
        s.layout.value, s.accent.value, s.name.value, s.title.value, s.contact.value,
        s.summary.value, s.experience.size, s.education.size, s.skills.value,
        s.experience.joinToString { it.role.value + it.bullets.value },
        s.education.joinToString { it.school.value }
    ) { renderResumeBitmap(s) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
        Box(Modifier.fillMaxWidth().clip(Shape.card).background(PaperSunk)
            .border(1.dp, PaperLine, Shape.card).padding(8.dp)) {
            Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent)
                    .clickable { savePdfToDownloads(ctx, renderResumePdf(s),
                        s.name.value.ifBlank { "resume" }.replace(" ", "_")) }
                    .padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Save PDF", color = Paper, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().clip(Shape.field).background(accent.copy(alpha = 0.10f))
                    .clickable { sharePdf(ctx, renderResumePdf(s),
                        s.name.value.ifBlank { "resume" }.replace(" ", "_")) }
                    .padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Share", color = accent, fontSize = 15.sp)
                }
            }
        }
    }
}
