package cc.devbangs.morpho.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val gutter = 18.dp   // screen horizontal padding
}

object Shape {
    val tile = RoundedCornerShape(18.dp)
    val card = RoundedCornerShape(20.dp)
    val chip = RoundedCornerShape(12.dp)
    val field = RoundedCornerShape(14.dp)
    val pill = RoundedCornerShape(100.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}
