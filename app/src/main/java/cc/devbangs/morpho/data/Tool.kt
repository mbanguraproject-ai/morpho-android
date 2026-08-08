package cc.devbangs.morpho.data

/**
 * A tool entry. [offline] = fully functional on-device this build.
 * Stubbed tools still appear (branded "coming soon" shell) so the full
 * 82-tool catalog is represented.
 */
data class Tool(
    val id: String,
    val name: String,
    val short: String,
    val category: ToolCategory,
    val iconKey: String,
    val offline: Boolean,
    val popular: Boolean = false,
    val keywords: List<String> = emptyList()
)
