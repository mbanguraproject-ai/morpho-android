package cc.devbangs.morpho.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.core.Space
import cc.devbangs.morpho.data.ToolRegistry
import cc.devbangs.morpho.ui.components.IconButtonMorpho
import cc.devbangs.morpho.ui.components.ToolRow
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val results = remember(query) {
        if (query.isBlank()) ToolRegistry.popular else ToolRegistry.search(query)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(Paper)) {
        // custom search top bar
        Column(Modifier.fillMaxWidth().background(Paper)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonMorpho("chevron-left", onBack)
                Spacer(Modifier.width(Space.xs))
                Row(
                    Modifier
                        .weight(1f)
                        .clip(Shape.field)
                        .background(PaperSunk)
                        .border(1.dp, PaperLine, Shape.field)
                        .padding(horizontal = Space.md, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MorphoIcon("tab-search", tint = InkFaint, size = 18.dp)
                    Spacer(Modifier.width(Space.sm))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty())
                            Text("Search tools…", color = InkFaint, fontSize = 15.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            cursorBrush = SolidColor(Cobalt),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }
                    if (query.isNotEmpty())
                        IconButtonMorpho("close", { query = "" }, tint = InkFaint)
                }
                Spacer(Modifier.width(Space.sm))
            }
        }

        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tools match “$query”.", color = InkSoft,
                    style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = Space.gutter),
                contentPadding = PaddingValues(
                    top = Space.sm,
                    bottom = contentPadding.calculateBottomPadding() + Space.xl
                )
            ) {
                if (query.isBlank()) {
                    item {
                        Text("Popular", style = MaterialTheme.typography.titleMedium,
                            color = InkSoft, modifier = Modifier.padding(vertical = Space.sm))
                    }
                }
                items(results, key = { it.id }) { t ->
                    ToolRow(t, onClick = { onOpenTool(t.id) })
                }
            }
        }
    }
}
