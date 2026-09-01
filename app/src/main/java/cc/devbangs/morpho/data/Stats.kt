package cc.devbangs.morpho.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Blueprint sections 21, 46 and 47 - turn usage into a product loop.
 *
 * The docs refer to an existing Pack Stats system, but nothing of the sort is
 * in the app; that will be an external dashboard. This is the in-app half, and
 * it is deliberately the smallest thing that answers the questions those
 * sections ask: which tools get opened, which ones fail, which ones get kept,
 * and what people search for and do not find.
 *
 * Everything stays on the device. No network, no SDK, no identifiers. The
 * point is to tell the developer which of 132 tools are worth the next hour of
 * work, which does not require sending anything anywhere.
 */
object Stats {

    private const val FILE = "morpho_stats"
    private const val KEY_OPENS = "app_opens"
    private const val KEY_MISSES = "search_misses"
    private const val MISS_LIMIT = 40

    private const val P_LAUNCH = "l:"
    private const val P_OK = "s:"
    private const val P_FAIL = "f:"
    private const val P_ADD = "a:"

    private var appCtx: Context? = null
    private val _version = mutableStateOf(0)

    /**
     * The tool whose screen is open.
     *
     * Save helpers report an outcome but do not know which tool asked for it,
     * and threading a tool id through every save call site would touch far more
     * code than this is worth. Only one tool screen exists at a time.
     */
    var currentTool: String? = null

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        bump(KEY_OPENS)
    }

    /** Section 46: Launch. */
    fun launched(toolId: String) { currentTool = toolId; bump(P_LAUNCH + toolId) }

    /** Section 46: Success and Failure, for whichever tool is open. */
    fun recordOutcome(ok: Boolean) {
        val id = currentTool ?: return
        bump((if (ok) P_OK else P_FAIL) + id)
    }

    /** Section 46: Workspace addition. */
    fun addedToWorkspace(toolId: String) = bump(P_ADD + toolId)

    /**
     * Section 46: "Search with no result" - called out explicitly, and the most
     * directly useful signal here, since it names tools people expect Morpho to
     * have and it does not.
     */
    fun searchMissed(query: String) {
        val q = query.trim().lowercase()
        if (q.length < 3) return
        val p = prefs() ?: return
        val existing = p.getString(KEY_MISSES, "").orEmpty()
            .split('\n').filter { it.isNotBlank() }
        val next = (listOf(q) + existing.filterNot { it == q }).take(MISS_LIMIT)
        p.edit().putString(KEY_MISSES, next.joinToString("\n")).apply()
        _version.value++
    }

    data class ToolStat(
        val toolId: String,
        val launches: Int,
        val successes: Int,
        val failures: Int,
        val adds: Int
    ) {
        /** Null until something has actually finished, so 0/0 is not shown as failure. */
        val failureRate: Float?
            get() = if (successes + failures == 0) null
            else failures.toFloat() / (successes + failures)
    }

    val appOpens: Int get() { _version.value; return prefs()?.getInt(KEY_OPENS, 0) ?: 0 }

    val missedSearches: List<String>
        get() {
            _version.value
            return prefs()?.getString(KEY_MISSES, "").orEmpty()
                .split('\n').filter { it.isNotBlank() }
        }

    /** Every tool that has been touched at all, busiest first. */
    fun tools(): List<ToolStat> {
        _version.value
        val p = prefs() ?: return emptyList()
        val ids = mutableSetOf<String>()
        p.all.keys.forEach { k ->
            listOf(P_LAUNCH, P_OK, P_FAIL, P_ADD).forEach { prefix ->
                if (k.startsWith(prefix)) ids += k.removePrefix(prefix)
            }
        }
        return ids.mapNotNull { id ->
            if (ToolRegistry.byId(id) == null) return@mapNotNull null
            ToolStat(
                toolId = id,
                launches = p.getInt(P_LAUNCH + id, 0),
                successes = p.getInt(P_OK + id, 0),
                failures = p.getInt(P_FAIL + id, 0),
                adds = p.getInt(P_ADD + id, 0)
            )
        }.sortedByDescending { it.launches }
    }

    /** Section 46: tools that fail often enough to be worth looking at. */
    fun troubled(): List<ToolStat> =
        tools().filter { (it.failureRate ?: 0f) > 0.25f && it.failures >= 2 }
            .sortedByDescending { it.failures }

    fun clear() {
        prefs()?.edit()?.clear()?.apply()
        _version.value++
    }

    private fun prefs() = appCtx?.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun bump(key: String) {
        val p = prefs() ?: return
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
        _version.value++
    }
}
