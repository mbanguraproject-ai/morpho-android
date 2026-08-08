package cc.devbangs.morpho.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cc.devbangs.morpho.data.ToolRegistry

@Composable
fun MorphoApp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Morpho File — ${ToolRegistry.all.size} tools")
    }
}
