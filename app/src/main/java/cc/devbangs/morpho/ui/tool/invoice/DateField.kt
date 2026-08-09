package cc.devbangs.morpho.ui.tool.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.devbangs.morpho.core.Shape
import cc.devbangs.morpho.ui.icon.MorphoIcon
import cc.devbangs.morpho.ui.theme.*
import cc.devbangs.morpho.ui.tool.kit.FieldLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: MutableState<String>, accent: Color) {
    var open by remember { mutableStateOf(false) }
    Column {
        FieldLabel(label)
        Row(
            Modifier.fillMaxWidth().clip(Shape.field).background(PaperSunk)
                .border(1.dp, PaperLine, Shape.field)
                .clickable { open = true }.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value.value.ifEmpty { "Select date" },
                color = if (value.value.isEmpty()) InkFaint else Ink, fontSize = 15.sp,
                modifier = Modifier.weight(1f))
            MorphoIcon("clock", tint = accent, size = 18.dp)
        }
    }
    if (open) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        value.value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                    }
                    open = false
                }) { Text("OK", color = accent) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel", color = InkSoft) } }
        ) { DatePicker(state = state) }
    }
}
