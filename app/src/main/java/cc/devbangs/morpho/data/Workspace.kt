package cc.devbangs.morpho.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * The user's personal workspace: the tools they chose to keep on Home, plus
 * the tools they most recently opened.
 *
 * Backed by SharedPreferences (same pattern as [cc.devbangs.morpho.notify.Prefs])
 * and mirrored into Compose state, so any screen reading [tools] or [recent]
 * recomposes the moment an entry is added, removed or reordered.
 *
 * Stored IDs are always validated against [ToolRegistry] on read. Tools get
 * renamed and recategorised between builds; a stale ID is dropped silently
 * rather than surfacing an empty card or crashing.
 *
 * Call [init] once from Application.onCreate before any screen reads state.
 */
object Workspace {
    private const val FILE = "morpho_workspace"
    private const val KEY_TOOLS = "workspace_tools"
    private const val KEY_RECENT = "recent_tools"
    private const val SEP = ","

    /** How many recently-opened tools to keep. */
    const val RECENT_LIMIT = 6

    private val _tools = mutableStateOf<List<String>>(emptyList())
    private val _recent = mutableStateOf<List<String>>(emptyList())

    private var appCtx: Context? = null

    /** Workspace tool IDs, in user order. */
    val toolIds: List<String> get() = _tools.value

    /** Recently opened tool IDs, most recent first. */
    val recentIds: List<String> get() = _recent.value

    /** Workspace entries resolved to [Tool], skipping any that no longer exist. */
    val tools: List<Tool> get() = _tools.value.mapNotNull { ToolRegistry.byId(it) }

    /** Recent entries resolved to [Tool], skipping any that no longer exist. */
    val recent: List<Tool> get() = _recent.value.mapNotNull { ToolRegistry.byId(it) }

    val isEmpty: Boolean get() = _tools.value.isEmpty()

    fun contains(id: String): Boolean = _tools.value.contains(id)

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        val p = prefs(ctx)
        _tools.value = decode(p.getString(KEY_TOOLS, "").orEmpty())
        _recent.value = decode(p.getString(KEY_RECENT, "").orEmpty())
    }

    /** Add a tool to the end of the workspace. No-op if unknown or already present. */
    fun add(id: String) {
        if (ToolRegistry.byId(id) == null || contains(id)) return
        _tools.value = _tools.value + id
        persist()
    }

    /** Remove a tool from the workspace. No-op if absent. */
    fun remove(id: String) {
        if (!contains(id)) return
        _tools.value = _tools.value - id
        persist()
    }

    /** Add if absent, remove if present. Returns true if the tool is now in the workspace. */
    fun toggle(id: String): Boolean {
        if (contains(id)) { remove(id); return false }
        add(id)
        return contains(id)
    }

    /** Reorder: move the entry at [from] so it sits at index [to]. */
    fun move(from: Int, to: Int) {
        val current = _tools.value
        if (from == to || from !in current.indices || to !in current.indices) return
        val next = current.toMutableList()
        next.add(to, next.removeAt(from))
        _tools.value = next
        persist()
    }

    /** Replace the whole workspace, e.g. when onboarding seeds it. */
    fun setAll(ids: List<String>) {
        _tools.value = ids.filter { ToolRegistry.byId(it) != null }.distinct()
        persist()
    }

    /** Record that a tool was opened. Moves it to the front of recent. */
    fun recordUse(id: String) {
        if (ToolRegistry.byId(id) == null) return
        _recent.value = (listOf(id) + (_recent.value - id)).take(RECENT_LIMIT)
        persist()
    }

    fun clearRecent() {
        if (_recent.value.isEmpty()) return
        _recent.value = emptyList()
        persist()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun persist() {
        val ctx = appCtx ?: return
        prefs(ctx).edit()
            .putString(KEY_TOOLS, _tools.value.joinToString(SEP))
            .putString(KEY_RECENT, _recent.value.joinToString(SEP))
            .apply()
    }

    /** Tool IDs are [a-z0-9-] only, so a comma is an unambiguous separator. */
    private fun decode(raw: String): List<String> =
        raw.split(SEP)
            .map { it.trim() }
            .filter { it.isNotEmpty() && ToolRegistry.byId(it) != null }
            .distinct()
}
