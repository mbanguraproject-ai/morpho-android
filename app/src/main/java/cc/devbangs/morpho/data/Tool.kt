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
    /**
     * Requires Plus.
     *
     * Kept apart from [offline] because they are different questions. The gate
     * used to read !offline, so "needs a server" and "is paid for" were the
     * same flag - which quietly paywalled three tools once they were rebuilt to
     * run on the device.
     */
    val plus: Boolean = false,
    val keywords: List<String> = emptyList()
)
