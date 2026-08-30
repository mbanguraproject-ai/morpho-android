package cc.devbangs.morpho.data

import cc.devbangs.morpho.workflow.WorkflowGraph

/**
 * Blueprint section 16 - recommendations reduce decisions.
 *
 * Every suggestion carries the reason it was made, because the section is
 * explicit that nothing should be labelled recommended without a credible
 * reason. Each signal below is real data the user generated: nothing is
 * suggested on a guess, and when there is no signal there is no section.
 *
 * Priority follows the section's ordering, narrowed to signals that exist:
 * related next step, then recent use, then category affinity, then popular.
 * "Continue current job" is handled in-tool by the next-step card.
 */
object Recommender {

    /**
     * [personal] is false when the only thing behind the suggestion is that the
     * tool is broadly popular. Callers use it to avoid calling something
     * recommended when nothing about this user informed it.
     */
    data class Suggestion(val tool: Tool, val reason: String, val personal: Boolean)

    /** Suggestions for tools the user does not already have. */
    fun forWorkspace(limit: Int = 6): List<Suggestion> {
        val owned = Workspace.toolIds.toSet()
        val recent = Workspace.recentIds
        val out = LinkedHashMap<String, Suggestion>()

        fun offer(tool: Tool?, reason: String, personal: Boolean = true) {
            if (tool == null) return
            if (tool.id in owned || tool.id in out) return
            if (out.size >= limit) return
            out[tool.id] = Suggestion(tool, reason, personal)
        }

        // 1. Related next step - tools that genuinely chain from what they use.
        (recent + Workspace.toolIds).distinct().forEach { id ->
            val from = ToolRegistry.byId(id) ?: return@forEach
            WorkflowGraph.nextSteps(id).forEach { step ->
                offer(ToolRegistry.byId(step.toolId), "Pairs with " + from.name)
            }
        }

        // 2. Recently opened but never added.
        recent.forEach { id ->
            offer(ToolRegistry.byId(id), "You opened this recently")
        }

        // 3. Category affinity - from onboarding picks and actual usage.
        affinity().forEach { category ->
            ToolRegistry.byCategory[category].orEmpty()
                .filter { it.popular }
                .forEach { offer(it, "You use " + category.label + " tools") }
        }

        // 4. Broadly popular, as a last resort.
        ToolRegistry.popular.forEach { offer(it, "Popular in Morpho", personal = false) }

        return out.values.toList()
    }

    /** True when at least one suggestion was informed by this user's own activity. */
    fun isPersonal(suggestions: List<Suggestion>): Boolean = suggestions.any { it.personal }

    /**
     * Categories the user has shown interest in, strongest first.
     * Onboarding picks and workspace choices weigh more than a single open.
     */
    private fun affinity(): List<ToolCategory> {
        val score = mutableMapOf<ToolCategory, Int>()
        fun bump(c: ToolCategory, by: Int) { score[c] = (score[c] ?: 0) + by }

        Workspace.needIds.mapNotNull { id ->
            ToolCategory.entries.firstOrNull { it.id == id }
        }.forEach { bump(it, 3) }

        Workspace.toolIds.mapNotNull { ToolRegistry.byId(it)?.category }
            .forEach { bump(it, 2) }

        Workspace.recentIds.mapNotNull { ToolRegistry.byId(it)?.category }
            .forEach { bump(it, 1) }

        return score.entries.sortedByDescending { it.value }.map { it.key }
    }
}
